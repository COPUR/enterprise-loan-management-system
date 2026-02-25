package com.loanmanagement.shared.application.port.out;

import java.util.function.Supplier;

/**
 * Outbound Port for Transaction Management
 * Abstracts transaction handling from the application layer
 */
public interface TransactionManager {
    
    /**
     * Execute an operation within a transaction
     */
    <T> T executeInTransaction(Supplier<T> operation);
    
    /**
     * Execute an operation within a read-only transaction
     */
    <T> T executeInReadOnlyTransaction(Supplier<T> operation);
    
    /**
     * Execute an operation without a transaction
     */
    void executeWithoutTransaction(Runnable operation);
    
    /**
     * Execute an operation with custom transaction attributes
     */
    <T> T executeWithAttributes(Supplier<T> operation, TransactionAttributes attributes);
    
    /**
     * Transaction attributes for custom transaction handling
     */
    record TransactionAttributes(
            boolean readOnly,
            int timeoutSeconds,
            IsolationLevel isolationLevel,
            PropagationBehavior propagationBehavior
    ) {
        public static TransactionAttributes defaultAttributes() {
            return new TransactionAttributes(
                    false, 
                    30, 
                    IsolationLevel.READ_COMMITTED, 
                    PropagationBehavior.REQUIRED
            );
        }
        
        public static TransactionAttributes readOnlyAttributes() {
            return new TransactionAttributes(
                    true, 
                    30, 
                    IsolationLevel.READ_COMMITTED, 
                    PropagationBehavior.REQUIRED
            );
        }
    }
    
    enum IsolationLevel {
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }
    
    enum PropagationBehavior {
        REQUIRED,
        SUPPORTS,
        MANDATORY,
        REQUIRES_NEW,
        NOT_SUPPORTED,
        NEVER,
        NESTED
    }
}