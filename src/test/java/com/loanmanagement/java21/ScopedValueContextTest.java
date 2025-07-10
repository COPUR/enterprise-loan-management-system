package com.loanmanagement.java21;

import com.loanmanagement.common.context.RequestContext;
import com.loanmanagement.loan.application.service.LoanProcessingService;
import com.loanmanagement.audit.domain.AuditEvent;
import com.loanmanagement.audit.application.port.out.AuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScopedValue Context Migration Tests")
class ScopedValueContextTest {

    @Mock
    private AuditRepository auditRepository;
    
    private LoanProcessingService loanProcessingService;
    private Executor virtualThreadExecutor;
    
    // ScopedValue instances - these will replace ThreadLocal
    private static ScopedValue<String> REQUEST_ID;
    private static ScopedValue<String> USER_ID;
    private static ScopedValue<String> TENANT_ID;
    private static ScopedValue<RequestContext> REQUEST_CONTEXT;

    @BeforeEach
    void setUp() {
        virtualThreadExecutor = createVirtualThreadExecutor();
        loanProcessingService = new LoanProcessingService(auditRepository, virtualThreadExecutor);
        
        // Initialize ScopedValue instances - will fail until Java 21
        REQUEST_ID = createScopedValue();
        USER_ID = createScopedValue();
        TENANT_ID = createScopedValue();
        REQUEST_CONTEXT = createScopedValue();
    }

