package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LoanEligibilityService {
    
    private static final int MINIMUM_CREDIT_SCORE = 600;
    private static final BigDecimal MAXIMUM_DEBT_TO_INCOME_RATIO = BigDecimal.valueOf(0.43); // 43%
    
    public boolean isEligibleForLoan(Customer customer, Money requestedAmount, LoanType loanType) {
        if (!customer.isEligibleForLoan(requestedAmount)) {
            return false;
        }
        
        if (!meetsCreditScoreRequirement(customer, loanType)) {
            return false;
        }
        
        if (!meetsAmountRequirement(requestedAmount, loanType)) {
            return false;
        }
        
        // Additional business rules can be added here
        return true;
    }
    
    private boolean meetsCreditScoreRequirement(Customer customer, LoanType loanType) {
        if (customer.getCreditScore() == null) {
            return false;
        }
        
        int requiredScore = getMinimumCreditScoreForLoanType(loanType);
        return customer.getCreditScore().getScore() >= requiredScore;
    }
    
    private boolean meetsAmountRequirement(Money requestedAmount, LoanType loanType) {
        Money maxAmount = getMaximumLoanAmountForType(loanType);
        return requestedAmount.isLessThan(maxAmount) || requestedAmount.equals(maxAmount);
    }
    
    private int getMinimumCreditScoreForLoanType(LoanType loanType) {
        return switch (loanType) {
            case PERSONAL -> 650;
            case HOME -> 620;
            case AUTO -> 600;
            case BUSINESS -> 680;
            case STUDENT -> 580;
            case CREDIT_LINE -> 700;
        };
    }
    
    private Money getMaximumLoanAmountForType(LoanType loanType) {
        return switch (loanType) {
            case PERSONAL -> Money.of(BigDecimal.valueOf(50000), java.util.Currency.getInstance("USD"));
            case HOME -> Money.of(BigDecimal.valueOf(500000), java.util.Currency.getInstance("USD"));
            case AUTO -> Money.of(BigDecimal.valueOf(100000), java.util.Currency.getInstance("USD"));
            case BUSINESS -> Money.of(BigDecimal.valueOf(1000000), java.util.Currency.getInstance("USD"));
            case STUDENT -> Money.of(BigDecimal.valueOf(200000), java.util.Currency.getInstance("USD"));
            case CREDIT_LINE -> Money.of(BigDecimal.valueOf(25000), java.util.Currency.getInstance("USD"));
        };
    }
    
    public BigDecimal calculateMaximumLoanAmount(Customer customer) {
        if (customer.getCreditScore() == null) {
            return BigDecimal.ZERO;
        }
        
        // Simplified calculation based on credit score
        int creditScore = customer.getCreditScore().getScore();
        BigDecimal baseAmount = BigDecimal.valueOf(10000);
        
        if (creditScore >= 800) {
            return baseAmount.multiply(BigDecimal.valueOf(5)); // $50,000
        } else if (creditScore >= 750) {
            return baseAmount.multiply(BigDecimal.valueOf(4)); // $40,000
        } else if (creditScore >= 700) {
            return baseAmount.multiply(BigDecimal.valueOf(3)); // $30,000
        } else if (creditScore >= 650) {
            return baseAmount.multiply(BigDecimal.valueOf(2)); // $20,000
        } else {
            return baseAmount; // $10,000
        }
    }
    
    public BigDecimal calculateInterestRate(Customer customer, LoanType loanType) {
        if (customer.getCreditScore() == null) {
            return getDefaultInterestRateForType(loanType);
        }
        
        BigDecimal baseRate = getBaseInterestRateForType(loanType);
        BigDecimal creditScoreAdjustment = calculateCreditScoreAdjustment(customer.getCreditScore().getScore());
        
        return baseRate.add(creditScoreAdjustment);
    }
    
    private BigDecimal getBaseInterestRateForType(LoanType loanType) {
        return switch (loanType) {
            case PERSONAL -> BigDecimal.valueOf(0.12); // 12%
            case HOME -> BigDecimal.valueOf(0.04); // 4%
            case AUTO -> BigDecimal.valueOf(0.06); // 6%
            case BUSINESS -> BigDecimal.valueOf(0.08); // 8%
            case STUDENT -> BigDecimal.valueOf(0.05); // 5%
            case CREDIT_LINE -> BigDecimal.valueOf(0.15); // 15%
        };
    }
    
    private BigDecimal getDefaultInterestRateForType(LoanType loanType) {
        return getBaseInterestRateForType(loanType).add(BigDecimal.valueOf(0.05)); // Add 5% for no credit score
    }
    
    private BigDecimal calculateCreditScoreAdjustment(int creditScore) {
        if (creditScore >= 800) {
            return BigDecimal.valueOf(-0.02); // -2% for excellent credit
        } else if (creditScore >= 750) {
            return BigDecimal.valueOf(-0.01); // -1% for very good credit
        } else if (creditScore >= 700) {
            return BigDecimal.ZERO; // No adjustment for good credit
        } else if (creditScore >= 650) {
            return BigDecimal.valueOf(0.01); // +1% for fair credit
        } else {
            return BigDecimal.valueOf(0.03); // +3% for poor credit
        }
    }
}