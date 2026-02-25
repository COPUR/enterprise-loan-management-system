package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Calculation for loan payoff
 */
public record PayoffCalculation(
    Money totalPayoffAmount,
    Money principalBalance,
    Money accruedInterest,
    Money fees,
    LocalDate payoffDate,
    BigDecimal dailyInterestRate,
    int daysUntilPayoff
) {
    
    public static PayoffCalculationBuilder builder() {
        return new PayoffCalculationBuilder();
    }
    
    public static class PayoffCalculationBuilder {
        private Money currentBalance;
        private Money accruedInterest;
        private Money payoffAmount;
        private Money interestSavings;
        private LocalDate payoffDate;
        private int remainingPayments;
        
        public PayoffCalculationBuilder currentBalance(Money currentBalance) {
            this.currentBalance = currentBalance;
            return this;
        }
        
        public PayoffCalculationBuilder accruedInterest(Money accruedInterest) {
            this.accruedInterest = accruedInterest;
            return this;
        }
        
        public PayoffCalculationBuilder payoffAmount(Money payoffAmount) {
            this.payoffAmount = payoffAmount;
            return this;
        }
        
        public PayoffCalculationBuilder interestSavings(Money interestSavings) {
            this.interestSavings = interestSavings;
            return this;
        }
        
        public PayoffCalculationBuilder payoffDate(LocalDate payoffDate) {
            this.payoffDate = payoffDate;
            return this;
        }
        
        public PayoffCalculationBuilder remainingPayments(int remainingPayments) {
            this.remainingPayments = remainingPayments;
            return this;
        }
        
        public PayoffCalculation build() {
            return new PayoffCalculation(
                payoffAmount,
                currentBalance,
                accruedInterest,
                Money.of(currentBalance.getCurrency(), BigDecimal.ZERO),
                payoffDate,
                BigDecimal.ZERO,
                remainingPayments
            );
        }
    }
}