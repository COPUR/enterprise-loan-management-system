package com.bank.loan.infrastructure.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer JPA Entity (Infrastructure Layer)
 * 
 * This entity handles persistence concerns and is separate from the domain model.
 * It maps between the database and the clean Customer domain model.
 */
@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class CustomerEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", unique = true, nullable = false)
    private String customerId;
    
    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Email
    @NotBlank
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @NotBlank
    @Column(name = "phone", nullable = false)
    private String phone;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "nationality")
    private String nationality;
    
    @Column(name = "occupation")
    private String occupation;
    
    @Column(name = "employer_name")
    private String employerName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Min(300)
    @Max(850)
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @DecimalMin("0.00")
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "existing_monthly_obligations", precision = 15, scale = 2)
    private BigDecimal existingMonthlyObligations;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private String status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    private String riskCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private String customerType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private String employmentType;
    
    @Column(name = "driving_license")
    private String drivingLicense;
    
    @Column(name = "business_license")
    private String businessLicense;
    
    @Column(name = "business_name")
    private String businessName;
    
    @Column(name = "years_in_business")
    private Integer yearsInBusiness;
    
    @Column(name = "religion_preference")
    private String religionPreference;
    
    @Column(name = "islamic_banking_preference")
    private Boolean islamicBankingPreference = false;
    
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "use_case_reference")
    private String useCaseReference;
    
    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}