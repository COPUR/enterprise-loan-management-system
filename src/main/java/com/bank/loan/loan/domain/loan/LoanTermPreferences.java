package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loanmanagement.loan.domain.shared.Money;

import java.util.List;
import java.util.Objects;

/**
 * Domain value object for customer loan preferences
 */
public class LoanTermPreferences {

    private final LoanTerm preferredTerm;
    private final InterestRate maxAcceptableRate;
    private final Money maxMonthlyPayment;
    private final boolean preferFixedRate;
    private final List<String> desiredFeatures;

    public LoanTermPreferences(
            LoanTerm preferredTerm,
            InterestRate maxAcceptableRate,
            Money maxMonthlyPayment,
            boolean preferFixedRate,
            List<String> desiredFeatures) {
        
        this.preferredTerm = preferredTerm;
        this.maxAcceptableRate = maxAcceptableRate;
        this.maxMonthlyPayment = maxMonthlyPayment;
        this.preferFixedRate = preferFixedRate;
        this.desiredFeatures = desiredFeatures != null ? List.copyOf(desiredFeatures) : List.of();
    }

    public static LoanTermPreferences defaultPreferences() {
        return new LoanTermPreferences(
                LoanTerm.ofYears(5),
                InterestRate.ofPercentage(12.0),
                null,
                true,
                List.of("Early Payment Option")
        );
    }

    public boolean isAcceptableRate(InterestRate rate) {
        return maxAcceptableRate == null || !rate.isHigherThan(maxAcceptableRate);
    }

    public boolean isAcceptablePayment(Money monthlyPayment) {
        return maxMonthlyPayment == null || !monthlyPayment.isGreaterThan(maxMonthlyPayment);
    }

    public boolean hasFeaturePreference(String feature) {
        return desiredFeatures.contains(feature);
    }

    // Getters
    public LoanTerm getPreferredTerm() { return preferredTerm; }
    public InterestRate getMaxAcceptableRate() { return maxAcceptableRate; }
    public Money getMaxMonthlyPayment() { return maxMonthlyPayment; }
    public boolean isPreferFixedRate() { return preferFixedRate; }
    public List<String> getDesiredFeatures() { return List.copyOf(desiredFeatures); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanTermPreferences that)) return false;
        return preferFixedRate == that.preferFixedRate &&
               Objects.equals(preferredTerm, that.preferredTerm) &&
               Objects.equals(maxAcceptableRate, that.maxAcceptableRate) &&
               Objects.equals(maxMonthlyPayment, that.maxMonthlyPayment) &&
               Objects.equals(desiredFeatures, that.desiredFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preferredTerm, maxAcceptableRate, maxMonthlyPayment, 
                          preferFixedRate, desiredFeatures);
    }
}