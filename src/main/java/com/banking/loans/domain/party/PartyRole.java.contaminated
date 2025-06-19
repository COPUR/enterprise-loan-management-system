package com.banking.loans.domain.party;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PartyRole Domain Entity - Banking DDD Value Object
 * Represents role assignments for parties in the banking system
 * These roles are authoritative and override external role assignments
 */
@Entity
@Table(name = "party_roles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "role_name"}),
       indexes = {
           @Index(name = "idx_party_role_name", columnList = "roleName"),
           @Index(name = "idx_party_role_active", columnList = "active"),
           @Index(name = "idx_party_role_effective", columnList = "effectiveFrom, effectiveTo")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"party", "roleName"})
public class PartyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the party that has this role
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    /**
     * Name of the role - maps to banking system roles
     * Examples: BANKING_ADMIN, LOAN_OFFICER, COMPLIANCE_OFFICER, etc.
     */
    @Column(nullable = false)
    private String roleName;

    /**
     * Human-readable description of the role
     */
    @Column(nullable = false)
    private String roleDescription;

    /**
     * Source of the role assignment - DATABASE, LDAP, KEYCLOAK
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleSource roleSource;

    /**
     * Whether this role assignment is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * When this role assignment becomes effective
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime effectiveFrom = LocalDateTime.now();

    /**
     * When this role assignment expires (null = never expires)
     */
    private LocalDateTime effectiveTo;

    /**
     * Authority level for this role (1-10, higher = more authority)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer authorityLevel = 1;

    /**
     * Business unit or department this role applies to
     */
    private String businessUnit;

    /**
     * Geographic scope of this role (branch, region, etc.)
     */
    private String geographicScope;

    /**
     * Monetary limit for this role (for approval authorities)
     */
    private Long monetaryLimit;

    /**
     * Who assigned this role
     */
    @Column(nullable = false)
    private String assignedBy;

    /**
     * Reason for role assignment
     */
    private String assignmentReason;

    /**
     * Approval reference for role assignment
     */
    private String approvalReference;

    /**
     * Audit fields for compliance tracking
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;

    /**
     * Compliance tracking fields
     */
    private LocalDateTime lastReviewedAt;
    private String reviewedBy;
    private LocalDateTime nextReviewDue;
    private Boolean requiresReview;
    private String complianceNotes;

    /**
     * Check if this role assignment is currently valid and active
     */
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return active && 
               (effectiveFrom == null || !effectiveFrom.isAfter(now)) &&
               (effectiveTo == null || !effectiveTo.isBefore(now));
    }

    /**
     * Check if this role has expired
     */
    public boolean isExpired() {
        return effectiveTo != null && effectiveTo.isBefore(LocalDateTime.now());
    }

    /**
     * Check if this role is not yet effective
     */
    public boolean isNotYetEffective() {
        return effectiveFrom != null && effectiveFrom.isAfter(LocalDateTime.now());
    }

    /**
     * Activate this role assignment
     */
    public void activate() {
        this.active = true;
        if (this.effectiveFrom == null) {
            this.effectiveFrom = LocalDateTime.now();
        }
    }

    /**
     * Deactivate this role assignment
     */
    public void deactivate(String reason) {
        this.active = false;
        this.complianceNotes = reason;
    }

    /**
     * Extend the validity of this role assignment
     */
    public void extendValidity(LocalDateTime newEffectiveTo, String reason) {
        this.effectiveTo = newEffectiveTo;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
    }

    /**
     * Mark this role for review
     */
    public void markForReview(String reason) {
        this.requiresReview = true;
        this.complianceNotes = reason;
    }

    /**
     * Complete review of this role assignment
     */
    public void completeReview(String reviewedBy, LocalDateTime nextReviewDue) {
        this.lastReviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewedBy;
        this.nextReviewDue = nextReviewDue;
        this.requiresReview = false;
    }

    /**
     * Check if this role requires review
     */
    public boolean requiresReview() {
        if (requiresReview != null && requiresReview) {
            return true;
        }
        return nextReviewDue != null && nextReviewDue.isBefore(LocalDateTime.now());
    }

    /**
     * Check if this role has sufficient authority for a given limit
     */
    public boolean hasAuthorityFor(Long amount) {
        return monetaryLimit == null || (amount != null && amount <= monetaryLimit);
    }
    
    /**
     * Alias for isCurrentlyActive() to match the usage in Party.java
     */
    public boolean isActive() {
        return isCurrentlyActive();
    }
}