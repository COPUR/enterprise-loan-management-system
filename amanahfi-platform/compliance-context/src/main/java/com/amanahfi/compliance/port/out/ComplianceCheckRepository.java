package com.amanahfi.compliance.port.out;

import com.amanahfi.compliance.domain.check.CheckStatus;
import com.amanahfi.compliance.domain.check.ComplianceCheck;
import com.amanahfi.compliance.domain.check.RiskScore;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ComplianceCheck aggregate
 * Follows hexagonal architecture - port for infrastructure adapters
 */
public interface ComplianceCheckRepository {

    /**
     * Saves compliance check
     */
    ComplianceCheck save(ComplianceCheck complianceCheck);

    /**
     * Finds compliance check by ID
     */
    Optional<ComplianceCheck> findById(String checkId);

    /**
     * Finds all compliance checks for an entity
     */
    List<ComplianceCheck> findByEntityId(String entityId);

    /**
     * Finds compliance checks by status
     */
    List<ComplianceCheck> findByStatus(CheckStatus status);

    /**
     * Finds compliance checks requiring manual review
     */
    List<ComplianceCheck> findByRequiresManualReviewTrue();

    /**
     * Finds compliance checks requiring Shariah board review
     */
    List<ComplianceCheck> findByRequiresShariahBoardReviewTrue();

    /**
     * Finds compliance checks by risk score
     */
    List<ComplianceCheck> findByFinalRiskScore(RiskScore riskScore);

    /**
     * Finds compliance checks assigned to specific officer
     */
    List<ComplianceCheck> findByAssignedOfficerId(String officerId);

    /**
     * Finds archived compliance checks
     */
    List<ComplianceCheck> findByArchivedTrue();

    /**
     * Finds active (non-archived) compliance checks
     */
    List<ComplianceCheck> findByArchivedFalse();

    /**
     * Deletes compliance check (typically not used - prefer archiving)
     */
    void deleteById(String checkId);

    /**
     * Checks if compliance check exists
     */
    boolean existsById(String checkId);

    /**
     * Counts total compliance checks
     */
    long count();

    /**
     * Counts compliance checks by status
     */
    long countByStatus(CheckStatus status);
}