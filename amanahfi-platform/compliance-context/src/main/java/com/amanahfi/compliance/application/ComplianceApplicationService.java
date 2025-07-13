package com.amanahfi.compliance.application;

import com.amanahfi.compliance.domain.check.*;
import com.amanahfi.compliance.port.out.ComplianceCheckRepository;
import com.amanahfi.compliance.port.out.ExternalAmlProvider;
import com.amanahfi.shared.domain.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application Service for Compliance Operations
 * Orchestrates compliance check workflows and external integrations
 */
@Service
@Transactional
public class ComplianceApplicationService {

    private final ComplianceCheckRepository complianceCheckRepository;
    private final ExternalAmlProvider externalAmlProvider;

    public ComplianceApplicationService(
            ComplianceCheckRepository complianceCheckRepository,
            ExternalAmlProvider externalAmlProvider) {
        this.complianceCheckRepository = complianceCheckRepository;
        this.externalAmlProvider = externalAmlProvider;
    }

    /**
     * Initiates AML compliance check for customer onboarding
     */
    public String initiateCustomerAmlCheck(String customerId, String reason) {
        ComplianceCheck check = ComplianceCheck.createAmlCheck(
            customerId, 
            CheckType.CUSTOMER_ONBOARDING, 
            reason
        );
        
        complianceCheckRepository.save(check);
        
        // Trigger automated AML screening
        performAutomatedAmlScreening(check.getCheckId());
        
        return check.getCheckId();
    }

    /**
     * Initiates Sharia compliance check for Islamic finance products
     */
    public String initiateShariahComplianceCheck(String contractId, String reason) {
        ComplianceCheck check = ComplianceCheck.createShariahCheck(
            contractId,
            CheckType.CONTRACT_REVIEW,
            reason
        );
        
        complianceCheckRepository.save(check);
        
        return check.getCheckId();
    }

    /**
     * Initiates transaction monitoring check
     */
    public String initiateTransactionMonitoring(String transactionId, Money amount, String reason) {
        ComplianceCheck check = ComplianceCheck.createTransactionCheck(
            transactionId,
            amount,
            reason
        );
        
        complianceCheckRepository.save(check);
        
        // Trigger automated screening for high-value transactions
        if (check.isHighValueTransaction()) {
            performAutomatedAmlScreening(check.getCheckId());
        }
        
        return check.getCheckId();
    }

    /**
     * Performs automated AML screening using external providers
     */
    public void performAutomatedAmlScreening(String checkId) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        AmlScreeningResult screeningResult = externalAmlProvider.performScreening(
            check.getEntityId(),
            check.getCheckType()
        );
        
        check.performAutomatedScreening(screeningResult);
        complianceCheckRepository.save(check);
    }

    /**
     * Assigns compliance check to officer for manual review
     */
    public void assignCheckToOfficer(String checkId, String officerId, String notes) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        check.assignToOfficer(officerId, notes);
        complianceCheckRepository.save(check);
    }

    /**
     * Completes compliance check with approval
     */
    public void approveComplianceCheck(String checkId, String officerId, String notes, RiskScore riskScore) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        check.complete(officerId, notes, riskScore);
        complianceCheckRepository.save(check);
    }

    /**
     * Rejects compliance check
     */
    public void rejectComplianceCheck(String checkId, String officerId, String rejectionReason) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        check.reject(officerId, rejectionReason);
        complianceCheckRepository.save(check);
    }

    /**
     * Adds violation to compliance check
     */
    public void addViolationToCheck(String checkId, ViolationType violationType, 
                                  SeverityLevel severity, String description) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        ComplianceViolation violation = ComplianceViolation.create(
            violationType, 
            severity, 
            description
        );
        
        check.addViolation(violation);
        complianceCheckRepository.save(check);
    }

    /**
     * Archives completed compliance check
     */
    public void archiveComplianceCheck(String checkId, String archiveReason) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        check.archive(archiveReason);
        complianceCheckRepository.save(check);
    }

    /**
     * Generates compliance report
     */
    public ComplianceReport generateComplianceReport(String checkId) {
        ComplianceCheck check = complianceCheckRepository.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Compliance check not found: " + checkId));
        
        return check.generateReport();
    }

    // Query methods

    /**
     * Finds compliance check by ID
     */
    @Transactional(readOnly = true)
    public Optional<ComplianceCheck> findComplianceCheck(String checkId) {
        return complianceCheckRepository.findById(checkId);
    }

    /**
     * Finds all compliance checks for entity
     */
    @Transactional(readOnly = true)
    public List<ComplianceCheck> findComplianceChecksForEntity(String entityId) {
        return complianceCheckRepository.findByEntityId(entityId);
    }

    /**
     * Finds compliance checks by status
     */
    @Transactional(readOnly = true)
    public List<ComplianceCheck> findComplianceChecksByStatus(CheckStatus status) {
        return complianceCheckRepository.findByStatus(status);
    }

    /**
     * Finds compliance checks requiring manual review
     */
    @Transactional(readOnly = true)
    public List<ComplianceCheck> findChecksRequiringManualReview() {
        return complianceCheckRepository.findByRequiresManualReviewTrue();
    }

    /**
     * Finds compliance checks requiring Shariah board review
     */
    @Transactional(readOnly = true)
    public List<ComplianceCheck> findChecksRequiringShariahReview() {
        return complianceCheckRepository.findByRequiresShariahBoardReviewTrue();
    }

    /**
     * Finds high-risk compliance checks
     */
    @Transactional(readOnly = true)
    public List<ComplianceCheck> findHighRiskChecks() {
        return complianceCheckRepository.findByFinalRiskScore(RiskScore.HIGH);
    }
}