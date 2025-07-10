package com.loanmanagement.customer;

import com.loanmanagement.customer.application.port.in.CreateCustomerUseCase;
import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.customer.domain.model.CustomerStatus;
import com.loanmanagement.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for Enhanced Customer Properties
 * These tests are designed to FAIL initially and drive the implementation
 * of enhanced customer features from backup-src
 */
@DisplayName("Customer Enhancements - TDD Tests")
class CustomerEnhancementsTest {

    @Test
    @DisplayName("Should create customer with complete credit information")
    void shouldCreateCustomerWithCreditInformation() {
        // Given
        CreateCustomerUseCase.CreateCustomerCommand command = new CreateCustomerUseCase.CreateCustomerCommand(
                "John",
                "Doe", 
                "john.doe@example.com",
                "555-0123",
                LocalDate.of(1985, 6, 15),
                Money.of(new BigDecimal("5000.00"), "USD"),
                750, // credit score
                "123 Main St",
                "Software Engineer"
        );
        
        // When
        Customer customer = Customer.fromCreateCommand(command);
        
        // Then
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(customer.getPhoneNumber()).isEqualTo("555-0123");
        assertThat(customer.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 6, 15));
        assertThat(customer.getMonthlyIncome()).isEqualTo(Money.of(new BigDecimal("5000.00"), "USD"));
        assertThat(customer.getCreditScore()).isEqualTo(750);
        assertThat(customer.getAddress()).isEqualTo("123 Main St");
        assertThat(customer.getOccupation()).isEqualTo("Software Engineer");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate initial credit limit based on income and credit score")
    void shouldCalculateInitialCreditLimit() {
        // Given
        CreateCustomerUseCase.CreateCustomerCommand highIncomeCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "Jane",
                "Smith",
                "jane.smith@example.com",
                "555-0124",
                LocalDate.of(1990, 3, 20),
                Money.of(new BigDecimal("8000.00"), "USD"), // High income
                800, // Excellent credit score
                "456 Oak Ave",
                "Senior Developer"
        );
        
        // When
        Customer customer = Customer.fromCreateCommand(highIncomeCommand);
        
