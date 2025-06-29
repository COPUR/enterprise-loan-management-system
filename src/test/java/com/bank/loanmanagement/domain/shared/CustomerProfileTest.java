package com.bank.loanmanagement.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Unit Tests for CustomerProfile Value Object
 * 
 * Tests the domain value object that represents customer information
 * within the Loan Management bounded context.
 * 
 * Architecture Compliance Testing:
 * ✅ DDD: Tests value object immutability and business methods
 * ✅ Clean Code: Tests intention-revealing names and business logic
 * ✅ Type Safety: Tests proper validation and business rules
 * ✅ Hexagonal Architecture: Tests domain concept independence
 */
@DisplayName("CustomerProfile Value Object Tests")
class CustomerProfileTest {

    @Nested
    @DisplayName("Creation and Validation Tests")
    class CreationAndValidationTests {

        @Test
        @DisplayName("Should create valid customer profile with all fields")
        void shouldCreateValidCustomerProfileWithAllFields() {
            // Given
            Long customerId = 123L;
            String firstName = "John";
            String lastName = "Doe";
            String email = "john.doe@email.com";
            String phone = "+1234567890";
            BigDecimal monthlyIncome = new BigDecimal("8000");
            CustomerProfile.EmploymentStatus employmentStatus = CustomerProfile.EmploymentStatus.EMPLOYED;
            Integer creditScore = 750;
            LocalDate dateOfBirth = LocalDate.of(1985, 6, 15);
            String address = "123 Main St, Anytown, ST 12345";
            boolean isActive = true;

            // When
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(customerId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .monthlyIncome(monthlyIncome)
                .employmentStatus(employmentStatus)
                .creditScore(creditScore)
                .dateOfBirth(dateOfBirth)
                .address(address)
                .isActive(isActive)
                .build();

            // Then
            assertThat(profile).isNotNull();
            assertThat(profile.getCustomerId()).isEqualTo(customerId);
            assertThat(profile.getFirstName()).isEqualTo(firstName);
            assertThat(profile.getLastName()).isEqualTo(lastName);
            assertThat(profile.getEmail()).isEqualTo(email);
            assertThat(profile.getPhone()).isEqualTo(phone);
            assertThat(profile.getMonthlyIncome()).isEqualTo(monthlyIncome);
            assertThat(profile.getEmploymentStatus()).isEqualTo(employmentStatus);
            assertThat(profile.getCreditScore()).isEqualTo(creditScore);
            assertThat(profile.getDateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(profile.getAddress()).isEqualTo(address);
            assertThat(profile.isActive()).isEqualTo(isActive);
        }

        @Test
        @DisplayName("Should create customer profile with minimal required fields")
        void shouldCreateCustomerProfileWithMinimalRequiredFields() {
            // Given & When
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .build();

            // Then
            assertThat(profile).isNotNull();
            assertThat(profile.getCustomerId()).isEqualTo(123L);
            assertThat(profile.getFirstName()).isEqualTo("John");
            assertThat(profile.getLastName()).isEqualTo("Doe");
            assertThat(profile.isActive()).isTrue();
            
            // Optional fields should be null
            assertThat(profile.getEmail()).isNull();
            assertThat(profile.getPhone()).isNull();
            assertThat(profile.getMonthlyIncome()).isNull();
            assertThat(profile.getEmploymentStatus()).isNull();
            assertThat(profile.getCreditScore()).isNull();
            assertThat(profile.getDateOfBirth()).isNull();
            assertThat(profile.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should require customer ID")
        void shouldRequireCustomerId() {
            // When & Then
            assertThatThrownBy(() -> 
                CustomerProfile.builder()
                    .customerId(null)
                    .firstName("John")
                    .lastName("Doe")
                    .isActive(true)
                    .build()
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Customer ID is required");
        }

        @Test
        @DisplayName("Should require first name")
        void shouldRequireFirstName() {
            // When & Then
            assertThatThrownBy(() -> 
                CustomerProfile.builder()
                    .customerId(123L)
                    .firstName(null)
                    .lastName("Doe")
                    .isActive(true)
                    .build()
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("First name is required");
        }

        @Test
        @DisplayName("Should require last name")
        void shouldRequireLastName() {
            // When & Then
            assertThatThrownBy(() -> 
                CustomerProfile.builder()
                    .customerId(123L)
                    .firstName("John")
                    .lastName(null)
                    .isActive(true)
                    .build()
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Last name is required");
        }
    }

    @Nested
    @DisplayName("Business Method Tests")
    class BusinessMethodTests {

        private CustomerProfile createSampleProfile() {
            return CustomerProfile.builder()
                .customerId(123L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phone("+1234567890")
                .monthlyIncome(new BigDecimal("8000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .creditScore(750)
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .address("123 Main St, Anytown, ST 12345")
                .isActive(true)
                .build();
        }

        @Test
        @DisplayName("Should provide full name")
        void shouldProvideFullName() {
            // Given
            CustomerProfile profile = createSampleProfile();

            // When
            String fullName = profile.getFullName();

            // Then
            assertThat(fullName).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should determine good credit correctly")
        void shouldDetermineGoodCreditCorrectly() {
            // Given
            CustomerProfile goodCreditProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Good")
                .lastName("Credit")
                .creditScore(720)
                .isActive(true)
                .build();

            CustomerProfile fairCreditProfile = CustomerProfile.builder()
                .customerId(124L)
                .firstName("Fair")
                .lastName("Credit")
                .creditScore(650)
                .isActive(true)
                .build();

            CustomerProfile noCreditProfile = CustomerProfile.builder()
                .customerId(125L)
                .firstName("No")
                .lastName("Credit")
                .creditScore(null)
                .isActive(true)
                .build();

            // When & Then
            assertThat(goodCreditProfile.hasGoodCredit()).isTrue();
            assertThat(fairCreditProfile.hasGoodCredit()).isFalse();
            assertThat(noCreditProfile.hasGoodCredit()).isFalse();
        }

        @Test
        @DisplayName("Should determine excellent credit correctly")
        void shouldDetermineExcellentCreditCorrectly() {
            // Given
            CustomerProfile excellentCreditProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Excellent")
                .lastName("Credit")
                .creditScore(820)
                .isActive(true)
                .build();

            CustomerProfile goodCreditProfile = CustomerProfile.builder()
                .customerId(124L)
                .firstName("Good")
                .lastName("Credit")
                .creditScore(750)
                .isActive(true)
                .build();

            // When & Then
            assertThat(excellentCreditProfile.hasExcellentCredit()).isTrue();
            assertThat(goodCreditProfile.hasExcellentCredit()).isFalse();
        }

        @Test
        @DisplayName("Should calculate age correctly")
        void shouldCalculateAgeCorrectly() {
            // Given
            LocalDate birthDate = LocalDate.now().minusYears(35).minusMonths(6);
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Age")
                .lastName("Test")
                .dateOfBirth(birthDate)
                .isActive(true)
                .build();

            CustomerProfile noAgeProfile = CustomerProfile.builder()
                .customerId(124L)
                .firstName("No")
                .lastName("Age")
                .dateOfBirth(null)
                .isActive(true)
                .build();

            // When & Then
            assertThat(profile.getAge()).isEqualTo(35);
            assertThat(noAgeProfile.getAge()).isNull();
        }

        @Test
        @DisplayName("Should determine employment status correctly")
        void shouldDetermineEmploymentStatusCorrectly() {
            // Test employed
            CustomerProfile employedProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Employed")
                .lastName("Person")
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // Test self-employed
            CustomerProfile selfEmployedProfile = CustomerProfile.builder()
                .customerId(124L)
                .firstName("Self")
                .lastName("Employed")
                .employmentStatus(CustomerProfile.EmploymentStatus.SELF_EMPLOYED)
                .isActive(true)
                .build();

            // Test unemployed
            CustomerProfile unemployedProfile = CustomerProfile.builder()
                .customerId(125L)
                .firstName("Unemployed")
                .lastName("Person")
                .employmentStatus(CustomerProfile.EmploymentStatus.UNEMPLOYED)
                .isActive(true)
                .build();

            // Test null employment status
            CustomerProfile unknownProfile = CustomerProfile.builder()
                .customerId(126L)
                .firstName("Unknown")
                .lastName("Employment")
                .employmentStatus(null)
                .isActive(true)
                .build();

            // When & Then
            assertThat(employedProfile.isEmployed()).isTrue();
            assertThat(selfEmployedProfile.isEmployed()).isTrue();
            assertThat(unemployedProfile.isEmployed()).isFalse();
            assertThat(unknownProfile.isEmployed()).isFalse();
        }

        @Test
        @DisplayName("Should determine stable income correctly")
        void shouldDetermineStableIncomeCorrectly() {
            // Test stable income
            CustomerProfile stableIncomeProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Stable")
                .lastName("Income")
                .monthlyIncome(new BigDecimal("5000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // Test no income
            CustomerProfile noIncomeProfile = CustomerProfile.builder()
                .customerId(124L)
                .firstName("No")
                .lastName("Income")
                .monthlyIncome(null)
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // Test zero income
            CustomerProfile zeroIncomeProfile = CustomerProfile.builder()
                .customerId(125L)
                .firstName("Zero")
                .lastName("Income")
                .monthlyIncome(BigDecimal.ZERO)
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // Test income but unemployed
            CustomerProfile incomeButUnemployedProfile = CustomerProfile.builder()
                .customerId(126L)
                .firstName("Income")
                .lastName("Unemployed")
                .monthlyIncome(new BigDecimal("2000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.UNEMPLOYED)
                .isActive(true)
                .build();

            // When & Then
            assertThat(stableIncomeProfile.hasStableIncome()).isTrue();
            assertThat(noIncomeProfile.hasStableIncome()).isFalse();
            assertThat(zeroIncomeProfile.hasStableIncome()).isFalse();
            assertThat(incomeButUnemployedProfile.hasStableIncome()).isFalse();
        }
    }

    @Nested
    @DisplayName("Credit Risk Assessment Tests")
    class CreditRiskAssessmentTests {

        @Test
        @DisplayName("Should qualify for loan consideration")
        void shouldQualifyForLoanConsideration() {
            // Given - Good candidate
            CustomerProfile qualifiedProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Qualified")
                .lastName("Customer")
                .monthlyIncome(new BigDecimal("6000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .creditScore(650)
                .isActive(true)
                .build();

            // When & Then
            assertThat(qualifiedProfile.qualifiesForLoanConsideration()).isTrue();
        }

        @Test
        @DisplayName("Should not qualify for loan consideration with poor credit")
        void shouldNotQualifyForLoanConsiderationWithPoorCredit() {
            // Given - Poor credit
            CustomerProfile poorCreditProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Poor")
                .lastName("Credit")
                .monthlyIncome(new BigDecimal("6000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .creditScore(450) // Below 500 minimum
                .isActive(true)
                .build();

            // When & Then
            assertThat(poorCreditProfile.qualifiesForLoanConsideration()).isFalse();
        }

        @Test
        @DisplayName("Should not qualify for loan consideration when inactive")
        void shouldNotQualifyForLoanConsiderationWhenInactive() {
            // Given - Inactive customer
            CustomerProfile inactiveProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Inactive")
                .lastName("Customer")
                .monthlyIncome(new BigDecimal("6000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .creditScore(650)
                .isActive(false)
                .build();

            // When & Then
            assertThat(inactiveProfile.qualifiesForLoanConsideration()).isFalse();
        }

        @Test
        @DisplayName("Should get correct credit risk level")
        void shouldGetCorrectCreditRiskLevel() {
            // Test all credit risk levels
            assertCreditRiskLevel(850, CustomerProfile.CreditRiskLevel.EXCELLENT);
            assertCreditRiskLevel(780, CustomerProfile.CreditRiskLevel.VERY_GOOD);
            assertCreditRiskLevel(700, CustomerProfile.CreditRiskLevel.GOOD);
            assertCreditRiskLevel(620, CustomerProfile.CreditRiskLevel.FAIR);
            assertCreditRiskLevel(500, CustomerProfile.CreditRiskLevel.POOR);
            
            // Test null credit score
            CustomerProfile unknownProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Unknown")
                .lastName("Credit")
                .creditScore(null)
                .isActive(true)
                .build();
            assertThat(unknownProfile.getCreditRiskLevel()).isEqualTo(CustomerProfile.CreditRiskLevel.UNKNOWN);
        }

        private void assertCreditRiskLevel(Integer creditScore, CustomerProfile.CreditRiskLevel expectedLevel) {
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Test")
                .lastName("Customer")
                .creditScore(creditScore)
                .isActive(true)
                .build();
            assertThat(profile.getCreditRiskLevel()).isEqualTo(expectedLevel);
        }
    }

    @Nested
    @DisplayName("Financial Calculation Tests")
    class FinancialCalculationTests {

        @Test
        @DisplayName("Should calculate projected debt-to-income ratio correctly")
        void shouldCalculateProjectedDebtToIncomeRatioCorrectly() {
            // Given
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Calculate")
                .lastName("DTI")
                .monthlyIncome(new BigDecimal("8000"))
                .isActive(true)
                .build();

            BigDecimal monthlyLoanPayment = new BigDecimal("2000");

            // When
            BigDecimal dtiRatio = profile.calculateProjectedDebtToIncomeRatio(monthlyLoanPayment);

            // Then
            assertThat(dtiRatio).isEqualTo(new BigDecimal("0.2500")); // 2000/8000 = 0.25
        }

        @Test
        @DisplayName("Should handle zero income in DTI calculation")
        void shouldHandleZeroIncomeInDTICalculation() {
            // Given
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Zero")
                .lastName("Income")
                .monthlyIncome(BigDecimal.ZERO)
                .isActive(true)
                .build();

            // When
            BigDecimal dtiRatio = profile.calculateProjectedDebtToIncomeRatio(new BigDecimal("1000"));

            // Then
            assertThat(dtiRatio).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle null income in DTI calculation")
        void shouldHandleNullIncomeInDTICalculation() {
            // Given
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Null")
                .lastName("Income")
                .monthlyIncome(null)
                .isActive(true)
                .build();

            // When
            BigDecimal dtiRatio = profile.calculateProjectedDebtToIncomeRatio(new BigDecimal("1000"));

            // Then
            assertThat(dtiRatio).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should determine if customer can afford monthly payment")
        void shouldDetermineIfCustomerCanAffordMonthlyPayment() {
            // Given - Customer with $10,000 monthly income
            CustomerProfile affordableProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Can")
                .lastName("Afford")
                .monthlyIncome(new BigDecimal("10000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // Test affordable payment (DTI = 30%)
            BigDecimal affordablePayment = new BigDecimal("3000");
            // Test unaffordable payment (DTI = 50%)
            BigDecimal unaffordablePayment = new BigDecimal("5000");

            // When & Then
            assertThat(affordableProfile.canAffordMonthlyPayment(affordablePayment)).isTrue();
            assertThat(affordableProfile.canAffordMonthlyPayment(unaffordablePayment)).isFalse();
        }

        @Test
        @DisplayName("Should not afford payment without stable income")
        void shouldNotAffordPaymentWithoutStableIncome() {
            // Given - Customer without stable income
            CustomerProfile unstableIncomeProfile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Unstable")
                .lastName("Income")
                .monthlyIncome(null)
                .employmentStatus(CustomerProfile.EmploymentStatus.UNEMPLOYED)
                .isActive(true)
                .build();

            // When & Then
            assertThat(unstableIncomeProfile.canAffordMonthlyPayment(new BigDecimal("1000"))).isFalse();
        }

        @Test
        @DisplayName("Should apply 43% DTI limit correctly")
        void shouldApply43PercentDTILimitCorrectly() {
            // Given - Customer with $5000 monthly income
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("DTI")
                .lastName("Limit")
                .monthlyIncome(new BigDecimal("5000"))
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // Test exactly 43% DTI
            BigDecimal exactLimitPayment = new BigDecimal("2150"); // 2150/5000 = 43%
            // Test just over 43% DTI
            BigDecimal overLimitPayment = new BigDecimal("2151");

            // When & Then
            assertThat(profile.canAffordMonthlyPayment(exactLimitPayment)).isTrue();
            assertThat(profile.canAffordMonthlyPayment(overLimitPayment)).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality and Identity Tests")
    class EqualityAndIdentityTests {

        @Test
        @DisplayName("Should implement equality based on customer ID")
        void shouldImplementEqualityBasedOnCustomerId() {
            // Given
            CustomerProfile profile1 = CustomerProfile.builder()
                .customerId(123L)
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .build();

            CustomerProfile profile2 = CustomerProfile.builder()
                .customerId(123L)
                .firstName("Jane") // Different name
                .lastName("Smith") // Different name
                .isActive(false) // Different status
                .build();

            CustomerProfile profile3 = CustomerProfile.builder()
                .customerId(456L) // Different ID
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .build();

            // When & Then
            assertThat(profile1).isEqualTo(profile2); // Same customer ID
            assertThat(profile1).isNotEqualTo(profile3); // Different customer ID
            assertThat(profile1.hashCode()).isEqualTo(profile2.hashCode());
            assertThat(profile1.hashCode()).isNotEqualTo(profile3.hashCode());
        }

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            // Given
            CustomerProfile profile = CustomerProfile.builder()
                .customerId(123L)
                .firstName("John")
                .lastName("Doe")
                .creditScore(750)
                .employmentStatus(CustomerProfile.EmploymentStatus.EMPLOYED)
                .isActive(true)
                .build();

            // When
            String toString = profile.toString();

            // Then
            assertThat(toString).contains("123");
            assertThat(toString).contains("John Doe");
            assertThat(toString).contains("750");
            assertThat(toString).contains("EMPLOYED");
            assertThat(toString).contains("true");
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("Should provide display names for employment status")
        void shouldProvideDisplayNamesForEmploymentStatus() {
            assertThat(CustomerProfile.EmploymentStatus.EMPLOYED.getDisplayName()).isEqualTo("Employed");
            assertThat(CustomerProfile.EmploymentStatus.SELF_EMPLOYED.getDisplayName()).isEqualTo("Self-Employed");
            assertThat(CustomerProfile.EmploymentStatus.UNEMPLOYED.getDisplayName()).isEqualTo("Unemployed");
            assertThat(CustomerProfile.EmploymentStatus.RETIRED.getDisplayName()).isEqualTo("Retired");
            assertThat(CustomerProfile.EmploymentStatus.STUDENT.getDisplayName()).isEqualTo("Student");
            assertThat(CustomerProfile.EmploymentStatus.OTHER.getDisplayName()).isEqualTo("Other");
        }

        @Test
        @DisplayName("Should provide display names for credit risk level")
        void shouldProvideDisplayNamesForCreditRiskLevel() {
            assertThat(CustomerProfile.CreditRiskLevel.EXCELLENT.getDisplayName()).isEqualTo("Excellent (800+)");
            assertThat(CustomerProfile.CreditRiskLevel.VERY_GOOD.getDisplayName()).isEqualTo("Very Good (740-799)");
            assertThat(CustomerProfile.CreditRiskLevel.GOOD.getDisplayName()).isEqualTo("Good (670-739)");
            assertThat(CustomerProfile.CreditRiskLevel.FAIR.getDisplayName()).isEqualTo("Fair (580-669)");
            assertThat(CustomerProfile.CreditRiskLevel.POOR.getDisplayName()).isEqualTo("Poor (300-579)");
            assertThat(CustomerProfile.CreditRiskLevel.UNKNOWN.getDisplayName()).isEqualTo("Unknown");
        }
    }
}