package com.bank.loanmanagement.domain.staff;

import com.bank.loanmanagement.domain.shared.AggregateRoot;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Underwriter Domain Entity (Pure Domain Model)
 * 
 * Represents underwriting staff with specialization and approval authority.
 * Follows DDD principles with proper business rules and validation.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Intention-revealing names and business methods
 * ✅ Hexagonal Architecture: Pure domain model without infrastructure concerns
 * ✅ DDD: Rich domain entity with business logic and validation
 * ✅ Type Safety: Strong typing with BigDecimal for financial amounts
 * 
 * This is a PURE DOMAIN MODEL - no infrastructure annotations.
 * Infrastructure mapping is handled by separate JPA entities.
 */
@Getter
public class Underwriter extends AggregateRoot<String> {
    
    // ✅ PURE DOMAIN MODEL - No infrastructure annotations
    // Infrastructure mapping handled by separate JPA entities
    
    private final String underwriterId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final UnderwriterSpecialization specialization;
    private final Integer yearsExperience;
    private final BigDecimal approvalLimit;
    private EmployeeStatus status;
    private final LocalDate hireDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    
    /**
     * Private constructor to enforce factory method usage
     */
    private Underwriter(String underwriterId, String firstName, String lastName, String email,
                       String phone, UnderwriterSpecialization specialization, Integer yearsExperience,
                       BigDecimal approvalLimit, EmployeeStatus status, LocalDate hireDate,
                       LocalDateTime createdAt, LocalDateTime updatedAt, Integer version) {
        
        // Validate required business rules
        this.underwriterId = validateUnderwriterId(underwriterId);
        this.firstName = validateFirstName(firstName);
        this.lastName = validateLastName(lastName);
        this.email = validateEmail(email);
        this.phone = phone; // Optional field
        this.specialization = Objects.requireNonNull(specialization, "Specialization is required");
        this.yearsExperience = validateYearsExperience(yearsExperience);
        this.approvalLimit = validateApprovalLimit(approvalLimit);
        this.status = Objects.requireNonNull(status, "Status is required");
        this.hireDate = validateHireDate(hireDate);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.version = version != null ? version : 0;
    }
    
    /**
     * Factory method to create a new underwriter
     */
    public static Underwriter create(String underwriterId, String firstName, String lastName,
                                   String email, String phone, UnderwriterSpecialization specialization,
                                   Integer yearsExperience, BigDecimal approvalLimit, LocalDate hireDate) {
        
        return new Underwriter(underwriterId, firstName, lastName, email, phone,
                             specialization, yearsExperience, approvalLimit,
                             EmployeeStatus.ACTIVE, hireDate, LocalDateTime.now(),
                             LocalDateTime.now(), 0);
    }
    
    /**
     * Factory method for reconstruction from infrastructure
     */
    public static Underwriter reconstruct(String underwriterId, String firstName, String lastName,
                                        String email, String phone, UnderwriterSpecialization specialization,
                                        Integer yearsExperience, BigDecimal approvalLimit, EmployeeStatus status,
                                        LocalDate hireDate, LocalDateTime createdAt, LocalDateTime updatedAt,
                                        Integer version) {
        
        return new Underwriter(underwriterId, firstName, lastName, email, phone,
                             specialization, yearsExperience, approvalLimit, status,
                             hireDate, createdAt, updatedAt, version);
    }
    
    /**
     * Business method to check if underwriter can approve a loan amount
     */
    public boolean canApprove(BigDecimal loanAmount) {
        if (status != EmployeeStatus.ACTIVE) {
            return false;
        }
        return loanAmount != null && approvalLimit.compareTo(loanAmount) >= 0;
    }
    
    /**
     * Business method to check if underwriter specializes in a loan type
     */
    public boolean specializesIn(String loanType) {
        if (specialization == null || loanType == null) {
            return false;
        }
        
        return switch (specialization) {
            case PERSONAL_LOANS -> "PERSONAL".equalsIgnoreCase(loanType);
            case BUSINESS_LOANS -> "BUSINESS".equalsIgnoreCase(loanType);
            case MORTGAGES -> "MORTGAGE".equalsIgnoreCase(loanType);
        };
    }
    
    /**
     * Business method to check if available for new loans
     */
    public boolean isAvailableForNewLoans() {
        return status == EmployeeStatus.ACTIVE;
    }
    
    /**
     * Business method to get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Business method to check if underwriter is senior (5+ years experience)
     */
    public boolean isSenior() {
        return yearsExperience != null && yearsExperience >= 5;
    }
    
