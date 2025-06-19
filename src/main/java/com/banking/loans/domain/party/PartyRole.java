package com.banking.loans.domain.party;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PartyRole Domain Model - Clean DDD Implementation
 * Represents role assignments for parties in the banking system
 * Pure domain model without infrastructure dependencies
 */
public class PartyRole {

    private Long id;
    private Party party;
    private String roleName;
    private String roleDescription;
    private RoleSource roleSource;
    private Boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Integer authorityLevel;
    private String businessUnit;
    private String geographicScope;
    private Long monetaryLimit;
    private String assignedBy;
    private String assignmentReason;
    private String approvalReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
    private LocalDateTime lastReviewedAt;
    private String reviewedBy;
    private LocalDateTime nextReviewDue;
    private Boolean requiresReview;
    private String complianceNotes;

    // Private constructor for domain creation
    private PartyRole(
        Party party,
        String roleName,
        String roleDescription,
        RoleSource roleSource,
        String assignedBy,
        String assignmentReason
    ) {
        this.party = Objects.requireNonNull(party, "Party cannot be null");
        this.roleName = Objects.requireNonNull(roleName, "Role name cannot be null");
        this.roleDescription = Objects.requireNonNull(roleDescription, "Role description cannot be null");
        this.roleSource = Objects.requireNonNull(roleSource, "Role source cannot be null");
        this.assignedBy = Objects.requireNonNull(assignedBy, "Assigned by cannot be null");
        this.assignmentReason = assignmentReason;
        this.active = true;
        this.effectiveFrom = LocalDateTime.now();
        this.authorityLevel = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.requiresReview = false;
    }

    // Factory method for creating new role assignments
    public static PartyRole create(
        Party party,
        String roleName,
        String roleDescription,
        RoleSource roleSource,
        String assignedBy,
        String assignmentReason
    ) {
        return new PartyRole(party, roleName, roleDescription, roleSource, assignedBy, assignmentReason);
    }

    // Business logic: Check if this role assignment is currently valid and active
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return active && 
               (effectiveFrom == null || !effectiveFrom.isAfter(now)) &&
               (effectiveTo == null || !effectiveTo.isBefore(now));
    }

    // Business logic: Check if this role has expired
    public boolean isExpired() {
        return effectiveTo != null && effectiveTo.isBefore(LocalDateTime.now());
    }

    // Business logic: Check if this role is not yet effective
    public boolean isNotYetEffective() {
        return effectiveFrom != null && effectiveFrom.isAfter(LocalDateTime.now());
    }

    // Business logic: Activate this role assignment
    public void activate() {
        this.active = true;
        if (this.effectiveFrom == null) {
            this.effectiveFrom = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Deactivate this role assignment
    public void deactivate(String reason) {
        this.active = false;
        this.complianceNotes = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Extend the validity of this role assignment
    public void extendValidity(LocalDateTime newEffectiveTo, String reason) {
        this.effectiveTo = newEffectiveTo;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Mark this role for review
    public void markForReview(String reason) {
        this.requiresReview = true;
        this.complianceNotes = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Complete review of this role assignment
    public void completeReview(String reviewedBy, LocalDateTime nextReviewDue) {
        this.lastReviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewedBy;
        this.nextReviewDue = nextReviewDue;
        this.requiresReview = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Check if this role requires review
    public boolean requiresReview() {
        if (requiresReview != null && requiresReview) {
            return true;
        }
        return nextReviewDue != null && nextReviewDue.isBefore(LocalDateTime.now());
    }

    // Business logic: Check if this role has sufficient authority for a given limit
    public boolean hasAuthorityFor(Long amount) {
        return monetaryLimit == null || (amount != null && amount <= monetaryLimit);
    }

    // Business logic: Update monetary limit with authorization
    public void updateMonetaryLimit(Long newLimit, String authorizedBy, String reason) {
        this.monetaryLimit = newLimit;
        this.assignedBy = authorizedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Party getParty() { return party; }
    public String getRoleName() { return roleName; }
    public String getRoleDescription() { return roleDescription; }
    public RoleSource getRoleSource() { return roleSource; }
    public Boolean getActive() { return active; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public Integer getAuthorityLevel() { return authorityLevel; }
    public String getBusinessUnit() { return businessUnit; }
    public String getGeographicScope() { return geographicScope; }
    public Long getMonetaryLimit() { return monetaryLimit; }
    public String getAssignedBy() { return assignedBy; }
    public String getAssignmentReason() { return assignmentReason; }
    public String getApprovalReference() { return approvalReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public Long getVersion() { return version; }
    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public LocalDateTime getNextReviewDue() { return nextReviewDue; }
    public Boolean getRequiresReview() { return requiresReview; }
    public String getComplianceNotes() { return complianceNotes; }

    // Public setters for reconstruction from persistence
    public void setId(Long id) { this.id = id; }
    public void setParty(Party party) { this.party = party; }
    public void setRoleName(String roleName) { this.roleName = roleName; this.updatedAt = LocalDateTime.now(); }
    public void setRoleDescription(String roleDescription) { this.roleDescription = roleDescription; this.updatedAt = LocalDateTime.now(); }
    public void setRoleSource(RoleSource roleSource) { this.roleSource = roleSource; }
    public void setActive(Boolean active) { this.active = active; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
    public void setAuthorityLevel(Integer authorityLevel) { this.authorityLevel = authorityLevel; this.updatedAt = LocalDateTime.now(); }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    public void setGeographicScope(String geographicScope) { this.geographicScope = geographicScope; }
    public void setMonetaryLimit(Long monetaryLimit) { this.monetaryLimit = monetaryLimit; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
    public void setApprovalReference(String approvalReference) { this.approvalReference = approvalReference; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setVersion(Long version) { this.version = version; }
    public void setLastReviewedAt(LocalDateTime lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setNextReviewDue(LocalDateTime nextReviewDue) { this.nextReviewDue = nextReviewDue; }
    public void setRequiresReview(Boolean requiresReview) { this.requiresReview = requiresReview; }
    public void setComplianceNotes(String complianceNotes) { this.complianceNotes = complianceNotes; }

    public boolean isActive() {
        return isCurrentlyActive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartyRole that = (PartyRole) o;
        return Objects.equals(party, that.party) && Objects.equals(roleName, that.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(party, roleName);
    }

    @Override
    public String toString() {
        return "PartyRole{" +
                "id=" + id +
                ", roleName='" + roleName + '\'' +
                ", roleSource=" + roleSource +
                ", active=" + active +
                '}';
    }
}