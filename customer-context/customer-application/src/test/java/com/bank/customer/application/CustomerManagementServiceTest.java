package com.bank.customer.application;

import com.bank.customer.application.dto.CreateCustomerRequest;
import com.bank.customer.application.dto.CreateCustomerRequestWithCreditScore;
import com.bank.customer.application.dto.CustomerResponse;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerRepository;
import com.bank.customer.domain.InsufficientCreditException;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Customer Management Service
 * 
 * Tests Functional Requirements:
 * - FR-001: Customer Registration
 * - FR-002: Customer Profile Management
 * - FR-003: Credit Limit Management
 * - FR-004: Customer Lookup & Search
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Management Service Tests")
class CustomerManagementServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    private CustomerManagementService customerService;
    
    @BeforeEach
    void setUp() {
        customerService = new CustomerManagementService(customerRepository);
    }
    
    @Test
    @DisplayName("FR-001: Should create new customer with valid data")
    void shouldCreateNewCustomerWithValidData() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "John", 
            "Doe", 
            "john.doe@example.com", 
            "+1-555-123-4567",
            BigDecimal.valueOf(50000),
            "USD"
        );
        
        CustomerId expectedCustomerId = CustomerId.generate();
        Customer expectedCustomer = Customer.create(
            expectedCustomerId,
            request.firstName(),
            request.lastName(), 
            request.email(),
            request.phoneNumber(),
            request.getCreditLimitAsMoney()
        );
        
        when(customerRepository.existsByEmail(request.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(expectedCustomer);
        
        // When
        CustomerResponse response = customerService.createCustomer(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.creditLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        
        verify(customerRepository).existsByEmail(request.email());
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("FR-001: Should reject customer creation with duplicate email")
    void shouldRejectCustomerCreationWithDuplicateEmail() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Jane", 
            "Smith", 
            "existing@example.com", 
            "+1-555-987-6543",
            BigDecimal.valueOf(25000),
            "USD"
        );
        
        when(customerRepository.existsByEmail(request.email())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer with email existing@example.com already exists");
        
        verify(customerRepository).existsByEmail(request.email());
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("FR-001: Should reject customer creation with invalid data")
    void shouldRejectCustomerCreationWithInvalidData() {
        // Given
        CreateCustomerRequest invalidRequest = new CreateCustomerRequest(
            "", 
            "Smith", 
            "invalid-email", 
            "+1-555-987-6543",
            BigDecimal.valueOf(-1000), // Negative credit limit
            "USD"
        );
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class);
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("FR-002: Should find customer by ID")
    void shouldFindCustomerById() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer expectedCustomer = Customer.create(
            customerId,
            "Alice",
            "Johnson",
            "alice.johnson@example.com",
            "+1-555-111-2222",
            Money.usd(BigDecimal.valueOf(75000))
        );
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(expectedCustomer));
        
        // When
        CustomerResponse response = customerService.findCustomerById(customerId.getValue());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId.getValue());
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Johnson");
        assertThat(response.email()).isEqualTo("alice.johnson@example.com");
        
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    @DisplayName("FR-002: Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        CustomerId nonExistentId = CustomerId.of("CUST-99999999");
        when(customerRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> customerService.findCustomerById(nonExistentId.getValue()))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessage("Customer not found with ID: CUST-99999999");
        
        verify(customerRepository).findById(nonExistentId);
    }
    
    @Test
    @DisplayName("FR-003: Should update customer credit limit")
    void shouldUpdateCustomerCreditLimit() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer existingCustomer = Customer.create(
            customerId,
            "Bob",
            "Wilson",
            "bob.wilson@example.com",
            "+1-555-333-4444",
            Money.usd(BigDecimal.valueOf(30000))
        );
        
        Money newCreditLimit = Money.usd(BigDecimal.valueOf(60000));
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CustomerResponse response = customerService.updateCreditLimit(customerId.getValue(), newCreditLimit);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.creditLimit()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        assertThat(response.availableCredit()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("FR-003: Should reserve credit for customer")
    void shouldReserveCreditForCustomer() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.create(
            customerId,
            "Carol",
            "Davis",
            "carol.davis@example.com",
            "+1-555-555-6666",
            Money.usd(BigDecimal.valueOf(50000))
        );
        
        Money reservationAmount = Money.usd(BigDecimal.valueOf(20000));
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CustomerResponse response = customerService.reserveCredit(customerId.getValue(), reservationAmount);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.creditLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.usedCredit()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(response.availableCredit()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("FR-003: Should reject credit reservation when insufficient credit available")
    void shouldRejectCreditReservationWhenInsufficientCredit() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.create(
            customerId,
            "David",
            "Brown",
            "david.brown@example.com",
            "+1-555-777-8888",
            Money.usd(BigDecimal.valueOf(10000))
        );
        
        Money excessiveAmount = Money.usd(BigDecimal.valueOf(15000)); // More than available
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        // When & Then
        assertThatThrownBy(() -> customerService.reserveCredit(customerId.getValue(), excessiveAmount))
            .isInstanceOf(InsufficientCreditException.class)
            .hasMessageContaining("insufficient credit");
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("FR-003: Should release reserved credit")
    void shouldReleaseReservedCredit() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.create(
            customerId,
            "Eva",
            "Miller",
            "eva.miller@example.com",
            "+1-555-999-0000",
            Money.usd(BigDecimal.valueOf(40000))
        );
        
        // Reserve some credit first
        Money reservationAmount = Money.usd(BigDecimal.valueOf(15000));
        customer.reserveCredit(reservationAmount);
        
        Money releaseAmount = Money.usd(BigDecimal.valueOf(5000));
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CustomerResponse response = customerService.releaseCredit(customerId.getValue(), releaseAmount);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.creditLimit()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(response.usedCredit()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(response.availableCredit()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should create customer with credit score and monthly income")
    void shouldCreateCustomerWithCreditScoreAndMonthlyIncome() {
        // Given
        CreateCustomerRequestWithCreditScore request = new CreateCustomerRequestWithCreditScore(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567",
            Money.aed(new BigDecimal("10000")), 720
        );
        
        CustomerId customerId = CustomerId.generate();
        Customer expectedCustomer = Customer.createWithCreditScore(
            customerId, request.firstName(), request.lastName(), 
            request.email(), request.phoneNumber(), request.monthlyIncome(), request.creditScore()
        );
        
        when(customerRepository.existsByEmail(request.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(expectedCustomer);
        
        // When
        CustomerResponse response = customerService.createCustomerWithCreditScore(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.creditScore()).isEqualTo(720);
        assertThat(response.monthlyIncome()).isEqualByComparingTo(new BigDecimal("10000"));
        
        verify(customerRepository).existsByEmail(request.email());
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should check loan eligibility based on credit score")
    void shouldCheckLoanEligibilityBasedOnCreditScore() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.createWithCreditScore(
            customerId, "Jane", "Smith", "jane.smith@example.com", "+1-555-987-6543",
            Money.aed(new BigDecimal("8000")), 680
        );
        
        Money loanAmount = Money.aed(new BigDecimal("20000"));
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        // When
        boolean isEligible = customerService.isEligibleForLoan(customerId.getValue(), loanAmount);
        
        // Then
        assertThat(isEligible).isTrue(); // Credit score >= 600 and amount within limit
        
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should reject loan eligibility for low credit score")
    void shouldRejectLoanEligibilityForLowCreditScore() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.createWithCreditScore(
            customerId, "Bob", "Johnson", "bob.johnson@example.com", "+1-555-555-5555",
            Money.aed(new BigDecimal("5000")), 580 // Below 600 threshold
        );
        
        Money loanAmount = Money.aed(new BigDecimal("10000"));
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        // When
        boolean isEligible = customerService.isEligibleForLoan(customerId.getValue(), loanAmount);
        
        // Then
        assertThat(isEligible).isFalse(); // Credit score < 600
        
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should update customer credit score")
    void shouldUpdateCustomerCreditScore() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.createWithCreditScore(
            customerId, "Alice", "Brown", "alice.brown@example.com", "+1-555-111-2222",
            Money.aed(new BigDecimal("6000")), 650
        );
        
        Integer newCreditScore = 750;
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CustomerResponse response = customerService.updateCreditScore(customerId.getValue(), newCreditScore);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.creditScore()).isEqualTo(750);
        // Credit limit should be recalculated: 6000 * 5 = 30000 (for score >= 750)
        assertThat(response.creditLimit()).isEqualByComparingTo(new BigDecimal("30000"));
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should find customer by email")
    void shouldFindCustomerByEmail() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.createWithCreditScore(
            customerId, "Charlie", "Davis", "charlie.davis@example.com", "+1-555-333-4444",
            Money.aed(new BigDecimal("7000")), 700
        );
        
        String email = "charlie.davis@example.com";
        
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
        
        // When
        CustomerResponse response = customerService.findCustomerByEmail(email);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.firstName()).isEqualTo("Charlie");
        assertThat(response.lastName()).isEqualTo("Davis");
        
        verify(customerRepository).findByEmail(email);
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should update customer contact information")
    void shouldUpdateCustomerContactInformation() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Customer customer = Customer.createWithCreditScore(
            customerId, "David", "Wilson", "david.wilson@example.com", "+1-555-777-8888",
            Money.aed(new BigDecimal("9000")), 720
        );
        
        String newEmail = "david.wilson.new@example.com";
        String newPhone = "+1-555-999-0000";
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CustomerResponse response = customerService.updateContactInformation(
            customerId.getValue(), newEmail, newPhone);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(newEmail);
        assertThat(response.phoneNumber()).isEqualTo(newPhone);
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should validate customer creation with duplicate email")
    void shouldValidateCustomerCreationWithDuplicateEmail() {
        // Given
        CreateCustomerRequestWithCreditScore request = new CreateCustomerRequestWithCreditScore(
            "John", "Duplicate", "existing@example.com", "+1-555-123-4567",
            Money.aed(new BigDecimal("5000")), 650
        );
        
        when(customerRepository.existsByEmail(request.email())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomerWithCreditScore(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
        
        verify(customerRepository).existsByEmail(request.email());
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should handle customer not found scenarios")
    void shouldHandleCustomerNotFoundScenarios() {
        // Given
        String nonExistentCustomerId = "CUST-NONEXISTENT";
        
        when(customerRepository.findById(CustomerId.of(nonExistentCustomerId))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> customerService.updateCreditScore(nonExistentCustomerId, 750))
            .isInstanceOf(CustomerNotFoundException.class);
        
        assertThatThrownBy(() -> customerService.isEligibleForLoan(nonExistentCustomerId, Money.aed(new BigDecimal("10000"))))
            .isInstanceOf(CustomerNotFoundException.class);
        
        verify(customerRepository, times(2)).findById(CustomerId.of(nonExistentCustomerId));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should validate credit score creation with invalid data")
    void shouldValidateCreditScoreCreationWithInvalidData() {
        // Given - Invalid credit score
        CreateCustomerRequestWithCreditScore invalidRequest = new CreateCustomerRequestWithCreditScore(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567",
            Money.aed(new BigDecimal("5000")), 900 // Invalid credit score > 850
        );
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomerWithCreditScore(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Credit score must be between 300 and 850");
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("Archive Business Logic: Should validate credit score creation with null monthly income")
    void shouldValidateCreditScoreCreationWithNullMonthlyIncome() {
        // Given - Null monthly income
        CreateCustomerRequestWithCreditScore invalidRequest = new CreateCustomerRequestWithCreditScore(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567",
            null, 720 // Null monthly income
        );
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomerWithCreditScore(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Monthly income must be positive");
        
        verify(customerRepository, never()).save(any(Customer.class));
    }
}