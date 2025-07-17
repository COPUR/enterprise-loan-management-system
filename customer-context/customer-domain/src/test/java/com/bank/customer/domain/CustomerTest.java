package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive TDD tests for Customer domain entity
 * 
 * Tests focus on:
 * - Credit eligibility rules and business logic
 * - Financial capacity assessment
 * - Credit profile management
 * - GRASP principles: Low Coupling, High Cohesion
 * - Property-based testing for edge cases
 */
@DisplayName("Customer Domain Entity Tests")
class CustomerTest {

    private CustomerId customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Money creditLimit;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerId = CustomerId.generate();
        firstName = "John";
        lastName = "Doe";
        email = "john.doe@bank.com";
        phoneNumber = "+971501234567";
        creditLimit = Money.aed(new BigDecimal("50000"));
        customer = Customer.create(customerId, firstName, lastName, email, phoneNumber, creditLimit);
    }

    @Nested
    @DisplayName("Customer Creation Tests - High Cohesion")
    class CustomerCreationTests {

        @Test
        @DisplayName("Should create customer with valid parameters")
        void shouldCreateCustomerWithValidParameters() {
            assertAll(
                () -> assertEquals(customerId, customer.getId()),
                () -> assertEquals(firstName, customer.getFirstName()),
                () -> assertEquals(lastName, customer.getLastName()),
                () -> assertEquals(email, customer.getEmail()),
                () -> assertEquals(phoneNumber, customer.getPhoneNumber()),
                () -> assertEquals(firstName + " " + lastName, customer.getFullName()),
                () -> assertNotNull(customer.getCreditProfile()),
                () -> assertEquals(creditLimit, customer.getCreditProfile().getCreditLimit()),
                () -> assertNotNull(customer.getCreatedAt()),
                () -> assertNotNull(customer.getUpdatedAt())
            );
        }

        @Test
        @DisplayName("Should reject null customer ID")
        void shouldRejectNullCustomerId() {
            assertThrows(NullPointerException.class, () ->
                Customer.create(null, firstName, lastName, email, phoneNumber, creditLimit)
            );
        }

        @Test
        @DisplayName("Should reject null or empty first name")
        void shouldRejectInvalidFirstName() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, null, lastName, email, phoneNumber, creditLimit)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, "", lastName, email, phoneNumber, creditLimit)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, "   ", lastName, email, phoneNumber, creditLimit))
            );
        }

        @Test
        @DisplayName("Should reject null or empty last name")
        void shouldRejectInvalidLastName() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, firstName, null, email, phoneNumber, creditLimit)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, firstName, "", email, phoneNumber, creditLimit)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, firstName, "   ", email, phoneNumber, creditLimit))
            );
        }

        @Test
        @DisplayName("Should reject invalid email format")
        void shouldRejectInvalidEmailFormat() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, firstName, lastName, null, phoneNumber, creditLimit)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, firstName, lastName, "invalid-email", phoneNumber, creditLimit)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Customer.create(customerId, firstName, lastName, "test@", phoneNumber, creditLimit))
            );
        }

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() {
            assertDoesNotThrow(() -> {
                Customer.create(customerId, firstName, lastName, "test@example.com", phoneNumber, creditLimit);
                Customer.create(CustomerId.generate(), firstName, lastName, "user.name@domain.co.uk", phoneNumber, creditLimit);
                Customer.create(CustomerId.generate(), firstName, lastName, "valid_email@test-domain.org", phoneNumber, creditLimit);
            });
        }
    }

    @Nested
    @DisplayName("Credit Eligibility Rules - Business Logic Cohesion")
    class CreditEligibilityTests {

        @Test
        @DisplayName("Should allow borrowing within credit limit")
        void shouldAllowBorrowingWithinCreditLimit() {
            Money borrowAmount = Money.aed(new BigDecimal("30000"));
            
            assertTrue(customer.canBorrowAmount(borrowAmount));
        }

        @Test
        @DisplayName("Should reject borrowing exceeding credit limit")
        void shouldRejectBorrowingExceedingCreditLimit() {
            Money excessiveAmount = Money.aed(new BigDecimal("60000"));
            
            assertFalse(customer.canBorrowAmount(excessiveAmount));
        }

        @Test
        @DisplayName("Should allow borrowing exactly at credit limit")
        void shouldAllowBorrowingExactlyAtCreditLimit() {
            assertTrue(customer.canBorrowAmount(creditLimit));
        }

        @Test
        @DisplayName("Should reject zero or negative borrow amounts")
        void shouldRejectInvalidBorrowAmounts() {
            Money zeroAmount = Money.aed(BigDecimal.ZERO);
            Money negativeAmount = Money.aed(new BigDecimal("-1000"));
            
            assertAll(
                () -> assertFalse(customer.canBorrowAmount(zeroAmount)),
                () -> assertFalse(customer.canBorrowAmount(negativeAmount))
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"1000", "5000", "10000", "25000", "45000", "50000"})
        @DisplayName("Should handle various borrow amounts within limit")
        void shouldHandleVariousBorrowAmountsWithinLimit(String amount) {
            Money borrowAmount = Money.aed(new BigDecimal(amount));
            
            if (borrowAmount.compareTo(creditLimit) <= 0 && borrowAmount.isPositive()) {
                assertTrue(customer.canBorrowAmount(borrowAmount));
            } else {
                assertFalse(customer.canBorrowAmount(borrowAmount));
            }
        }
    }

    @Nested
    @DisplayName("Credit Reservation Tests - Low Coupling Design")
    class CreditReservationTests {

        @Test
        @DisplayName("Should reserve credit successfully within available limit")
        void shouldReserveCreditSuccessfully() {
            Money reserveAmount = Money.aed(new BigDecimal("20000"));
            Money expectedAvailable = creditLimit.subtract(reserveAmount);
            
            customer.reserveCredit(reserveAmount);
            
            assertEquals(expectedAvailable, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should reject credit reservation exceeding available credit")
        void shouldRejectCreditReservationExceedingAvailableCredit() {
            Money excessiveReservation = Money.aed(new BigDecimal("60000"));
            
            assertThrows(InsufficientCreditException.class, () ->
                customer.reserveCredit(excessiveReservation)
            );
        }

        @Test
        @DisplayName("Should handle multiple credit reservations correctly")
        void shouldHandleMultipleCreditReservationsCorrectly() {
            Money firstReservation = Money.aed(new BigDecimal("15000"));
            Money secondReservation = Money.aed(new BigDecimal("20000"));
            
            customer.reserveCredit(firstReservation);
            customer.reserveCredit(secondReservation);
            
            Money expectedAvailable = creditLimit.subtract(firstReservation).subtract(secondReservation);
            assertEquals(expectedAvailable, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should prevent over-reservation with multiple attempts")
        void shouldPreventOverReservationWithMultipleAttempts() {
            Money firstReservation = Money.aed(new BigDecimal("40000"));
            Money secondReservation = Money.aed(new BigDecimal("15000"));
            
            customer.reserveCredit(firstReservation);
            
            assertThrows(InsufficientCreditException.class, () ->
                customer.reserveCredit(secondReservation)
            );
        }

        @Test
        @DisplayName("Should release reserved credit correctly")
        void shouldReleaseReservedCreditCorrectly() {
            Money reserveAmount = Money.aed(new BigDecimal("30000"));
            Money releaseAmount = Money.aed(new BigDecimal("10000"));
            
            customer.reserveCredit(reserveAmount);
            customer.releaseCredit(releaseAmount);
            
            Money expectedAvailable = creditLimit.subtract(reserveAmount).add(releaseAmount);
            assertEquals(expectedAvailable, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should handle complete credit release")
        void shouldHandleCompleteCreditRelease() {
            Money reserveAmount = Money.aed(new BigDecimal("25000"));
            
            customer.reserveCredit(reserveAmount);
            customer.releaseCredit(reserveAmount);
            
            assertEquals(creditLimit, customer.getCreditProfile().getAvailableCredit());
        }
    }

    @Nested
    @DisplayName("Contact Information Management - Single Responsibility")
    class ContactInformationTests {

        @Test
        @DisplayName("Should update email successfully with valid format")
        void shouldUpdateEmailSuccessfully() {
            String newEmail = "john.updated@bank.com";
            
            customer.updateContactInformation(newEmail, null);
            
            assertEquals(newEmail, customer.getEmail());
        }

        @Test
        @DisplayName("Should update phone number successfully")
        void shouldUpdatePhoneNumberSuccessfully() {
            String newPhone = "+971509876543";
            
            customer.updateContactInformation(null, newPhone);
            
            assertEquals(newPhone, customer.getPhoneNumber());
        }

        @Test
        @DisplayName("Should update both email and phone together")
        void shouldUpdateBothEmailAndPhoneTogether() {
            String newEmail = "updated@example.com";
            String newPhone = "+971551234567";
            
            customer.updateContactInformation(newEmail, newPhone);
            
            assertAll(
                () -> assertEquals(newEmail, customer.getEmail()),
                () -> assertEquals(newPhone, customer.getPhoneNumber())
            );
        }

        @Test
        @DisplayName("Should ignore invalid email update")
        void shouldIgnoreInvalidEmailUpdate() {
            String originalEmail = customer.getEmail();
            String invalidEmail = "invalid-email-format";
            
            customer.updateContactInformation(invalidEmail, null);
            
            assertEquals(originalEmail, customer.getEmail());
        }

        @Test
        @DisplayName("Should ignore empty phone number update")
        void shouldIgnoreEmptyPhoneNumberUpdate() {
            String originalPhone = customer.getPhoneNumber();
            
            customer.updateContactInformation(null, "");
            customer.updateContactInformation(null, "   ");
            
            assertEquals(originalPhone, customer.getPhoneNumber());
        }
    }

    @Nested
    @DisplayName("Credit Limit Management - Information Expert Pattern")
    class CreditLimitManagementTests {

        @Test
        @DisplayName("Should update credit limit successfully")
        void shouldUpdateCreditLimitSuccessfully() {
            Money newCreditLimit = Money.aed(new BigDecimal("75000"));
            
            customer.updateCreditLimit(newCreditLimit);
            
            assertEquals(newCreditLimit, customer.getCreditProfile().getCreditLimit());
        }

        @Test
        @DisplayName("Should maintain available credit when increasing limit")
        void shouldMaintainAvailableCreditWhenIncreasingLimit() {
            Money reserveAmount = Money.aed(new BigDecimal("20000"));
            customer.reserveCredit(reserveAmount);
            
            Money newCreditLimit = Money.aed(new BigDecimal("80000"));
            customer.updateCreditLimit(newCreditLimit);
            
            Money expectedAvailable = newCreditLimit.subtract(reserveAmount);
            assertEquals(expectedAvailable, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should handle credit limit decrease appropriately")
        void shouldHandleCreditLimitDecreaseAppropriately() {
            Money newCreditLimit = Money.aed(new BigDecimal("30000"));
            
            customer.updateCreditLimit(newCreditLimit);
            
            assertEquals(newCreditLimit, customer.getCreditProfile().getCreditLimit());
            assertEquals(newCreditLimit, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should reject null credit limit update")
        void shouldRejectNullCreditLimitUpdate() {
            assertThrows(NullPointerException.class, () ->
                customer.updateCreditLimit(null)
            );
        }

        @Test
        @DisplayName("Should reject negative credit limit")
        void shouldRejectNegativeCreditLimit() {
            Money negativeCreditLimit = Money.aed(new BigDecimal("-10000"));
            
            assertThrows(IllegalArgumentException.class, () ->
                customer.updateCreditLimit(negativeCreditLimit)
            );
        }
    }

    @Nested
    @DisplayName("Business Rule Integration Tests - High Cohesion")
    class BusinessRuleIntegrationTests {

        @Test
        @DisplayName("Should maintain financial integrity across all operations")
        void shouldMaintainFinancialIntegrityAcrossAllOperations() {
            // Initial state
            Money initialCredit = customer.getCreditProfile().getCreditLimit();
            Money initialAvailable = customer.getCreditProfile().getAvailableCredit();
            assertEquals(initialCredit, initialAvailable);
            
            // Reserve some credit
            Money reservation1 = Money.aed(new BigDecimal("15000"));
            customer.reserveCredit(reservation1);
            assertEquals(initialCredit.subtract(reservation1), customer.getCreditProfile().getAvailableCredit());
            
            // Update credit limit
            Money newLimit = Money.aed(new BigDecimal("70000"));
            customer.updateCreditLimit(newLimit);
            assertEquals(newLimit.subtract(reservation1), customer.getCreditProfile().getAvailableCredit());
            
            // Make another reservation
            Money reservation2 = Money.aed(new BigDecimal("25000"));
            customer.reserveCredit(reservation2);
            
            Money expectedFinal = newLimit.subtract(reservation1).subtract(reservation2);
            assertEquals(expectedFinal, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should enforce business invariants consistently")
        void shouldEnforceBusinessInvariantsConsistently() {
            // Invariant: Available credit should never exceed credit limit
            // Invariant: Available credit should never be negative
            // Invariant: Customer should have valid contact information
            
            Money testLimit = Money.aed(new BigDecimal("40000"));
            customer.updateCreditLimit(testLimit);
            
            // Test various operations maintain invariants
            assertTrue(customer.getCreditProfile().getAvailableCredit().compareTo(testLimit) <= 0);
            assertTrue(customer.getCreditProfile().getAvailableCredit().isPositive() || 
                      customer.getCreditProfile().getAvailableCredit().isZero());
            assertTrue(customer.getEmail().contains("@"));
            assertFalse(customer.getFirstName().trim().isEmpty());
            assertFalse(customer.getLastName().trim().isEmpty());
        }

        @Test
        @DisplayName("Should handle complex credit scenarios correctly")
        void shouldHandleComplexCreditScenariosCorrectly() {
            // Scenario: Customer with existing reservations, limit changes, and mixed operations
            
            // Step 1: Reserve credit
            Money reservation = Money.aed(new BigDecimal("20000"));
            customer.reserveCredit(reservation);
            
            // Step 2: Try to borrow more than available (should fail)
            Money excessiveBorrow = Money.aed(new BigDecimal("40000"));
            assertFalse(customer.canBorrowAmount(excessiveBorrow));
            
            // Step 3: Increase credit limit
            Money newLimit = Money.aed(new BigDecimal("80000"));
            customer.updateCreditLimit(newLimit);
            
            // Step 4: Now should be able to borrow the previously excessive amount
            assertTrue(customer.canBorrowAmount(excessiveBorrow));
            
            // Step 5: Verify the math
            Money expectedAvailable = newLimit.subtract(reservation);
            assertEquals(expectedAvailable, customer.getCreditProfile().getAvailableCredit());
        }
    }

    @Nested
    @DisplayName("Credit Score-Based Credit Limit Calculation - Archive Business Logic")
    class CreditScoreBasedCreditLimitTests {

        @Test
        @DisplayName("Should calculate credit limit based on credit score and monthly income")
        void shouldCalculateCreditLimitBasedOnCreditScoreAndMonthlyIncome() {
            // High credit score (750+) should get 5x multiplier
            Money monthlyIncome = Money.aed(new BigDecimal("10000"));
            Integer highCreditScore = 780;
            Money expectedHighCreditLimit = Money.aed(new BigDecimal("50000")); // 10000 * 5
            
            Customer highScoreCustomer = Customer.createWithCreditScore(
                CustomerId.generate(), "High", "Score", "high@example.com", 
                "+971501234567", monthlyIncome, highCreditScore);
                
            assertEquals(expectedHighCreditLimit, highScoreCustomer.getCreditProfile().getCreditLimit());
        }

        @Test
        @DisplayName("Should use 4x multiplier for medium credit score (650-749)")
        void shouldUseFourTimesMultiplierForMediumCreditScore() {
            Money monthlyIncome = Money.aed(new BigDecimal("8000"));
            Integer mediumCreditScore = 700;
            Money expectedMediumCreditLimit = Money.aed(new BigDecimal("32000")); // 8000 * 4
            
            Customer mediumScoreCustomer = Customer.createWithCreditScore(
                CustomerId.generate(), "Medium", "Score", "medium@example.com", 
                "+971501234567", monthlyIncome, mediumCreditScore);
                
            assertEquals(expectedMediumCreditLimit, mediumScoreCustomer.getCreditProfile().getCreditLimit());
        }

        @Test
        @DisplayName("Should use 3x multiplier for low credit score (below 650)")
        void shouldUseThreeTimesMultiplierForLowCreditScore() {
            Money monthlyIncome = Money.aed(new BigDecimal("5000"));
            Integer lowCreditScore = 600;
            Money expectedLowCreditLimit = Money.aed(new BigDecimal("15000")); // 5000 * 3
            
            Customer lowScoreCustomer = Customer.createWithCreditScore(
                CustomerId.generate(), "Low", "Score", "low@example.com", 
                "+971501234567", monthlyIncome, lowCreditScore);
                
            assertEquals(expectedLowCreditLimit, lowScoreCustomer.getCreditProfile().getCreditLimit());
        }

        @Test
        @DisplayName("Should reject credit score below minimum (300)")
        void shouldRejectCreditScoreBelowMinimum() {
            Money monthlyIncome = Money.aed(new BigDecimal("5000"));
            Integer invalidCreditScore = 250;
            
            assertThrows(IllegalArgumentException.class, () ->
                Customer.createWithCreditScore(
                    CustomerId.generate(), "Invalid", "Score", "invalid@example.com", 
                    "+971501234567", monthlyIncome, invalidCreditScore)
            );
        }

        @Test
        @DisplayName("Should reject credit score above maximum (850)")
        void shouldRejectCreditScoreAboveMaximum() {
            Money monthlyIncome = Money.aed(new BigDecimal("5000"));
            Integer invalidCreditScore = 900;
            
            assertThrows(IllegalArgumentException.class, () ->
                Customer.createWithCreditScore(
                    CustomerId.generate(), "Invalid", "Score", "invalid@example.com", 
                    "+971501234567", monthlyIncome, invalidCreditScore)
            );
        }

        @Test
        @DisplayName("Should reject monthly income below minimum (1000)")
        void shouldRejectMonthlyIncomeBelowMinimum() {
            Money lowIncome = Money.aed(new BigDecimal("800"));
            Integer validCreditScore = 650;
            
            assertThrows(IllegalArgumentException.class, () ->
                Customer.createWithCreditScore(
                    CustomerId.generate(), "Low", "Income", "low@example.com", 
                    "+971501234567", lowIncome, validCreditScore)
            );
        }

        @Test
        @DisplayName("Should update credit limit when credit score changes")
        void shouldUpdateCreditLimitWhenCreditScoreChanges() {
            Money monthlyIncome = Money.aed(new BigDecimal("10000"));
            Integer initialCreditScore = 600;
            
            Customer customer = Customer.createWithCreditScore(
                CustomerId.generate(), "Update", "Score", "update@example.com", 
                "+971501234567", monthlyIncome, initialCreditScore);
                
            // Initial credit limit should be 10000 * 3 = 30000
            assertEquals(Money.aed(new BigDecimal("30000")), customer.getCreditProfile().getCreditLimit());
            
            // Update credit score to high range
            Integer newCreditScore = 780;
            customer.updateCreditScore(newCreditScore);
            
            // New credit limit should be 10000 * 5 = 50000
            assertEquals(Money.aed(new BigDecimal("50000")), customer.getCreditProfile().getCreditLimit());
        }

        @Test
        @DisplayName("Should assess loan eligibility based on credit score")
        void shouldAssessLoanEligibilityBasedOnCreditScore() {
            Money monthlyIncome = Money.aed(new BigDecimal("10000"));
            Integer goodCreditScore = 700;
            
            Customer customer = Customer.createWithCreditScore(
                CustomerId.generate(), "Eligible", "Customer", "eligible@example.com", 
                "+971501234567", monthlyIncome, goodCreditScore);
                
            Money loanAmount = Money.aed(new BigDecimal("20000"));
            assertTrue(customer.isEligibleForLoan(loanAmount));
            
            // Customer with credit score below 600 should not be eligible
            Integer poorCreditScore = 550;
            Customer poorCreditCustomer = Customer.createWithCreditScore(
                CustomerId.generate(), "Poor", "Credit", "poor@example.com", 
                "+971501234567", monthlyIncome, poorCreditScore);
                
            assertFalse(poorCreditCustomer.isEligibleForLoan(loanAmount));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Property-Based Testing")
    class EdgeCasesTests {

        @ParameterizedTest
        @ValueSource(strings = {"0.01", "1", "100", "1000", "10000", "100000", "1000000"})
        @DisplayName("Should handle various credit limit amounts correctly")
        void shouldHandleVariousCreditLimitAmountsCorrectly(String limitValue) {
            Money testLimit = Money.aed(new BigDecimal(limitValue));
            Customer testCustomer = Customer.create(
                CustomerId.generate(), "Test", "User", "test@example.com", "+971501234567", testLimit
            );
            
            // Properties that should always hold
            assertEquals(testLimit, testCustomer.getCreditProfile().getCreditLimit());
            assertEquals(testLimit, testCustomer.getCreditProfile().getAvailableCredit());
            assertTrue(testCustomer.canBorrowAmount(testLimit));
            
            if (testLimit.isPositive()) {
                Money halfLimit = testLimit.divide(new BigDecimal("2"));
                assertTrue(testCustomer.canBorrowAmount(halfLimit));
            }
        }

        @Test
        @DisplayName("Should handle minimum viable credit scenarios")
        void shouldHandleMinimumViableCreditScenarios() {
            Money minimumCredit = Money.aed(new BigDecimal("0.01"));
            Customer minCustomer = Customer.create(
                CustomerId.generate(), "Min", "User", "min@example.com", "+971501234567", minimumCredit
            );
            
            assertTrue(minCustomer.canBorrowAmount(minimumCredit));
            
            minCustomer.reserveCredit(minimumCredit);
            assertEquals(Money.aed(BigDecimal.ZERO), minCustomer.getCreditProfile().getAvailableCredit());
            
            minCustomer.releaseCredit(minimumCredit);
            assertEquals(minimumCredit, minCustomer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should maintain consistency under concurrent-like operations")
        void shouldMaintainConsistencyUnderConcurrentLikeOperations() {
            // Simulate rapid sequence of operations that might occur in concurrent scenarios
            Money baseLimit = Money.aed(new BigDecimal("100000"));
            customer.updateCreditLimit(baseLimit);
            
            // Rapid sequence of reserve/release operations
            for (int i = 0; i < 10; i++) {
                Money amount = Money.aed(new BigDecimal("5000"));
                customer.reserveCredit(amount);
                customer.releaseCredit(amount);
            }
            
            // Should end up back at original state
            assertEquals(baseLimit, customer.getCreditProfile().getAvailableCredit());
        }

        @Test
        @DisplayName("Should handle edge case combinations correctly")
        void shouldHandleEdgeCaseCombinationsCorrectly() {
            // Test combination of edge cases
            Money edgeLimit = Money.aed(new BigDecimal("99999.99"));
            customer.updateCreditLimit(edgeLimit);
            
            // Reserve almost all credit
            Money largeReservation = Money.aed(new BigDecimal("99999.98"));
            customer.reserveCredit(largeReservation);
            
            // Should have 0.01 available
            Money expectedRemaining = Money.aed(new BigDecimal("0.01"));
            assertEquals(expectedRemaining, customer.getCreditProfile().getAvailableCredit());
            
            // Should be able to borrow exactly the remaining amount
            assertTrue(customer.canBorrowAmount(expectedRemaining));
            
            // But not more
            Money slightlyMore = Money.aed(new BigDecimal("0.02"));
            assertFalse(customer.canBorrowAmount(slightlyMore));
        }
    }
}