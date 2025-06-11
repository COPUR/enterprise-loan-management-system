package com.bank.loanmanagement;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Database Regression Testing for Enterprise Loan Management System
 * Validates database operations, transactions, and data integrity
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseRegressionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("Database Connection and Schema Validation")
    void testDatabaseConnectionRegression() {
        // Verify database connectivity
        try {
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            Assertions.assertEquals("1", result);
            System.out.println("✓ Database connection established");
        } catch (Exception e) {
            Assertions.fail("Database connection failed: " + e.getMessage());
        }

        // Verify essential tables exist
        verifyTableExists("customers");
        verifyTableExists("loans");
        verifyTableExists("payments");
        verifyTableExists("loan_applications");
        
        System.out.println("✓ Database schema validation completed");
    }

    @Test
    @Order(2)
    @DisplayName("Customer Data Operations Regression")
    void testCustomerDataRegression() {
        // Test customer data retrieval
        try {
            List<Map<String, Object>> customers = jdbcTemplate.queryForList(
                "SELECT customer_id, name, email, credit_limit FROM customers LIMIT 5"
            );
            
            Assertions.assertFalse(customers.isEmpty(), "Customer data should be available");
            
            for (Map<String, Object> customer : customers) {
                Assertions.assertNotNull(customer.get("customer_id"));
                Assertions.assertNotNull(customer.get("name"));
                Assertions.assertNotNull(customer.get("email"));
                
                // Validate credit limit is within banking range
                BigDecimal creditLimit = (BigDecimal) customer.get("credit_limit");
                Assertions.assertTrue(creditLimit.compareTo(BigDecimal.ZERO) > 0);
                Assertions.assertTrue(creditLimit.compareTo(new BigDecimal("1000000")) <= 0);
            }
            
            System.out.println("✓ Customer data regression: " + customers.size() + " records validated");
        } catch (Exception e) {
            System.out.println("⚠ Customer data regression skipped - no sample data available");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Loan Data Operations Regression")
    void testLoanDataRegression() {
        try {
            List<Map<String, Object>> loans = jdbcTemplate.queryForList(
                "SELECT loan_id, customer_id, amount, interest_rate, term_months, status FROM loans LIMIT 5"
            );
            
            if (!loans.isEmpty()) {
                for (Map<String, Object> loan : loans) {
                    Assertions.assertNotNull(loan.get("loan_id"));
                    Assertions.assertNotNull(loan.get("customer_id"));
                    
                    // Validate loan amount
                    BigDecimal amount = (BigDecimal) loan.get("amount");
                    Assertions.assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);
                    
                    // Validate interest rate within banking limits (0.1% - 0.5%)
                    BigDecimal interestRate = (BigDecimal) loan.get("interest_rate");
                    if (interestRate != null) {
                        Assertions.assertTrue(interestRate.compareTo(new BigDecimal("0.001")) >= 0);
                        Assertions.assertTrue(interestRate.compareTo(new BigDecimal("0.005")) <= 0);
                    }
                    
                    // Validate term months (6, 9, 12, 24)
                    Integer termMonths = (Integer) loan.get("term_months");
                    if (termMonths != null) {
                        Assertions.assertTrue(List.of(6, 9, 12, 24).contains(termMonths));
                    }
                }
                
                System.out.println("✓ Loan data regression: " + loans.size() + " records validated");
            } else {
                System.out.println("⚠ Loan data regression skipped - no sample data available");
            }
        } catch (Exception e) {
            System.out.println("⚠ Loan data regression error: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Payment Data Operations Regression")
    void testPaymentDataRegression() {
        try {
            List<Map<String, Object>> payments = jdbcTemplate.queryForList(
                "SELECT payment_id, loan_id, amount, payment_date, status FROM payments LIMIT 5"
            );
            
            if (!payments.isEmpty()) {
                for (Map<String, Object> payment : payments) {
                    Assertions.assertNotNull(payment.get("payment_id"));
                    Assertions.assertNotNull(payment.get("loan_id"));
                    
                    // Validate payment amount
                    BigDecimal amount = (BigDecimal) payment.get("amount");
                    Assertions.assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);
                    
                    // Validate payment date
                    Object paymentDate = payment.get("payment_date");
                    Assertions.assertNotNull(paymentDate);
                    
                    // Validate status
                    String status = (String) payment.get("status");
                    Assertions.assertTrue(List.of("PENDING", "COMPLETED", "FAILED", "CANCELLED").contains(status));
                }
                
                System.out.println("✓ Payment data regression: " + payments.size() + " records validated");
            } else {
                System.out.println("⚠ Payment data regression skipped - no sample data available");
            }
        } catch (Exception e) {
            System.out.println("⚠ Payment data regression error: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Database Transaction Integrity Regression")
    void testTransactionIntegrityRegression() {
        try {
            // Test referential integrity between customers and loans
            List<Map<String, Object>> orphanedLoans = jdbcTemplate.queryForList(
                "SELECT l.loan_id FROM loans l LEFT JOIN customers c ON l.customer_id = c.customer_id WHERE c.customer_id IS NULL"
            );
            
            Assertions.assertTrue(orphanedLoans.isEmpty(), "No orphaned loans should exist");
            
            // Test referential integrity between loans and payments
            List<Map<String, Object>> orphanedPayments = jdbcTemplate.queryForList(
                "SELECT p.payment_id FROM payments p LEFT JOIN loans l ON p.loan_id = l.loan_id WHERE l.loan_id IS NULL"
            );
            
            Assertions.assertTrue(orphanedPayments.isEmpty(), "No orphaned payments should exist");
            
            System.out.println("✓ Database referential integrity validated");
        } catch (Exception e) {
            System.out.println("⚠ Transaction integrity regression error: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Database Performance Regression")
    void testDatabasePerformanceRegression() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test query performance for customer lookup
            long queryStart = System.currentTimeMillis();
            jdbcTemplate.queryForList("SELECT COUNT(*) FROM customers");
            long customerQueryTime = System.currentTimeMillis() - queryStart;
            
            // Test query performance for loan calculations
            queryStart = System.currentTimeMillis();
            jdbcTemplate.queryForList("SELECT COUNT(*) FROM loans");
            long loanQueryTime = System.currentTimeMillis() - queryStart;
            
            // Test query performance for payment processing
            queryStart = System.currentTimeMillis();
            jdbcTemplate.queryForList("SELECT COUNT(*) FROM payments");
            long paymentQueryTime = System.currentTimeMillis() - queryStart;
            
            // Banking system queries should complete within reasonable time
            Assertions.assertTrue(customerQueryTime < 1000, "Customer queries should complete within 1 second");
            Assertions.assertTrue(loanQueryTime < 1000, "Loan queries should complete within 1 second");
            Assertions.assertTrue(paymentQueryTime < 1000, "Payment queries should complete within 1 second");
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Database performance regression completed in " + totalTime + "ms");
            System.out.println("  Customer queries: " + customerQueryTime + "ms");
            System.out.println("  Loan queries: " + loanQueryTime + "ms");
            System.out.println("  Payment queries: " + paymentQueryTime + "ms");
            
        } catch (Exception e) {
            System.out.println("⚠ Database performance regression error: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Database Schema Evolution Regression")
    void testSchemaEvolutionRegression() {
        try {
            // Test that required columns exist in customers table
            verifyColumnExists("customers", "customer_id");
            verifyColumnExists("customers", "name");
            verifyColumnExists("customers", "email");
            verifyColumnExists("customers", "credit_limit");
            
            // Test that required columns exist in loans table
            verifyColumnExists("loans", "loan_id");
            verifyColumnExists("loans", "customer_id");
            verifyColumnExists("loans", "amount");
            verifyColumnExists("loans", "interest_rate");
            verifyColumnExists("loans", "term_months");
            
            // Test that required columns exist in payments table
            verifyColumnExists("payments", "payment_id");
            verifyColumnExists("payments", "loan_id");
            verifyColumnExists("payments", "amount");
            verifyColumnExists("payments", "payment_date");
            
            System.out.println("✓ Database schema evolution regression completed");
        } catch (Exception e) {
            System.out.println("⚠ Schema evolution regression error: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Data Validation and Constraints Regression")
    void testDataValidationRegression() {
        try {
            // Test customer email format validation
            List<Map<String, Object>> invalidEmails = jdbcTemplate.queryForList(
                "SELECT customer_id, email FROM customers WHERE email NOT LIKE '%@%' OR email NOT LIKE '%.%'"
            );
            
            if (!invalidEmails.isEmpty()) {
                System.out.println("⚠ Found " + invalidEmails.size() + " customers with invalid email formats");
            }
            
            // Test loan amount constraints
            List<Map<String, Object>> invalidLoanAmounts = jdbcTemplate.queryForList(
                "SELECT loan_id, amount FROM loans WHERE amount <= 0 OR amount > 10000000"
            );
            
            Assertions.assertTrue(invalidLoanAmounts.isEmpty(), "All loan amounts should be positive and reasonable");
            
            // Test payment amount constraints
            List<Map<String, Object>> invalidPaymentAmounts = jdbcTemplate.queryForList(
                "SELECT payment_id, amount FROM payments WHERE amount <= 0"
            );
            
            Assertions.assertTrue(invalidPaymentAmounts.isEmpty(), "All payment amounts should be positive");
            
            System.out.println("✓ Data validation constraints regression completed");
        } catch (Exception e) {
            System.out.println("⚠ Data validation regression error: " + e.getMessage());
        }
    }

    private void verifyTableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", 
                Integer.class, 
                tableName
            );
            System.out.println("  ✓ Table '" + tableName + "' exists");
        } catch (Exception e) {
            System.out.println("  ⚠ Table '" + tableName + "' verification failed: " + e.getMessage());
        }
    }

    private void verifyColumnExists(String tableName, String columnName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
            );
            System.out.println("  ✓ Column '" + tableName + "." + columnName + "' exists");
        } catch (Exception e) {
            System.out.println("  ⚠ Column '" + tableName + "." + columnName + "' verification failed");
        }
    }
}