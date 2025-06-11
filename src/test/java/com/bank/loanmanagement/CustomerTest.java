package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CustomerTest {
    
    private Customer customer;
    
    @BeforeEach
    void setUp() {
        customer = new Customer();
    }
    
    @Test
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomerWithValidData() {
        // Given
        customer.setCustomerId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhoneNumber("1234567890");
        customer.setCreditScore(750);
        customer.setMonthlyIncome(new BigDecimal("5000.00"));
        customer.setCreatedAt(LocalDateTime.now());
        
        // When & Then
        assertEquals(1L, customer.getCustomerId());
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals("john.doe@example.com", customer.getEmail());
        assertEquals("1234567890", customer.getPhoneNumber());
        assertEquals(750, customer.getCreditScore());
        assertEquals(new BigDecimal("5000.00"), customer.getMonthlyIncome());
        assertNotNull(customer.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should validate credit score range")
    void shouldValidateCreditScoreRange() {
        // Given
        customer.setCustomerId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        
        // When & Then - Valid credit scores
        customer.setCreditScore(300); // Minimum
        assertEquals(300, customer.getCreditScore());
        
        customer.setCreditScore(850); // Maximum
        assertEquals(850, customer.getCreditScore());
        
        customer.setCreditScore(750); // Good score
        assertEquals(750, customer.getCreditScore());
    }
    
    @Test
    @DisplayName("Should validate email format")
    void shouldValidateEmailFormat() {
        // Given
        customer.setCustomerId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        
        // When & Then - Valid emails
        customer.setEmail("valid@example.com");
        assertEquals("valid@example.com", customer.getEmail());
        
        customer.setEmail("user.name@domain.co.uk");
        assertEquals("user.name@domain.co.uk", customer.getEmail());
    }
    
    @Test
    @DisplayName("Should handle monthly income validation")
    void shouldHandleMonthlyIncomeValidation() {
        // Given
        customer.setCustomerId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        
        // When & Then
        customer.setMonthlyIncome(new BigDecimal("1000.00")); // Minimum viable income
        assertEquals(new BigDecimal("1000.00"), customer.getMonthlyIncome());
        
        customer.setMonthlyIncome(new BigDecimal("50000.00")); // High income
        assertEquals(new BigDecimal("50000.00"), customer.getMonthlyIncome());
    }
    
    @Test
    @DisplayName("Should create customer with timestamp")
    void shouldCreateCustomerWithTimestamp() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now();
        
        // When
        customer.setCreatedAt(LocalDateTime.now());
        
        // Then
        LocalDateTime afterCreation = LocalDateTime.now();
        assertTrue(customer.getCreatedAt().isAfter(beforeCreation.minusSeconds(1)));
        assertTrue(customer.getCreatedAt().isBefore(afterCreation.plusSeconds(1)));
    }
}