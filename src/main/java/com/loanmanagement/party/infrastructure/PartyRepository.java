package com.loanmanagement.party.infrastructure;

import com.loanmanagement.party.domain.Party;
import com.loanmanagement.party.domain.PartyStatus;
import com.loanmanagement.party.domain.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Party repository interface for data access
 * Provides data access methods for Party entities
 */
@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
    
    /**
     * Find party by identifier (for authentication)
     */
    Optional<Party> findByIdentifier(String identifier);
    
    /**
     * Find party by external ID (for Keycloak integration)
     */
    Optional<Party> findByExternalId(String externalId);
    
    /**
     * Find party by email address
     */
    Optional<Party> findByEmail(String email);
    
    /**
     * Find parties by type and status
     */
    List<Party> findByPartyTypeAndStatus(PartyType partyType, PartyStatus status);
    
    /**
     * Find active parties by type
     */
    @Query("SELECT p FROM Party p WHERE p.partyType = :partyType AND p.status = 'ACTIVE'")
    List<Party> findActivePartiesByType(@Param("partyType") PartyType partyType);
    
    /**
     * Find parties by display name (partial match)
     */
    @Query("SELECT p FROM Party p WHERE LOWER(p.displayName) LIKE LOWER(CONCAT('%', :displayName, '%'))")
    List<Party> findByDisplayNameContainingIgnoreCase(@Param("displayName") String displayName);
    
    /**
     * Find parties by compliance level
     */
    @Query("SELECT p FROM Party p WHERE p.complianceLevel = :complianceLevel")
    List<Party> findByComplianceLevel(@Param("complianceLevel") String complianceLevel);
    
    /**
     * Count parties by status
     */
    @Query("SELECT COUNT(p) FROM Party p WHERE p.status = :status")
    long countByStatus(@Param("status") PartyStatus status);
    
    /**
     * Find parties that need compliance review
     */
    @Query("SELECT p FROM Party p WHERE p.complianceLevel IN ('BASIC', 'STANDARD') AND p.status = 'ACTIVE'")
    List<Party> findPartiesNeedingComplianceReview();
    
    /**
     * Find parties with specific role
     */
    @Query("SELECT DISTINCT p FROM Party p JOIN p.partyRoles pr WHERE pr.roleName = :roleName AND pr.active = true")
    List<Party> findPartiesWithRole(@Param("roleName") String roleName);
    
    /**
     * Find parties in specific group
     */
    @Query("SELECT DISTINCT p FROM Party p JOIN p.partyGroups pg WHERE pg.groupName = :groupName AND pg.active = true")
    List<Party> findPartiesInGroup(@Param("groupName") String groupName);
}