package com.loanmanagement.loan.domain.service;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import com.loanmanagement.payment.domain.model.PaymentAllocation;
import com.loanmanagement.payment.domain.model.ScheduledPayment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan Calculation Service
 * Handles all loan-related calculations including payments, amortization, and payoff amounts
 */
@Slf4j
@Service
public class LoanCalculationService {

    private static final int CALCULATION_SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculate monthly payment amount
     */
    public Money calculateMonthlyPayment(Money principalAmount, LoanTerms terms) {
        return calculatePaymentAmount(principalAmount, terms);
    }

    /**
     * Calculate payment amount based on frequency
     */
    public Money calculatePaymentAmount(Money principalAmount, LoanTerms terms) {
        BigDecimal principal = principalAmount.getAmount();
        BigDecimal annualRate = terms.getInterestRate().divide(new BigDecimal("100"), CALCULATION_SCALE, ROUNDING_MODE);
        
        // Handle zero interest rate
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            int totalPayments = calculateTotalPayments(terms);
            BigDecimal payment = principal.divide(new BigDecimal(totalPayments), 2, ROUNDING_MODE);
            return Money.of(principalAmount.getCurrency(), payment);
        }
        
        // Convert annual rate to payment frequency rate
        BigDecimal periodRate = calculatePeriodRate(annualRate, terms.getPaymentFrequency());
        int totalPayments = calculateTotalPayments(terms);
        
