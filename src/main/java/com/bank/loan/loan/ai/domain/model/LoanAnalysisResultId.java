package com.bank.loan.loan.ai.domain.model;

import com.bank.loan.loan.domain.shared.DomainId;
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