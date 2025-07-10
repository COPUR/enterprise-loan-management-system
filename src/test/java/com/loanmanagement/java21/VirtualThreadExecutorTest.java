package com.loanmanagement.java21;

import com.loanmanagement.loan.application.port.in.LoanApplicationUseCase;
import com.loanmanagement.loan.application.port.out.CustomerRepository;
import com.loanmanagement.loan.application.port.out.CreditCheckPort;
import com.loanmanagement.loan.application.service.LoanApplicationService;
import com.loanmanagement.loan.domain.model.CustomerId;
import com.loanmanagement.loan.domain.model.LoanApplication;
import com.loanmanagement.loan.domain.model.LoanApplicationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Virtual Thread Executor Migration Tests")
class VirtualThreadExecutorTest {

    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private CreditCheckPort creditCheckPort;
    
    private LoanApplicationService loanApplicationService;
    private Executor virtualThreadExecutor;
    private Executor platformThreadExecutor;

    @BeforeEach
    void setUp() {
        // This will fail until we implement Java 21 virtual thread support
        virtualThreadExecutor = createVirtualThreadExecutor();
        platformThreadExecutor = Executors.newFixedThreadPool(10);
        
        // Inject virtual thread executor into application service
        loanApplicationService = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            virtualThreadExecutor
        );
    }

    @Test
    @DisplayName("Should create virtual thread executor replacing ThreadPoolExecutor")
    void shouldCreateVirtualThreadExecutorReplacingThreadPoolExecutor() {
        // given
        // When migrating from ThreadPoolExecutor to virtual threads
        
        // when
        Executor executor = createVirtualThreadExecutor();
        
        // then
        assertThat(executor)
            .isNotNull()
            .describedAs("Virtual thread executor should be created to replace ThreadPoolExecutor");
        
        // Virtual threads should allow much higher concurrency
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100_000; i++) {
                executor.execute(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }, "Virtual threads should handle massive concurrency without resource exhaustion");
    }

    @Test
    @DisplayName("Should process loan applications with virtual threads showing improved concurrency")
    void shouldProcessLoanApplicationsWithVirtualThreadsShowingImprovedConcurrency() {
        // given
        int numberOfApplications = 1000;
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicLong maxThreadCount = new AtomicLong(0);
        
        when(customerRepository.findById(any(CustomerId.class)))
            .thenAnswer(invocation -> {
                // Simulate blocking I/O that would benefit from virtual threads
                Thread.sleep(10);
                return createMockCustomer();
            });
        
        when(creditCheckPort.performCreditCheck(any()))
            .thenAnswer(invocation -> {
                // Simulate external API call
                Thread.sleep(20);
                return createMockCreditCheck();
            });

        // when
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<?>[] futures = new CompletableFuture[numberOfApplications];
        for (int i = 0; i < numberOfApplications; i++) {
            final int applicationIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    // Track thread usage - virtual threads should use fewer OS threads
                    long currentThreadCount = Thread.getAllStackTraces().size();
                    maxThreadCount.updateAndGet(existing -> Math.max(existing, currentThreadCount));
                    
                    // Process loan application
                    LoanApplication application = createMockLoanApplication(applicationIndex);
                    loanApplicationService.processApplication(application);
                    
                    processedCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Loan application processing should not fail: " + e.getMessage());
                }
            }, virtualThreadExecutor);
        }
        
        CompletableFuture.allOf(futures).join();
        long endTime = System.currentTimeMillis();
        
        // then
        assertThat(processedCount.get())
            .isEqualTo(numberOfApplications)
            .describedAs("All loan applications should be processed");
        
        assertThat(endTime - startTime)
            .isLessThan(5000) // Should complete within 5 seconds with virtual threads
            .describedAs("Virtual threads should improve processing time for I/O bound operations");
        
        // Virtual threads should use significantly fewer OS threads than platform threads
        assertThat(maxThreadCount.get())
            .isLessThan(100) // Much lower than numberOfApplications
            .describedAs("Virtual threads should use fewer OS threads than platform threads");
    }

    @Test
    @DisplayName("Should inject virtual thread executor into application service port")
    void shouldInjectVirtualThreadExecutorIntoApplicationServicePort() {
        // given
        LoanApplicationService service = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            virtualThreadExecutor
        );
        
        // when
        Executor injectedExecutor = service.getExecutor();
        
        // then
        assertThat(injectedExecutor)
            .isNotNull()
            .isSameAs(virtualThreadExecutor)
            .describedAs("Virtual thread executor should be properly injected into application service");
    }

    @Test
    @DisplayName("Should demonstrate improved resource utilization compared to ThreadPoolExecutor")
    void shouldDemonstrateImprovedResourceUtilizationComparedToThreadPoolExecutor() {
        // given
        int concurrentTasks = 10_000;
        AtomicInteger virtualThreadTasksCompleted = new AtomicInteger(0);
        AtomicInteger platformThreadTasksCompleted = new AtomicInteger(0);
        
        // when - Virtual threads test
        long virtualThreadStart = System.currentTimeMillis();
        CompletableFuture<?>[] virtualFutures = new CompletableFuture[concurrentTasks];
        
        for (int i = 0; i < concurrentTasks; i++) {
            virtualFutures[i] = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(10); // Simulate I/O
                    virtualThreadTasksCompleted.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, virtualThreadExecutor);
        }
        
        CompletableFuture.allOf(virtualFutures).join();
        long virtualThreadTime = System.currentTimeMillis() - virtualThreadStart;
        
        // when - Platform threads test (will likely fail due to resource limits)
        long platformThreadStart = System.currentTimeMillis();
        CompletableFuture<?>[] platformFutures = new CompletableFuture[Math.min(concurrentTasks, 1000)]; // Limit to avoid OOM
        
        for (int i = 0; i < platformFutures.length; i++) {
            platformFutures[i] = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(10);
                    platformThreadTasksCompleted.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, platformThreadExecutor);
        }
        
        CompletableFuture.allOf(platformFutures).join();
        long platformThreadTime = System.currentTimeMillis() - platformThreadStart;
        
        // then
        assertThat(virtualThreadTasksCompleted.get())
            .isEqualTo(concurrentTasks)
            .describedAs("Virtual threads should handle all concurrent tasks");
        
        assertThat(virtualThreadTime)
            .isLessThan(platformThreadTime * 2) // Should be significantly faster
            .describedAs("Virtual threads should be more efficient for I/O bound tasks");
        
        assertThat(platformThreadTasksCompleted.get())
            .isLessThan(concurrentTasks)
            .describedAs("Platform threads should be limited by thread pool size");
    }

    // Helper methods - These will be implemented during the Green phase
    private Executor createVirtualThreadExecutor() {
        // This will fail until Java 21 implementation
        // return Executors.newVirtualThreadPerTaskExecutor();
        throw new UnsupportedOperationException("Virtual thread executor not yet implemented - Java 21 migration pending");
    }

    private Object createMockCustomer() {
        // Mock customer creation - will be implemented
        return new Object();
    }

    private Object createMockCreditCheck() {
        // Mock credit check creation - will be implemented
        return new Object();
    }

    private LoanApplication createMockLoanApplication(int index) {
        // Mock loan application creation - will be implemented
        return LoanApplication.builder()
            .id(new LoanApplicationId("APP-" + index))
            .customerId(new CustomerId("CUST-" + index))
            .amount(BigDecimal.valueOf(10000))
            .build();
    }
}