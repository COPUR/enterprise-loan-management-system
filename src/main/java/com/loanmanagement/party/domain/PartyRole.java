package com.loanmanagement.party.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Party role domain entity
 * Represents a role assigned to a party in the banking system
 */
public class PartyRole {
    
    private Long id;
    private String roleName;
    private String roleDescription;
    private RoleSource source;
    private boolean active;
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private String assignedBy;
    private String revokedBy;
    private LocalDateTime revokedAt;
    
    // Default constructor for persistence
    public PartyRole() {}
    
    // Constructor for creating new roles
    public PartyRole(String roleName, String roleDescription, RoleSource source, String assignedBy) {
        this.roleName = Objects.requireNonNull(roleName, "Role name cannot be null");
        this.roleDescription = roleDescription;
        this.source = Objects.requireNonNull(source, "Role source cannot be null");
        this.assignedBy = Objects.requireNonNull(assignedBy, "Assigned by cannot be null");
        this.active = true;
        this.assignedAt = LocalDateTime.now();
    }
    
    // Business logic: Revoke role
    public void revoke(String revokedBy) {
        this.active = false;
        this.revokedBy = Objects.requireNonNull(revokedBy, "Revoked by cannot be null");
        this.revokedAt = LocalDateTime.now();
    }
    
    // Business logic: Activate role
    public void activate() {
        this.active = true;
        this.revokedBy = null;
        this.revokedAt = null;
    }
    
    // Business logic: Set expiration
    public void setExpiration(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    // Business logic: Check if role is expired
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    // Business logic: Check if role is currently valid
    public boolean isValid() {
        return active && !isExpired();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    
    public String getRoleDescription() { return roleDescription; }
    public void setRoleDescription(String roleDescription) { this.roleDescription = roleDescription; }
    
    public RoleSource getSource() { return source; }
    public void setSource(RoleSource source) { this.source = source; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartyRole partyRole = (PartyRole) o;
        return Objects.equals(id, partyRole.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PartyRole{" +
                "id=" + id +
                ", roleName='" + roleName + '\'' +
                ", active=" + active +
                ", assignedAt=" + assignedAt +
                '}';
    }
}