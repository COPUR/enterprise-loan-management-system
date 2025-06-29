package com.bank.loanmanagement.infrastructure.anticorruption;

import com.bank.loanmanagement.domain.shared.CustomerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Anti-Corruption Layer for Customer Management Context
 * 
 * Protects the Loan Management domain from external Customer context changes
 * by translating external customer data into our domain concepts.
 * 
 * Architecture Compliance:
 * ✅ Hexagonal Architecture: Anti-corruption layer pattern implementation
 * ✅ Clean Code: Single responsibility for external context translation
 * ✅ DDD: Bounded context protection and translation
 * ✅ Type Safety: Strong typing with domain value objects
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerContextAdapter {
    
    private final ExternalCustomerService externalCustomerService;
    
    /**
     * Get customer profile from external Customer Management context
     * Translates external customer data to our domain model
     */
    public Optional<CustomerProfile> getCustomerProfile(Long customerId) {
        log.debug("Fetching customer profile for ID: {}", customerId);
        
        try {
            Optional<ExternalCustomer> externalCustomer = externalCustomerService.findById(customerId);
            
            if (externalCustomer.isEmpty()) {
                log.debug("Customer not found in external system: {}", customerId);
                return Optional.empty();
            }
            
            CustomerProfile profile = translateToCustomerProfile(externalCustomer.get());
            log.debug("Successfully translated customer profile for ID: {}", customerId);
            
            return Optional.of(profile);
            
        } catch (Exception e) {
            log.error("Failed to fetch customer profile for ID: {}", customerId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Validate customer exists and is active
     */
    public boolean isCustomerActiveAndValid(Long customerId) {
        log.debug("Validating customer status for ID: {}", customerId);
        
        try {
            Optional<ExternalCustomer> customer = externalCustomerService.findById(customerId);
            
            if (customer.isEmpty()) {
                log.debug("Customer not found: {}", customerId);
                return false;
            }
            
            boolean isValid = isCustomerValid(customer.get());
            log.debug("Customer {} validation result: {}", customerId, isValid);
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating customer: {}", customerId, e);
            return false;
        }
    }
    
    /**
     * Get customer credit score from external system
     */
    public Optional<Integer> getCustomerCreditScore(Long customerId) {
        log.debug("Fetching credit score for customer: {}", customerId);
        
        try {
            return externalCustomerService.getCreditScore(customerId);
            
        } catch (Exception e) {
            log.error("Failed to fetch credit score for customer: {}", customerId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if customer has sufficient income for loan amount
     */
    public boolean hassufficientIncome(Long customerId, java.math.BigDecimal loanAmount) {
        log.debug("Checking income sufficiency for customer: {} and amount: {}", customerId, loanAmount);
        
        try {
            Optional<CustomerProfile> profile = getCustomerProfile(customerId);
            
            if (profile.isEmpty()) {
                return false;
            }
            
            // Business rule: Monthly income should be at least 1/36 of loan amount
            java.math.BigDecimal monthlyIncome = profile.get().getMonthlyIncome();
            java.math.BigDecimal requiredIncome = loanAmount.divide(new java.math.BigDecimal("36"), 2, java.math.RoundingMode.HALF_UP);
            
            boolean hasSufficientIncome = monthlyIncome.compareTo(requiredIncome) >= 0;
            log.debug("Income sufficiency check result: {} (income: {}, required: {})", 
                     hasSufficientIncome, monthlyIncome, requiredIncome);
            
            return hasSufficientIncome;
            
        } catch (Exception e) {
            log.error("Error checking income sufficiency for customer: {}", customerId, e);
            return false;
        }
    }
    
    /**
     * Private method to translate external customer data to domain model
     */
    private CustomerProfile translateToCustomerProfile(ExternalCustomer externalCustomer) {
        return CustomerProfile.builder()
            .customerId(externalCustomer.getId())
            .firstName(externalCustomer.getPersonalInfo().getFirstName())
            .lastName(externalCustomer.getPersonalInfo().getLastName())
            .email(externalCustomer.getContactInfo().getEmail())
            .phone(externalCustomer.getContactInfo().getPhone())
            .monthlyIncome(externalCustomer.getFinancialInfo().getMonthlyIncome())
            .employmentStatus(translateEmploymentStatus(externalCustomer.getEmploymentInfo().getStatus()))
            .creditScore(externalCustomer.getCreditInfo().getScore())
            .dateOfBirth(externalCustomer.getPersonalInfo().getDateOfBirth())
            .address(translateAddress(externalCustomer.getContactInfo().getAddress()))
            .isActive(externalCustomer.getStatus().equals("ACTIVE"))
            .build();
    }
    
    /**
     * Private method to validate customer data
     */
    private boolean isCustomerValid(ExternalCustomer customer) {
        return customer.getStatus().equals("ACTIVE") &&
               customer.getPersonalInfo() != null &&
               customer.getFinancialInfo() != null &&
               customer.getCreditInfo() != null &&
               customer.getCreditInfo().getScore() != null &&
               customer.getCreditInfo().getScore() > 0;
    }
    
    /**
     * Translate external employment status to domain employment status
     */
    private CustomerProfile.EmploymentStatus translateEmploymentStatus(String externalStatus) {
        return switch (externalStatus.toUpperCase()) {
            case "EMPLOYED", "FULL_TIME" -> CustomerProfile.EmploymentStatus.EMPLOYED;
            case "SELF_EMPLOYED" -> CustomerProfile.EmploymentStatus.SELF_EMPLOYED;
            case "UNEMPLOYED" -> CustomerProfile.EmploymentStatus.UNEMPLOYED;
            case "RETIRED" -> CustomerProfile.EmploymentStatus.RETIRED;
            case "STUDENT" -> CustomerProfile.EmploymentStatus.STUDENT;
            default -> CustomerProfile.EmploymentStatus.OTHER;
        };
    }
    
    /**
     * Translate external address to domain address
     */
    private String translateAddress(ExternalAddress externalAddress) {
        if (externalAddress == null) {
            return null;
        }
        
        return String.format("%s, %s, %s %s", 
                           externalAddress.getStreet(),
                           externalAddress.getCity(),
                           externalAddress.getState(),
                           externalAddress.getZipCode());
    }
}

/**
 * External Customer Service Interface
 * Represents the external Customer Management system
 */
interface ExternalCustomerService {
    Optional<ExternalCustomer> findById(Long customerId);
    Optional<Integer> getCreditScore(Long customerId);
}

/**
 * External Customer Data Transfer Objects
 * These represent the external system's data structure
 */
class ExternalCustomer {
    private Long id;
    private String status;
    private ExternalPersonalInfo personalInfo;
    private ExternalContactInfo contactInfo;
    private ExternalFinancialInfo financialInfo;
    private ExternalEmploymentInfo employmentInfo;
    private ExternalCreditInfo creditInfo;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ExternalPersonalInfo getPersonalInfo() { return personalInfo; }
    public void setPersonalInfo(ExternalPersonalInfo personalInfo) { this.personalInfo = personalInfo; }
    public ExternalContactInfo getContactInfo() { return contactInfo; }
    public void setContactInfo(ExternalContactInfo contactInfo) { this.contactInfo = contactInfo; }
    public ExternalFinancialInfo getFinancialInfo() { return financialInfo; }
    public void setFinancialInfo(ExternalFinancialInfo financialInfo) { this.financialInfo = financialInfo; }
    public ExternalEmploymentInfo getEmploymentInfo() { return employmentInfo; }
    public void setEmploymentInfo(ExternalEmploymentInfo employmentInfo) { this.employmentInfo = employmentInfo; }
    public ExternalCreditInfo getCreditInfo() { return creditInfo; }
    public void setCreditInfo(ExternalCreditInfo creditInfo) { this.creditInfo = creditInfo; }
}

class ExternalPersonalInfo {
    private String firstName;
    private String lastName;
    private java.time.LocalDate dateOfBirth;
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}

class ExternalContactInfo {
    private String email;
    private String phone;
    private ExternalAddress address;
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public ExternalAddress getAddress() { return address; }
    public void setAddress(ExternalAddress address) { this.address = address; }
}

class ExternalFinancialInfo {
    private java.math.BigDecimal monthlyIncome;
    private java.math.BigDecimal totalAssets;
    private java.math.BigDecimal totalLiabilities;
    
    public java.math.BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(java.math.BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public java.math.BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(java.math.BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public java.math.BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(java.math.BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
}

class ExternalEmploymentInfo {
    private String status;
    private String employer;
    private Integer yearsEmployed;
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }
    public Integer getYearsEmployed() { return yearsEmployed; }
    public void setYearsEmployed(Integer yearsEmployed) { this.yearsEmployed = yearsEmployed; }
}

class ExternalCreditInfo {
    private Integer score;
    private String rating;
    
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
}

class ExternalAddress {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
}