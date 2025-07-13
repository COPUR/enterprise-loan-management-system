package com.amanahfi.risk.application;

import com.amanahfi.risk.domain.assessment.*;
import com.amanahfi.risk.port.out.RiskAssessmentRepository;
import com.amanahfi.risk.port.out.RiskScoringEngine;
import com.amanahfi.shared.domain.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Application Service for Risk Assessment Operations
 * Orchestrates risk assessment workflows and integrations
 */
@Service
@Transactional
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskScoringEngine riskScoringEngine;

    public RiskAssessmentService(
            RiskAssessmentRepository riskAssessmentRepository,
            RiskScoringEngine riskScoringEngine) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskScoringEngine = riskScoringEngine;
    }

    /**
     * Initiates credit risk assessment for loan application
     */
    public String initiateCreditRiskAssessment(String customerId, Money loanAmount, String reason) {
        RiskAssessment assessment = RiskAssessment.createCreditRiskAssessment(
            customerId,
            loanAmount,
            reason
        );
        
        riskAssessmentRepository.save(assessment);
        
        // Trigger automated risk scoring
        performAutomatedRiskScoring(assessment.getAssessmentId());
        
        return assessment.getAssessmentId();
    }

    /**
     * Initiates market risk assessment for investment products
     */
    public String initiateMarketRiskAssessment(String productId, Money exposureAmount, String reason) {
        RiskAssessment assessment = RiskAssessment.createMarketRiskAssessment(
            productId,
            exposureAmount,
            reason
        );
        
        riskAssessmentRepository.save(assessment);
        
        return assessment.getAssessmentId();
    }

    /**
     * Initiates operational risk assessment
     */
    public String initiateOperationalRiskAssessment(String operationId, String reason) {
        RiskAssessment assessment = RiskAssessment.createOperationalRiskAssessment(
            operationId,
            reason
        );
        
        riskAssessmentRepository.save(assessment);
        
        return assessment.getAssessmentId();
    }

    /**
     * Performs automated risk scoring
     */
    public void performAutomatedRiskScoring(String assessmentId) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found: " + assessmentId));
        
        RiskScoringResult scoringResult = riskScoringEngine.calculateRiskScores(
            assessment.getEntityId(),
            assessment.getRiskType(),
            assessment.getAssessmentAmount()
        );
        
        assessment.performAutomatedScoring(scoringResult);
        riskAssessmentRepository.save(assessment);
    }

    /**
     * Assigns risk assessment to officer
     */
    public void assignToRiskOfficer(String assessmentId, String officerId, String analystId) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found: " + assessmentId));
        
        assessment.assignToRiskOfficer(officerId, analystId);
        riskAssessmentRepository.save(assessment);
    }

    /**
     * Adds risk factor to assessment
     */
    public void addRiskFactor(String assessmentId, RiskFactorType factorType, 
                             RiskLevel impact, String description, BigDecimal weighting) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found: " + assessmentId));
        
        assessment.addRiskFactor(factorType, impact, description, weighting);
        riskAssessmentRepository.save(assessment);
    }

    /**
     * Adds risk mitigation measure
     */
    public void addMitigationMeasure(String assessmentId, MitigationType mitigationType, 
                                   String description, BigDecimal riskReduction, 
                                   LocalDateTime implementationDate) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found: " + assessmentId));
        
        assessment.addMitigationMeasure(mitigationType, description, riskReduction, implementationDate);
        riskAssessmentRepository.save(assessment);
    }

    /**
     * Approves risk assessment
     */
    public void approveRiskAssessment(String assessmentId, String approverId, String approvalNotes) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found: " + assessmentId));
        
        assessment.approve(approverId, approvalNotes);
        riskAssessmentRepository.save(assessment);
    }

    /**
     * Rejects risk assessment
     */
    public void rejectRiskAssessment(String assessmentId, String rejectorId, String rejectionReason) {
        RiskAssessment assessment = riskAssessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found: " + assessmentId));
        
        assessment.reject(rejectorId, rejectionReason);
        riskAssessmentRepository.save(assessment);
    }

    /**
     * Calculates portfolio risk exposure
     */
    public PortfolioRiskSummary calculatePortfolioRisk(String portfolioId) {
        List<RiskAssessment> assessments = riskAssessmentRepository.findByEntityId(portfolioId);
        
        BigDecimal totalExposure = assessments.stream()
            .filter(a -> a.getAssessmentAmount() != null)
            .map(a -> a.getAssessmentAmount().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal weightedRiskScore = assessments.stream()
            .filter(a -> a.getOverallRiskScore() != null && a.getAssessmentAmount() != null)
            .map(a -> a.getOverallRiskScore().multiply(a.getAssessmentAmount().getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal portfolioRiskScore = totalExposure.compareTo(BigDecimal.ZERO) > 0 
            ? weightedRiskScore.divide(totalExposure, 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;
        
        return PortfolioRiskSummary.builder()
            .portfolioId(portfolioId)
            .totalExposure(Money.aed(totalExposure))
            .portfolioRiskScore(portfolioRiskScore)
            .riskLevel(RiskLevel.fromScore(portfolioRiskScore.intValue()))
            .numberOfAssessments(assessments.size())
            .highRiskAssessments((int) assessments.stream().filter(RiskAssessment::isHighRisk).count())
            .build();
    }

    // Query methods

    /**
     * Finds risk assessment by ID
     */
    @Transactional(readOnly = true)
    public Optional<RiskAssessment> findRiskAssessment(String assessmentId) {
        return riskAssessmentRepository.findById(assessmentId);
    }

    /**
     * Finds risk assessments for entity
     */
    @Transactional(readOnly = true)
    public List<RiskAssessment> findRiskAssessmentsForEntity(String entityId) {
        return riskAssessmentRepository.findByEntityId(entityId);
    }

    /**
     * Finds risk assessments by status
     */
    @Transactional(readOnly = true)
    public List<RiskAssessment> findRiskAssessmentsByStatus(AssessmentStatus status) {
        return riskAssessmentRepository.findByStatus(status);
    }

    /**
     * Finds high-risk assessments requiring attention
     */
    @Transactional(readOnly = true)
    public List<RiskAssessment> findHighRiskAssessments() {
        return riskAssessmentRepository.findByOverallRiskLevel(RiskLevel.HIGH);
    }

    /**
     * Finds assessments requiring manual review
     */
    @Transactional(readOnly = true)
    public List<RiskAssessment> findAssessmentsRequiringReview() {
        return riskAssessmentRepository.findByRequiresManualReviewTrue();
    }

    /**
     * Finds assessments assigned to specific officer
     */
    @Transactional(readOnly = true)
    public List<RiskAssessment> findAssessmentsForOfficer(String officerId) {
        return riskAssessmentRepository.findByAssignedRiskOfficerId(officerId);
    }
}