    @Test
    @DisplayName("Should preserve context using ScopedValue inside virtual threads")
    void shouldRetainContextWithScopedValueInVirtualThread() {
        // given
        String expectedRequestId = "REQ-1234";
        String expectedUserId = "USER-5678";
        String expectedTenantId = "TENANT-ABCD";
        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        // when
        ScopedValue.runWhere(REQUEST_ID, expectedRequestId, () -> {
            ScopedValue.runWhere(USER_ID, expectedUserId, () -> {
                ScopedValue.runWhere(TENANT_ID, expectedTenantId, () -> {
                    
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        // This should retain the scoped values from parent virtual thread
                        capturedRequestId.set(REQUEST_ID.get());
                        capturedUserId.set(USER_ID.get());
                        capturedTenantId.set(TENANT_ID.get());
                        
                        // Simulate some business logic that needs context
                        processLoanWithContext();
                        
                    }, virtualThreadExecutor);
                    
                    assertDoesNotThrow(() -> future.join(), 
                        "Virtual thread execution should not throw exceptions");
                });
            });
        });

        // then
        assertThat(capturedRequestId.get())
            .isEqualTo(expectedRequestId)
            .describedAs("Request ID should propagate via ScopedValue to virtual thread");
        
        assertThat(capturedUserId.get())
            .isEqualTo(expectedUserId)
            .describedAs("User ID should propagate via ScopedValue to virtual thread");
        
        assertThat(capturedTenantId.get())
            .isEqualTo(expectedTenantId)
            .describedAs("Tenant ID should propagate via ScopedValue to virtual thread");
    }

    @Test
    @DisplayName("Should replace ThreadLocal with ScopedValue for request context")
    void shouldReplaceThreadLocalWithScopedValueForRequestContext() {
        // given
        RequestContext originalContext = RequestContext.builder()
            .requestId("REQ-9999")
            .userId("USER-8888")
            .tenantId("TENANT-7777")
            .timestamp(Instant.now())
            .correlationId("CORR-6666")
            .build();
        
        AtomicReference<RequestContext> capturedContext = new AtomicReference<>();
        
        // when - Using ScopedValue instead of ThreadLocal
        ScopedValue.runWhere(REQUEST_CONTEXT, originalContext, () -> {
            
            // Execute in virtual thread
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                
                // Context should be accessible without explicit passing
                RequestContext currentContext = REQUEST_CONTEXT.get();
                capturedContext.set(currentContext);
                
                // Simulate nested service calls that need context
                performNestedServiceCalls();
                
            }, virtualThreadExecutor);
            
            assertDoesNotThrow(() -> future.join());
        });
        
        // then
        assertThat(capturedContext.get())
            .isNotNull()
            .isEqualTo(originalContext)
            .describedAs("ScopedValue should replace ThreadLocal for context propagation");
        
        assertThat(capturedContext.get().getRequestId())
            .isEqualTo("REQ-9999");
        assertThat(capturedContext.get().getUserId())
            .isEqualTo("USER-8888");
        assertThat(capturedContext.get().getTenantId())
            .isEqualTo("TENANT-7777");
    }

    @Test
    @DisplayName("Should maintain context isolation between concurrent virtual threads")
    void shouldMaintainContextIsolationBetweenConcurrentVirtualThreads() {
        // given
        int numberOfThreads = 100;
        CompletableFuture<?>[] futures = new CompletableFuture[numberOfThreads];
        AtomicReference<String>[] capturedRequestIds = new AtomicReference[numberOfThreads];
        
        for (int i = 0; i < numberOfThreads; i++) {
            capturedRequestIds[i] = new AtomicReference<>();
        }
        
        // when - Each virtual thread should have isolated context
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            final String requestId = "REQ-" + threadIndex;
            
            futures[threadIndex] = CompletableFuture.runAsync(() -> {
                
                ScopedValue.runWhere(REQUEST_ID, requestId, () -> {
                    
                    // Execute in nested virtual thread
                    CompletableFuture<Void> nestedFuture = CompletableFuture.runAsync(() -> {
                        try {
                            // Simulate some processing time
                            Thread.sleep(10);
                            
                            // Each thread should see its own context
                            String currentRequestId = REQUEST_ID.get();
                            capturedRequestIds[threadIndex].set(currentRequestId);
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }, virtualThreadExecutor);
                    
                    assertDoesNotThrow(() -> nestedFuture.join());
                });
                
            }, virtualThreadExecutor);
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures).join();
        
        // then - Each thread should have maintained its own context
        for (int i = 0; i < numberOfThreads; i++) {
            assertThat(capturedRequestIds[i].get())
                .isEqualTo("REQ-" + i)
                .describedAs("Thread %d should maintain its own isolated context", i);
        }
    }

    @Test
    @DisplayName("Should propagate context through domain service layers using ScopedValue")
    void shouldPropagateContextThroughDomainServiceLayersUsingScopedValue() {
        // given
        RequestContext context = RequestContext.builder()
            .requestId("REQ-DOMAIN")
            .userId("USER-DOMAIN")
            .tenantId("TENANT-DOMAIN")
            .timestamp(Instant.now())
            .build();
        
        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        
        // when
        ScopedValue.runWhere(REQUEST_CONTEXT, context, () -> {
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                
                // This should use ScopedValue context internally
                loanProcessingService.processLoanApplication("LOAN-123");
                
            }, virtualThreadExecutor);
            
            assertDoesNotThrow(() -> future.join());
        });
        
        // then
        verify(auditRepository, atLeastOnce()).save(auditEventCaptor.capture());
        
        AuditEvent capturedEvent = auditEventCaptor.getValue();
        assertThat(capturedEvent.getRequestId())
            .isEqualTo("REQ-DOMAIN")
            .describedAs("Audit event should contain context from ScopedValue");
        
        assertThat(capturedEvent.getUserId())
            .isEqualTo("USER-DOMAIN")
            .describedAs("User context should propagate through service layers");
    }

    @Test
    @DisplayName("Should handle ScopedValue inheritance in structured concurrency")
    void shouldHandleScopedValueInheritanceInStructuredConcurrency() {
        // given
        String parentRequestId = "PARENT-REQ-001";
        AtomicReference<String> childRequestId = new AtomicReference<>();
        
        // when - Using structured concurrency with ScopedValue
        ScopedValue.runWhere(REQUEST_ID, parentRequestId, () -> {
            
            try (var scope = StructuredTaskScope.newShutdownOnFailure()) {
                
                // Submit task to structured scope
                StructuredTaskScope.Subtask<String> subtask = scope.fork(() -> {
                    // Child task should inherit parent's ScopedValue
                    String inherited = REQUEST_ID.get();
                    childRequestId.set(inherited);
                    
                    // Simulate some business logic
                    Thread.sleep(50);
                    
                    return "Task completed with context: " + inherited;
                });
                
                // Join all subtasks
                scope.join();
                scope.throwIfFailed();
                
                // Verify subtask completed successfully
                assertThat(subtask.state())
                    .isEqualTo(StructuredTaskScope.Subtask.State.SUCCESS)
                    .describedAs("Subtask should complete successfully");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Structured concurrency should not be interrupted");
            }
        });
        
        // then
        assertThat(childRequestId.get())
            .isEqualTo(parentRequestId)
            .describedAs("ScopedValue should be inherited in structured concurrency");
    }

    // Helper methods - These will fail until Java 21 implementation
    
    private Executor createVirtualThreadExecutor() {
        // Will be implemented in Green phase
        throw new UnsupportedOperationException("Virtual thread executor not yet implemented - Java 21 migration pending");
    }
    
    private <T> ScopedValue<T> createScopedValue() {
        // Will be implemented in Green phase
        // return ScopedValue.newInstance();
        throw new UnsupportedOperationException("ScopedValue not yet implemented - Java 21 migration pending");
    }
    
    private void processLoanWithContext() {
        // Mock implementation - will access ScopedValue context
        String requestId = REQUEST_ID.get();
        String userId = USER_ID.get();
        
        // Simulate business logic that needs context
        assertThat(requestId).isNotNull();
        assertThat(userId).isNotNull();
    }
    
    private void performNestedServiceCalls() {
        // Mock nested service calls that should access ScopedValue context
        RequestContext context = REQUEST_CONTEXT.get();
        assertThat(context).isNotNull();
        
        // Simulate audit logging with context
        when(auditRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());
    }
}