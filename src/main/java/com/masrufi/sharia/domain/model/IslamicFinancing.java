package com.masrufi.sharia.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Islamic Financing Entity
 * Represents Sharia-compliant financing arrangements
 * 
 * Supported Models:
 * - MURABAHA: Cost-plus financing
 * - MUSHARAKAH: Partnership financing
 * - IJARAH: Lease financing
 * - SALAM: Forward sale financing
 * - ISTISNA: Manufacturing financing
 * - QARD_HASSAN: Benevolent loan
 */
@Entity
@Table(name = "islamic_financing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IslamicFinancing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String financingReference;
    
    @Column(nullable = false)
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancingType financingType;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal profitAmount;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency paymentFrequency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancingStatus status;
    
    @Column(nullable = false)
    private LocalDateTime startDate;
    
    @Column(nullable = false)
    private LocalDateTime maturityDate;
    
    @Column(nullable = false)
    private LocalDateTime nextPaymentDate;
    
    @Column(nullable = false)
    private Boolean isAssetBacked;
    
    @Column(length = 1000)
    private String assetDetails;
    
    @Column(length = 500)
    private String shariaCertificateHash;
    
    @Column(nullable = false)
    private String blockchainNetwork;
    
    @Column
    private String smartContractAddress;
    
    @Column(length = 100)
    private String cryptoCurrency;
    
    @Column(precision = 19, scale = 8)
    private BigDecimal cryptoAmount;
    
    @OneToMany(mappedBy = "financing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentSchedule> paymentSchedules = new ArrayList<>();
    
    @OneToMany(mappedBy = "financing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ComplianceCheck> complianceChecks = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column
    private String createdBy;
    
    @Column
    private String lastModifiedBy;
    
    // Business Methods
    
    /**
     * Calculate total financing cost (profit amount)
     * Based on Sharia-compliant models, not interest
     */
    public BigDecimal calculateProfitAmount() {
        switch (this.financingType) {
            case MURABAHA:
                // Cost-plus financing: disclosed profit margin
                return this.principalAmount.multiply(BigDecimal.valueOf(0.05)); // 5% example
            case MUSHARAKAH:
                // Partnership: profit sharing based on contribution
                return this.principalAmount.multiply(BigDecimal.valueOf(0.03)); // 3% example
            case IJARAH:
                // Lease: rental income
                return this.principalAmount.multiply(BigDecimal.valueOf(0.06)); // 6% example
            case SALAM:
                // Forward sale: price difference
                return this.principalAmount.multiply(BigDecimal.valueOf(0.02)); // 2% example
            case QARD_HASSAN:
                // Benevolent loan: no profit
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculate payment frequency multiplier
     */
    public int getPaymentFrequencyMultiplier() {
        switch (this.paymentFrequency) {
            case HOURLY:
                return 24 * 365; // Hours per year
            case DAILY:
                return 365; // Days per year
            case WEEKLY:
                return 52; // Weeks per year
            case MONTHLY:
                return 12; // Months per year
            case QUARTERLY:
                return 4; // Quarters per year
            case ANNUAL:
                return 1; // Years
            case END_OF_TERM:
                return 1; // Single payment
            default:
                return 12; // Default to monthly
        }
    }
    
    /**
     * Check if financing is Sharia compliant
     */
    public boolean isShariaCompliant() {
        return this.isAssetBacked && 
               this.shariaCertificateHash != null && 
               !this.shariaCertificateHash.isEmpty() &&
               this.financingType != null;
    }
    
    /**
     * Get remaining financing amount
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal paidAmount = this.paymentSchedules.stream()
            .filter(PaymentSchedule::isPaid)
            .map(PaymentSchedule::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return this.totalAmount.subtract(paidAmount);
    }
    
    /**
     * Check if financing is overdue
     */
    public boolean isOverdue() {
        return this.nextPaymentDate.isBefore(LocalDateTime.now()) && 
               this.status == FinancingStatus.ACTIVE;
    }
}

enum FinancingType {
    MURABAHA("Cost-Plus Financing"),
    MUSHARAKAH("Partnership Financing"), 
    IJARAH("Lease Financing"),
    SALAM("Forward Sale Financing"),
    ISTISNA("Manufacturing Financing"),
    QARD_HASSAN("Benevolent Loan");
    
    private final String description;
    
    FinancingType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

enum PaymentFrequency {
    HOURLY("Every Hour"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    ANNUAL("Annually"),
    END_OF_TERM("End of Term");
    
    private final String description;
    
    PaymentFrequency(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

enum FinancingStatus {
    PENDING_APPROVAL("Pending Sharia Board Approval"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    DEFAULTED("Defaulted"),
    RESTRUCTURED("Restructured"),
    CANCELLED("Cancelled");
    
    private final String description;
    
    FinancingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}