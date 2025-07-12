package com.amanahfi.platform.islamicfinance.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a new Sharia-compliant Murabaha financing arrangement")
public class CreateMurabahaRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(
        description = "Unique identifier of the customer requesting financing",
        example = "123e4567-e89b-12d3-a456-426614174000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID customerId;

    @NotNull(message = "Asset details are required")
    @Valid
    @Schema(
        description = "Details of the asset being financed",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AssetDetailsRequest assetDetails;

    @NotNull(message = "Financing terms are required")
    @Valid
    @Schema(
        description = "Terms and conditions of the Murabaha financing",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private FinancingTermsRequest financingTerms;

    @Size(max = 1000, message = "Comments must not exceed 1000 characters")
    @Schema(
        description = "Additional notes or comments for the financing request",
        example = "Customer requires expedited processing for time-sensitive asset acquisition"
    )
    private String comments;

    @Schema(
        description = "Customer preferences for the financing arrangement"
    )
    private CustomerPreferencesRequest customerPreferences;

    // Default constructor
    public CreateMurabahaRequest() {}

    // Full constructor
    public CreateMurabahaRequest(UUID customerId, AssetDetailsRequest assetDetails, 
                                FinancingTermsRequest financingTerms, String comments,
                                CustomerPreferencesRequest customerPreferences) {
        this.customerId = customerId;
        this.assetDetails = assetDetails;
        this.financingTerms = financingTerms;
        this.comments = comments;
        this.customerPreferences = customerPreferences;
    }

    // Getters and Setters
    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public AssetDetailsRequest getAssetDetails() {
        return assetDetails;
    }

    public void setAssetDetails(AssetDetailsRequest assetDetails) {
        this.assetDetails = assetDetails;
    }

    public FinancingTermsRequest getFinancingTerms() {
        return financingTerms;
    }

    public void setFinancingTerms(FinancingTermsRequest financingTerms) {
        this.financingTerms = financingTerms;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public CustomerPreferencesRequest getCustomerPreferences() {
        return customerPreferences;
    }

    public void setCustomerPreferences(CustomerPreferencesRequest customerPreferences) {
        this.customerPreferences = customerPreferences;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Asset details for Islamic financing")
    public static class AssetDetailsRequest {

        @NotBlank(message = "Asset description is required")
        @Size(max = 500, message = "Asset description must not exceed 500 characters")
        @Schema(
            description = "Detailed description of the asset being financed",
            example = "Toyota Camry 2024 - White Color, Fully Loaded with Premium Package",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String description;

        @NotNull(message = "Asset category is required")
        @Schema(
            description = "Category classification of the asset",
            example = "VEHICLE",
            allowableValues = {"VEHICLE", "REAL_ESTATE", "MACHINERY", "COMMODITY", "EQUIPMENT"},
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String category;

        @NotNull(message = "Asset cost is required")
        @Valid
        @Schema(
            description = "Total cost of the asset",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private MoneyRequest cost;

        @NotNull(message = "Supplier details are required")
        @Valid
        @Schema(
            description = "Information about the asset supplier",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private SupplierDetailsRequest supplier;

        @Schema(
            description = "Additional technical specifications and characteristics of the asset"
        )
        private Map<String, Object> specifications;

        @AssertTrue(message = "Asset must be Halal for Islamic financing")
        @Schema(
            description = "Indicates whether the asset is Halal (permissible) according to Islamic principles",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Boolean halal;

        // Constructors, getters, and setters
        public AssetDetailsRequest() {}

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public MoneyRequest getCost() { return cost; }
        public void setCost(MoneyRequest cost) { this.cost = cost; }

        public SupplierDetailsRequest getSupplier() { return supplier; }
        public void setSupplier(SupplierDetailsRequest supplier) { this.supplier = supplier; }

        public Map<String, Object> getSpecifications() { return specifications; }
        public void setSpecifications(Map<String, Object> specifications) { this.specifications = specifications; }

        public Boolean getHalal() { return halal; }
        public void setHalal(Boolean halal) { this.halal = halal; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Financing terms and conditions for Murabaha")
    public static class FinancingTermsRequest {

        @NotNull(message = "Profit margin is required")
        @DecimalMin(value = "0.01", message = "Profit margin must be positive")
        @DecimalMax(value = "0.30", message = "Profit margin cannot exceed 30% (HSA limit)")
        @Digits(integer = 1, fraction = 4, message = "Profit margin format is invalid")
        @Schema(
            description = "Profit margin as a decimal (e.g., 0.15 for 15%)",
            example = "0.15",
            minimum = "0.01",
            maximum = "0.30",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private BigDecimal profitMargin;

        @NotNull(message = "Maturity date is required")
        @Future(message = "Maturity date must be in the future")
        @Schema(
            description = "Final payment date for the financing",
            example = "2027-12-31",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private LocalDate maturityDate;

        @NotNull(message = "Payment frequency is required")
        @Schema(
            description = "Frequency of installment payments",
            example = "MONTHLY",
            allowableValues = {"MONTHLY", "QUARTERLY", "SEMI_ANNUALLY", "ANNUALLY"},
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String paymentFrequency;

        @Valid
        @Schema(description = "Down payment amount (if applicable)")
        private MoneyRequest downPayment;

        @Schema(
            description = "Grace period in days before first payment",
            example = "30",
            minimum = "0",
            maximum = "180"
        )
        private Integer gracePeriodDays;

        @Schema(
            description = "Special conditions or terms for the financing",
            example = "Early settlement allowed without penalty"
        )
        private String specialConditions;

        // Constructors, getters, and setters
        public FinancingTermsRequest() {}

        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

        public LocalDate getMaturityDate() { return maturityDate; }
        public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

        public String getPaymentFrequency() { return paymentFrequency; }
        public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }

        public MoneyRequest getDownPayment() { return downPayment; }
        public void setDownPayment(MoneyRequest downPayment) { this.downPayment = downPayment; }

        public Integer getGracePeriodDays() { return gracePeriodDays; }
        public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }

        public String getSpecialConditions() { return specialConditions; }
        public void setSpecialConditions(String specialConditions) { this.specialConditions = specialConditions; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Monetary amount with currency")
    public static class MoneyRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "Amount format is invalid")
        @Schema(
            description = "Monetary amount",
            example = "80000.00",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private BigDecimal amount;

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}(-[A-Z]+)?$", message = "Invalid currency format")
        @Schema(
            description = "Currency code (ISO 4217 format or CBDC format)",
            example = "AED",
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
    @Schema(description = "Supplier information for asset acquisition")
    public static class SupplierDetailsRequest {

        @NotBlank(message = "Supplier name is required")
        @Size(max = 255, message = "Supplier name must not exceed 255 characters")
        @Schema(
            description = "Legal name of the supplier",
            example = "Toyota Motors UAE LLC",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String name;

        @Size(max = 100, message = "Registration number must not exceed 100 characters")
        @Schema(
            description = "Business registration number",
            example = "CN-1234567"
        )
        private String registrationNumber;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        @Schema(
            description = "Contact phone number",
            example = "+971441234567"
        )
        private String contactNumber;

        @Email(message = "Invalid email format")
        @Schema(
            description = "Contact email address",
            example = "sales@toyota-uae.com"
        )
        private String email;

        @Schema(description = "Supplier's business address")
        private AddressRequest address;

        // Constructors, getters, and setters
        public SupplierDetailsRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRegistrationNumber() { return registrationNumber; }
        public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public AddressRequest getAddress() { return address; }
        public void setAddress(AddressRequest address) { this.address = address; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Address information")
    public static class AddressRequest {

        @Schema(description = "Street address", example = "Sheikh Zayed Road")
        private String street;

        @Schema(description = "City", example = "Dubai")
        private String city;

        @Schema(description = "State or emirate", example = "Dubai")
        private String state;

        @Schema(description = "Postal code", example = "12345")
        private String postalCode;

        @Schema(description = "Country code", example = "AE")
        private String country;

        // Constructors, getters, and setters
        public AddressRequest() {}

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Customer preferences for financing")
    public static class CustomerPreferencesRequest {

        @Schema(
            description = "Preferred communication language",
            example = "ARABIC",
            allowableValues = {"ARABIC", "ENGLISH"}
        )
        private String preferredLanguage;

        @Schema(
            description = "Preferred payment method",
            example = "DIGITAL_DIRHAM",
            allowableValues = {"BANK_TRANSFER", "DIGITAL_DIRHAM", "DIRECT_DEBIT"}
        )
        private String preferredPaymentMethod;

        @Schema(
            description = "Whether customer wants digital-only documentation",
            example = "true"
        )
        private Boolean digitalDocumentation;

        @Schema(
            description = "Notification preferences"
        )
        private Map<String, Boolean> notificationPreferences;

        // Constructors, getters, and setters
        public CustomerPreferencesRequest() {}

        public String getPreferredLanguage() { return preferredLanguage; }
        public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

        public String getPreferredPaymentMethod() { return preferredPaymentMethod; }
        public void setPreferredPaymentMethod(String preferredPaymentMethod) { this.preferredPaymentMethod = preferredPaymentMethod; }

        public Boolean getDigitalDocumentation() { return digitalDocumentation; }
        public void setDigitalDocumentation(Boolean digitalDocumentation) { this.digitalDocumentation = digitalDocumentation; }

        public Map<String, Boolean> getNotificationPreferences() { return notificationPreferences; }
        public void setNotificationPreferences(Map<String, Boolean> notificationPreferences) { this.notificationPreferences = notificationPreferences; }
    }
}