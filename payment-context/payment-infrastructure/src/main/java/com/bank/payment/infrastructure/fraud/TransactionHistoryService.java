package com.bank.payment.infrastructure.fraud;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transaction History Service for Fraud Detection
 */
@Service
public class TransactionHistoryService {
    
    private final Map<String, List<TransactionContext>> customerHistory = new ConcurrentHashMap<>();
    
    public List<TransactionContext> getRecentTransactions(String customerId, int hours) {
        List<TransactionContext> history = customerHistory.getOrDefault(customerId, new ArrayList<>());
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        return history.stream()
            .filter(tx -> tx.getTimestamp().isAfter(cutoff))
            .toList();
    }
    
    public void recordTransaction(String customerId, TransactionContext context) {
        List<TransactionContext> history = customerHistory.computeIfAbsent(
            customerId, k -> new ArrayList<>()
        );
        
        history.add(context);
        
        // Keep only recent history
        if (history.size() > 1000) {
            history.remove(0);
        }
    }
}