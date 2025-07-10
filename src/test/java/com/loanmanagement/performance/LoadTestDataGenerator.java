package com.loanmanagement.performance;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.loan.application.port.in.CreateLoanUseCase;
import com.loanmanagement.payment.application.port.in.ProcessPaymentUseCase;
import com.loanmanagement.payment.domain.model.PaymentType;
import com.loanmanagement.shared.domain.model.Money;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Load Test Data Generator
 * Generates realistic test data for performance testing scenarios
 */
@Slf4j
public class LoadTestDataGenerator {
    
    private final Random random = new Random();
    private final String[] firstNames = {
        "John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Mary",
        "James", "Patricia", "William", "Jennifer", "Richard", "Elizabeth"
    };
    
    private final String[] lastNames = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
        "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez"
    };
    
    private final String[] emailDomains = {
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "company.com"
    };
    
    /**
     * Generate realistic customer data for testing
     */
    public List<TestCustomerData> generateCustomers(int count) {
        log.info("Generating {} test customers", count);
        
        List<TestCustomerData> customers = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String email = generateEmail(firstName, lastName);
            String phone = generatePhoneNumber();
            LocalDate dateOfBirth = generateDateOfBirth();
            Money monthlyIncome = generateMonthlyIncome();
            
            customers.add(new TestCustomerData(
                    (long) (i + 1),
                    firstName,
                    lastName,
                    email,
                    phone,
                    dateOfBirth,
                    monthlyIncome
            ));
        }
        
        return customers;
    }
    
    /**
     * Generate realistic loan application data
     */
    public List<CreateLoanUseCase.CreateLoanCommand> generateLoanApplications(
            List<TestCustomerData> customers, int loansPerCustomer) {
        
        log.info("Generating loan applications for {} customers with {} loans each", 
                customers.size(), loansPerCustomer);
        
        List<CreateLoanUseCase.CreateLoanCommand> loanApplications = new ArrayList<>();
        
        for (TestCustomerData customer : customers) {
            for (int i = 0; i < loansPerCustomer; i++) {
                Money principalAmount = generateLoanAmount(customer.monthlyIncome());
                BigDecimal interestRate = generateInterestRate();
                Integer termMonths = generateLoanTerm();
                
                loanApplications.add(new CreateLoanUseCase.CreateLoanCommand(
                        customer.customerId(),
                        principalAmount,
                        interestRate,
                        termMonths
                ));
            }
        }
        
        return loanApplications;
    }
    
    /**
     * Generate payment data for existing loans
     */
    public List<ProcessPaymentUseCase.ProcessPaymentCommand> generatePayments(
            List<Long> loanIds, int paymentsPerLoan) {
        
        log.info("Generating payments for {} loans with {} payments each", 
                loanIds.size(), paymentsPerLoan);
        
        List<ProcessPaymentUseCase.ProcessPaymentCommand> payments = new ArrayList<>();
        
        for (Long loanId : loanIds) {
            for (int i = 0; i < paymentsPerLoan; i++) {
                Money paymentAmount = generatePaymentAmount();
                LocalDate paymentDate = generatePaymentDate();
                PaymentType paymentType = generatePaymentType();
                String paymentMethod = generatePaymentMethod();
                String reference = generatePaymentReference();
                String processedBy = "LoadTest_" + random.nextInt(100);
                
                payments.add(new ProcessPaymentUseCase.ProcessPaymentCommand(
                        loanId,
                        paymentAmount,
                        paymentDate,
                        paymentType,
                        paymentMethod,
                        reference,
                        processedBy
                ));
            }
        }
        
        return payments;
    }
    
    /**
     * Generate test scenarios with different load patterns
     */
    public List<LoadTestScenario> generateLoadTestScenarios() {
        List<LoadTestScenario> scenarios = new ArrayList<>();
        
        // Steady load scenario
        scenarios.add(new LoadTestScenario(
                "SteadyLoad",
                "Constant load over time",
                LoadTestScenario.LoadPattern.STEADY,
                100, // users
                300, // duration in seconds
                1.0  // ramp up factor
        ));
        
        // Spike load scenario
        scenarios.add(new LoadTestScenario(
                "SpikeLoad",
                "Sudden spike in load",
                LoadTestScenario.LoadPattern.SPIKE,
                500, // users
                60,  // duration in seconds
                5.0  // ramp up factor
        ));
        
        // Gradual ramp up scenario
        scenarios.add(new LoadTestScenario(
                "GradualRampUp",
                "Gradual increase in load",
                LoadTestScenario.LoadPattern.RAMP_UP,
                200, // users
                600, // duration in seconds
                0.5  // ramp up factor
        ));
        
        // Stress test scenario
        scenarios.add(new LoadTestScenario(
                "StressTest",
                "High load to test system limits",
                LoadTestScenario.LoadPattern.STRESS,
                1000, // users
                180,  // duration in seconds
                2.0   // ramp up factor
        ));
        
        return scenarios;
    }
    
    // Helper methods for generating realistic data
    
    private String generateEmail(String firstName, String lastName) {
        String domain = emailDomains[random.nextInt(emailDomains.length)];
        return String.format("%s.%s%d@%s", 
                firstName.toLowerCase(), 
                lastName.toLowerCase(), 
                random.nextInt(1000), 
                domain);
    }
    
    private String generatePhoneNumber() {
        return String.format("+1-%03d-%03d-%04d",
                random.nextInt(800) + 200,  // Area code
                random.nextInt(800) + 200,  // Exchange
                random.nextInt(10000)       // Number
        );
    }
    
    private LocalDate generateDateOfBirth() {
        // Generate ages between 18 and 70
        int ageInYears = 18 + random.nextInt(52);
        return LocalDate.now().minusYears(ageInYears).minusDays(random.nextInt(365));
    }
    
    private Money generateMonthlyIncome() {
        // Generate income between $2,000 and $15,000 per month
        BigDecimal amount = BigDecimal.valueOf(2000 + random.nextDouble() * 13000);
        amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return Money.of("USD", amount);
    }
    
    private Money generateLoanAmount(Money monthlyIncome) {
        // Loan amount between 2x and 8x monthly income
        double multiplier = 2.0 + random.nextDouble() * 6.0;
        BigDecimal amount = monthlyIncome.getAmount().multiply(BigDecimal.valueOf(multiplier));
        amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return Money.of(monthlyIncome.getCurrency(), amount);
    }
    
    private BigDecimal generateInterestRate() {
        // Interest rates between 3% and 18%
        double rate = 3.0 + random.nextDouble() * 15.0;
        return BigDecimal.valueOf(rate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    private Integer generateLoanTerm() {
        // Common loan terms: 12, 24, 36, 48, 60 months
        Integer[] terms = {12, 24, 36, 48, 60};
        return terms[random.nextInt(terms.length)];
    }
    
    private Money generatePaymentAmount() {
        // Payment amounts between $100 and $2,000
        BigDecimal amount = BigDecimal.valueOf(100 + random.nextDouble() * 1900);
        amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        return Money.of("USD", amount);
    }
    
    private LocalDate generatePaymentDate() {
        // Payment dates within the last 30 days
        return LocalDate.now().minusDays(random.nextInt(30));
    }
    
    private PaymentType generatePaymentType() {
        PaymentType[] types = PaymentType.values();
        return types[random.nextInt(types.length)];
    }
    
    private String generatePaymentMethod() {
        String[] methods = {"BANK_TRANSFER", "CREDIT_CARD", "DEBIT_CARD", "CHECK", "CASH"};
        return methods[random.nextInt(methods.length)];
    }
    
    private String generatePaymentReference() {
        return "REF" + ThreadLocalRandom.current().nextLong(100000000, 999999999);
    }
    
    // Data classes
    
    public record TestCustomerData(
            Long customerId,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            Money monthlyIncome
    ) {}
    
    public record LoadTestScenario(
            String name,
            String description,
            LoadPattern pattern,
            int maxUsers,
            int durationSeconds,
            double rampUpFactor
    ) {
        public enum LoadPattern {
            STEADY,    // Constant load
            SPIKE,     // Sudden spike
            RAMP_UP,   // Gradual increase
            STRESS     // High load
        }
        
        public int getUsersAtTime(int currentSecond) {
            return switch (pattern) {
                case STEADY -> maxUsers;
                case SPIKE -> currentSecond < 10 ? (int) (maxUsers * rampUpFactor) : maxUsers / 2;
                case RAMP_UP -> (int) Math.min(maxUsers, (currentSecond * maxUsers * rampUpFactor) / durationSeconds);
                case STRESS -> (int) (maxUsers * rampUpFactor);
            };
        }
    }
}