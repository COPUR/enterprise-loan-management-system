package com.loanmanagement.loan.domain.model;

import com.loanmanagement.payment.domain.model.ScheduledPayment;
import com.loanmanagement.shared.domain.Money;
import java.time.LocalDateTime;
import java.util.SequencedSet;
import java.util.List;

/**
 * Amortization schedule for a loan
 */
public record AmortizationSchedule(
    LoanId loanId,
    SequencedSet<AmortizationEntry> amortizationEntries,
    LocalDateTime generatedAt
) {
    
    public static AmortizationScheduleBuilder builder() {
        return new AmortizationScheduleBuilder();
    }
    
    public static class AmortizationScheduleBuilder {
        private Money principalAmount;
        private LoanTerms terms;
        private List<AmortizationEntry> payments;
        private Money totalPayments;
        private Money totalInterest;
        
        public AmortizationScheduleBuilder principalAmount(Money principalAmount) {
            this.principalAmount = principalAmount;
            return this;
        }
        
        public AmortizationScheduleBuilder terms(LoanTerms terms) {
            this.terms = terms;
            return this;
        }
        
        public AmortizationScheduleBuilder payments(List<AmortizationEntry> payments) {
            this.payments = payments;
            return this;
        }
        
        public AmortizationScheduleBuilder totalPayments(Money totalPayments) {
            this.totalPayments = totalPayments;
            return this;
        }
        
        public AmortizationScheduleBuilder totalInterest(Money totalInterest) {
            this.totalInterest = totalInterest;
            return this;
        }
        
        public AmortizationSchedule build() {
            return new AmortizationSchedule(
                LoanId.generate(),
                new java.util.LinkedHashSet<>(payments),
                LocalDateTime.now()
            );
        }
    }
}