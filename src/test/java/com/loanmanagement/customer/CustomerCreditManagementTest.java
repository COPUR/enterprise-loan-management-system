package com.loanmanagement.customer;

import com.loanmanagement.customer.domain.event.CreditReservedEvent;
import com.loanmanagement.customer.domain.event.CreditReleasedEvent;
import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.customer.domain.model.CustomerStatus;
import com.loanmanagement.customer.domain.model.InsufficientCreditException;
import com.loanmanagement.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for Customer Credit Management functionality
 * These tests are designed to FAIL initially and drive the implementation
 * of enhanced credit management features from backup-src
 */
@DisplayName("Customer Credit Management - TDD Tests")
class CustomerCreditManagementTest {

    private Customer customer;
    private Money availableCredit;
    private Money monthlyIncome;

    @BeforeEach
    void setUp() {
        // Setup customer with credit information for testing
        monthlyIncome = Money.of(new BigDecimal("5000.00"), "USD");
        availableCredit = Money.of(new BigDecimal("5000.00"), "USD");
        
        customer = Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("555-0123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .monthlyIncome(monthlyIncome)
                .creditScore(750)
                .creditLimit(Money.of(new BigDecimal("10000.00"), "USD"))
                .availableCredit(availableCredit)
                .status(CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should reserve credit when sufficient available credit exists")
    void shouldReserveCreditWhenSufficientAvailable() {
        // Given
        Money reservationAmount = Money.of(new BigDecimal("1000.00"), "USD");
        Money expectedRemainingCredit = Money.of(new BigDecimal("4000.00"), "USD");
        
        // When
        customer.reserveCredit(reservationAmount);
        
        // Then
        assertThat(customer.getAvailableCredit()).isEqualTo(expectedRemainingCredit);
        assertThat(customer.getDomainEvents()).hasSize(1);
        
        CreditReservedEvent event = (CreditReservedEvent) customer.getDomainEvents().get(0);
        assertThat(event.customerId()).isEqualTo(1L);
        assertThat(event.amount()).isEqualTo(reservationAmount);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when attempting to reserve more credit than available")
    void shouldThrowExceptionWhenInsufficientCredit() {
        // Given
        Money reservationAmount = Money.of(new BigDecimal("6000.00"), "USD");
        
        // When & Then
        assertThatThrownBy(() -> customer.reserveCredit(reservationAmount))
                .isInstanceOf(InsufficientCreditException.class)
                .hasMessageContaining("Insufficient credit available");
        
        // Verify state unchanged
        assertThat(customer.getAvailableCredit()).isEqualTo(availableCredit);
        assertThat(customer.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should release credit and publish domain event")
    void shouldReleaseCreditAndPublishEvent() {
        // Given - First reserve some credit
        Money reservationAmount = Money.of(new BigDecimal("2000.00"), "USD");
        customer.reserveCredit(reservationAmount);
        customer.clearDomainEvents(); // Clear reservation event
        
        Money releaseAmount = Money.of(new BigDecimal("1000.00"), "USD");
        Money expectedAvailableCredit = Money.of(new BigDecimal("4000.00"), "USD");
        
        // When
        customer.releaseCredit(releaseAmount);
        
        // Then
        assertThat(customer.getAvailableCredit()).isEqualTo(expectedAvailableCredit);
        assertThat(customer.getDomainEvents()).hasSize(1);
        
        CreditReleasedEvent event = (CreditReleasedEvent) customer.getDomainEvents().get(0);
        assertThat(event.customerId()).isEqualTo(1L);
        assertThat(event.amount()).isEqualTo(releaseAmount);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple credit reservations correctly")
    void shouldHandleMultipleCreditReservations() {
        // Given
        Money firstReservation = Money.of(new BigDecimal("1500.00"), "USD");
        Money secondReservation = Money.of(new BigDecimal("2000.00"), "USD");
        Money expectedRemainingCredit = Money.of(new BigDecimal("1500.00"), "USD");
        
        // When
        customer.reserveCredit(firstReservation);
        customer.reserveCredit(secondReservation);
        
        // Then
        assertThat(customer.getAvailableCredit()).isEqualTo(expectedRemainingCredit);
        assertThat(customer.getDomainEvents()).hasSize(2);
    }

    @Test
    @DisplayName("Should prevent credit reservation when customer is inactive")
    void shouldPreventCreditReservationWhenCustomerInactive() {
        // Given
        customer = customer.toBuilder().status(CustomerStatus.INACTIVE).build();
        Money reservationAmount = Money.of(new BigDecimal("1000.00"), "USD");
        
        // When & Then
        assertThatThrownBy(() -> customer.reserveCredit(reservationAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reserve credit for inactive customer");
    }

    @Test
    @DisplayName("Should validate credit reservation amount is positive")
    void shouldValidateCreditReservationAmountIsPositive() {
        // Given
        Money negativeAmount = Money.of(new BigDecimal("-500.00"), "USD");
        
        // When & Then
        assertThatThrownBy(() -> customer.reserveCredit(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit reservation amount must be positive");
    }

    @Test
    @DisplayName("Should validate credit release amount is positive")
    void shouldValidateCreditReleaseAmountIsPositive() {
        // Given
        Money negativeAmount = Money.of(new BigDecimal("-500.00"), "USD");
        
        // When & Then
        assertThatThrownBy(() -> customer.releaseCredit(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit release amount must be positive");
    }

    @Test
    @DisplayName("Should calculate current utilization ratio correctly")
    void shouldCalculateCurrentUtilizationRatio() {
        // Given
        Money reservationAmount = Money.of(new BigDecimal("3000.00"), "USD");
        customer.reserveCredit(reservationAmount);
        
        // When
        BigDecimal utilizationRatio = customer.getCreditUtilizationRatio();
        
        // Then
        assertThat(utilizationRatio).isEqualByComparingTo(new BigDecimal("0.80")); // 80% utilization (8000/10000)
    }

    @Test
    @DisplayName("Should determine if customer is eligible for additional credit")
    void shouldDetermineIfCustomerIsEligibleForAdditionalCredit() {
        // Given - Customer with good credit score and low utilization
        Money smallReservation = Money.of(new BigDecimal("1000.00"), "USD");
        customer.reserveCredit(smallReservation);
        
        // When
        boolean isEligible = customer.isEligibleForCreditIncrease();
        
        // Then
        assertThat(isEligible).isTrue();
    }

    @Test
    @DisplayName("Should not be eligible for credit increase with poor credit score")
    void shouldNotBeEligibleForCreditIncreaseWithPoorCreditScore() {
        // Given - Customer with poor credit score
        customer = customer.toBuilder().creditScore(550).build();
        
        // When
        boolean isEligible = customer.isEligibleForCreditIncrease();
        
        // Then
        assertThat(isEligible).isFalse();
    }

    @Test
    @DisplayName("Should not be eligible for credit increase with high utilization")
    void shouldNotBeEligibleForCreditIncreaseWithHighUtilization() {
        // Given - Customer with high credit utilization
        Money highReservation = Money.of(new BigDecimal("4500.00"), "USD");
        customer.reserveCredit(highReservation);
        
        // When
        boolean isEligible = customer.isEligibleForCreditIncrease();
        
        // Then
        assertThat(isEligible).isFalse();
    }
}