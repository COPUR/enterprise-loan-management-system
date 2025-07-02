package com.bank.loan.sharedkernel.domain;

import java.math.BigDecimal;

public class Money {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, "USD");
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.getAmount()), this.currency);
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.getAmount()), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.getAmount()) > 0;
    }
}
