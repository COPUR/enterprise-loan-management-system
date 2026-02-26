package com.loanmanagement.party.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Party group domain entity
 * Represents a group that parties can belong to in the banking system
 */
public class PartyGroup {
    
    private Long id;
    private String groupName;
    private String groupDescription;
    private GroupType groupType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Default constructor for persistence
    public PartyGroup() {}
    
    // Constructor for creating new groups
    public PartyGroup(String groupName, String groupDescription, GroupType groupType, String createdBy) {
        this.groupName = Objects.requireNonNull(groupName, "Group name cannot be null");
        this.groupDescription = groupDescription;
        this.groupType = Objects.requireNonNull(groupType, "Group type cannot be null");
        this.createdBy = Objects.requireNonNull(createdBy, "Created by cannot be null");
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business logic: Activate group
    public void activate(String updatedBy) {
        this.active = true;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business logic: Deactivate group
    public void deactivate(String updatedBy) {
        this.active = false;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business logic: Update group details
    public void updateDetails(String groupDescription, String updatedBy) {
        this.groupDescription = groupDescription;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }
    
    public GroupType getGroupType() { return groupType; }
    public void setGroupType(GroupType groupType) { this.groupType = groupType; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartyGroup partyGroup = (PartyGroup) o;
        return Objects.equals(id, partyGroup.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
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