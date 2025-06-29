package com.bank.loanmanagement.infrastructure.persistence.jpa;

import com.bank.loanmanagement.domain.staff.EmployeeStatus;
import com.bank.loanmanagement.domain.staff.UnderwriterSpecialization;
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
 * JPA Infrastructure Entity for Underwriter
 * 
 * This class handles ONLY infrastructure concerns (persistence mapping).
 * Domain logic is kept in the pure domain model.
 * 
 * Architecture Compliance:
 * ✅ Hexagonal Architecture: Infrastructure adapter (JPA) separated from domain
 * ✅ Clean Code: Single responsibility for persistence mapping
 * ✅ DDD: Infrastructure layer properly separated from domain
 */
@Entity
@Table(name = "underwriters", indexes = {
    @Index(name = "idx_underwriters_specialization", columnList = "specialization"),
    @Index(name = "idx_underwriters_status", columnList = "status"),
    @Index(name = "idx_underwriters_approval_limit", columnList = "approval_limit"),
    @Index(name = "idx_underwriters_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnderwriterJpaEntity {
    
    @Id
    @Column(name = "underwriter_id", length = 20)
    @NotBlank(message = "Underwriter ID is required")
    @Pattern(regexp = "^UW\\d{3}$", message = "Underwriter ID must follow pattern UW###")
    private String underwriterId;
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false, length = 50)
    @NotNull(message = "Specialization is required")
    private UnderwriterSpecialization specialization;
    
    @Column(name = "years_experience", nullable = false)
    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 50, message = "Years of experience cannot exceed 50")
    private Integer yearsExperience;
    
    @Column(name = "approval_limit", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Approval limit is required")
    @DecimalMin(value = "1000.00", message = "Approval limit must be at least $1,000")
    @DecimalMax(value = "10000000.00", message = "Approval limit cannot exceed $10,000,000")
    private BigDecimal approvalLimit;
    
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnderwriterJpaEntity)) return false;
        UnderwriterJpaEntity that = (UnderwriterJpaEntity) o;
        return underwriterId != null && underwriterId.equals(that.underwriterId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "UnderwriterJpaEntity{" +
                "underwriterId='" + underwriterId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", specialization=" + specialization +
                ", approvalLimit=" + approvalLimit +
                ", status=" + status +
                '}';
    }
}