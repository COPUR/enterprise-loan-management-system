package com.bank.payment.infrastructure.fraud.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Batch request for processing multiple transactions simultaneously
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchFraudRequest {
    
    private Map<String, ExternalFraudRequest> transactions;
    private String batchId;
    private LocalDateTime timestamp;
    private BatchConfiguration configuration;

    public BatchFraudRequest(Map<String, ExternalFraudRequest> transactions) {
        this.transactions = transactions;
        this.batchId = "BATCH-" + System.currentTimeMillis();
        this.timestamp = LocalDateTime.now();
        this.configuration = new BatchConfiguration();
    }

    @Data
    public static class BatchConfiguration {
        private Boolean parallelProcessing = true;
        private Integer timeoutSeconds = 30;
        private Boolean continueOnError = true;
        private String priority = "NORMAL"; // LOW, NORMAL, HIGH
    }
}