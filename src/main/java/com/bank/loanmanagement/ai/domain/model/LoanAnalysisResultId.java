package com.bank.loanmanagement.ai.domain.model;

import com.bank.loanmanagement.domain.shared.DomainId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/**
 * Value Object representing the unique identifier for a Loan Analysis Result
 */
@Embeddable
public class LoanAnalysisResultId extends DomainId {

    protected LoanAnalysisResultId() {
        super();
    }

    private LoanAnalysisResultId(String value) {
        super(value);
    }

    public static LoanAnalysisResultId generate() {
        return new LoanAnalysisResultId(UUID.randomUUID().toString());
    }

    public static LoanAnalysisResultId of(String value) {
        return new LoanAnalysisResultId(value);
    }
}