package com.bank.loanmanagement.ai.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.model.AggregateRoot;
import lombok.AccessLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * AI Loan Analysis Request - Domain Aggregate Root
 * Pure domain model without infrastructure concerns
 * Following DDD and Hexagonal Architecture principles
 */
@Getter
public class LoanAnalysisRequest extends AggregateRoot<LoanAnalysisRequestId> {

    private final LoanAnalysisRequestId id;
    private final String applicantId;
    private final String applicantName;
    private final BigDecimal requestedAmount;
    private final BigDecimal monthlyIncome;
    private final BigDecimal monthlyExpenses;
    private final EmploymentType employmentType;
    private final Integer employmentTenureMonths;
    private final LoanPurpose loanPurpose;
    private final Integer requestedTermMonths;
    private final BigDecimal currentDebt;
    private final Integer creditScore;
    private final String naturalLanguageRequest;
    private final Map<String, Object> additionalData;
    private final Instant requestedAt;

    private AnalysisStatus analysisStatus;
    private Instant processedAt;

    // Private constructor for aggregate creation
    private LoanAnalysisRequest(LoanAnalysisRequestId id, String applicantId, String applicantName,
                               BigDecimal requestedAmount, BigDecimal monthlyIncome, BigDecimal monthlyExpenses,
                               EmploymentType employmentType, Integer employmentTenureMonths, LoanPurpose loanPurpose,
                               Integer requestedTermMonths, BigDecimal currentDebt, Integer creditScore,
                               String naturalLanguageRequest, Map<String, Object> additionalData) {
        this.id = id;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.requestedAmount = requestedAmount;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.employmentType = employmentType;
        this.employmentTenureMonths = employmentTenureMonths;
        this.loanPurpose = loanPurpose;
        this.requestedTermMonths = requestedTermMonths;
        this.currentDebt = currentDebt;
        this.creditScore = creditScore;
        this.naturalLanguageRequest = naturalLanguageRequest;
        this.additionalData = additionalData;
        this.analysisStatus = naturalLanguageRequest != null ? AnalysisStatus.PENDING_NLP_PROCESSING : AnalysisStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    /**
     * Factory method to create structured loan analysis request
     * Following DDD factory pattern
     */
    public static LoanAnalysisRequest createStructured(LoanAnalysisRequestId id, BigDecimal requestedAmount,
                                                     String applicantId, String applicantName, BigDecimal monthlyIncome,
                                                     BigDecimal monthlyExpenses, EmploymentType employmentType,
                                                     Integer employmentTenureMonths, LoanPurpose loanPurpose,
                                                     Integer requestedTermMonths, BigDecimal currentDebt,
                                                     Integer creditScore, Map<String, Object> additionalData) {
        validateStructuredRequest(applicantId, applicantName, requestedAmount, monthlyIncome, employmentType, loanPurpose);
        
        return new LoanAnalysisRequest(id, applicantId, applicantName, requestedAmount, monthlyIncome,
                monthlyExpenses, employmentType, employmentTenureMonths, loanPurpose, requestedTermMonths,
                currentDebt, creditScore, null, additionalData);
    }

    /**
     * Factory method to create natural language processing request
     * Following DDD factory pattern
     */
    public static LoanAnalysisRequest createForNlpProcessing(LoanAnalysisRequestId id, String applicantId,
                                                           String applicantName, String naturalLanguageRequest) {
        validateNlpRequest(applicantId, applicantName, naturalLanguageRequest);
        
        return new LoanAnalysisRequest(id, applicantId, applicantName, null, null, null, null, null,
                null, null, null, null, naturalLanguageRequest, null);
    }

    // Business methods following DDD aggregate pattern

    /**
     * Mark request as processing - domain invariant enforcement
     */
    public void markAsProcessing() {
        if (!analysisStatus.canBeProcessed()) {
            throw new IllegalStateException("Request cannot be processed in current status: " + analysisStatus);
        }
        this.analysisStatus = AnalysisStatus.PROCESSING;
    }

    /**
     * Mark request as completed - domain invariant enforcement
     */
    public void markAsCompleted() {
        if (analysisStatus != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("Request must be in PROCESSING status to be completed");
        }
        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.processedAt = Instant.now();
    }

    /**
     * Mark request as failed - domain invariant enforcement
     */
    public void markAsFailed() {
        if (analysisStatus.isComplete()) {
            throw new IllegalStateException("Cannot mark completed request as failed");
        }
        this.analysisStatus = AnalysisStatus.FAILED;
        this.processedAt = Instant.now();
    }

    /**
     * Check if request is ready for AI analysis
     */
    public boolean isReadyForAnalysis() {
        return analysisStatus == AnalysisStatus.PENDING && hasRequiredData();
    }

    /**
     * Check if request requires NLP processing
     */
    public boolean requiresNlpProcessing() {
        return analysisStatus == AnalysisStatus.PENDING_NLP_PROCESSING;
    }

    /**
     * Calculate debt-to-income ratio - domain logic
     */
    public BigDecimal calculateDebtToIncomeRatio() {
        if (currentDebt != null && monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            return currentDebt.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    // Private validation methods - domain invariants

    private static void validateStructuredRequest(String applicantId, String applicantName, BigDecimal requestedAmount,
                                                BigDecimal monthlyIncome, EmploymentType employmentType, LoanPurpose loanPurpose) {
        Objects.requireNonNull(applicantId, "Applicant ID cannot be null");
        Objects.requireNonNull(applicantName, "Applicant name cannot be null");
        Objects.requireNonNull(requestedAmount, "Requested amount cannot be null");
        Objects.requireNonNull(monthlyIncome, "Monthly income cannot be null");
        Objects.requireNonNull(employmentType, "Employment type cannot be null");
        Objects.requireNonNull(loanPurpose, "Loan purpose cannot be null");

        if (applicantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Applicant name cannot be empty");
        }
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested amount must be positive");
        }
        if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly income must be positive");
        }
    }

    private static void validateNlpRequest(String applicantId, String applicantName, String naturalLanguageRequest) {
        Objects.requireNonNull(applicantId, "Applicant ID cannot be null");
        Objects.requireNonNull(applicantName, "Applicant name cannot be null");
        Objects.requireNonNull(naturalLanguageRequest, "Natural language request cannot be null");

        if (applicantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Applicant name cannot be empty");
        }
        if (naturalLanguageRequest.trim().isEmpty()) {
            throw new IllegalArgumentException("Natural language request cannot be empty");
        }
    }

    private boolean hasRequiredData() {
        return requestedAmount != null && monthlyIncome != null && 
               employmentType != null && loanPurpose != null;
    }

    @Override
    public LoanAnalysisRequestId getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanAnalysisRequest that = (LoanAnalysisRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LoanAnalysisRequest{" +
                "id=" + id +
                ", applicantId='" + applicantId + '\'' +
                ", applicantName='" + applicantName + '\'' +
                ", requestedAmount=" + requestedAmount +
                ", analysisStatus=" + analysisStatus +
                ", requestedAt=" + requestedAt +
                '}';
    }
}