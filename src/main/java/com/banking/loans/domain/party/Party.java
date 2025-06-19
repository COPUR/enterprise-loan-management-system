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
import java.util.Set;
import java.util.HashSet;

/**
 * Party Domain Entity - Banking DDD Aggregate Root
 * Represents any party (person, organization) in the banking system
 * Following Domain-Driven Design principles for banking domain
 */
@Entity
@Table(name = "parties", indexes = {
    @Index(name = "idx_party_external_id", columnList = "externalId"),
    @Index(name = "idx_party_type_status", columnList = "partyType, status"),
    @Index(name = "idx_party_compliance_level", columnList = "complianceLevel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External identifier for integration with Keycloak/LDAP
     * Maps to Keycloak user ID or LDAP DN
     */
    @Column(unique = true, nullable = false)
    private String externalId;

    /**
     * Party identification - username, email, or unique identifier
     */
    @Column(unique = true, nullable = false)
    private String identifier;

    /**
     * Display name for the party
     */
    @Column(nullable = false)
    private String displayName;

    /**
     * Email address for the party
     */
    @Column(nullable = false)
    private String email;

    /**
     * Type of party - INDIVIDUAL, ORGANIZATION, SERVICE_ACCOUNT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType partyType;

    /**
     * Current status of the party - ACTIVE, INACTIVE, SUSPENDED, LOCKED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyStatus status;

    /**
     * Banking compliance level - BASIC, ENHANCED, PREMIUM
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceLevel complianceLevel;

    /**
     * Department or organizational unit
     */
    private String department;

    /**
     * Job title or role description
     */
    private String title;

    /**
     * Employee number or unique business identifier
     */
    private String employeeNumber;

    /**
     * Phone number for contact
     */
    private String phoneNumber;

    /**
     * Roles assigned to this party from the party data management system
     * These roles are authoritative and override any external role assignments
     */
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PartyRole> partyRoles = new HashSet<>();

    /**
     * Groups this party belongs to
     */
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PartyGroup> partyGroups = new HashSet<>();

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
     * Compliance and audit fields
     */
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastAccessReviewAt;
    private Boolean requiresAccessReview;
    private String complianceNotes;

    /**
     * Add a role to this party
     */
    public void addRole(PartyRole role) {
        partyRoles.add(role);
        role.setParty(this);
    }

    /**
     * Remove a role from this party
     */
    public void removeRole(PartyRole role) {
        partyRoles.remove(role);
        role.setParty(null);
    }

    /**
     * Add this party to a group
     */
    public void addToGroup(PartyGroup group) {
        partyGroups.add(group);
        group.setParty(this);
    }

    /**
     * Remove this party from a group
     */
    public void removeFromGroup(PartyGroup group) {
        partyGroups.remove(group);
        group.setParty(null);
    }

    /**
     * Check if party has a specific role
     */
    public boolean hasRole(String roleName) {
        return partyRoles.stream()
                .anyMatch(partyRole -> partyRole.getRoleName().equals(roleName) && partyRole.isActive());
    }

    /**
     * Check if party is in a specific group
     */
    public boolean isInGroup(String groupName) {
        return partyGroups.stream()
                .anyMatch(partyGroup -> partyGroup.getGroupName().equals(groupName) && partyGroup.isActive());
    }

    /**
     * Check if party is active and can access the system
     */
    public boolean isActive() {
        return status == PartyStatus.ACTIVE;
    }

    /**
     * Update last login timestamp for audit tracking
     */
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Mark that password was changed for compliance tracking
     */
    public void recordPasswordChange() {
        this.passwordChangedAt = LocalDateTime.now();
    }

    /**
     * Mark that access review was completed for compliance
     */
    public void recordAccessReview() {
        this.lastAccessReviewAt = LocalDateTime.now();
        this.requiresAccessReview = false;
    }

    /**
     * Mark that access review is required
     */
    public void requireAccessReview(String reason) {
        this.requiresAccessReview = true;
        this.complianceNotes = reason;
    }
}