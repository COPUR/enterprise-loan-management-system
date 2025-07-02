package com.bank.loan.loan.ai.infrastructure.persistence;

import com.bank.loan.loan.ai.domain.model.LoanAnalysisResultId;
import com.bank.loan.loan.ai.domain.model.LoanRecommendation;
import com.bank.loan.loan.ai.infrastructure.persistence.entity.LoanAnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for LoanAnalysisResult
 * Infrastructure layer - Data access
 */
@Repository
public interface SpringDataLoanAnalysisResultRepository extends JpaRepository<LoanAnalysisResultEntity, LoanAnalysisResultId> {

    /**
     * Find result by request ID
     */
    Optional<LoanAnalysisResultEntity> findByRequestId(String requestId);

    /**
     * Find results by recommendation type
     */
    List<LoanAnalysisResultEntity> findByOverallRecommendation(LoanRecommendation recommendation);

    /**
     * Find results by confidence score range
     */
    List<LoanAnalysisResultEntity> findByConfidenceScoreBetween(BigDecimal minConfidence, BigDecimal maxConfidence);

    /**
     * Find results by risk score range
     */
    List<LoanAnalysisResultEntity> findByRiskScoreBetween(BigDecimal minRisk, BigDecimal maxRisk);

    /**
     * Find results processed within date range
     */
    List<LoanAnalysisResultEntity> findByProcessedAtBetween(Instant startDate, Instant endDate);

    /**
     * Find high-confidence results
     */
    @Query("SELECT r FROM LoanAnalysisResultEntity r WHERE r.confidenceScore >= :threshold")
    List<LoanAnalysisResultEntity> findHighConfidenceResults(@Param("threshold") BigDecimal threshold);

    /**
     * Find results with fraud risk indicators
     */
    @Query("SELECT r FROM LoanAnalysisResultEntity r WHERE r.fraudRiskIndicators IS NOT NULL AND r.fraudRiskIndicators != ''")
    List<LoanAnalysisResultEntity> findResultsWithFraudRisk();

    /**
     * Get average confidence score by recommendation type
     */
    @Query("SELECT r.overallRecommendation, AVG(r.confidenceScore) FROM LoanAnalysisResultEntity r GROUP BY r.overallRecommendation")
    List<Object[]> getAverageConfidenceByRecommendation();

    /**
     * Count results by recommendation type
     */
    @Query("SELECT r.overallRecommendation, COUNT(r) FROM LoanAnalysisResultEntity r GROUP BY r.overallRecommendation")
    List<Object[]> countResultsByRecommendation();
}