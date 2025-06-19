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
 * PartyGroup Domain Entity - Banking DDD Value Object
 * Represents group memberships for parties in the banking system
 */
@Entity
@Table(name = "party_groups", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "group_name"}),
       indexes = {
           @Index(name = "idx_party_group_name", columnList = "groupName"),
           @Index(name = "idx_party_group_active", columnList = "active"),
           @Index(name = "idx_party_group_type", columnList = "groupType")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"party", "groupName"})
public class PartyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the party that belongs to this group
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    /**
     * Name of the group
     * Examples: Banking Operations, Loan Officers, Compliance Team, etc.
     */
    @Column(nullable = false)
    private String groupName;

    /**
     * Human-readable description of the group
     */
    @Column(nullable = false)
    private String groupDescription;

    /**
     * Type of group - DEPARTMENT, TEAM, FUNCTIONAL, SECURITY, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType groupType;

    /**
     * Source of the group membership - DATABASE, LDAP, KEYCLOAK
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleSource groupSource;

    /**
     * Whether this group membership is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * When this group membership becomes effective
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime effectiveFrom = LocalDateTime.now();

    /**
     * When this group membership expires (null = never expires)
     */
    private LocalDateTime effectiveTo;

    /**
     * Role within the group (MEMBER, LEADER, ADMINISTRATOR, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GroupRole groupRole = GroupRole.MEMBER;

    /**
     * Priority level within the group (1-10, higher = more priority)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    /**
     * Business unit or department this group belongs to
     */
    private String businessUnit;

    /**
     * Geographic scope of this group (branch, region, etc.)
     */
    private String geographicScope;

    /**
     * Who assigned this group membership
     */
    @Column(nullable = false)
    private String assignedBy;

    /**
     * Reason for group assignment
     */
    private String assignmentReason;

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
     * Check if this group membership is currently valid and active
     */
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return active && 
               (effectiveFrom == null || !effectiveFrom.isAfter(now)) &&
               (effectiveTo == null || !effectiveTo.isBefore(now));
    }

    /**
     * Check if this group membership has expired
     */
    public boolean isExpired() {
        return effectiveTo != null && effectiveTo.isBefore(LocalDateTime.now());
    }

    /**
     * Activate this group membership
     */
    public void activate() {
        this.active = true;
        if (this.effectiveFrom == null) {
            this.effectiveFrom = LocalDateTime.now();
        }
    }

    /**
     * Deactivate this group membership
     */
    public void deactivate(String reason) {
        this.active = false;
        this.complianceNotes = reason;
    }

    /**
     * Check if this party is a leader in this group
     */
    public boolean isLeader() {
        return groupRole == GroupRole.LEADER || groupRole == GroupRole.ADMINISTRATOR;
    }

    /**
     * Check if this party is an administrator of this group
     */
    public boolean isAdministrator() {
        return groupRole == GroupRole.ADMINISTRATOR;
    }

    /**
     * Promote party to leader role in this group
     */
    public void promoteToLeader(String promotedBy, String reason) {
        this.groupRole = GroupRole.LEADER;
        this.assignedBy = promotedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
    }

    /**
     * Promote party to administrator role in this group
     */
    public void promoteToAdministrator(String promotedBy, String reason) {
        this.groupRole = GroupRole.ADMINISTRATOR;
        this.assignedBy = promotedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
    }

    /**
     * Demote party to member role in this group
     */
    public void demoteToMember(String demotedBy, String reason) {
        this.groupRole = GroupRole.MEMBER;
        this.assignedBy = demotedBy;
        this.assignmentReason = reason;
        this.lastReviewedAt = LocalDateTime.now();
    }
}