        // Then
        Money expectedCreditLimit = Money.of(new BigDecimal("16000.00"), "USD"); // 2x monthly income for excellent credit
        assertThat(customer.getCreditLimit()).isEqualTo(expectedCreditLimit);
        assertThat(customer.getAvailableCredit()).isEqualTo(expectedCreditLimit);
    }

    @Test
    @DisplayName("Should calculate lower credit limit for lower credit score")
    void shouldCalculateLowerCreditLimitForLowerCreditScore() {
        // Given
        CreateCustomerUseCase.CreateCustomerCommand fairCreditCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "Bob",
                "Johnson",
                "bob.johnson@example.com",
                "555-0125",
                LocalDate.of(1980, 11, 5),
                Money.of(new BigDecimal("4000.00"), "USD"),
                650, // Fair credit score
                "789 Pine St",
                "Manager"
        );
        
        // When
        Customer customer = Customer.fromCreateCommand(fairCreditCommand);
        
        // Then
        Money expectedCreditLimit = Money.of(new BigDecimal("4000.00"), "USD"); // 1x monthly income for fair credit
        assertThat(customer.getCreditLimit()).isEqualTo(expectedCreditLimit);
        assertThat(customer.getAvailableCredit()).isEqualTo(expectedCreditLimit);
    }

    @Test
    @DisplayName("Should calculate minimal credit limit for poor credit score")
    void shouldCalculateMinimalCreditLimitForPoorCreditScore() {
        // Given
        CreateCustomerUseCase.CreateCustomerCommand poorCreditCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "Alice",
                "Brown",
                "alice.brown@example.com",
                "555-0126",
                LocalDate.of(1975, 8, 12),
                Money.of(new BigDecimal("3000.00"), "USD"),
                550, // Poor credit score
                "321 Elm St",
                "Assistant"
        );
        
        // When
        Customer customer = Customer.fromCreateCommand(poorCreditCommand);
        
        // Then
        Money expectedCreditLimit = Money.of(new BigDecimal("1500.00"), "USD"); // 0.5x monthly income for poor credit
        assertThat(customer.getCreditLimit()).isEqualTo(expectedCreditLimit);
        assertThat(customer.getAvailableCredit()).isEqualTo(expectedCreditLimit);
    }

    @Test
    @DisplayName("Should validate required fields during customer creation")
    void shouldValidateRequiredFieldsDuringCustomerCreation() {
        // Given - Command with missing required fields
        CreateCustomerUseCase.CreateCustomerCommand invalidCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                null, // Missing first name
                "Doe",
                "john.doe@example.com",
                "555-0123",
                LocalDate.of(1985, 6, 15),
                Money.of(new BigDecimal("5000.00"), "USD"),
                750,
                "123 Main St",
                "Software Engineer"
        );
        
        // When & Then
        assertThatThrownBy(() -> Customer.fromCreateCommand(invalidCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name is required");
    }

    @Test
    @DisplayName("Should validate email format during customer creation")
    void shouldValidateEmailFormatDuringCustomerCreation() {
        // Given - Command with invalid email
        CreateCustomerUseCase.CreateCustomerCommand invalidEmailCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "John",
                "Doe",
                "invalid-email", // Invalid email format
                "555-0123",
                LocalDate.of(1985, 6, 15),
                Money.of(new BigDecimal("5000.00"), "USD"),
                750,
                "123 Main St",
                "Software Engineer"
        );
        
        // When & Then
        assertThatThrownBy(() -> Customer.fromCreateCommand(invalidEmailCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("Should validate credit score range during customer creation")
    void shouldValidateCreditScoreRangeDuringCustomerCreation() {
        // Given - Command with invalid credit score
        CreateCustomerUseCase.CreateCustomerCommand invalidScoreCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "John",
                "Doe",
                "john.doe@example.com",
                "555-0123",
                LocalDate.of(1985, 6, 15),
                Money.of(new BigDecimal("5000.00"), "USD"),
                900, // Invalid credit score (max is 850)
                "123 Main St",
                "Software Engineer"
        );
        
        // When & Then
        assertThatThrownBy(() -> Customer.fromCreateCommand(invalidScoreCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit score must be between 300 and 850");
    }

    @Test
    @DisplayName("Should validate age requirements during customer creation")
    void shouldValidateAgeRequirementsDuringCustomerCreation() {
        // Given - Command with underage customer
        CreateCustomerUseCase.CreateCustomerCommand underageCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "Young",
                "Person",
                "young.person@example.com",
                "555-0127",
                LocalDate.now().minusYears(17), // Under 18
                Money.of(new BigDecimal("2000.00"), "USD"),
                650,
                "456 Youth St",
                "Student"
        );
        
        // When & Then
        assertThatThrownBy(() -> Customer.fromCreateCommand(underageCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer must be at least 18 years old");
    }

    @Test
    @DisplayName("Should validate minimum income requirements")
    void shouldValidateMinimumIncomeRequirements() {
        // Given - Command with insufficient income
        CreateCustomerUseCase.CreateCustomerCommand lowIncomeCommand = new CreateCustomerUseCase.CreateCustomerCommand(
                "Low",
                "Income",
                "low.income@example.com",
                "555-0128",
                LocalDate.of(1990, 1, 1),
                Money.of(new BigDecimal("500.00"), "USD"), // Below minimum income
                650,
                "789 Budget St",
                "Part-time Worker"
        );
        
        // When & Then
        assertThatThrownBy(() -> Customer.fromCreateCommand(lowIncomeCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monthly income must be at least $1000");
    }

    @Test
    @DisplayName("Should update customer information and preserve audit fields")
    void shouldUpdateCustomerInformationAndPreserveAuditFields() {
        // Given
        Customer existingCustomer = Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("555-0123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .monthlyIncome(Money.of(new BigDecimal("5000.00"), "USD"))
                .creditScore(750)
                .address("123 Main St")
                .occupation("Software Engineer")
                .status(CustomerStatus.ACTIVE)
                .build();
        
        // When
        Customer updatedCustomer = existingCustomer.updateContactInfo(
                "555-9999",
                "456 New Address",
                "Senior Software Engineer"
        );
        
        // Then
        assertThat(updatedCustomer.getPhoneNumber()).isEqualTo("555-9999");
        assertThat(updatedCustomer.getAddress()).isEqualTo("456 New Address");
        assertThat(updatedCustomer.getOccupation()).isEqualTo("Senior Software Engineer");
        assertThat(updatedCustomer.getUpdatedAt()).isAfter(existingCustomer.getUpdatedAt());
        
        // Verify immutable fields are preserved
        assertThat(updatedCustomer.getId()).isEqualTo(existingCustomer.getId());
        assertThat(updatedCustomer.getEmail()).isEqualTo(existingCustomer.getEmail());
        assertThat(updatedCustomer.getCreatedAt()).isEqualTo(existingCustomer.getCreatedAt());
    }

    @Test
    @DisplayName("Should calculate customer risk level based on credit score and income")
    void shouldCalculateCustomerRiskLevelBasedOnCreditScoreAndIncome() {
        // Given - High credit score and income
        Customer lowRiskCustomer = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("555-0123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .creditScore(800)
                .monthlyIncome(Money.of(new BigDecimal("10000.00"), "USD"))
                .build();
        
        // When
        String riskLevel = lowRiskCustomer.calculateRiskLevel();
        
        // Then
        assertThat(riskLevel).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should determine loan eligibility based on customer profile")
    void shouldDetermineLoanEligibilityBasedOnCustomerProfile() {
        // Given
        Customer eligibleCustomer = Customer.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("555-0124")
                .dateOfBirth(LocalDate.of(1990, 3, 20))
                .status(CustomerStatus.ACTIVE)
                .creditScore(720)
                .monthlyIncome(Money.of(new BigDecimal("6000.00"), "USD"))
                .availableCredit(Money.of(new BigDecimal("5000.00"), "USD"))
                .build();
        
        Money requestedLoanAmount = Money.of(new BigDecimal("25000.00"), "USD");
        
        // When
        boolean isEligible = eligibleCustomer.isEligibleForLoan(requestedLoanAmount);
        
        // Then
        assertThat(isEligible).isTrue();
    }
}