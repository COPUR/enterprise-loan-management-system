package com.enterprise.openfinance.payeeverification.domain.service;

import com.enterprise.openfinance.payeeverification.domain.model.NameMatchDecision;

public final class ConfirmationDecisionPolicy {

    private final int closeMatchThreshold;

    public ConfirmationDecisionPolicy(int closeMatchThreshold) {
        if (closeMatchThreshold < 1 || closeMatchThreshold > 99) {
            throw new IllegalArgumentException("closeMatchThreshold must be between 1 and 99");
        }
        this.closeMatchThreshold = closeMatchThreshold;
    }

    public NameMatchDecision decide(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        if (score == 100) {
            return NameMatchDecision.MATCH;
        }
        if (score >= closeMatchThreshold) {
            return NameMatchDecision.CLOSE_MATCH;
        }
        return NameMatchDecision.NO_MATCH;
    }
}
