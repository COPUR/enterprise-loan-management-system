package com.amanahfi.platform.islamicfinance.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing Murabaha financing details and current status")
public class MurabahaFinancingResponse extends RepresentationModel<MurabahaFinancingResponse> {

    @Schema(
        description = "Unique identifier of the Murabaha financing",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String financingId;

    @Schema(
        description = "Unique identifier of the customer",
        example = "456e7890-e89b-12d3-a456-426614174000"
    )
    private String customerId;

    @Schema(
        description = "Customer information",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private CustomerInfo customerInfo;

    @Schema(
        description = "Asset details being financed",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AssetDetails assetDetails;

    @Schema(
        description = "Financing terms and calculations",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private FinancingDetails financingDetails;

    @Schema(
        description = "Sharia compliance information",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ShariaComplianceInfo shariaCompliance;

    @Schema(
        description = "Current status of the financing",
        example = "PENDING_APPROVAL",
        allowableValues = {"PENDING_APPROVAL", "APPROVED", "ACTIVE", "COMPLETED", "CANCELLED", "DEFAULTED"}
    )
    private String status;

    @Schema(
        description = "Payment schedule summary"
    )
    private PaymentScheduleSummary paymentSchedule;

    @Schema(
        description = "Audit trail information"
    )
    private AuditInfo auditInfo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @Schema(
        description = "Timestamp when the financing was created",
        example = "2024-12-11T10:30:00.000Z"
    )
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @Schema(
        description = "Timestamp when the financing was last updated",
        example = "2024-12-11T10:30:00.000Z"
    )
    private LocalDateTime updatedAt;

    // Default constructor
    public MurabahaFinancingResponse() {}

    // Getters and Setters
    public String getFinancingId() {
        return financingId;
    }

    public void setFinancingId(String financingId) {
        this.financingId = financingId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(CustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
    }

    public AssetDetails getAssetDetails() {
        return assetDetails;
    }

    public void setAssetDetails(AssetDetails assetDetails) {
        this.assetDetails = assetDetails;
    }

    public FinancingDetails getFinancingDetails() {
        return financingDetails;
    }

    public void setFinancingDetails(FinancingDetails financingDetails) {
        this.financingDetails = financingDetails;
    }

    public ShariaComplianceInfo getShariaCompliance() {
        return shariaCompliance;
    }

    public void setShariaCompliance(ShariaComplianceInfo shariaCompliance) {
        this.shariaCompliance = shariaCompliance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PaymentScheduleSummary getPaymentSchedule() {
        return paymentSchedule;
    }

    public void setPaymentSchedule(PaymentScheduleSummary paymentSchedule) {
        this.paymentSchedule = paymentSchedule;
    }

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Nested classes for structured data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Customer information summary")
    public static class CustomerInfo {

        @Schema(description = "Customer's full name", example = "Ahmed Al-Rashid")
        private String fullName;

        @Schema(description = "Customer's email address", example = "ahmed.alrashid@email.ae")
        private String email;

        @Schema(description = "Customer's phone number", example = "+971501234567")
        private String phoneNumber;

        @Schema(description = "Customer type", example = "INDIVIDUAL")
        private String customerType;

        @Schema(description = "Customer's risk profile", example = "LOW")
        private String riskProfile;

        // Constructors, getters, and setters
        public CustomerInfo() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getCustomerType() { return customerType; }
        public void setCustomerType(String customerType) { this.customerType = customerType; }

        public String getRiskProfile() { return riskProfile; }
        public void setRiskProfile(String riskProfile) { this.riskProfile = riskProfile; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Asset details and specifications")
    public static class AssetDetails {

        @Schema(description = "Asset description", example = "Toyota Camry 2024")
        private String description;

        @Schema(description = "Asset category", example = "VEHICLE")
        private String category;

        @Schema(description = "Asset cost")
        private MoneyInfo cost;

        @Schema(description = "Supplier information")
        private SupplierInfo supplier;

        @Schema(description = "Asset specifications")
        private Map<String, Object> specifications;

        @Schema(description = "Halal certification status", example = "true")
        private Boolean halal;

        @Schema(description = "Physical existence confirmation", example = "true")
        private Boolean physicalExistence;

        // Constructors, getters, and setters
        public AssetDetails() {}

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public MoneyInfo getCost() { return cost; }
        public void setCost(MoneyInfo cost) { this.cost = cost; }

        public SupplierInfo getSupplier() { return supplier; }
        public void setSupplier(SupplierInfo supplier) { this.supplier = supplier; }

        public Map<String, Object> getSpecifications() { return specifications; }
        public void setSpecifications(Map<String, Object> specifications) { this.specifications = specifications; }

        public Boolean getHalal() { return halal; }
        public void setHalal(Boolean halal) { this.halal = halal; }

        public Boolean getPhysicalExistence() { return physicalExistence; }
        public void setPhysicalExistence(Boolean physicalExistence) { this.physicalExistence = physicalExistence; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Financing calculations and terms")
    public static class FinancingDetails {

        @Schema(description = "Principal amount")
        private MoneyInfo principalAmount;

        @Schema(description = "Profit margin percentage", example = "0.15")
        private BigDecimal profitMargin;

        @Schema(description = "Total profit amount")
        private MoneyInfo profitAmount;

        @Schema(description = "Total financing amount")
        private MoneyInfo totalAmount;

        @Schema(description = "Remaining balance")
        private MoneyInfo remainingBalance;

        @Schema(description = "Monthly installment amount")
        private MoneyInfo monthlyInstallment;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Maturity date", example = "2027-12-31")
        private LocalDate maturityDate;

        @Schema(description = "Payment frequency", example = "MONTHLY")
        private String paymentFrequency;

        @Schema(description = "Number of installments", example = "36")
        private Integer numberOfInstallments;

        @Schema(description = "Grace period in days", example = "30")
        private Integer gracePeriodDays;

        // Constructors, getters, and setters
        public FinancingDetails() {}

        public MoneyInfo getPrincipalAmount() { return principalAmount; }
        public void setPrincipalAmount(MoneyInfo principalAmount) { this.principalAmount = principalAmount; }

        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

        public MoneyInfo getProfitAmount() { return profitAmount; }
        public void setProfitAmount(MoneyInfo profitAmount) { this.profitAmount = profitAmount; }

        public MoneyInfo getTotalAmount() { return totalAmount; }
        public void setTotalAmount(MoneyInfo totalAmount) { this.totalAmount = totalAmount; }

        public MoneyInfo getRemainingBalance() { return remainingBalance; }
        public void setRemainingBalance(MoneyInfo remainingBalance) { this.remainingBalance = remainingBalance; }

        public MoneyInfo getMonthlyInstallment() { return monthlyInstallment; }
        public void setMonthlyInstallment(MoneyInfo monthlyInstallment) { this.monthlyInstallment = monthlyInstallment; }

        public LocalDate getMaturityDate() { return maturityDate; }
        public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

        public String getPaymentFrequency() { return paymentFrequency; }
        public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }

        public Integer getNumberOfInstallments() { return numberOfInstallments; }
        public void setNumberOfInstallments(Integer numberOfInstallments) { this.numberOfInstallments = numberOfInstallments; }

        public Integer getGracePeriodDays() { return gracePeriodDays; }
        public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Sharia compliance validation information")
    public static class ShariaComplianceInfo {

        @Schema(description = "Overall compliance status", example = "true")
        private Boolean compliant;

        @Schema(description = "Sharia board authority", example = "UAE_HIGHER_SHARIA_AUTHORITY")
        private String shariaBoard;

        @Schema(description = "Compliance certificate number", example = "HSA-CERT-20241211-001")
        private String certificateNumber;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @Schema(description = "Compliance validation date")
        private LocalDateTime validationDate;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @Schema(description = "Certificate expiry date")
        private LocalDateTime expiryDate;

        @Schema(description = "List of compliance checks performed")
        private List<ComplianceCheck> complianceChecks;

        @Schema(description = "Any compliance violations or concerns")
        private List<String> violations;

        // Constructors, getters, and setters
        public ShariaComplianceInfo() {}

        public Boolean getCompliant() { return compliant; }
        public void setCompliant(Boolean compliant) { this.compliant = compliant; }

        public String getShariaBoard() { return shariaBoard; }
        public void setShariaBoard(String shariaBoard) { this.shariaBoard = shariaBoard; }

        public String getCertificateNumber() { return certificateNumber; }
        public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

        public LocalDateTime getValidationDate() { return validationDate; }
        public void setValidationDate(LocalDateTime validationDate) { this.validationDate = validationDate; }

        public LocalDateTime getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

        public List<ComplianceCheck> getComplianceChecks() { return complianceChecks; }
        public void setComplianceChecks(List<ComplianceCheck> complianceChecks) { this.complianceChecks = complianceChecks; }

        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Individual compliance check result")
    public static class ComplianceCheck {

        @Schema(description = "Type of compliance check", example = "RIBA_PROHIBITION")
        private String checkType;

        @Schema(description = "Check result", example = "PASSED")
        private String result;

        @Schema(description = "Details of the check", example = "No interest-based elements detected")
        private String details;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @Schema(description = "When the check was performed")
        private LocalDateTime checkedAt;

        // Constructors, getters, and setters
        public ComplianceCheck() {}

        public String getCheckType() { return checkType; }
        public void setCheckType(String checkType) { this.checkType = checkType; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public LocalDateTime getCheckedAt() { return checkedAt; }
        public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Monetary information with currency")
    public static class MoneyInfo {

        @Schema(description = "Monetary amount", example = "80000.00")
        private BigDecimal amount;

        @Schema(description = "Currency code", example = "AED")
        private String currency;

        @Schema(description = "Formatted amount for display", example = "AED 80,000.00")
        private String displayValue;

        // Constructors, getters, and setters
        public MoneyInfo() {}

        public MoneyInfo(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getDisplayValue() { return displayValue; }
        public void setDisplayValue(String displayValue) { this.displayValue = displayValue; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Supplier information")
    public static class SupplierInfo {

        @Schema(description = "Supplier name", example = "Toyota Motors UAE LLC")
        private String name;

        @Schema(description = "Registration number", example = "CN-1234567")
        private String registrationNumber;

        @Schema(description = "Contact information", example = "+971441234567")
        private String contactNumber;

        // Constructors, getters, and setters
        public SupplierInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRegistrationNumber() { return registrationNumber; }
        public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Payment schedule summary")
    public static class PaymentScheduleSummary {

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Next payment due date", example = "2025-01-11")
        private LocalDate nextPaymentDate;

        @Schema(description = "Next payment amount")
        private MoneyInfo nextPaymentAmount;

        @Schema(description = "Number of payments made", example = "5")
        private Integer paymentsMade;

        @Schema(description = "Number of remaining payments", example = "31")
        private Integer paymentsRemaining;

        @Schema(description = "Total amount paid so far")
        private MoneyInfo totalPaid;

        @Schema(description = "Payment status", example = "CURRENT")
        private String paymentStatus;

        // Constructors, getters, and setters
        public PaymentScheduleSummary() {}

        public LocalDate getNextPaymentDate() { return nextPaymentDate; }
        public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }

        public MoneyInfo getNextPaymentAmount() { return nextPaymentAmount; }
        public void setNextPaymentAmount(MoneyInfo nextPaymentAmount) { this.nextPaymentAmount = nextPaymentAmount; }

        public Integer getPaymentsMade() { return paymentsMade; }
        public void setPaymentsMade(Integer paymentsMade) { this.paymentsMade = paymentsMade; }

        public Integer getPaymentsRemaining() { return paymentsRemaining; }
        public void setPaymentsRemaining(Integer paymentsRemaining) { this.paymentsRemaining = paymentsRemaining; }

        public MoneyInfo getTotalPaid() { return totalPaid; }
        public void setTotalPaid(MoneyInfo totalPaid) { this.totalPaid = totalPaid; }

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Audit trail information")
    public static class AuditInfo {

        @Schema(description = "Who created the financing", example = "user@example.com")
        private String createdBy;

        @Schema(description = "Who last updated the financing", example = "user@example.com")
        private String updatedBy;

        @Schema(description = "Who approved the financing", example = "user@example.com")
        private String approvedBy;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        @Schema(description = "When the financing was approved")
        private LocalDateTime approvedAt;

        @Schema(description = "Version number for optimistic locking", example = "1")
        private Integer version;

        // Constructors, getters, and setters
        public AuditInfo() {}

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

        public String getApprovedBy() { return approvedBy; }
        public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

        public LocalDateTime getApprovedAt() { return approvedAt; }
        public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
    }
}
