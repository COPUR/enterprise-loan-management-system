package com.amanahfi.platform.cbdc.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to transfer Digital Dirham funds between wallets")
public class TransferFundsRequest {

    @NotNull(message = "Sender wallet ID is required")
    @Schema(
        description = "Unique identifier of the sender's Digital Dirham wallet",
        example = "123e4567-e89b-12d3-a456-426614174000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID senderWalletId;

    @NotNull(message = "Receiver wallet ID is required")
    @Schema(
        description = "Unique identifier of the receiver's Digital Dirham wallet",
        example = "456e7890-e89b-12d3-a456-426614174000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID receiverWalletId;

    @NotNull(message = "Transfer amount is required")
    @Valid
    @Schema(
        description = "Amount to transfer",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MoneyRequest amount;

    @NotBlank(message = "Transfer purpose is required")
    @Size(max = 200, message = "Transfer purpose must not exceed 200 characters")
    @Schema(
        description = "Purpose or reason for the transfer",
        example = "Murabaha installment payment",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String purpose;

    @Schema(
        description = "Transfer type classification",
        example = "ISLAMIC_FINANCE",
        allowableValues = {"DOMESTIC", "CROSS_BORDER", "ISLAMIC_FINANCE", "COMMERCIAL", "GOVERNMENT", "REMITTANCE"}
    )
    private String transferType;

    @Schema(
        description = "Priority level for transfer processing",
        example = "NORMAL",
        allowableValues = {"LOW", "NORMAL", "HIGH", "URGENT"}
    )
    private String priority;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(
        description = "Additional notes or instructions for the transfer",
        example = "Monthly payment for vehicle financing - Asset ID: VEH-2024-001"
    )
    private String notes;

    @Schema(
        description = "Reference number from external system",
        example = "EXT-REF-20241211-001"
    )
    private String externalReference;

    @Schema(
        description = "Compliance information for regulatory requirements"
    )
    private ComplianceInfo complianceInfo;

    @Schema(
        description = "Notifications preferences for transfer updates"
    )
    private NotificationPreferences notificationPreferences;

    @Schema(
        description = "Additional metadata for the transfer"
    )
    private Map<String, Object> metadata;

    @Schema(
        description = "Scheduled transfer date (for future-dated transfers)",
        example = "2024-12-15T10:00:00Z"
    )
    private String scheduledAt;

    // Default constructor
    public TransferFundsRequest() {}

    // Full constructor
    public TransferFundsRequest(UUID senderWalletId, UUID receiverWalletId, MoneyRequest amount,
                               String purpose, String transferType, String priority,
                               String notes, String externalReference,
                               ComplianceInfo complianceInfo, NotificationPreferences notificationPreferences,
                               Map<String, Object> metadata, String scheduledAt) {
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.purpose = purpose;
        this.transferType = transferType;
        this.priority = priority;
        this.notes = notes;
        this.externalReference = externalReference;
        this.complianceInfo = complianceInfo;
        this.notificationPreferences = notificationPreferences;
        this.metadata = metadata;
        this.scheduledAt = scheduledAt;
    }

    // Getters and Setters
    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(UUID senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public void setReceiverWalletId(UUID receiverWalletId) {
        this.receiverWalletId = receiverWalletId;
    }

    public MoneyRequest getAmount() {
        return amount;
    }

    public void setAmount(MoneyRequest amount) {
        this.amount = amount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public ComplianceInfo getComplianceInfo() {
        return complianceInfo;
    }

    public void setComplianceInfo(ComplianceInfo complianceInfo) {
        this.complianceInfo = complianceInfo;
    }

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(NotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    // Nested classes
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Monetary amount with currency")
    public static class MoneyRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        @Digits(integer = 15, fraction = 8, message = "Amount format is invalid")
        @Schema(
            description = "Transfer amount (supports up to 8 decimal places for CBDC precision)",
            example = "1000.00000000",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private BigDecimal amount;

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}(-[A-Z]+)?$", message = "Invalid currency format")
        @Schema(
            description = "Currency code (supports both traditional and CBDC currencies)",
            example = "AED-CBDC",
            allowableValues = {"AED", "AED-CBDC", "SAR-CBDC", "QAR-CBDC", "KWD-CBDC", "BHD-CBDC", "OMR-CBDC"},
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String currency;

        // Constructors, getters, and setters
        public MoneyRequest() {}

        public MoneyRequest(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Compliance information for regulatory requirements")
    public static class ComplianceInfo {

        @Schema(
            description = "Source of funds declaration",
            example = "SALARY",
            allowableValues = {"SALARY", "BUSINESS_INCOME", "INVESTMENT", "GIFT", "INHERITANCE", "LOAN_PROCEEDS", "OTHER"}
        )
        private String sourceOfFunds;

        @Schema(
            description = "Beneficiary relationship to sender",
            example = "FAMILY",
            allowableValues = {"SELF", "FAMILY", "BUSINESS_PARTNER", "EMPLOYEE", "CUSTOMER", "SUPPLIER", "OTHER"}
        )
        private String beneficiaryRelationship;

        @Schema(
            description = "Expected frequency of similar transfers",
            example = "MONTHLY",
            allowableValues = {"ONE_TIME", "WEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY", "IRREGULAR"}
        )
        private String expectedFrequency;

        @Schema(
            description = "Business purpose code for commercial transfers",
            example = "TRADE_FINANCE"
        )
        private String businessPurposeCode;

        @Schema(
            description = "Ultimate beneficiary information (if different from receiver)"
        )
        private UltimateBeneficiaryInfo ultimateBeneficiary;

        @Schema(
            description = "Whether enhanced due diligence is required",
            example = "false"
        )
        private Boolean enhancedDueDiligenceRequired;

        @Schema(
            description = "PEP (Politically Exposed Person) declaration",
            example = "false"
        )
        private Boolean pepDeclaration;

        @Schema(
            description = "Sanctions screening override (if applicable)"
        )
        private String sanctionsOverride;

        // Constructors, getters, and setters
        public ComplianceInfo() {}

        public String getSourceOfFunds() { return sourceOfFunds; }
        public void setSourceOfFunds(String sourceOfFunds) { this.sourceOfFunds = sourceOfFunds; }

        public String getBeneficiaryRelationship() { return beneficiaryRelationship; }
        public void setBeneficiaryRelationship(String beneficiaryRelationship) { this.beneficiaryRelationship = beneficiaryRelationship; }

        public String getExpectedFrequency() { return expectedFrequency; }
        public void setExpectedFrequency(String expectedFrequency) { this.expectedFrequency = expectedFrequency; }

        public String getBusinessPurposeCode() { return businessPurposeCode; }
        public void setBusinessPurposeCode(String businessPurposeCode) { this.businessPurposeCode = businessPurposeCode; }

        public UltimateBeneficiaryInfo getUltimateBeneficiary() { return ultimateBeneficiary; }
        public void setUltimateBeneficiary(UltimateBeneficiaryInfo ultimateBeneficiary) { this.ultimateBeneficiary = ultimateBeneficiary; }

        public Boolean getEnhancedDueDiligenceRequired() { return enhancedDueDiligenceRequired; }
        public void setEnhancedDueDiligenceRequired(Boolean enhancedDueDiligenceRequired) { this.enhancedDueDiligenceRequired = enhancedDueDiligenceRequired; }

        public Boolean getPepDeclaration() { return pepDeclaration; }
        public void setPepDeclaration(Boolean pepDeclaration) { this.pepDeclaration = pepDeclaration; }

        public String getSanctionsOverride() { return sanctionsOverride; }
        public void setSanctionsOverride(String sanctionsOverride) { this.sanctionsOverride = sanctionsOverride; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Ultimate beneficiary information")
    public static class UltimateBeneficiaryInfo {

        @Schema(description = "Full name of ultimate beneficiary", example = "Ahmed Al-Rashid")
        private String fullName;

        @Schema(description = "National identification number")
        private String nationalId;

        @Schema(description = "Relationship to immediate beneficiary", example = "SPOUSE")
        private String relationship;

        @Schema(description = "Country of residence", example = "AE")
        private String countryOfResidence;

        // Constructors, getters, and setters
        public UltimateBeneficiaryInfo() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getNationalId() { return nationalId; }
        public void setNationalId(String nationalId) { this.nationalId = nationalId; }

        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }

        public String getCountryOfResidence() { return countryOfResidence; }
        public void setCountryOfResidence(String countryOfResidence) { this.countryOfResidence = countryOfResidence; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Notification preferences for transfer updates")
    public static class NotificationPreferences {

        @Schema(description = "Send SMS notifications", example = "true")
        private Boolean smsNotification;

        @Schema(description = "Send email notifications", example = "true")
        private Boolean emailNotification;

        @Schema(description = "Send push notifications", example = "true")
        private Boolean pushNotification;

        @Schema(description = "Notify on transfer initiation", example = "true")
        private Boolean notifyOnInitiation;

        @Schema(description = "Notify on transfer confirmation", example = "true")
        private Boolean notifyOnConfirmation;

        @Schema(description = "Notify on transfer completion", example = "true")
        private Boolean notifyOnCompletion;

        @Schema(description = "Notify on transfer failure", example = "true")
        private Boolean notifyOnFailure;

        @Schema(description = "Phone number for SMS notifications", example = "+971501234567")
        private String smsPhoneNumber;

        @Schema(description = "Email address for notifications", example = "customer@email.ae")
        private String emailAddress;

        @Schema(description = "Preferred language for notifications", example = "ARABIC")
        private String preferredLanguage;

        // Constructors, getters, and setters
        public NotificationPreferences() {}

        public Boolean getSmsNotification() { return smsNotification; }
        public void setSmsNotification(Boolean smsNotification) { this.smsNotification = smsNotification; }

        public Boolean getEmailNotification() { return emailNotification; }
        public void setEmailNotification(Boolean emailNotification) { this.emailNotification = emailNotification; }

        public Boolean getPushNotification() { return pushNotification; }
        public void setPushNotification(Boolean pushNotification) { this.pushNotification = pushNotification; }

        public Boolean getNotifyOnInitiation() { return notifyOnInitiation; }
        public void setNotifyOnInitiation(Boolean notifyOnInitiation) { this.notifyOnInitiation = notifyOnInitiation; }

        public Boolean getNotifyOnConfirmation() { return notifyOnConfirmation; }
        public void setNotifyOnConfirmation(Boolean notifyOnConfirmation) { this.notifyOnConfirmation = notifyOnConfirmation; }

        public Boolean getNotifyOnCompletion() { return notifyOnCompletion; }
        public void setNotifyOnCompletion(Boolean notifyOnCompletion) { this.notifyOnCompletion = notifyOnCompletion; }

        public Boolean getNotifyOnFailure() { return notifyOnFailure; }
        public void setNotifyOnFailure(Boolean notifyOnFailure) { this.notifyOnFailure = notifyOnFailure; }

        public String getSmsPhoneNumber() { return smsPhoneNumber; }
        public void setSmsPhoneNumber(String smsPhoneNumber) { this.smsPhoneNumber = smsPhoneNumber; }

        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

        public String getPreferredLanguage() { return preferredLanguage; }
        public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    }
}