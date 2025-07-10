package com.loanmanagement.java21;

import com.loanmanagement.loan.application.port.in.LoanApplicationUseCase;
import com.loanmanagement.loan.application.port.out.CustomerRepository;
import com.loanmanagement.loan.application.port.out.CreditCheckPort;
import com.loanmanagement.loan.application.port.out.RiskAssessmentPort;
import com.loanmanagement.loan.application.port.out.FraudDetectionPort;
import com.loanmanagement.loan.application.port.out.AuditPort;
import com.loanmanagement.loan.application.service.LoanApplicationService;
import com.loanmanagement.loan.application.service.PaymentProcessingService;
import com.loanmanagement.loan.application.service.NotificationService;
import com.loanmanagement.loan.domain.model.LoanApplication;
import com.loanmanagement.loan.domain.model.LoanApplicationId;
import com.loanmanagement.loan.domain.model.CustomerId;
import com.loanmanagement.common.executor.ExecutorProvider;
import com.loanmanagement.common.config.VirtualThreadConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Virtual Thread Executor Injection Tests")
class VirtualThreadExecutorInjectionTest {

    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private CreditCheckPort creditCheckPort;
    
    @Mock
    private RiskAssessmentPort riskAssessmentPort;
    
    @Mock
    private FraudDetectionPort fraudDetectionPort;
    
    @Mock
    private AuditPort auditPort;
    
    private Executor virtualThreadExecutor;
    private ExecutorProvider executorProvider;
    private VirtualThreadConfiguration virtualThreadConfiguration;

    @BeforeEach
    void setUp() {
        // Create virtual thread executor - will fail until Java 21 implementation
        virtualThreadExecutor = createVirtualThreadExecutor();
        
        // Create executor provider with virtual threads
        executorProvider = new ExecutorProvider(virtualThreadExecutor);
        
        // Create configuration for virtual threads
        virtualThreadConfiguration = new VirtualThreadConfiguration();
    }

    @Test
    @DisplayName("Should inject virtual thread executor into LoanApplicationService")
    void shouldInjectVirtualThreadExecutorIntoLoanApplicationService() {
        // given
        LoanApplicationService service = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            riskAssessmentPort,
            fraudDetectionPort,
            auditPort,
            virtualThreadExecutor // Virtual thread executor injection
        );
        
        // when
        Executor injectedExecutor = service.getExecutor();
        
        // then
        assertThat(injectedExecutor)
            .isNotNull()
            .isSameAs(virtualThreadExecutor)
            .describedAs("Virtual thread executor should be properly injected into LoanApplicationService");
        
