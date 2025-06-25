package com.bank.loanmanagement.domain.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for clean domain Customer model
 * Validates pure business logic without infrastructure dependencies
 */
@DisplayName("Clean Customer Domain Model Tests")
class CustomerCleanTest {
    
    private CustomerClean customer;
    private CustomerId customerId;
    
    @BeforeEach
    void setUp() {
        customerId = new CustomerId("test-customer-123");
        customer = new CustomerClean(
            customerId,
            "John",
            "Doe",
            "john.doe@example.com",
            "+1-555-123-4567",
            CustomerType.INDIVIDUAL
        );
    }
    
    @Test
    @DisplayName("Should create customer with pending status")
    void shouldCreateCustomerWithPendingStatus() {
        assertEquals(CustomerStatus.PENDING, customer.getStatus());
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals("john.doe@example.com", customer.getEmail());
        assertEquals(CustomerType.INDIVIDUAL, customer.getType());
    }
    
    @Test
    @DisplayName("Should activate customer from pending status")
    void shouldActivateCustomerFromPendingStatus() {
        // Act
        customer.activate();
        
        // Assert
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertFalse(customer.getDomainEvents().isEmpty());
    }
    
    @Test
    @DisplayName("Should not activate customer if not pending")
    void shouldNotActivateCustomerIfNotPending() {
        // Arrange
        customer.activate(); // First activation
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> customer.activate());
    }
    
    @Test
    @DisplayName("Should suspend active customer")
    void shouldSuspendActiveCustomer() {
        // Arrange
        customer.activate();
        String reason = "Suspicious activity detected";
        
        // Act
        customer.suspend(reason);
        
        // Assert
        assertEquals(CustomerStatus.SUSPENDED, customer.getStatus());
    }
    
    @Test
    @DisplayName("Should not suspend non-active customer")
    void shouldNotSuspendNonActiveCustomer() {
        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> customer.suspend("Some reason"));
    }
    
    @Test
    @DisplayName("Should close customer with reason")
    void shouldCloseCustomerWithReason() {
        // Arrange
        String reason = "Customer request";
        
        // Act
        customer.close(reason);
        
        // Assert
        assertEquals(CustomerStatus.CLOSED, customer.getStatus());
    }
    
    @Test
    @DisplayName("Should update credit score")
    void shouldUpdateCreditScore() {
        // Act
        customer.updateCreditScore(750, "Experian");
        
        // Assert
        assertNotNull(customer.getCreditScore());
        assertEquals(750, customer.getCurrentCreditScore());
        assertEquals("Experian", customer.getCreditScore().getReportingAgency());
    }
    
    @Test
    @DisplayName("Should validate credit score range")
    void shouldValidateCreditScoreRange() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customer.updateCreditScore(200, "Experian")); // Too low
        assertThrows(IllegalArgumentException.class, 
            () -> customer.updateCreditScore(900, "Experian")); // Too high
    }
    
    @Test
    @DisplayName("Should check loan eligibility")
    void shouldCheckLoanEligibility() {
        // Arrange
        customer.activate();
        customer.updateCreditScore(650, "Experian");
        
        // Act & Assert
        assertTrue(customer.isEligibleForLoan());
    }
    
    @Test
    @DisplayName("Should not be eligible for loan with low credit score")
    void shouldNotBeEligibleForLoanWithLowCreditScore() {
        // Arrange
        customer.activate();
        customer.updateCreditScore(550, "Experian");
        
        // Act & Assert
        assertFalse(customer.isEligibleForLoan());
    }
    
    @Test
    @DisplayName("Should not be eligible for loan if not active")
    void shouldNotBeEligibleForLoanIfNotActive() {
        // Arrange
        customer.updateCreditScore(750, "Experian");
        // Customer remains in PENDING status
        
        // Act & Assert
        assertFalse(customer.isEligibleForLoan());
    }
    
    @Test
    @DisplayName("Should calculate credit limits based on score")
    void shouldCalculateCreditLimitsBasedOnScore() {
        // Arrange
        customer.activate();
        
        // Test excellent credit (750+)
        customer.updateCreditScore(780, "Experian");
        assertTrue(customer.canReceiveCredit(400000));
        assertFalse(customer.canReceiveCredit(600000));
        
        // Test good credit (700-749)
        customer.updateCreditScore(720, "Experian");
        assertTrue(customer.canReceiveCredit(250000));
        assertFalse(customer.canReceiveCredit(400000));
        
        // Test fair credit (650-699)
        customer.updateCreditScore(670, "Experian");
        assertTrue(customer.canReceiveCredit(100000));
        assertFalse(customer.canReceiveCredit(200000));
        
        // Test poor credit (600-649)
        customer.updateCreditScore(620, "Experian");
        assertTrue(customer.canReceiveCredit(30000));
        assertFalse(customer.canReceiveCredit(100000));
    }
    
    @Test
    @DisplayName("Should add and remove addresses")
    void shouldAddAndRemoveAddresses() {
        // Arrange
        AddressClean address = new AddressClean(
            "123 Main St",
            "New York",
            "NY",
            "10001",
            "USA",
            AddressType.HOME
        );
        
        // Act
        customer.addAddress(address);
        
        // Assert
        assertEquals(1, customer.getAddresses().size());
        assertTrue(customer.getAddresses().contains(address));
        
        // Remove address
        customer.removeAddress(address);
        assertEquals(0, customer.getAddresses().size());
    }
    
    @Test
    @DisplayName("Should get full name")
    void shouldGetFullName() {
        assertEquals("John Doe", customer.getFullName());
    }
    
    @Test
    @DisplayName("Should validate contact info")
    void shouldValidateContactInfo() {
        assertTrue(customer.hasValidContactInfo());
        
        // Test with empty email
        CustomerClean invalidCustomer = new CustomerClean(
            new CustomerId("invalid"),
            "Jane",
            "Smith",
            "",
            "+1-555-999-8888",
            CustomerType.INDIVIDUAL
        );
        assertFalse(invalidCustomer.hasValidContactInfo());
    }
    
    @Test
    @DisplayName("Should throw exception for null address operations")
    void shouldThrowExceptionForNullAddressOperations() {
        assertThrows(IllegalArgumentException.class, 
            () -> customer.addAddress(null));
        assertThrows(IllegalArgumentException.class, 
            () -> customer.removeAddress(null));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid credit score parameters")
    void shouldThrowExceptionForInvalidCreditScoreParameters() {
        assertThrows(IllegalArgumentException.class, 
            () -> customer.updateCreditScore(750, null));
        assertThrows(IllegalArgumentException.class, 
            () -> customer.updateCreditScore(750, ""));
        assertThrows(IllegalArgumentException.class, 
            () -> customer.updateCreditScore(750, "   "));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid suspension/closure parameters")
    void shouldThrowExceptionForInvalidSuspensionClosureParameters() {
        customer.activate();
        
        assertThrows(IllegalArgumentException.class, 
            () -> customer.suspend(null));
        assertThrows(IllegalArgumentException.class, 
            () -> customer.suspend(""));
        assertThrows(IllegalArgumentException.class, 
            () -> customer.close(null));
        assertThrows(IllegalArgumentException.class, 
            () -> customer.close(""));
    }
}