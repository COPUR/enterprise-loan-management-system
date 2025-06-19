package com.banking.loans.domain.party;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

/**
 * Party Domain Aggregate Root - Clean DDD Implementation
 * Represents any party (person, organization) in the banking system
 * Pure domain model without infrastructure dependencies
 */
public class Party {

    private Long id;
    private String externalId;
    private String identifier;
    private String displayName;
    private String email;
    private PartyType partyType;
    private PartyStatus status;
    private ComplianceLevel complianceLevel;
    private Set<PartyRole> partyRoles;
    private Set<PartyGroup> partyGroups;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;

    // Private constructor for domain creation
    private Party(
        String externalId,
        String identifier,
        String displayName,
        String email,
        PartyType partyType,
        String createdBy
    ) {
        this.externalId = Objects.requireNonNull(externalId, "External ID cannot be null");
        this.identifier = Objects.requireNonNull(identifier, "Identifier cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.partyType = Objects.requireNonNull(partyType, "Party type cannot be null");
        this.createdBy = Objects.requireNonNull(createdBy, "Created by cannot be null");
        this.status = PartyStatus.ACTIVE;
        this.complianceLevel = ComplianceLevel.STANDARD;
        this.partyRoles = new HashSet<>();
        this.partyGroups = new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Factory method for creating new parties
    public static Party create(
        String externalId,
        String identifier,
        String displayName,
        String email,
        PartyType partyType,
        String createdBy
    ) {
        return new Party(externalId, identifier, displayName, email, partyType, createdBy);
    }

    // Business logic: Activate party
    public void activate() {
        this.status = PartyStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Deactivate party
    public void deactivate(String reason) {
        this.status = PartyStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Suspend party
    public void suspend(String reason) {
        this.status = PartyStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Add role to party
    public void addRole(PartyRole role) {
        Objects.requireNonNull(role, "Role cannot be null");
        this.partyRoles.add(role);
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Remove role from party
    public void removeRole(PartyRole role) {
        Objects.requireNonNull(role, "Role cannot be null");
        this.partyRoles.remove(role);
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Add party to group
    public void addToGroup(PartyGroup group) {
        Objects.requireNonNull(group, "Group cannot be null");
        this.partyGroups.add(group);
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Remove party from group
    public void removeFromGroup(PartyGroup group) {
        Objects.requireNonNull(group, "Group cannot be null");
        this.partyGroups.remove(group);
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Check if party has specific role
    public boolean hasRole(String roleName) {
        return partyRoles.stream()
            .anyMatch(role -> role.getRoleName().equals(roleName) && role.isActive());
    }

    // Business logic: Check if party is in specific group
    public boolean isInGroup(String groupName) {
        return partyGroups.stream()
            .anyMatch(group -> group.getGroupName().equals(groupName) && group.isActive());
    }

    // Business logic: Get active roles
    public Set<PartyRole> getActiveRoles() {
        return partyRoles.stream()
            .filter(PartyRole::isActive)
            .collect(java.util.stream.Collectors.toSet());
    }

    // Business logic: Get active groups
    public Set<PartyGroup> getActiveGroups() {
        return partyGroups.stream()
            .filter(PartyGroup::isActive)
            .collect(java.util.stream.Collectors.toSet());
    }

    // Business logic: Update compliance level
    public void updateComplianceLevel(ComplianceLevel newLevel, String updatedBy) {
        this.complianceLevel = Objects.requireNonNull(newLevel, "Compliance level cannot be null");
        this.updatedBy = Objects.requireNonNull(updatedBy, "Updated by cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic: Check if party is active
    public boolean isActive() {
        return status == PartyStatus.ACTIVE;
    }

    // Business logic: Check if party is compliant
    public boolean isCompliant() {
        return complianceLevel == ComplianceLevel.HIGH || complianceLevel == ComplianceLevel.STANDARD;
    }

    // Getters
    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getIdentifier() { return identifier; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public PartyType getPartyType() { return partyType; }
    public PartyStatus getStatus() { return status; }
    public ComplianceLevel getComplianceLevel() { return complianceLevel; }
    public Set<PartyRole> getPartyRoles() { return new HashSet<>(partyRoles); }
    public Set<PartyGroup> getPartyGroups() { return new HashSet<>(partyGroups); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public Long getVersion() { return version; }

    // Public setters for reconstruction from persistence
    public void setId(Long id) { this.id = id; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public void setIdentifier(String identifier) { this.identifier = identifier; this.updatedAt = LocalDateTime.now(); }
    public void setDisplayName(String displayName) { this.displayName = displayName; this.updatedAt = LocalDateTime.now(); }
    public void setEmail(String email) { this.email = email; this.updatedAt = LocalDateTime.now(); }
    public void setPartyType(PartyType partyType) { this.partyType = partyType; }
    public void setStatus(PartyStatus status) { this.status = status; }
    public void setComplianceLevel(ComplianceLevel complianceLevel) { this.complianceLevel = complianceLevel; }
    public void setPartyRoles(Set<PartyRole> partyRoles) { this.partyRoles = new HashSet<>(partyRoles); }
    public void setPartyGroups(Set<PartyGroup> partyGroups) { this.partyGroups = new HashSet<>(partyGroups); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Party party = (Party) o;
        return Objects.equals(id, party.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Party{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", displayName='" + displayName + '\'' +
                ", partyType=" + partyType +
                ", status=" + status +
                '}';
    }
}