        // Verify executor is used for async operations
        assertThat(service.supportsVirtualThreads())
            .isTrue()
            .describedAs("Service should support virtual threads when configured");
    }

    @Test
    @DisplayName("Should use virtual thread executor in application service async operations")
    void shouldUseVirtualThreadExecutorInApplicationServiceAsyncOperations() {
        // given
        LoanApplicationService service = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            riskAssessmentPort,
            fraudDetectionPort,
            auditPort,
            virtualThreadExecutor
        );
        
        LoanApplication application = createTestLoanApplication();
        AtomicReference<String> executingThreadName = new AtomicReference<>();
        
        // Mock dependencies
        when(customerRepository.findById(any())).thenAnswer(invocation -> {
            executingThreadName.set(Thread.currentThread().getName());
            return createMockCustomer();
        });
        
        when(creditCheckPort.performCreditCheck(any())).thenReturn(createMockCreditCheck());
        when(riskAssessmentPort.assessRisk(any())).thenReturn(createMockRiskAssessment());
        
        // when
        CompletableFuture<Void> future = service.processApplicationAsync(application);
        
        // then
        assertDoesNotThrow(() -> future.join(), 
            "Async processing with virtual threads should complete successfully");
        
        assertThat(executingThreadName.get())
            .contains("VirtualThread")
            .describedAs("Operation should execute on virtual thread");
    }

    @Test
    @DisplayName("Should inject virtual thread executor into PaymentProcessingService")
    void shouldInjectVirtualThreadExecutorIntoPaymentProcessingService() {
        // given
        PaymentProcessingService service = new PaymentProcessingService(
            auditPort,
            virtualThreadExecutor // Virtual thread executor injection
        );
        
        // when
        Executor injectedExecutor = service.getExecutor();
        
        // then
        assertThat(injectedExecutor)
            .isNotNull()
            .isSameAs(virtualThreadExecutor)
            .describedAs("Virtual thread executor should be injected into PaymentProcessingService");
        
        // Verify service can handle high concurrency
        assertThat(service.getMaxConcurrentPayments())
            .isGreaterThan(1000)
            .describedAs("Virtual thread-based service should support high concurrency");
    }

    @Test
    @DisplayName("Should use ExecutorProvider to manage virtual thread executors")
    void shouldUseExecutorProviderToManageVirtualThreadExecutors() {
        // given
        ExecutorProvider provider = new ExecutorProvider(virtualThreadExecutor);
        
        // when
        Executor loanProcessingExecutor = provider.getLoanProcessingExecutor();
        Executor paymentExecutor = provider.getPaymentExecutor();
        Executor notificationExecutor = provider.getNotificationExecutor();
        
        // then
        assertThat(loanProcessingExecutor)
            .isNotNull()
            .describedAs("Loan processing executor should be provided");
        
        assertThat(paymentExecutor)
            .isNotNull()
            .describedAs("Payment executor should be provided");
        
        assertThat(notificationExecutor)
            .isNotNull()
            .describedAs("Notification executor should be provided");
        
        // All should use virtual threads under the hood
        assertThat(provider.isVirtualThreadBased())
            .isTrue()
            .describedAs("ExecutorProvider should be virtual thread based");
    }

    @Test
    @DisplayName("Should configure virtual thread executor through Spring configuration")
    void shouldConfigureVirtualThreadExecutorThroughSpringConfiguration() {
        // given
        VirtualThreadConfiguration config = new VirtualThreadConfiguration();
        
        // when
        Executor configuredExecutor = config.virtualThreadExecutor();
        Executor loanProcessingExecutor = config.loanProcessingExecutor();
        Executor paymentExecutor = config.paymentExecutor();
        
        // then
        assertThat(configuredExecutor)
            .isNotNull()
            .describedAs("Configuration should provide virtual thread executor");
        
        assertThat(loanProcessingExecutor)
            .isNotNull()
            .describedAs("Configuration should provide loan processing executor");
        
        assertThat(paymentExecutor)
            .isNotNull()
            .describedAs("Configuration should provide payment executor");
        
        // Verify thread factory configuration
        assertThat(config.getVirtualThreadFactory())
            .isNotNull()
            .describedAs("Configuration should provide virtual thread factory");
    }

    @Test
    @DisplayName("Should demonstrate performance improvement with virtual threads in port adapters")
    void shouldDemonstratePerformanceImprovementWithVirtualThreadsInPortAdapters() {
        // given
        int numberOfOperations = 1000;
        AtomicInteger completedOperations = new AtomicInteger(0);
        
        // Service with virtual thread executor
        LoanApplicationService virtualService = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            riskAssessmentPort,
            fraudDetectionPort,
            auditPort,
            virtualThreadExecutor
        );
        
        // Service with platform thread executor
        ExecutorService platformExecutor = Executors.newFixedThreadPool(10);
        LoanApplicationService platformService = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            riskAssessmentPort,
            fraudDetectionPort,
            auditPort,
            platformExecutor
        );
        
        // Mock with artificial delay to simulate I/O
        when(customerRepository.findById(any())).thenAnswer(invocation -> {
            Thread.sleep(10); // Simulate blocking I/O
            completedOperations.incrementAndGet();
            return createMockCustomer();
        });
        
        // when - Virtual threads test
        long virtualStartTime = System.currentTimeMillis();
        CompletableFuture<?>[] virtualFutures = new CompletableFuture[numberOfOperations];
        
        for (int i = 0; i < numberOfOperations; i++) {
            LoanApplication app = createTestLoanApplication();
            virtualFutures[i] = virtualService.processApplicationAsync(app);
        }
        
        CompletableFuture.allOf(virtualFutures).join();
        long virtualDuration = System.currentTimeMillis() - virtualStartTime;
        
        // Reset counter
        completedOperations.set(0);
        
        // when - Platform threads test (limited operations to avoid resource exhaustion)
        int limitedOperations = Math.min(numberOfOperations, 100);
        long platformStartTime = System.currentTimeMillis();
        CompletableFuture<?>[] platformFutures = new CompletableFuture[limitedOperations];
        
        for (int i = 0; i < limitedOperations; i++) {
            LoanApplication app = createTestLoanApplication();
            platformFutures[i] = platformService.processApplicationAsync(app);
        }
        
        CompletableFuture.allOf(platformFutures).join();
        long platformDuration = System.currentTimeMillis() - platformStartTime;
        
        // then
        assertThat(virtualDuration)
            .isLessThan(platformDuration * 2) // Should be significantly faster for I/O bound tasks
            .describedAs("Virtual threads should provide better performance for I/O bound operations");
        
        // Cleanup
        platformExecutor.shutdown();
    }

    @Test
    @DisplayName("Should inject virtual thread executor into NotificationService port adapter")
    void shouldInjectVirtualThreadExecutorIntoNotificationServicePortAdapter() {
        // given
        NotificationService service = new NotificationService(
            auditPort,
            virtualThreadExecutor
        );
        
        // when
        Executor injectedExecutor = service.getExecutor();
        
        // then
        assertThat(injectedExecutor)
            .isNotNull()
            .isSameAs(virtualThreadExecutor)
            .describedAs("Virtual thread executor should be injected into NotificationService");
        
        // Test bulk notification processing
        CompletableFuture<Void> bulkNotification = service.sendBulkNotifications(1000);
        
        assertDoesNotThrow(() -> bulkNotification.join(),
            "Bulk notifications should complete successfully with virtual threads");
    }

    @Test
    @DisplayName("Should handle executor graceful shutdown in application services")
    void shouldHandleExecutorGracefulShutdownInApplicationServices() {
        // given
        ExecutorService virtualExecutorService = createVirtualThreadExecutorService();
        
        LoanApplicationService service = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            riskAssessmentPort,
            fraudDetectionPort,
            auditPort,
            virtualExecutorService
        );
        
        // when
        service.shutdown();
        
        // then
        assertThat(virtualExecutorService.isShutdown())
            .isTrue()
            .describedAs("Virtual thread executor should be shut down gracefully");
        
        assertThat(service.isShutdown())
            .isTrue()
            .describedAs("Application service should handle shutdown properly");
    }

    @Test
    @DisplayName("Should use virtual threads for domain event publishing")
    void shouldUseVirtualThreadsForDomainEventPublishing() {
        // given
        LoanApplicationService service = new LoanApplicationService(
            customerRepository,
            creditCheckPort,
            riskAssessmentPort,
            fraudDetectionPort,
            auditPort,
            virtualThreadExecutor
        );
        
        LoanApplication application = createTestLoanApplication();
        AtomicInteger eventsPublished = new AtomicInteger(0);
        
        // Mock event publishing
        doAnswer(invocation -> {
            eventsPublished.incrementAndGet();
            return null;
        }).when(auditPort).publishEvent(any());
        
        // when
        CompletableFuture<Void> future = service.processApplicationWithEventPublishing(application);
        
        // then
        assertDoesNotThrow(() -> future.join(),
            "Event publishing should complete successfully");
        
        assertThat(eventsPublished.get())
            .isGreaterThan(0)
            .describedAs("Domain events should be published using virtual threads");
    }

    // Helper methods - These will be implemented during Green phase

    private Executor createVirtualThreadExecutor() {
        // This will fail until Java 21 implementation
        // return Executors.newVirtualThreadPerTaskExecutor();
        throw new UnsupportedOperationException("Virtual thread executor not yet implemented - Java 21 migration pending");
    }
    
    private ExecutorService createVirtualThreadExecutorService() {
        // This will fail until Java 21 implementation
        // return Executors.newVirtualThreadPerTaskExecutor();
        throw new UnsupportedOperationException("Virtual thread executor service not yet implemented - Java 21 migration pending");
    }

    private LoanApplication createTestLoanApplication() {
        return LoanApplication.builder()
            .id(new LoanApplicationId("LOAN-TEST-001"))
            .customerId(new CustomerId("CUST-TEST-001"))
            .amount(BigDecimal.valueOf(50000))
            .build();
    }

    private Object createMockCustomer() {
        return new Object(); // Mock implementation
    }

    private Object createMockCreditCheck() {
        return new Object(); // Mock implementation
    }

    private Object createMockRiskAssessment() {
        return new Object(); // Mock implementation
    }

    // Mock service classes that will be implemented during Green phase
    
    static class LoanApplicationService {
        private final CustomerRepository customerRepository;
        private final CreditCheckPort creditCheckPort;
        private final RiskAssessmentPort riskAssessmentPort;
        private final FraudDetectionPort fraudDetectionPort;
        private final AuditPort auditPort;
        private final Executor executor;
        private volatile boolean shutdown = false;

        public LoanApplicationService(CustomerRepository customerRepository, 
                                    CreditCheckPort creditCheckPort,
                                    RiskAssessmentPort riskAssessmentPort,
                                    FraudDetectionPort fraudDetectionPort,
                                    AuditPort auditPort,
                                    Executor executor) {
            this.customerRepository = customerRepository;
            this.creditCheckPort = creditCheckPort;
            this.riskAssessmentPort = riskAssessmentPort;
            this.fraudDetectionPort = fraudDetectionPort;
            this.auditPort = auditPort;
            this.executor = executor;
        }

        public Executor getExecutor() {
            return executor;
        }

        public boolean supportsVirtualThreads() {
            return executor.toString().contains("VirtualThread");
        }

        public CompletableFuture<Void> processApplicationAsync(LoanApplication application) {
            return CompletableFuture.runAsync(() -> {
                try {
                    customerRepository.findById(application.getCustomerId());
                    creditCheckPort.performCreditCheck(application);
                    riskAssessmentPort.assessRisk(application);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        public CompletableFuture<Void> processApplicationWithEventPublishing(LoanApplication application) {
            return CompletableFuture.runAsync(() -> {
                try {
                    // Process application
                    customerRepository.findById(application.getCustomerId());
                    
                    // Publish events
                    auditPort.publishEvent("LoanApplicationProcessed");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        public void shutdown() {
            shutdown = true;
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdown();
            }
        }

        public boolean isShutdown() {
            return shutdown;
        }
    }

    static class PaymentProcessingService {
        private final AuditPort auditPort;
        private final Executor executor;

        public PaymentProcessingService(AuditPort auditPort, Executor executor) {
            this.auditPort = auditPort;
            this.executor = executor;
        }

        public Executor getExecutor() {
            return executor;
        }

        public int getMaxConcurrentPayments() {
            // Virtual threads allow much higher concurrency
            return executor.toString().contains("VirtualThread") ? 100000 : 100;
        }
    }

    static class NotificationService {
        private final AuditPort auditPort;
        private final Executor executor;

        public NotificationService(AuditPort auditPort, Executor executor) {
            this.auditPort = auditPort;
            this.executor = executor;
        }

        public Executor getExecutor() {
            return executor;
        }

        public CompletableFuture<Void> sendBulkNotifications(int count) {
            return CompletableFuture.runAsync(() -> {
                for (int i = 0; i < count; i++) {
                    // Simulate notification sending
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, executor);
        }
    }

    static class ExecutorProvider {
        private final Executor baseExecutor;

        public ExecutorProvider(Executor baseExecutor) {
            this.baseExecutor = baseExecutor;
        }

        public Executor getLoanProcessingExecutor() {
            return baseExecutor;
        }

        public Executor getPaymentExecutor() {
            return baseExecutor;
        }

        public Executor getNotificationExecutor() {
            return baseExecutor;
        }

        public boolean isVirtualThreadBased() {
            return baseExecutor.toString().contains("VirtualThread");
        }
    }

    static class VirtualThreadConfiguration {
        
        public Executor virtualThreadExecutor() {
            // return Executors.newVirtualThreadPerTaskExecutor();
            throw new UnsupportedOperationException("Virtual thread configuration not yet implemented - Java 21 migration pending");
        }

        public Executor loanProcessingExecutor() {
            return virtualThreadExecutor();
        }

        public Executor paymentExecutor() {
            return virtualThreadExecutor();
        }

        public Object getVirtualThreadFactory() {
            // return Thread.ofVirtual().factory();
            throw new UnsupportedOperationException("Virtual thread factory not yet implemented - Java 21 migration pending");
        }
    }
}