package com.bank.payment.infrastructure.fraud.external;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fraud case report to submit to external fraud detection system
 */
@Data
@Builder
public class FraudCaseReport {
    
    private String transactionId;
    private String customerId;
    private String caseType; // CONFIRMED_FRAUD, SUSPECTED_FRAUD, FALSE_POSITIVE
    private String fraudType; // CARD_FRAUD, ACCOUNT_TAKEOVER, SYNTHETIC_ID, etc.
    private BigDecimal amount;
    private String currency;
    private LocalDateTime transactionDate;
    private LocalDateTime reportedDate;
    private String reportedBy; // CUSTOMER, BANK, MERCHANT, AUTOMATED
    private FraudDetails fraudDetails;
    private List<Evidence> evidence;
    private String status; // OPEN, INVESTIGATING, CLOSED, ESCALATED
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private Map<String, Object> additionalData;

    @Data
    @Builder
    public static class FraudDetails {
        private String description;
        private String method; // How the fraud was committed
        private String source; // Where the fraud originated
        private Boolean isFirstPartyFraud;
        private String victimType; // INDIVIDUAL, BUSINESS
        private BigDecimal estimatedLoss;
        private String recoveryStatus;
    }

    @Data
    @Builder
    public static class Evidence {
        private String evidenceType; // TRANSACTION_LOG, IP_ADDRESS, DEVICE_ID, etc.
        private String description;
        private String value;
        private LocalDateTime timestamp;
        private String source;
        private String reliability; // HIGH, MEDIUM, LOW
    }
}