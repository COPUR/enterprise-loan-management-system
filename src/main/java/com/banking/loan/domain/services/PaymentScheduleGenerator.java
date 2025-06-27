package com.banking.loan.domain.services;

import com.banking.loan.domain.loan.LoanAmount;
import com.banking.loan.domain.loan.LoanTerm;
import com.banking.loan.domain.loan.InterestRate;
import com.banking.loan.domain.loan.PaymentSchedule;
import com.banking.loan.domain.loan.LoanInstallment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for generating payment schedules
 * Following DDD principles - pure domain logic with no infrastructure concerns
 */
public class PaymentScheduleGenerator {
    
    /**
     * Generate payment schedule using standard amortization formula
     * Following 12-Factor App principle: configuration-driven calculations
     */
    public static PaymentSchedule generate(LoanAmount loanAmount, LoanTerm term, InterestRate interestRate) {
        BigDecimal principal = loanAmount.value();
        int numberOfPayments = term.months();
        BigDecimal monthlyRate = interestRate.getMonthlyRate();
        
        // Calculate monthly payment using amortization formula
        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, monthlyRate, numberOfPayments);
        
        List<LoanInstallment> installments = new ArrayList<>();
        BigDecimal remainingBalance = principal;
        LocalDate paymentDate = LocalDate.now().plusMonths(1);
        
        for (int paymentNumber = 1; paymentNumber <= numberOfPayments; paymentNumber++) {
            BigDecimal interestPayment = remainingBalance.multiply(monthlyRate)
                .setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal principalPayment = monthlyPayment.subtract(interestPayment);
            
            // Adjust last payment for any rounding differences
            if (paymentNumber == numberOfPayments) {
                principalPayment = remainingBalance;
                monthlyPayment = principalPayment.add(interestPayment);
            }
            
            remainingBalance = remainingBalance.subtract(principalPayment);
            
            LoanInstallment installment = LoanInstallment.create(
                paymentNumber,
                paymentDate,
                monthlyPayment,
                principalPayment,
                interestPayment,
                remainingBalance
            );
            
            installments.add(installment);
            paymentDate = paymentDate.plusMonths(1);
        }
        
        return new PaymentSchedule(
            LocalDate.now(),
            numberOfPayments,
            monthlyPayment,
            principal,
            installments
        );
    }
    
    private static BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int numberOfPayments) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // No interest case
            return principal.divide(BigDecimal.valueOf(numberOfPayments), 2, RoundingMode.HALF_UP);
        }
        
        // Standard amortization formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowN = onePlusRate.pow(numberOfPayments);
        
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRatePowN);
        BigDecimal denominator = onePlusRatePowN.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}