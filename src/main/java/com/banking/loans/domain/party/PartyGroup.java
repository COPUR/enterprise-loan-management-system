package com.banking.loans.domain.party;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PartyGroup Domain Model - Clean DDD Implementation
 * Represents group memberships for parties in the banking system
 * Pure domain model without infrastructure dependencies
 */
public class PartyGroup {

    private Long id;
    private Party party;
    private String groupName;
    private String groupDescription;
    private GroupType groupType;
    private RoleSource groupSource;
    private Boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private GroupRole groupRole;
    private Integer priority;
    private String businessUnit;
    private String geographicScope;
    private String assignedBy;
    private String assignmentReason;
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
    private PartyGroup(
        Party party,
        String groupName,
        String groupDescription,
        GroupType groupType,
        RoleSource groupSource,
        String assignedBy,
        String assignmentReason
    ) {
        this.party = Objects.requireNonNull(party, "Party cannot be null");
        this.groupName = Objects.requireNonNull(groupName, "Group name cannot be null");
        this.groupDescription = Objects.requireNonNull(groupDescription, "Group description cannot be null");
        this.groupType = Objects.requireNonNull(groupType, "Group type cannot be null");
        this.groupSource = Objects.requireNonNull(groupSource, "Group source cannot be null");
        this.assignedBy = Objects.requireNonNull(assignedBy, "Assigned by cannot be null");
        this.assignmentReason = assignmentReason;
        this.active = true;
        this.effectiveFrom = LocalDateTime.now();
        this.groupRole = GroupRole.MEMBER;
        this.priority = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.requiresReview = false;
    }

    // Factory method for creating new group memberships
    public static PartyGroup create(
        Party party,
        String groupName,
        String groupDescription,
        GroupType groupType,
        RoleSource groupSource,
        String assignedBy,
        String assignmentReason
    ) {
        return new PartyGroup(party, groupName, groupDescription, groupType, groupSource, assignedBy, assignmentReason);
    }

    // Business logic: Check if this group membership is currently valid and active
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return active && 
               (effectiveFrom == null || !effectiveFrom.isAfter(now)) &&
               (effectiveTo == null || !effectiveTo.isBefore(now));
    }

    // Business logic: Check if this group membership has expired
    public boolean isExpired() {
        return effectiveTo != null && effectiveTo.isBefore(LocalDateTime.now());
    }

    // Business logic: Activate this group membership
    public void activate() {
        this.active = true;
        if (this.effectiveFrom == null) {
            this.effectiveFrom = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Deactivate this group membership
    public void deactivate(String reason) {
        this.active = false;
        this.complianceNotes = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Check if this party is a leader in this group
    public boolean isLeader() {
        return groupRole == GroupRole.LEADER || groupRole == GroupRole.ADMINISTRATOR;
    }

    // Business logic: Check if this party is an administrator of this group
    public boolean isAdministrator() {
        return groupRole == GroupRole.ADMINISTRATOR;
    }

    // Business logic: Promote party to leader role in this group
    public void promoteToLeader(String promotedBy, String reason) {
        this.groupRole = GroupRole.LEADER;
        this.assignedBy = promotedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Promote party to administrator role in this group
    public void promoteToAdministrator(String promotedBy, String reason) {
        this.groupRole = GroupRole.ADMINISTRATOR;
        this.assignedBy = promotedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Demote party to member role in this group
    public void demoteToMember(String demotedBy, String reason) {
        this.groupRole = GroupRole.MEMBER;
        this.assignedBy = demotedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Party getParty() { return party; }
    public String getGroupName() { return groupName; }
    public String getGroupDescription() { return groupDescription; }
    public GroupType getGroupType() { return groupType; }
    public RoleSource getGroupSource() { return groupSource; }
    public Boolean getActive() { return active; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public GroupRole getGroupRole() { return groupRole; }
    public Integer getPriority() { return priority; }
    public String getBusinessUnit() { return businessUnit; }
    public String getGeographicScope() { return geographicScope; }
    public String getAssignedBy() { return assignedBy; }
    public String getAssignmentReason() { return assignmentReason; }
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
    public void setGroupName(String groupName) { this.groupName = groupName; this.updatedAt = LocalDateTime.now(); }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; this.updatedAt = LocalDateTime.now(); }
    public void setGroupType(GroupType groupType) { this.groupType = groupType; this.updatedAt = LocalDateTime.now(); }
    public void setGroupSource(RoleSource groupSource) { this.groupSource = groupSource; }
    public void setActive(Boolean active) { this.active = active; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
    public void setGroupRole(GroupRole groupRole) { this.groupRole = groupRole; this.updatedAt = LocalDateTime.now(); }
    public void setPriority(Integer priority) { this.priority = priority; this.updatedAt = LocalDateTime.now(); }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    public void setGeographicScope(String geographicScope) { this.geographicScope = geographicScope; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
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
        PartyGroup that = (PartyGroup) o;
        return Objects.equals(party, that.party) && Objects.equals(groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(party, groupName);
    }

    @Override
    public String toString() {
        return "PartyGroup{" +
                "id=" + id +
                ", groupName='" + groupName + '\'' +
                ", groupType=" + groupType +
                ", active=" + active +
                '}';
    }
}