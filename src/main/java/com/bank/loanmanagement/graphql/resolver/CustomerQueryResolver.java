package com.bank.loanmanagement.graphql.resolver;

import com.bank.loanmanagement.entity.Customer;
import com.bank.loanmanagement.entity.Loan;
import com.bank.loanmanagement.entity.Payment;
import com.bank.loanmanagement.service.CustomerService;
import com.bank.loanmanagement.service.LoanService;
import com.bank.loanmanagement.service.PaymentService;
import com.bank.loanmanagement.graphql.dto.*;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class CustomerQueryResolver {

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private PaymentService paymentService;

    public CompletableFuture<Customer> getCustomer(DataFetchingEnvironment environment) {
        String id = environment.getArgument("id");
        return CompletableFuture.supplyAsync(() -> {
            return customerService.findById(Long.valueOf(id));
        });
    }

    public CompletableFuture<CustomerConnection> getCustomers(DataFetchingEnvironment environment) {
        Map<String, Object> filter = environment.getArgument("filter");
        Map<String, Object> pageInput = environment.getArgument("page");
        
        return CompletableFuture.supplyAsync(() -> {
            Pageable pageable = createPageable(pageInput);
            Page<Customer> customersPage = customerService.findCustomersWithFilters(filter, pageable);
            
            return CustomerConnection.builder()
                .nodes(customersPage.getContent())
                .totalCount((int) customersPage.getTotalElements())
                .pageInfo(PageInfo.builder()
                    .hasNextPage(customersPage.hasNext())
                    .hasPreviousPage(customersPage.hasPrevious())
                    .build())
                .build();
        });
    }

    public CompletableFuture<List<CreditTransaction>> getCustomerCreditHistory(DataFetchingEnvironment environment) {
        String customerId = environment.getArgument("customerId");
        Integer limit = environment.getArgument("limit");
        
        return CompletableFuture.supplyAsync(() -> {
            return customerService.getCreditHistory(Long.valueOf(customerId), limit != null ? limit : 10);
        });
    }

    // Field resolvers for Customer type
    public CompletableFuture<List<Loan>> getCustomerLoans(DataFetchingEnvironment environment) {
        Customer customer = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return loanService.findByCustomerId(customer.getId());
        });
    }

    public CompletableFuture<List<Payment>> getCustomerPayments(DataFetchingEnvironment environment) {
        Customer customer = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return paymentService.findByCustomerId(customer.getId());
        });
    }

    public CompletableFuture<List<CreditTransaction>> getCustomerCreditHistoryField(DataFetchingEnvironment environment) {
        Customer customer = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return customerService.getCreditHistory(customer.getId(), 10);
        });
    }

    public CompletableFuture<RiskProfile> getCustomerRiskProfile(DataFetchingEnvironment environment) {
        Customer customer = environment.getSource();
        return CompletableFuture.supplyAsync(() -> {
            return customerService.calculateRiskProfile(customer.getId());
        });
    }

    private Pageable createPageable(Map<String, Object> pageInput) {
        if (pageInput == null) {
            return PageRequest.of(0, 20);
        }
        
        int page = (Integer) pageInput.getOrDefault("page", 0);
        int size = (Integer) pageInput.getOrDefault("size", 20);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sortList = (List<Map<String, Object>>) pageInput.get("sort");
        
        if (sortList != null && !sortList.isEmpty()) {
            Sort sort = Sort.unsorted();
            for (Map<String, Object> sortItem : sortList) {
                String field = (String) sortItem.get("field");
                String direction = (String) sortItem.get("direction");
                Sort.Direction dir = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
                sort = sort.and(Sort.by(dir, field));
            }
            return PageRequest.of(page, size, sort);
        }
        
        return PageRequest.of(page, size);
    }
}