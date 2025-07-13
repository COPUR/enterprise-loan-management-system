package com.bank.payment.infrastructure.fraud.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response after submitting a fraud case report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudReportResponse {
    
    private String transactionId;
    private String status; // RECEIVED, PROCESSING, COMPLETED, FAILED
    private String caseId;
    private String referenceNumber;
    private LocalDateTime timestamp;
    private String message;
    private String nextAction;
    private LocalDateTime expectedResolution;
}