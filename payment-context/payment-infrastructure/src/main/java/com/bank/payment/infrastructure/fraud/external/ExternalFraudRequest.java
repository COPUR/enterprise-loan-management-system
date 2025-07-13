package com.bank.payment.infrastructure.fraud.external;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request model for external fraud detection services
 */
@Data
@Builder
public class ExternalFraudRequest {
    
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private String merchantCategory;
    private String paymentMethod;
    private String cardToken; // Tokenized card number
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private LocationData location;
    private CustomerProfile customerProfile;
    private TransactionContext transactionContext;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalData;

    @Data
    @Builder
    public static class LocationData {
        private String country;
        private String city;
        private String region;
        private Double latitude;
        private Double longitude;
        private String zipCode;
    }

    @Data
    @Builder
    public static class CustomerProfile {
        private String customerId;
        private String customerType;
        private LocalDateTime accountCreatedDate;
        private Integer transactionCount30Days;
        private BigDecimal totalSpent30Days;
        private String riskProfile;
        private Boolean isVerified;
    }

    @Data
    @Builder
    public static class TransactionContext {
        private String channelType; // ONLINE, MOBILE, ATM, POS
        private String authenticationMethod; // PIN, BIOMETRIC, OTP, PASSWORD
        private Boolean isRecurring;
        private String referenceTransactionId;
        private String sessionId;
        private Integer velocityCount1Hour;
        private Integer velocityCount24Hour;
        private BigDecimal velocityAmount1Hour;
        private BigDecimal velocityAmount24Hour;
    }
}