package com.bank.loan.loan.ai.infrastructure.persistence;

import com.bank.loan.loan.ai.domain.model.AnalysisStatus;
import com.bank.loan.loan.ai.domain.model.LoanAnalysisRequestId;
import com.bank.loan.loan.ai.infrastructure.persistence.entity.LoanAnalysisRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for LoanAnalysisRequest
 * Infrastructure layer - Data access
 */
@Repository
public interface SpringDataLoanAnalysisRequestRepository extends JpaRepository<LoanAnalysisRequestEntity, LoanAnalysisRequestId> {

    /**
     * Find requests by applicant ID ordered by request date
     */
    List<LoanAnalysisRequestEntity> findByApplicantIdOrderByRequestedAtDesc(String applicantId);

    /**
     * Find requests by analysis status ordered by request date
     */
    List<LoanAnalysisRequestEntity> findByAnalysisStatusOrderByRequestedAt(AnalysisStatus status);

    /**
     * Find requests by date range
     */
    List<LoanAnalysisRequestEntity> findByRequestedAtBetween(Instant startDate, Instant endDate);

    /**
     * Find requests requiring NLP processing
     */
    List<LoanAnalysisRequestEntity> findByAnalysisStatus(AnalysisStatus status);

    /**
     * Count requests by applicant and status
     */
    @Query("SELECT COUNT(r) FROM LoanAnalysisRequestEntity r WHERE r.applicantId = :applicantId AND r.analysisStatus = :status")
    long countByApplicantIdAndStatus(@Param("applicantId") String applicantId, @Param("status") AnalysisStatus status);

    /**
     * Find requests by employment type and loan purpose
     */
    @Query("SELECT r FROM LoanAnalysisRequestEntity r WHERE r.employmentType = :employmentType AND r.loanPurpose = :loanPurpose")
    List<LoanAnalysisRequestEntity> findByEmploymentTypeAndLoanPurpose(
        @Param("employmentType") com.bank.loan.loan.ai.domain.model.EmploymentType employmentType,
@Param("loanPurpose") com.bank.loan.loan.ai.domain.model.LoanPurpose loanPurpose
    );
}