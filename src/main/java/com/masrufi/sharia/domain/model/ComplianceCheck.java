package com.masrufi.sharia.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Compliance Check Entity
 * Tracks Sharia compliance validation for each financing
 */
@Entity
@Table(name = "compliance_checks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financing_id", nullable = false)
    private IslamicFinancing financing;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceStatus status;
    
    @Column(nullable = false)
    private String scholarName;
    
    @Column(length = 1000)
    private String notes;
    
    @Column(length = 500)
    private String fatwaBasis; // Religious ruling basis
    
    @Column
    private String certificateHash; // Blockchain certificate
    
    @Column
    private LocalDateTime expiryDate;
    
    @CreationTimestamp
    private LocalDateTime checkedAt;
    
    @Column
    private String checkedBy;
    
    // Business Methods
    
    /**
     * Check if compliance is still valid
     */
    public boolean isValid() {
        return this.status == ComplianceStatus.APPROVED && 
               (this.expiryDate == null || this.expiryDate.isAfter(LocalDateTime.now()));
    }
    
    /**
     * Check if compliance is expired
     */
    public boolean isExpired() {
        return this.expiryDate != null && this.expiryDate.isBefore(LocalDateTime.now());
    }
    
    /**
     * Get days until expiry
     */
    public long getDaysUntilExpiry() {
        if (this.expiryDate == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(), this.expiryDate
        );
    }
}

enum ComplianceType {
    SHARIA_SCREENING("Initial Sharia Screening"),
    ASSET_VERIFICATION("Asset Backing Verification"),
    PURPOSE_VALIDATION("Purpose Validation"),
    COUNTERPARTY_CHECK("Counterparty Compliance"),
    ONGOING_MONITORING("Ongoing Compliance Monitoring"),
    ANNUAL_REVIEW("Annual Sharia Review");
    
    private final String description;
    
    ComplianceType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

enum ComplianceStatus {
    PENDING("Pending Review"),
    UNDER_REVIEW("Under Sharia Board Review"),
    APPROVED("Sharia Compliant"),
    REJECTED("Non-Compliant"),
    CONDITIONAL("Conditionally Approved"),
    EXPIRED("Compliance Expired");
    
    private final String description;
    
    ComplianceStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}