        // Calculate payment using amortization formula: P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusR = BigDecimal.ONE.add(periodRate);
        BigDecimal onePlusRPowN = onePlusR.pow(totalPayments);
        BigDecimal numerator = principal.multiply(periodRate).multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        
        BigDecimal payment = numerator.divide(denominator, 2, ROUNDING_MODE);
        return Money.of(principalAmount.getCurrency(), payment);
    }

    /**
     * Allocate payment between principal, interest, and fees
     */
    public PaymentAllocation allocatePayment(Money paymentAmount, Money currentBalance, LoanTerms terms) {
        BigDecimal annualRate = terms.getInterestRate().divide(new BigDecimal("100"), CALCULATION_SCALE, ROUNDING_MODE);
        BigDecimal periodRate = calculatePeriodRate(annualRate, terms.getPaymentFrequency());
        
        // Calculate interest portion
        Money interestAmount = Money.of(currentBalance.getCurrency(),
                currentBalance.getAmount().multiply(periodRate).setScale(2, ROUNDING_MODE));
        
        // Principal is remaining amount after interest
        Money principalAmount = paymentAmount.subtract(interestAmount);
        
        // Handle overpayment
        Money excessAmount = null;
        if (principalAmount.getAmount().compareTo(currentBalance.getAmount()) > 0) {
            excessAmount = principalAmount.subtract(currentBalance);
            principalAmount = currentBalance;
        }
        
        return PaymentAllocation.builder()
                .principalAmount(principalAmount.getAmount())
                .interestAmount(interestAmount.getAmount())
                .feesAmount(BigDecimal.ZERO)
                .totalAmount(paymentAmount.getAmount())
                .build();
    }

    /**
     * Generate complete amortization schedule
     */
    public AmortizationSchedule generateAmortizationSchedule(Money principalAmount, LoanTerms terms) {
        List<AmortizationEntry> payments = new ArrayList<>();
        Money paymentAmount = calculatePaymentAmount(principalAmount, terms);
        Money remainingBalance = principalAmount;
        
        int totalPayments = calculateTotalPayments(terms);
        BigDecimal annualRate = terms.getInterestRate().divide(new BigDecimal("100"), CALCULATION_SCALE, ROUNDING_MODE);
        BigDecimal periodRate = calculatePeriodRate(annualRate, terms.getPaymentFrequency());
        
        for (int paymentNumber = 1; paymentNumber <= totalPayments; paymentNumber++) {
            // Calculate interest for this period
            Money interestAmount = Money.of(principalAmount.getCurrency(),
                    remainingBalance.getAmount().multiply(periodRate).setScale(2, ROUNDING_MODE));
            
            // Calculate principal portion
            Money principalPortion = paymentAmount.subtract(interestAmount);
            
            // Adjust for final payment to handle rounding
            if (paymentNumber == totalPayments) {
                principalPortion = remainingBalance;
                paymentAmount = principalPortion.add(interestAmount);
            }
            
            // Update remaining balance
            remainingBalance = remainingBalance.subtract(principalPortion);
            
            // Create scheduled payment
            AmortizationEntry amortizationEntry = AmortizationEntry.builder()
                    .paymentNumber(paymentNumber)
                    .paymentAmount(paymentAmount)
                    .principalAmount(principalPortion)
                    .interestAmount(interestAmount)
                    .remainingBalance(remainingBalance)
                    .build();
            
            payments.add(amortizationEntry);
        }
        
        return AmortizationSchedule.builder()
                .principalAmount(principalAmount)
                .terms(terms)
                .payments(payments)
                .totalPayments(calculateTotalPayments(payments))
                .totalInterest(calculateTotalInterest(payments))
                .build();
    }

    /**
     * Calculate payoff amount for early payoff
     */
    public PayoffCalculation calculatePayoffAmount(Money currentBalance, LoanTerms terms, 
                                                  LocalDate payoffDate, LocalDate lastPaymentDate) {
        
        // Calculate accrued interest since last payment
        long daysSinceLastPayment = ChronoUnit.DAYS.between(lastPaymentDate, payoffDate);
        BigDecimal dailyRate = calculateDailyRate(terms.getInterestRate());
        
        Money accruedInterest = Money.of(currentBalance.getCurrency(),
                currentBalance.getAmount()
                        .multiply(dailyRate)
                        .multiply(new BigDecimal(daysSinceLastPayment))
                        .setScale(2, ROUNDING_MODE));
        
        Money payoffAmount = currentBalance.add(accruedInterest);
        
        // Calculate interest savings
        int remainingPayments = calculateRemainingPayments(currentBalance, terms);
        Money monthlyPayment = calculatePaymentAmount(currentBalance, terms);
        Money totalRemainingPayments = Money.of(currentBalance.getCurrency(),
                monthlyPayment.getAmount().multiply(new BigDecimal(remainingPayments)));
        Money interestSavings = totalRemainingPayments.subtract(payoffAmount);
        
        return PayoffCalculation.builder()
                .currentBalance(currentBalance)
                .accruedInterest(accruedInterest)
                .payoffAmount(payoffAmount)
                .interestSavings(interestSavings)
                .payoffDate(payoffDate)
                .remainingPayments(remainingPayments)
                .build();
    }

    /**
     * Calculate remaining number of payments
     */
    public int calculateRemainingPayments(Money currentBalance, LoanTerms terms) {
        Money monthlyPayment = calculatePaymentAmount(currentBalance, terms);
        BigDecimal annualRate = terms.getInterestRate().divide(new BigDecimal("100"), CALCULATION_SCALE, ROUNDING_MODE);
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), CALCULATION_SCALE, ROUNDING_MODE);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return currentBalance.getAmount().divide(monthlyPayment.getAmount(), 0, RoundingMode.CEILING).intValue();
        }
        
        // Calculate remaining payments using logarithm
        // n = -log(1 - (B * r / P)) / log(1 + r)
        BigDecimal balanceTimesRate = currentBalance.getAmount().multiply(monthlyRate);
        BigDecimal paymentAmount = monthlyPayment.getAmount();
        
        if (balanceTimesRate.compareTo(paymentAmount) >= 0) {
            throw new IllegalArgumentException("Payment amount is insufficient to cover interest");
        }
        
        double numerator = Math.log(1 - balanceTimesRate.divide(paymentAmount, CALCULATION_SCALE, ROUNDING_MODE).doubleValue());
        double denominator = Math.log(1 + monthlyRate.doubleValue());
        
        return (int) Math.ceil(-numerator / denominator);
    }

    /**
     * Calculate late fee
     */
    public Money calculateLateFee(Money missedPayment, int daysLate, LateFeeStructure feeStructure) {
        if (daysLate <= feeStructure.getGracePeriodDays()) {
            return Money.of(missedPayment.getCurrency(), BigDecimal.ZERO);
        }
        
        // Calculate percentage fee
        Money percentageFee = Money.of(missedPayment.getCurrency(),
                missedPayment.getAmount()
                        .multiply(feeStructure.getPercentageFee())
                        .divide(new BigDecimal("100"), 2, ROUNDING_MODE));
        
        // Use higher of flat fee or percentage fee
        Money lateFee = feeStructure.getFlatFee().getAmount().compareTo(percentageFee.getAmount()) > 0 
                ? feeStructure.getFlatFee() : percentageFee;
        
        // Apply maximum fee limit
        if (lateFee.getAmount().compareTo(feeStructure.getMaxFee().getAmount()) > 0) {
            lateFee = feeStructure.getMaxFee();
        }
        
        return lateFee;
    }

    // Private helper methods
    
    private BigDecimal calculatePeriodRate(BigDecimal annualRate, PaymentFrequency frequency) {
        int periodsPerYear = getPeriodsPerYear(frequency);
        return annualRate.divide(new BigDecimal(periodsPerYear), CALCULATION_SCALE, ROUNDING_MODE);
    }
    
    private BigDecimal calculateDailyRate(BigDecimal annualRatePercent) {
        BigDecimal annualRate = annualRatePercent.divide(new BigDecimal("100"), CALCULATION_SCALE, ROUNDING_MODE);
        return annualRate.divide(new BigDecimal("365"), CALCULATION_SCALE, ROUNDING_MODE);
    }
    
    private int calculateTotalPayments(LoanTerms terms) {
        int periodsPerYear = getPeriodsPerYear(terms.getPaymentFrequency());
        return (terms.getTermInMonths() * periodsPerYear) / 12;
    }
    
    private int getPeriodsPerYear(PaymentFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case SEMI_ANNUALLY -> 2;
            case ANNUALLY -> 1;
        };
    }
    
    private Money calculateTotalPayments(List<AmortizationEntry> payments) {
        if (payments.isEmpty()) {
            return Money.of("USD", BigDecimal.ZERO);
        }
        
        return payments.stream()
                .map(AmortizationEntry::getPaymentAmount)
                .reduce(Money.of(payments.get(0).getPaymentAmount().getCurrency(), BigDecimal.ZERO), Money::add);
    }
    
    private Money calculateTotalInterest(List<AmortizationEntry> payments) {
        if (payments.isEmpty()) {
            return Money.of("USD", BigDecimal.ZERO);
        }
        
        return payments.stream()
                .map(AmortizationEntry::getInterestAmount)
                .reduce(Money.of(payments.get(0).getInterestAmount().getCurrency(), BigDecimal.ZERO), Money::add);
    }
}