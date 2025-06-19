package com.banking.loans.domain.party;

/**
 * GroupRole enumeration for banking domain
 * Defines different roles a party can have within a group
 */
public enum GroupRole {
    /**
     * Regular member of the group
     */
    MEMBER,
    
    /**
     * Leader of the group with additional responsibilities
     */
    LEADER,
    
    /**
     * Administrator of the group with full management privileges
     */
    ADMINISTRATOR,
    
    /**
     * Deputy or assistant leader
     */
    DEPUTY,
    
    /**
     * Senior member with mentoring responsibilities
     */
    SENIOR_MEMBER,
    
    /**
     * Observer role with read-only access
     */
    OBSERVER,
    
    /**
     * Guest member with limited access
     */
    GUEST
}