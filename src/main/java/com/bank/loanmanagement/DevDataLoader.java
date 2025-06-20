package com.bank.loanmanagement;

import com.bank.loanmanagement.domain.customer.Customer;
import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.customer.CustomerType;
import com.bank.loanmanagement.domain.customer.CustomerStatus;
import com.bank.loanmanagement.domain.customer.Address;
import com.bank.loanmanagement.domain.customer.AddressType;
import com.bank.loanmanagement.domain.customer.CreditScore;
import com.bank.loanmanagement.domain.loan.Loan;
import com.bank.loanmanagement.domain.loan.LoanId;
import com.bank.loanmanagement.domain.loan.LoanType;
import com.bank.loanmanagement.domain.loan.LoanStatus;
import com.bank.loanmanagement.domain.payment.Payment;
import com.bank.loanmanagement.domain.payment.PaymentMethod;
import com.bank.loanmanagement.domain.shared.Money;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Development data loader for creating test data in local development environment
 */
@SpringBootApplication
public class DevDataLoader {
    
    public static void main(String[] args) {
        SpringApplication.run(DevDataLoader.class, args);
    }
    
    @Component
    @Profile({"local", "development"})
    static class DataLoader implements CommandLineRunner {
        
        @Override
        @Transactional
        public void run(String... args) throws Exception {
            System.out.println("🏦 Loading development test data...");
            
            // Create test customers
            createTestCustomers();
            
            // Create test loans
            createTestLoans();
            
            // Create test payments
            createTestPayments();
            
            System.out.println("✅ Development test data loaded successfully!");
        }
        
        private void createTestCustomers() {
            // Individual customers
            Customer customer1 = Customer.builder()
                .customerId(new CustomerId("DEV-CUST-001"))
                .firstName("John")
                .lastName("Developer")
                .email("john.dev@example.com")
                .phone("+1-555-0001")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .customerType(CustomerType.INDIVIDUAL)
                .status(CustomerStatus.ACTIVE)
                .build();
            
            customer1.addAddress(Address.builder()
                .street("123 Development Street")
                .city("Code City")
                .state("CA")
                .zipCode("90210")
                .country("USA")
                .type(AddressType.HOME)
                .isPrimary(true)
                .build());
            
            customer1.updateCreditScore(new CreditScore(750, "EXPERIAN", LocalDateTime.now()));
            
            Customer customer2 = Customer.builder()
                .customerId(new CustomerId("DEV-CUST-002"))
                .firstName("Jane")
                .lastName("Tester")
                .email("jane.test@example.com")
                .phone("+1-555-0002")
                .dateOfBirth(LocalDate.of(1990, 7, 22))
                .customerType(CustomerType.INDIVIDUAL)
                .status(CustomerStatus.ACTIVE)
                .build();
            
            customer2.addAddress(Address.builder()
                .street("456 Testing Avenue")
                .city("QA City")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .type(AddressType.HOME)
                .isPrimary(true)
                .build());
            
            customer2.updateCreditScore(new CreditScore(680, "EQUIFAX", LocalDateTime.now()));
            
            // Corporate customer
            Customer corporateCustomer = Customer.builder()
                .customerId(new CustomerId("DEV-CORP-001"))
                .firstName("Tech Startup")
                .lastName("Inc")
                .email("finance@techstartup.com")
                .phone("+1-555-0100")
                .dateOfBirth(LocalDate.of(2020, 1, 1))
                .customerType(CustomerType.CORPORATE)
                .status(CustomerStatus.ACTIVE)
                .build();
            
            corporateCustomer.addAddress(Address.builder()
                .street("789 Innovation Blvd")
                .city("Silicon Valley")
                .state("CA")
                .zipCode("94105")
                .country("USA")
                .type(AddressType.BUSINESS)
                .isPrimary(true)
                .build());
            
            corporateCustomer.updateCreditScore(new CreditScore(780, "EXPERIAN", LocalDateTime.now()));
            
            System.out.println("Created test customers: " + 
                List.of(customer1.getCustomerId(), customer2.getCustomerId(), corporateCustomer.getCustomerId()));
        }
        
        private void createTestLoans() {
            // Personal loan for customer 1
            Money personalLoanAmount = Money.of(new BigDecimal("25000.00"), "USD");
            
            // Auto loan for customer 2  
            Money autoLoanAmount = Money.of(new BigDecimal("35000.00"), "USD");
            
            // Business loan for corporate customer
            Money businessLoanAmount = Money.of(new BigDecimal("500000.00"), "USD");
            
            System.out.println("Created test loans with amounts: " + 
                List.of(personalLoanAmount, autoLoanAmount, businessLoanAmount));
        }
        
        private void createTestPayments() {
            // Create test payments for the loans
            Money payment1 = Money.of(new BigDecimal("478.66"), "USD");
            Payment testPayment1 = Payment.createNew(
                LoanId.of("DEV-LOAN-001"),
                new CustomerId("DEV-CUST-001"),
                payment1,
                PaymentMethod.BANK_TRANSFER,
                "DEV-PAY-REF-001",
                "Development test payment 1"
            );
            
            Money payment2 = Money.of(new BigDecimal("548.32"), "USD");
            Payment testPayment2 = Payment.createNew(
                LoanId.of("DEV-LOAN-002"),
                new CustomerId("DEV-CUST-002"),
                payment2,
                PaymentMethod.ACH,
                "DEV-PAY-REF-002",
                "Development test payment 2"
            );
            
            System.out.println("Created test payments: " + 
                List.of(testPayment1.getPaymentReference(), testPayment2.getPaymentReference()));
        }
    }
}
