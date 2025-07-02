package com.bank.loan.sharedkernel.domain.valueobjects;

public class CreditScore {

    private final int score;

    private CreditScore(int score) {
        this.score = score;
    }

    public static CreditScore of(int score) {
        return new CreditScore(score);
    }

    public int getScore() {
        return score;
    }
}
