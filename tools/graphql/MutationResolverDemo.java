package com.bank.loanmanagement.infrastructure.graphql;

import com.bank.loanmanagement.infrastructure.graphql.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GraphQL Mutation resolver with demo implementations
 * Following hexagonal architecture principles
 */
@Controller
@Slf4j
public class MutationResolverDemo {

    @MutationMapping
    public CustomerMutationResultGraphQL createCustomer(@Argument CreateCustomerInputGraphQL input) {
        log.info("GraphQL Mutation: createCustomer for {}", input.getEmail());
        
        try {
            CustomerGraphQL customer = CustomerGraphQL.builder()
                    .id("CUST-" + System.currentTimeMillis())
                    .customerId(input.getCustomerId())
                    .firstName(input.getFirstName())
                    .lastName(input.getLastName())
                    .fullName(input.getFirstName() + " " + input.getLastName())
                    .email(input.getEmail())
                    .phone(input.getPhone())
                    .dateOfBirth(input.getDateOfBirth())
                    .creditLimit(input.getCreditLimit())
                    .availableCredit(input.getCreditLimit())
                    .usedCredit(BigDecimal.ZERO)
                    .annualIncome(input.getAnnualIncome())
                    .employmentStatus(input.getEmploymentStatus())
                    .creditScore(650) // Default credit score
                    .accountStatus("ACTIVE")
                    .riskLevel("MEDIUM")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            return CustomerSuccessGraphQL.builder()
                    .customer(customer)
                    .message("Customer created successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create customer", e);
            return CustomerErrorGraphQL.builder()
                    .message("Failed to create customer: " + e.getMessage())
                    .code("CUSTOMER_CREATION_FAILED")
                    .field("general")
                    .build();
        }
    }

    @MutationMapping
    public LoanMutationResultGraphQL createLoan(@Argument CreateLoanInputGraphQL input) {
        log.info("GraphQL Mutation: createLoan for customer {}", input.getCustomerId());
        
        try {
            LoanGraphQL loan = LoanGraphQL.builder()
                    .id("LOAN-" + System.currentTimeMillis())
                    .loanId("L" + System.currentTimeMillis())
                    .loanAmount(input.getLoanAmount())
                    .outstandingAmount(input.getLoanAmount())
                    .interestRate(input.getInterestRate())
                    .installmentCount(input.getInstallmentCount())
                    .installmentAmount(calculateInstallmentAmount(input.getLoanAmount(), input.getInterestRate(), input.getInstallmentCount()))
                    .totalRepaymentAmount(calculateTotalRepayment(input.getLoanAmount(), input.getInterestRate(), input.getInstallmentCount()))
                    .loanType(input.getLoanType())
                    .purpose(input.getPurpose())
                    .status("PENDING_APPROVAL")
                    .applicationDate(LocalDateTime.now())
                    .maturityDate(LocalDate.now().plusMonths(input.getInstallmentCount()))
                    .overdueAmount(BigDecimal.ZERO)
                    .daysOverdue(0)
                    .build();
            
            return LoanSuccessGraphQL.builder()
                    .loan(loan)
                    .message("Loan application created successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create loan", e);
            return LoanErrorGraphQL.builder()
                    .message("Failed to create loan: " + e.getMessage())
                    .code("LOAN_CREATION_FAILED")
                    .field("general")
                    .build();
        }
    }

    @MutationMapping
    public PaymentMutationResultGraphQL processPayment(@Argument ProcessPaymentInputGraphQL input) {
        log.info("GraphQL Mutation: processPayment for loan {}", input.getLoanId());
        
        try {
            PaymentGraphQL payment = PaymentGraphQL.builder()
                    .id("PAY-" + System.currentTimeMillis())
                    .paymentId("P" + System.currentTimeMillis())
                    .paymentAmount(input.getPaymentAmount())
                    .paymentDate(LocalDateTime.now())
                    .paymentMethod(input.getPaymentMethod())
                    .paymentReference(input.getPaymentReference())
                    .status("COMPLETED")
                    .processingFee(input.getPaymentAmount().multiply(new BigDecimal("0.01"))) // 1% fee
                    .totalAmount(input.getPaymentAmount().multiply(new BigDecimal("1.01")))
                    .build();
            
            return PaymentSuccessGraphQL.builder()
                    .payment(payment)
                    .message("Payment processed successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to process payment", e);
            return PaymentErrorGraphQL.builder()
                    .message("Failed to process payment: " + e.getMessage())
                    .code("PAYMENT_PROCESSING_FAILED")
                    .field("general")
                    .build();
        }
    }

    @MutationMapping
    public String processBankingQuery(@Argument String query, @Argument String customerId) {
        log.info("GraphQL Mutation: processBankingQuery for customer {}", customerId);
        
        // Simple demo response for banking queries
        return String.format(
            "AI Banking Assistant Response: Based on your query '%s', I can help you with banking services. " +
            "Customer %s has an active account with loan opportunities available. " +
            "Would you like me to provide specific loan recommendations or account details?",
            query, customerId != null ? customerId : "guest"
        );
    }

    // Helper methods for loan calculations
    private BigDecimal calculateInstallmentAmount(BigDecimal loanAmount, BigDecimal annualRate, Integer months) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return loanAmount.divide(new BigDecimal(months), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("100")).divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal factor = monthlyRate.multiply(
            BigDecimal.ONE.add(monthlyRate).pow(months)
        ).divide(
            BigDecimal.ONE.add(monthlyRate).pow(months).subtract(BigDecimal.ONE),
            10, java.math.RoundingMode.HALF_UP
        );
        
        return loanAmount.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateTotalRepayment(BigDecimal loanAmount, BigDecimal annualRate, Integer months) {
        BigDecimal installmentAmount = calculateInstallmentAmount(loanAmount, annualRate, months);
        return installmentAmount.multiply(new BigDecimal(months));
    }
}