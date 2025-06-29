package com.bank.loanmanagement.domain.staff;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan Officer Domain Entity
 * 
 * Represents sales staff responsible for loan origination with regional assignment
 * and commission tracking. Follows DDD principles with proper business rules.
 * 
 * Architecture Guardrails Compliance:
 * ✅ Request Parsing: N/A (Domain Entity)
 * ✅ Validation: Jakarta Bean Validation with business rules
 * ✅ Response Types: N/A (Domain Entity)
 * ✅ Type Safety: Strong typing with BigDecimal for financial amounts
 * ✅ Dependency Inversion: Pure domain entity
 */
@Entity
@Table(name = "loan_officers", indexes = {
    @Index(name = "idx_loan_officers_region", columnList = "region"),
    @Index(name = "idx_loan_officers_status", columnList = "status"),
    @Index(name = "idx_loan_officers_portfolio_size", columnList = "portfolio_size"),
    @Index(name = "idx_loan_officers_email", columnList = "email"),
    @Index(name = "idx_loan_officers_commission_rate", columnList = "commission_rate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanOfficer {
    
    @Id
    @Column(name = "officer_id", length = 20)
    @NotBlank(message = "Loan Officer ID is required")
    @Pattern(regexp = "^LO\\d{3}$", message = "Loan Officer ID must follow pattern LO###")
    private String officerId;
    
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Column(name = "phone", length = 20)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    private String phone;
    
    @Column(name = "region", nullable = false, length = 50)
    @NotBlank(message = "Region is required")
    @Size(max = 50, message = "Region must not exceed 50 characters")
    private String region;
    
    @Column(name = "portfolio_size")
    @Min(value = 0, message = "Portfolio size cannot be negative")
    @Max(value = 1000, message = "Portfolio size cannot exceed 1000 loans")
    private Integer portfolioSize = 0;
    
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.0000", message = "Commission rate cannot be negative")
    @DecimalMax(value = "0.1000", message = "Commission rate cannot exceed 10%")
    private BigDecimal commissionRate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @NotNull(message = "Status is required")
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    
    @Column(name = "hire_date", nullable = false)
    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    private LocalDate hireDate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Integer version = 0;
    
    /**
     * Business method to check if loan officer is available for new loans
     */
    public boolean isAvailableForNewLoans() {
        return status == EmployeeStatus.ACTIVE && portfolioSize < getMaxPortfolioSize();
    }
    
    /**
     * Business method to get maximum portfolio size based on experience
     */
    public int getMaxPortfolioSize() {
        LocalDate now = LocalDate.now();
        long yearsOfService = hireDate.until(now).getYears();
        
        if (yearsOfService < 1) {
            return 50;  // Junior officers
        } else if (yearsOfService < 3) {
            return 150; // Mid-level officers
        } else if (yearsOfService < 7) {
            return 250; // Senior officers
        } else {
            return 400; // Expert officers
        }
    }
    
    /**
     * Business method to calculate commission for a loan amount
     */
    public BigDecimal calculateCommission(BigDecimal loanAmount) {
        if (loanAmount == null || commissionRate == null) {
            return BigDecimal.ZERO;
        }
        return loanAmount.multiply(commissionRate);
    }
    
    /**
     * Business method to add a loan to portfolio
     */
    public void addLoanToPortfolio() {
        if (!isAvailableForNewLoans()) {
            throw new IllegalStateException("Loan officer is not available for new loans");
        }
        this.portfolioSize = portfolioSize + 1;
    }
    
    /**
     * Business method to remove a loan from portfolio
     */
    public void removeLoanFromPortfolio() {
        if (portfolioSize > 0) {
            this.portfolioSize = portfolioSize - 1;
        }
    }
    
    /**
     * Business method to get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Business method to get years of service
     */
    public long getYearsOfService() {
        return hireDate.until(LocalDate.now()).getYears();
    }
    
    /**
     * Business method to get experience level
     */
    public String getExperienceLevel() {
        long yearsOfService = getYearsOfService();
        
        if (yearsOfService < 1) {
            return "Trainee";
        } else if (yearsOfService < 3) {
            return "Junior";
        } else if (yearsOfService < 7) {
            return "Senior";
        } else {
            return "Expert";
        }
    }
    
    /**
     * Business method to check if officer covers a specific region
     */
    public boolean coversRegion(String targetRegion) {
        if (region == null || targetRegion == null) {
            return false;
        }
        return region.equalsIgnoreCase(targetRegion);
    }
    
    /**
     * Business method to get portfolio utilization percentage
     */
    public double getPortfolioUtilization() {
        int maxSize = getMaxPortfolioSize();
        if (maxSize == 0) {
            return 0.0;
        }
        return (double) portfolioSize / maxSize * 100.0;
    }
    
    /**
     * Business method to check if portfolio is at capacity
     */
    public boolean isAtCapacity() {
        return portfolioSize >= getMaxPortfolioSize();
    }
    
    /**
     * Business method to validate commission rate for region
     */
    public boolean hasValidCommissionRateForRegion() {
        if (commissionRate == null || region == null) {
            return false;
        }
        
        // Different regions may have different commission structures
        return switch (region.toUpperCase()) {
            case "NORTHEAST", "WEST" -> 
                commissionRate.compareTo(new BigDecimal("0.0030")) <= 0; // High cost areas
            case "SOUTHEAST", "MIDWEST" -> 
                commissionRate.compareTo(new BigDecimal("0.0035")) <= 0; // Standard areas
            case "SOUTHWEST" -> 
                commissionRate.compareTo(new BigDecimal("0.0040")) <= 0; // Growth areas
            default -> 
                commissionRate.compareTo(new BigDecimal("0.0030")) <= 0; // Default
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanOfficer)) return false;
        LoanOfficer that = (LoanOfficer) o;
        return officerId != null && officerId.equals(that.officerId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "LoanOfficer{" +
                "officerId='" + officerId + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", region='" + region + '\'' +
                ", portfolioSize=" + portfolioSize +
                ", commissionRate=" + commissionRate +
                ", status=" + status +
                '}';
    }
}