package com.bank.loanmanagement.ai.domain.model;

import com.bank.loanmanagement.domain.shared.DomainId;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/**
 * Value Object representing the unique identifier for a Loan Analysis Request
 */
@Embeddable
public class LoanAnalysisRequestId extends DomainId {

    protected LoanAnalysisRequestId() {
        super();
    }

    private LoanAnalysisRequestId(String value) {
        super(value);
    }

    public static LoanAnalysisRequestId generate() {
        return new LoanAnalysisRequestId(UUID.randomUUID().toString());
    }

    public static LoanAnalysisRequestId of(String value) {
        return new LoanAnalysisRequestId(value);
    }
}