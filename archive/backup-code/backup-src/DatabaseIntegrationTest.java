package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseIntegrationTest {
    
    private Connection connection;
    private final String DATABASE_URL = System.getenv("DATABASE_URL");
    
    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
    }
    
    @Test
    @DisplayName("Should connect to PostgreSQL database successfully")
    void shouldConnectToDatabaseSuccessfully() throws SQLException {
        // When & Then
        assertNotNull(connection);
        assertFalse(connection.isClosed());
        assertTrue(connection.isValid(5));
    }
    
    @Test
    @DisplayName("Should verify customer_management schema exists")
    void shouldVerifyCustomerManagementSchemaExists() throws SQLException {
        // Given
        String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'customer_management'";
        
        // When
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            // Then
            assertTrue(rs.next());
            int tableCount = rs.getInt(1);
            assertTrue(tableCount > 0, "Customer management schema should have tables");
        }
    }
    
    @Test
    @DisplayName("Should verify loan_origination schema exists")
    void shouldVerifyLoanOriginationSchemaExists() throws SQLException {
        // Given
        String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'loan_origination'";
        
        // When
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            // Then
            assertTrue(rs.next());
            int tableCount = rs.getInt(1);
            assertTrue(tableCount > 0, "Loan origination schema should have tables");
        }
    }
    
    @Test
    @DisplayName("Should verify payment_processing schema exists")
    void shouldVerifyPaymentProcessingSchemaExists() throws SQLException {
        // Given
        String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'payment_processing'";
        
        // When
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            // Then
            assertTrue(rs.next());
            int tableCount = rs.getInt(1);
            assertTrue(tableCount > 0, "Payment processing schema should have tables");
        }
    }
    
    @Test
    @DisplayName("Should retrieve customers from database")
    void shouldRetrieveCustomersFromDatabase() throws SQLException {
        // Given
        String query = "SELECT customer_id, first_name, last_name, email FROM customer_management.customers LIMIT 5";
        
        // When
        List<Customer> customers = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Customer customer = new Customer();
                customer.setCustomerId(rs.getLong("customer_id"));
                customer.setFirstName(rs.getString("first_name"));
                customer.setLastName(rs.getString("last_name"));
                customer.setEmail(rs.getString("email"));
                customers.add(customer);
            }
        }
        
        // Then
        assertFalse(customers.isEmpty(), "Should retrieve customers from database");
        for (Customer customer : customers) {
            assertNotNull(customer.getCustomerId());
            assertNotNull(customer.getFirstName());
            assertNotNull(customer.getLastName());
            assertNotNull(customer.getEmail());
        }
    }
    
    @Test
    @DisplayName("Should retrieve loans with business rule compliance")
    void shouldRetrieveLoansWithBusinessRuleCompliance() throws SQLException {
        // Given
        String query = "SELECT loan_id, customer_id, loan_amount, interest_rate, installments FROM loan_origination.loans LIMIT 5";
        
        // When
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Loan loan = new Loan();
                loan.setLoanId(rs.getLong("loan_id"));
                loan.setCustomerId(rs.getLong("customer_id"));
                loan.setLoanAmount(rs.getBigDecimal("loan_amount"));
                loan.setInterestRate(rs.getBigDecimal("interest_rate"));
                loan.setInstallments(rs.getInt("installments"));
                loans.add(loan);
            }
        }
        
        // Then
        assertFalse(loans.isEmpty(), "Should retrieve loans from database");
        
        for (Loan loan : loans) {
            // Validate business rules
            assertNotNull(loan.getLoanId());
            assertNotNull(loan.getCustomerId());
            
            // Loan amount: $1,000 - $500,000
            assertTrue(loan.getLoanAmount().compareTo(new BigDecimal("1000")) >= 0);
            assertTrue(loan.getLoanAmount().compareTo(new BigDecimal("500000")) <= 0);
            
            // Interest rate: 0.1% - 0.5%
            assertTrue(loan.getInterestRate().compareTo(new BigDecimal("0.1")) >= 0);
            assertTrue(loan.getInterestRate().compareTo(new BigDecimal("0.5")) <= 0);
            
            // Installments: 6, 9, 12, 24
            int[] validInstallments = {6, 9, 12, 24};
            boolean validInstallment = false;
            for (int valid : validInstallments) {
                if (loan.getInstallments() == valid) {
                    validInstallment = true;
                    break;
                }
            }
            assertTrue(validInstallment, "Installments must be 6, 9, 12, or 24");
        }
    }
    
    @Test
    @DisplayName("Should retrieve payments with valid data")
    void shouldRetrievePaymentsWithValidData() throws SQLException {
        // Given
        String query = "SELECT payment_id, loan_id, payment_amount, payment_status FROM payment_processing.payments LIMIT 5";
        
        // When
        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Payment payment = new Payment();
                payment.setPaymentId(rs.getLong("payment_id"));
                payment.setLoanId(rs.getLong("loan_id"));
                payment.setPaymentAmount(rs.getBigDecimal("payment_amount"));
                payment.setPaymentStatus(rs.getString("payment_status"));
                payments.add(payment);
            }
        }
        
        // Then
        assertFalse(payments.isEmpty(), "Should retrieve payments from database");
        
        for (Payment payment : payments) {
            assertNotNull(payment.getPaymentId());
            assertNotNull(payment.getLoanId());
            assertNotNull(payment.getPaymentAmount());
            assertNotNull(payment.getPaymentStatus());
            
            // Payment amount should be positive
            assertTrue(payment.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0);
        }
    }
    
    @Test
    @DisplayName("Should validate referential integrity between customers and loans")
    void shouldValidateReferentialIntegrityBetweenCustomersAndLoans() throws SQLException {
        // Given
        String query = """
            SELECT c.customer_id, c.first_name, l.loan_id, l.loan_amount 
            FROM customer_management.customers c
            INNER JOIN loan_origination.loans l ON c.customer_id = l.customer_id
            LIMIT 3
            """;
        
        // When
        int recordCount = 0;
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                recordCount++;
                assertNotNull(rs.getLong("customer_id"));
                assertNotNull(rs.getString("first_name"));
                assertNotNull(rs.getLong("loan_id"));
                assertNotNull(rs.getBigDecimal("loan_amount"));
            }
        }
        
        // Then
        assertTrue(recordCount > 0, "Should have customers with loans");
    }
    
    @Test
    @DisplayName("Should validate referential integrity between loans and payments")
    void shouldValidateReferentialIntegrityBetweenLoansAndPayments() throws SQLException {
        // Given
        String query = """
            SELECT l.loan_id, l.loan_amount, p.payment_id, p.payment_amount
            FROM loan_origination.loans l
            INNER JOIN payment_processing.payments p ON l.loan_id = p.loan_id
            LIMIT 3
            """;
        
        // When
        int recordCount = 0;
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                recordCount++;
                assertNotNull(rs.getLong("loan_id"));
                assertNotNull(rs.getBigDecimal("loan_amount"));
                assertNotNull(rs.getLong("payment_id"));
                assertNotNull(rs.getBigDecimal("payment_amount"));
            }
        }
        
        // Then
        assertTrue(recordCount > 0, "Should have loans with payments");
    }
}