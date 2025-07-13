package com.amanahfi.onboarding.domain.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Customer Aggregate Root
 * 
 * Represents a customer in the AmanahFi platform following Islamic banking principles
 * and UAE regulatory requirements including CBUAE, VARA, and HSA compliance.
 */
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    private static final Pattern EMIRATES_ID_PATTERN = 
        Pattern.compile("^784-\\d{4}-\\d{7}-\\d{1}$");
    
    private static final Pattern UAE_MOBILE_PATTERN = 
        Pattern.compile("^\\+971(50|51|52|55|56)\\d{7}$");

    @Id
    private String customerId;

    @NotBlank
    @Column(unique = true)
    private String emiratesId;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String mobileNumber;

    @NotNull
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @NotNull
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private CustomerStatus status;

    @NotNull
    private LocalDateTime registrationDate;

    private LocalDateTime kycCompletionDate;

    private String kycRejectionReason;

    private String suspensionReason;

    // Islamic Banking Preferences
    private boolean islamicBankingPreferred = false;

    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference = NotificationPreference.EMAIL;

    private String preferredLanguage = "English";

    // Business Customer Details
    @Embedded
    private BusinessDetails businessDetails;

    // KYC Documents
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KycDocument> kycDocuments = new ArrayList<>();

    // Domain Events (for event sourcing)
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Creates a new individual customer
     */
    public static Customer create(String emiratesId, String fullName, String email, 
                                String mobileNumber, LocalDate dateOfBirth, CustomerType customerType) {
        Customer customer = new Customer();
        customer.customerId = generateCustomerId();
        customer.emiratesId = validateEmiratesId(emiratesId);
        customer.fullName = validateFullName(fullName);
        customer.email = validateEmail(email);
        customer.mobileNumber = validateMobileNumber(mobileNumber);
        customer.dateOfBirth = validateDateOfBirth(dateOfBirth);
        customer.customerType = customerType;
        customer.status = CustomerStatus.PENDING_KYC;
        customer.registrationDate = LocalDateTime.now();
        
        // Add domain event
        customer.addDomainEvent(new CustomerRegisteredEvent(customer.customerId, emiratesId, fullName));
        
        return customer;
    }

    /**
     * Creates a new business customer
     */
    public static Customer createBusiness(String authorizedPersonEmiratesId, String businessName, 
                                        String email, String mobileNumber, String tradeLicenseNumber, 
                                        LocalDate establishmentDate) {
        Customer customer = create(
            authorizedPersonEmiratesId, 
            businessName, 
            email, 
            mobileNumber, 
            establishmentDate, 
            CustomerType.BUSINESS
        );
        
        customer.businessDetails = new BusinessDetails(
            businessName,
            tradeLicenseNumber,
            establishmentDate,
            authorizedPersonEmiratesId
        );
        
        return customer;
    }

    /**
     * Sets Islamic banking preference
     */
    public Customer withIslamicBankingPreference(boolean preferred) {
        this.islamicBankingPreferred = preferred;
        return this;
    }

    /**
     * Submits KYC documents for verification
     */
    public void submitKycDocuments(KycDocument... documents) {
        if (status != CustomerStatus.PENDING_KYC && status != CustomerStatus.KYC_REJECTED) {
            throw new IllegalStateException("KYC documents can only be submitted when status is PENDING_KYC or KYC_REJECTED");
        }
        
        for (KycDocument document : documents) {
            document.assignToCustomer(this);
            this.kycDocuments.add(document);
        }
        
        addDomainEvent(new KycDocumentsSubmittedEvent(customerId, documents.length));
    }

    /**
     * Approves KYC after verification
     */
    public void approveKyc(String officerId, String notes) {
        if (status != CustomerStatus.PENDING_KYC) {
            throw new IllegalStateException("KYC can only be approved when status is PENDING_KYC");
        }
        
        this.status = CustomerStatus.ACTIVE;
        this.kycCompletionDate = LocalDateTime.now();
        this.kycRejectionReason = null;
        
        addDomainEvent(new CustomerKycApprovedEvent(customerId, officerId, notes));
    }

    /**
     * Rejects KYC with reason
     */
    public void rejectKyc(String officerId, String reason) {
        if (status != CustomerStatus.PENDING_KYC) {
            throw new IllegalStateException("KYC can only be rejected when status is PENDING_KYC");
        }
        
        this.status = CustomerStatus.KYC_REJECTED;
        this.kycRejectionReason = reason;
        
        addDomainEvent(new CustomerKycRejectedEvent(customerId, officerId, reason));
    }

    /**
     * Suspends customer account
     */
    public void suspend(String reason) {
        if (status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Only active customers can be suspended");
        }
        
        this.status = CustomerStatus.SUSPENDED;
        this.suspensionReason = reason;
        
        addDomainEvent(new CustomerSuspendedEvent(customerId, reason));
    }

    /**
     * Reactivates suspended customer
     */
    public void reactivate(String reason) {
        if (status != CustomerStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended customers can be reactivated");
        }
        
        this.status = CustomerStatus.ACTIVE;
        this.suspensionReason = null;
        
        addDomainEvent(new CustomerReactivatedEvent(customerId, reason));
    }

    /**
     * Updates customer preferences
     */
    public void updatePreferences(boolean islamicBankingPreferred, 
                                NotificationPreference notificationPreference, 
                                String preferredLanguage) {
        this.islamicBankingPreferred = islamicBankingPreferred;
        this.notificationPreference = notificationPreference;
        this.preferredLanguage = preferredLanguage;
        
        addDomainEvent(new CustomerPreferencesUpdatedEvent(customerId, islamicBankingPreferred));
    }

    // Business Logic Methods

    public boolean isKycCompleted() {
        return status == CustomerStatus.ACTIVE && kycCompletionDate != null;
    }

    public boolean canAccessConventionalProducts() {
        return !islamicBankingPreferred && isKycCompleted();
    }

    public boolean canAccessIslamicProducts() {
        return islamicBankingPreferred && isKycCompleted();
    }

    public boolean requiresShariaSupervisoryBoardApproval() {
        return islamicBankingPreferred;
    }

    public int getAgeInYears() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    // Validation Methods

    private static String validateEmiratesId(String emiratesId) {
        if (emiratesId == null || !EMIRATES_ID_PATTERN.matcher(emiratesId).matches()) {
            throw new IllegalArgumentException("Invalid Emirates ID format. Expected: 784-YYYY-NNNNNNN-C");
        }
        
        // Extract birth year and validate it's not in the future
        String birthYear = emiratesId.substring(4, 8);
        int year = Integer.parseInt(birthYear);
        if (year > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Emirates ID birth year cannot be in the future");
        }
        
        return emiratesId;
    }

    private static String validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new IllegalArgumentException("Full name must be between 2 and 100 characters");
        }
        return fullName.trim();
    }

    private static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        // Basic email validation - more comprehensive validation would be done by Jakarta validation
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.toLowerCase().trim();
    }

    private static String validateMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !UAE_MOBILE_PATTERN.matcher(mobileNumber).matches()) {
            throw new IllegalArgumentException("Invalid UAE mobile number format. Expected: +971XXXXXXXXX");
        }
        return mobileNumber;
    }

    private static LocalDate validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
        
        if (age > 120) {
            throw new IllegalArgumentException("Invalid date of birth - age cannot exceed 120 years");
        }
        
        return dateOfBirth;
    }

    private static String generateCustomerId() {
        return "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}