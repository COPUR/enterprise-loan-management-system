package com.bank.payment.infrastructure.fraud.external;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Batch response containing results for multiple transactions
 */
@Data
public class BatchFraudResponse {
    
    private String batchId;
    private Map<String, ExternalFraudResponse> responses;
    private BatchStatistics statistics;
    private LocalDateTime timestamp;

    @Data
    public static class BatchStatistics {
        private Integer totalTransactions;
        private Integer successfulScreenings;
        private Integer failedScreenings;
        private Integer highRiskTransactions;
        private Integer blockedTransactions;
        private Long totalProcessingTime;
        private Double averageRiskScore;
    }
}