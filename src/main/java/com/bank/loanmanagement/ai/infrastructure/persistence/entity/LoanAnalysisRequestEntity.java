package com.bank.loanmanagement.ai.infrastructure.persistence.entity;

import com.bank.loanmanagement.ai.domain.model.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Entity for LoanAnalysisRequest persistence
 * Infrastructure layer - separate from domain model to avoid contamination
 */
@Entity
@Table(name = "ai_loan_analysis_requests", schema = "ai_domain")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanAnalysisRequestEntity {

    @EmbeddedId
    private LoanAnalysisRequestId id;

    @Column(name = "applicant_id", nullable = false, length = 50)
    private String applicantId;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "requested_amount", precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "monthly_expenses", precision = 15, scale = 2)
    private BigDecimal monthlyExpenses;

    @Column(name = "employment_type")
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(name = "employment_tenure_months")
    private Integer employmentTenureMonths;

    @Column(name = "loan_purpose")
    @Enumerated(EnumType.STRING)
    private LoanPurpose loanPurpose;

    @Column(name = "requested_term_months")
    private Integer requestedTermMonths;

    @Column(name = "current_debt", precision = 15, scale = 2)
    private BigDecimal currentDebt;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "natural_language_request", columnDefinition = "TEXT")
    private String naturalLanguageRequest;

    @Column(name = "analysis_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @ElementCollection
    @CollectionTable(name = "loan_request_additional_data", schema = "ai_domain",
                    joinColumns = @JoinColumn(name = "request_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value", columnDefinition = "TEXT")
    private Map<String, String> additionalData = new HashMap<>();

    @Version
    private Long version;

    /**
     * Convert domain model to JPA entity
     */
    public static LoanAnalysisRequestEntity fromDomain(LoanAnalysisRequest domain) {
        LoanAnalysisRequestEntity entity = new LoanAnalysisRequestEntity();
        entity.setId(domain.getId());
        entity.setApplicantId(domain.getApplicantId());
        entity.setApplicantName(domain.getApplicantName());
        entity.setRequestedAmount(domain.getRequestedAmount());
        entity.setMonthlyIncome(domain.getMonthlyIncome());
        entity.setMonthlyExpenses(domain.getMonthlyExpenses());
        entity.setEmploymentType(domain.getEmploymentType());
        entity.setEmploymentTenureMonths(domain.getEmploymentTenureMonths());
        entity.setLoanPurpose(domain.getLoanPurpose());
        entity.setRequestedTermMonths(domain.getRequestedTermMonths());
        entity.setCurrentDebt(domain.getCurrentDebt());
        entity.setCreditScore(domain.getCreditScore());
        entity.setNaturalLanguageRequest(domain.getNaturalLanguageRequest());
        entity.setAnalysisStatus(domain.getAnalysisStatus());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setProcessedAt(domain.getProcessedAt());
        
        // Convert additional data map
        if (domain.getAdditionalData() != null) {
            Map<String, String> stringData = new HashMap<>();
            domain.getAdditionalData().forEach((k, v) -> 
                stringData.put(k, v != null ? v.toString() : null));
            entity.setAdditionalData(stringData);
        }
        
        return entity;
    }

    /**
     * Convert JPA entity to domain model
     */
    public LoanAnalysisRequest toDomain() {
        // Convert string data back to Object map
        Map<String, Object> objectData = new HashMap<>();
        if (additionalData != null) {
            additionalData.forEach((k, v) -> objectData.put(k, v));
        }

        if (naturalLanguageRequest != null) {
            LoanAnalysisRequest domain = LoanAnalysisRequest.createForNlpProcessing(
                id, applicantId, applicantName, naturalLanguageRequest);
            
            // Set status if different from initial
            if (analysisStatus != AnalysisStatus.PENDING_NLP_PROCESSING) {
                updateDomainStatus(domain);
            }
            
            return domain;
        } else {
            LoanAnalysisRequest domain = LoanAnalysisRequest.createStructured(
                id, requestedAmount, applicantId, applicantName, monthlyIncome, monthlyExpenses,
                employmentType, employmentTenureMonths, loanPurpose, requestedTermMonths,
                currentDebt, creditScore, objectData);
            
            // Set status if different from initial
            if (analysisStatus != AnalysisStatus.PENDING) {
                updateDomainStatus(domain);
            }
            
            return domain;
        }
    }

    private void updateDomainStatus(LoanAnalysisRequest domain) {
        // Use reflection or status update methods to sync status
        switch (analysisStatus) {
            case PROCESSING -> domain.markAsProcessing();
            case COMPLETED -> {
                if (domain.getAnalysisStatus() == AnalysisStatus.PENDING) {
                    domain.markAsProcessing();
                }
                domain.markAsCompleted();
            }
            case FAILED -> domain.markAsFailed();
        }
    }
}