package com.bank.loanmanagement.graphql.resolver;

import com.bank.loanmanagement.entity.Payment;
import com.bank.loanmanagement.entity.Loan;
import com.bank.loanmanagement.entity.Customer;
import com.bank.loanmanagement.service.PaymentService;
import com.bank.loanmanagement.service.LoanService;
import com.bank.loanmanagement.service.CustomerService;
import com.bank.loanmanagement.graphql.dto.*;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class PaymentQueryResolver {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private CustomerService customerService;

    public CompletableFuture<Payment> getPayment(DataFetchingEnvironment environment) {
        String id = environment.getArgument("id");
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.findById(Long.valueOf(id));
        });
    }

    public CompletableFuture<PaymentConnection> getPayments(DataFetchingEnvironment environment) {
        Map<String, Object> filter = environment.getArgument("filter");
        Map<String, Object> pageInput = environment.getArgument("page");
        
        return CompletableFuture.supplyAsync(() -> {
            Pageable pageable = createPageable(pageInput);
            Page<Payment> paymentsPage = paymentService.findPaymentsWithFilters(filter, pageable);
            
            return PaymentConnection.builder()
                .nodes(paymentsPage.getContent().stream().map(payment -> (Object) payment).toList())
                .totalCount((int) paymentsPage.getTotalElements())
                .pageInfo(PageInfo.builder()
                    .hasNextPage(paymentsPage.hasNext())
                    .hasPreviousPage(paymentsPage.hasPrevious())
                    .build())
                .build();
        });
    }

    public CompletableFuture<List<Payment>> getPaymentsByLoan(DataFetchingEnvironment environment) {
        String loanId = environment.getArgument("loanId");
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.findByLoanId(Long.valueOf(loanId));
        });
    }

    public CompletableFuture<Object> calculatePayment(DataFetchingEnvironment environment) {
        Map<String, Object> input = environment.getArgument("input");
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.calculatePaymentWithDetails(input);
        });
    }

    // Field resolvers for Payment type
    public CompletableFuture<Loan> getPaymentLoan(DataFetchingEnvironment environment) {
        Payment payment = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return loanService.findById(payment.getLoanId());
        });
    }

    public CompletableFuture<Customer> getPaymentCustomer(DataFetchingEnvironment environment) {
        Payment payment = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return customerService.findById(payment.getCustomerId());
        });
    }

    public CompletableFuture<List<Object>> getInstallmentPayments(DataFetchingEnvironment environment) {
        Payment payment = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.getInstallmentPayments(payment.getId());
        });
    }

    public CompletableFuture<Object> getPaymentCalculationResult(DataFetchingEnvironment environment) {
        Payment payment = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.getPaymentCalculationDetails(payment.getId());
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
}