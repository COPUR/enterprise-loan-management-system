package com.bank.payment.infrastructure.fraud.external;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request to update customer risk profile in external fraud system
 */
@Data
@Builder
public class CustomerRiskUpdateRequest {
    
    private String customerId;
    private String eventType; // TRANSACTION, INCIDENT, PROFILE_UPDATE, VERIFICATION
    private TransactionHistory transactionHistory;
    private IncidentHistory incidentHistory;
    private ProfileChanges profileChanges;
    private LocalDateTime timestamp;

    @Data
    @Builder
    public static class TransactionHistory {
        private Integer transactionCount30Days;
        private BigDecimal totalAmount30Days;
        private BigDecimal averageTransactionAmount;
        private Integer distinctMerchants;
        private Integer distinctCountries;
        private List<String> paymentMethods;
        private Integer declinedTransactions;
        private Integer chargebacks;
    }

    @Data
    @Builder
    public static class IncidentHistory {
        private Integer fraudCases30Days;
        private Integer disputesCases30Days;
        private Integer accountTakeovers;
        private Integer suspiciousActivities;
        private String lastIncidentType;
        private LocalDateTime lastIncidentDate;
    }

    @Data
    @Builder
    public static class ProfileChanges {
        private Boolean addressChanged;
        private Boolean phoneChanged;
        private Boolean emailChanged;
        private Boolean bankingDetailsChanged;
        private LocalDateTime lastProfileUpdate;
        private String changeReason;
    }
}