    /**
     * Business method to get experience level description
     */
    public String getExperienceLevel() {
        if (yearsExperience == null) {
            return "Unknown";
        }
        
        if (yearsExperience < 2) {
            return "Junior";
        } else if (yearsExperience < 5) {
            return "Mid-Level";
        } else if (yearsExperience < 10) {
            return "Senior";
        } else {
            return "Expert";
        }
    }
    
    /**
     * Business method to validate approval limit for specialization
     */
    public boolean hasValidApprovalLimitForSpecialization() {
        if (specialization == null || approvalLimit == null) {
            return false;
        }
        
        return switch (specialization) {
            case PERSONAL_LOANS -> approvalLimit.compareTo(new BigDecimal("100000")) <= 0;
            case BUSINESS_LOANS -> approvalLimit.compareTo(new BigDecimal("5000000")) <= 0;
            case MORTGAGES -> approvalLimit.compareTo(new BigDecimal("10000000")) <= 0;
        };
    }
    
    /**
     * Business method to update status
     */
    public void updateStatus(EmployeeStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status is required");
        
        if (this.status != newStatus) {
            this.status = newStatus;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Business method to check if underwriter can handle urgent cases
     */
    public boolean canHandleUrgentCases() {
        return isSenior() && status == EmployeeStatus.ACTIVE;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Underwriter)) return false;
        Underwriter that = (Underwriter) o;
        return underwriterId != null && underwriterId.equals(that.underwriterId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    /**
     * Domain validation methods
     */
    private static String validateUnderwriterId(String underwriterId) {
        Objects.requireNonNull(underwriterId, "Underwriter ID is required");
        if (!underwriterId.matches("^UW\\d{3}$")) {
            throw new IllegalArgumentException("Underwriter ID must follow pattern UW###");
        }
        return underwriterId;
    }
    
    private static String validateFirstName(String firstName) {
        Objects.requireNonNull(firstName, "First name is required");
        if (firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (firstName.length() > 100) {
            throw new IllegalArgumentException("First name must not exceed 100 characters");
        }
        return firstName.trim();
    }
    
    private static String validateLastName(String lastName) {
        Objects.requireNonNull(lastName, "Last name is required");
        if (lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }
        if (lastName.length() > 100) {
            throw new IllegalArgumentException("Last name must not exceed 100 characters");
        }
        return lastName.trim();
    }
    
    private static String validateEmail(String email) {
        Objects.requireNonNull(email, "Email is required");
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email must be valid");
        }
        if (email.length() > 255) {
            throw new IllegalArgumentException("Email must not exceed 255 characters");
        }
        return email.toLowerCase();
    }
    
    private static Integer validateYearsExperience(Integer yearsExperience) {
        Objects.requireNonNull(yearsExperience, "Years of experience is required");
        if (yearsExperience < 0) {
            throw new IllegalArgumentException("Years of experience cannot be negative");
        }
        if (yearsExperience > 50) {
            throw new IllegalArgumentException("Years of experience cannot exceed 50");
        }
        return yearsExperience;
    }
    
    private static BigDecimal validateApprovalLimit(BigDecimal approvalLimit) {
        Objects.requireNonNull(approvalLimit, "Approval limit is required");
        if (approvalLimit.compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("Approval limit must be at least $1,000");
        }
        if (approvalLimit.compareTo(new BigDecimal("10000000")) > 0) {
            throw new IllegalArgumentException("Approval limit cannot exceed $10,000,000");
        }
        return approvalLimit;
    }
    
    private static LocalDate validateHireDate(LocalDate hireDate) {
        Objects.requireNonNull(hireDate, "Hire date is required");
        if (hireDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Hire date cannot be in the future");
        }
        return hireDate;
    }
    
    /**
     * Implement AggregateRoot's getId() method
     */
    @Override
    public String getId() {
        return underwriterId;
    }
    
    @Override
    public String toString() {
        return "Underwriter{" +
                "underwriterId='" + underwriterId + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", specialization=" + specialization +
                ", approvalLimit=" + approvalLimit +
                ", status=" + status +
                '}';
    }
}

/**
 * Underwriter specialization enum
 */
enum UnderwriterSpecialization {
    PERSONAL_LOANS("Personal Loans"),
    BUSINESS_LOANS("Business Loans"), 
    MORTGAGES("Mortgages");
    
    private final String displayName;
    
    UnderwriterSpecialization(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

/**
 * Employee status enum
 */
enum EmployeeStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ON_LEAVE("On Leave");
    
    private final String displayName;
    
    EmployeeStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}