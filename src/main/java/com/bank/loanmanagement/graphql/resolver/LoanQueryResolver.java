package com.bank.loanmanagement.graphql.resolver;

import com.bank.loanmanagement.entity.Loan;
import com.bank.loanmanagement.entity.LoanInstallment;
import com.bank.loanmanagement.entity.Customer;
import com.bank.loanmanagement.entity.Payment;
import com.bank.loanmanagement.service.LoanService;
import com.bank.loanmanagement.service.CustomerService;
import com.bank.loanmanagement.service.PaymentService;
import com.bank.loanmanagement.graphql.dto.*;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class LoanQueryResolver {

    @Autowired
    private LoanService loanService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private PaymentService paymentService;

    public CompletableFuture<Loan> getLoan(DataFetchingEnvironment environment) {
        String id = environment.getArgument("id");
        return CompletableFuture.supplyAsync(() -> {
            return loanService.findById(Long.valueOf(id));
        });
    }

    public CompletableFuture<LoanConnection> getLoans(DataFetchingEnvironment environment) {
        Map<String, Object> filter = environment.getArgument("filter");
        Map<String, Object> pageInput = environment.getArgument("page");
        
        return CompletableFuture.supplyAsync(() -> {
            Pageable pageable = createPageable(pageInput);
            Page<Loan> loansPage = loanService.findLoansWithFilters(filter, pageable);
            
            return LoanConnection.builder()
                .nodes(loansPage.getContent().stream().map(loan -> (Object) loan).toList())
                .totalCount((int) loansPage.getTotalElements())
                .pageInfo(PageInfo.builder()
                    .hasNextPage(loansPage.hasNext())
                    .hasPreviousPage(loansPage.hasPrevious())
                    .build())
                .build();
        });
    }

    public CompletableFuture<List<Loan>> getLoansByCustomer(DataFetchingEnvironment environment) {
        String customerId = environment.getArgument("customerId");
        String status = environment.getArgument("status");
        
        return CompletableFuture.supplyAsync(() -> {
            if (status != null) {
                return loanService.findByCustomerIdAndStatus(Long.valueOf(customerId), status);
            }
            return loanService.findByCustomerId(Long.valueOf(customerId));
        });
    }

    public CompletableFuture<List<LoanInstallment>> getLoanInstallments(DataFetchingEnvironment environment) {
        String loanId = environment.getArgument("loanId");
        return CompletableFuture.supplyAsync(() -> {
            return loanService.getInstallments(Long.valueOf(loanId));
        });
    }

    // Field resolvers for Loan type
    public CompletableFuture<Customer> getLoanCustomer(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return customerService.findById(loan.getCustomerId());
        });
    }

    public CompletableFuture<List<LoanInstallment>> getLoanInstallmentsField(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return loanService.getInstallments(loan.getId());
        });
    }

    public CompletableFuture<List<Payment>> getLoanPayments(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.findByLoanId(loan.getId());
        });
    }

    public CompletableFuture<List<Object>> getLoanDocuments(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return loanService.getDocuments(loan.getId());
        });
    }

    public CompletableFuture<Object> getLoanPaymentHistory(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            List<Payment> payments = paymentService.findByLoanId(loan.getId());
            BigDecimal totalPaid = payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return Map.of(
                "totalPaid", totalPaid,
                "remainingAmount", loan.getOutstandingAmount(),
                "lastPaymentDate", payments.isEmpty() ? null : 
                    payments.get(payments.size() - 1).getPaymentDate(),
                "nextDueDate", getNextDueDate(loan.getId())
            );
        });
    }

    public CompletableFuture<LoanInstallment> getNextInstallment(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            List<LoanInstallment> installments = loanService.getInstallments(loan.getId());
            return installments.stream()
                .filter(installment -> "PENDING".equals(installment.getStatus()))
                .findFirst()
                .orElse(null);
        });
    }

    public CompletableFuture<BigDecimal> getOverdueAmount(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            List<LoanInstallment> installments = loanService.getInstallments(loan.getId());
            LocalDate today = LocalDate.now();
            
            return installments.stream()
                .filter(installment -> installment.getDueDate().isBefore(today) && 
                    "PENDING".equals(installment.getStatus()))
                .map(LoanInstallment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        });
    }

    public CompletableFuture<Integer> getDaysOverdue(DataFetchingEnvironment environment) {
        Loan loan = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            List<LoanInstallment> installments = loanService.getInstallments(loan.getId());
            LocalDate today = LocalDate.now();
            
            return installments.stream()
                .filter(installment -> installment.getDueDate().isBefore(today) && 
                    "PENDING".equals(installment.getStatus()))
                .map(installment -> (int) ChronoUnit.DAYS.between(installment.getDueDate(), today))
                .max(Integer::compareTo)
                .orElse(0);
        });
    }

    private Pageable createPageable(Map<String, Object> pageInput) {
        if (pageInput == null) {
            return PageRequest.of(0, 20);
        }
        
        int page = (Integer) pageInput.getOrDefault("page", 0);
        int size = (Integer) pageInput.getOrDefault("size", 20);
        
        return PageRequest.of(page, size);
    }

    private LocalDate getNextDueDate(Long loanId) {
        List<LoanInstallment> installments = loanService.getInstallments(loanId);
        return installments.stream()
            .filter(installment -> "PENDING".equals(installment.getStatus()))
            .map(LoanInstallment::getDueDate)
            .min(LocalDate::compareTo)
            .orElse(null);
    }
}