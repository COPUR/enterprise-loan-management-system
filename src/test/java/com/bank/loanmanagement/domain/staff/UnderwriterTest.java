package com.bank.loanmanagement.domain.staff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Unit Tests for Underwriter Domain Model
 * 
 * Tests the pure domain logic without any infrastructure dependencies.
 * Validates business rules, behavior, and domain constraints.
 * 
 * Architecture Compliance Testing:
 * ✅ Clean Code: Tests clearly express business requirements
 * ✅ DDD: Tests validate domain invariants and business rules
 * ✅ Type Safety: Tests validate input validation and error handling
 * ✅ Hexagonal Architecture: Pure domain testing without infrastructure
 */
@DisplayName("Underwriter Domain Model Tests")
class UnderwriterTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create valid underwriter with required fields")
        void shouldCreateValidUnderwriter() {
            // Given
            String underwriterId = "UW001";
            String firstName = "John";
            String lastName = "Smith";
            String email = "john.smith@bank.com";
            String phone = "+1234567890";
            UnderwriterSpecialization specialization = UnderwriterSpecialization.PERSONAL_LOANS;
            Integer yearsExperience = 5;
            BigDecimal approvalLimit = new BigDecimal("100000");
            LocalDate hireDate = LocalDate.now().minusYears(5);

            // When
            Underwriter underwriter = Underwriter.create(
                underwriterId, firstName, lastName, email, phone,
                specialization, yearsExperience, approvalLimit, hireDate
            );

            // Then
            assertThat(underwriter).isNotNull();
            assertThat(underwriter.getUnderwriterId()).isEqualTo(underwriterId);
            assertThat(underwriter.getFirstName()).isEqualTo(firstName);
            assertThat(underwriter.getLastName()).isEqualTo(lastName);
            assertThat(underwriter.getEmail()).isEqualTo(email.toLowerCase());
            assertThat(underwriter.getPhone()).isEqualTo(phone);
            assertThat(underwriter.getSpecialization()).isEqualTo(specialization);
            assertThat(underwriter.getYearsExperience()).isEqualTo(yearsExperience);
            assertThat(underwriter.getApprovalLimit()).isEqualTo(approvalLimit);
            assertThat(underwriter.getHireDate()).isEqualTo(hireDate);
            assertThat(underwriter.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should validate underwriter ID format")
        void shouldValidateUnderwriterIdFormat() {
            // Given
            String invalidUnderwriterId = "INVALID";

            // When & Then
            assertThatThrownBy(() -> 
                Underwriter.create(invalidUnderwriterId, "John", "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 5, new BigDecimal("100000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Underwriter ID must follow pattern UW###");
        }

        @Test
        @DisplayName("Should validate required fields are not null")
        void shouldValidateRequiredFieldsAreNotNull() {
            // Test null first name
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", null, "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 5, new BigDecimal("100000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("First name is required");

            // Test null specialization
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 "john@bank.com", null, null,
                                 5, new BigDecimal("100000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Specialization is required");
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() {
            // Given
            String invalidEmail = "invalid-email";

            // When & Then
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 invalidEmail, null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 5, new BigDecimal("100000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email must be valid");
        }

        @Test
        @DisplayName("Should validate years of experience range")
        void shouldValidateYearsOfExperienceRange() {
            // Test negative experience
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 -1, new BigDecimal("100000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Years of experience cannot be negative");

            // Test excessive experience
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 60, new BigDecimal("100000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Years of experience cannot exceed 50");
        }

        @Test
        @DisplayName("Should validate approval limit range")
        void shouldValidateApprovalLimitRange() {
            // Test minimum approval limit
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 5, new BigDecimal("500"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Approval limit must be at least $1,000");

            // Test maximum approval limit
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 5, new BigDecimal("15000000"), LocalDate.now().minusYears(5))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Approval limit cannot exceed $10,000,000");
        }

        @Test
        @DisplayName("Should validate hire date is not in future")
        void shouldValidateHireDateIsNotInFuture() {
            // Given
            LocalDate futureDate = LocalDate.now().plusDays(1);

            // When & Then
            assertThatThrownBy(() -> 
                Underwriter.create("UW001", "John", "Smith", 
                                 "john@bank.com", null, UnderwriterSpecialization.PERSONAL_LOANS,
                                 5, new BigDecimal("100000"), futureDate)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hire date cannot be in the future");
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // Given
            String uppercaseEmail = "JOHN.SMITH@BANK.COM";

            // When
            Underwriter underwriter = Underwriter.create(
                "UW001", "John", "Smith", uppercaseEmail, null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, 
                new BigDecimal("100000"), LocalDate.now().minusYears(5)
            );

            // Then
            assertThat(underwriter.getEmail()).isEqualTo("john.smith@bank.com");
        }

        @Test
        @DisplayName("Should trim names")
        void shouldTrimNames() {
            // Given
            String firstNameWithSpaces = "  John  ";
            String lastNameWithSpaces = "  Smith  ";

            // When
            Underwriter underwriter = Underwriter.create(
                "UW001", firstNameWithSpaces, lastNameWithSpaces, "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, 
                new BigDecimal("100000"), LocalDate.now().minusYears(5)
            );

            // Then
            assertThat(underwriter.getFirstName()).isEqualTo("John");
            assertThat(underwriter.getLastName()).isEqualTo("Smith");
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        private Underwriter createSampleUnderwriter() {
            return Underwriter.create(
                "UW001", "John", "Smith", "john.smith@bank.com", "+1234567890",
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("100000"),
                LocalDate.now().minusYears(5)
            );
        }

        @Test
        @DisplayName("Should approve loan within approval limit")
        void shouldApproveLoanWithinApprovalLimit() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();
            BigDecimal loanAmount = new BigDecimal("50000");

            // When & Then
            assertThat(underwriter.canApprove(loanAmount)).isTrue();
        }

        @Test
        @DisplayName("Should not approve loan exceeding approval limit")
        void shouldNotApproveLoanExceedingApprovalLimit() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();
            BigDecimal loanAmount = new BigDecimal("150000");

            // When & Then
            assertThat(underwriter.canApprove(loanAmount)).isFalse();
        }

        @Test
        @DisplayName("Should approve loan equal to approval limit")
        void shouldApproveLoanEqualToApprovalLimit() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();
            BigDecimal loanAmount = new BigDecimal("100000");

            // When & Then
            assertThat(underwriter.canApprove(loanAmount)).isTrue();
        }

        @Test
        @DisplayName("Should not approve loan when inactive")
        void shouldNotApproveLoanWhenInactive() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();
            underwriter.updateStatus(EmployeeStatus.INACTIVE);
            BigDecimal loanAmount = new BigDecimal("50000");

            // When & Then
            assertThat(underwriter.canApprove(loanAmount)).isFalse();
        }

        @Test
        @DisplayName("Should handle null loan amount gracefully")
        void shouldHandleNullLoanAmountGracefully() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();

            // When & Then
            assertThat(underwriter.canApprove(null)).isFalse();
        }

        @Test
        @DisplayName("Should check specialization correctly")
        void shouldCheckSpecializationCorrectly() {
            // Given
            Underwriter personalLoansUW = createSampleUnderwriter();
            Underwriter businessLoansUW = Underwriter.create(
                "UW002", "Jane", "Doe", "jane.doe@bank.com", null,
                UnderwriterSpecialization.BUSINESS_LOANS, 8, new BigDecimal("5000000"),
                LocalDate.now().minusYears(8)
            );
            Underwriter mortgageUW = Underwriter.create(
                "UW003", "Bob", "Johnson", "bob.johnson@bank.com", null,
                UnderwriterSpecialization.MORTGAGES, 10, new BigDecimal("10000000"),
                LocalDate.now().minusYears(10)
            );

            // When & Then
            assertThat(personalLoansUW.specializesIn("PERSONAL")).isTrue();
            assertThat(personalLoansUW.specializesIn("BUSINESS")).isFalse();
            assertThat(personalLoansUW.specializesIn("MORTGAGE")).isFalse();

            assertThat(businessLoansUW.specializesIn("BUSINESS")).isTrue();
            assertThat(businessLoansUW.specializesIn("PERSONAL")).isFalse();
            assertThat(businessLoansUW.specializesIn("MORTGAGE")).isFalse();

            assertThat(mortgageUW.specializesIn("MORTGAGE")).isTrue();
            assertThat(mortgageUW.specializesIn("PERSONAL")).isFalse();
            assertThat(mortgageUW.specializesIn("BUSINESS")).isFalse();
        }

        @Test
        @DisplayName("Should handle case insensitive loan type matching")
        void shouldHandleCaseInsensitiveLoanTypeMatching() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();

            // When & Then
            assertThat(underwriter.specializesIn("personal")).isTrue();
            assertThat(underwriter.specializesIn("PERSONAL")).isTrue();
            assertThat(underwriter.specializesIn("Personal")).isTrue();
        }

        @Test
        @DisplayName("Should provide full name")
        void shouldProvideFullName() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();

            // When
            String fullName = underwriter.getFullName();

            // Then
            assertThat(fullName).isEqualTo("John Smith");
        }

        @Test
        @DisplayName("Should determine if underwriter is senior")
        void shouldDetermineIfUnderwriterIsSenior() {
            // Given
            Underwriter seniorUW = createSampleUnderwriter(); // 5 years experience
            Underwriter juniorUW = Underwriter.create(
                "UW004", "Junior", "Writer", "junior@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 3, new BigDecimal("50000"),
                LocalDate.now().minusYears(3)
            );

            // When & Then
            assertThat(seniorUW.isSenior()).isTrue();
            assertThat(juniorUW.isSenior()).isFalse();
        }

        @Test
        @DisplayName("Should get experience level description")
        void shouldGetExperienceLevelDescription() {
            // Test different experience levels
            Underwriter junior = Underwriter.create(
                "UW001", "Junior", "Writer", "junior@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 1, new BigDecimal("25000"),
                LocalDate.now().minusYears(1)
            );
            assertThat(junior.getExperienceLevel()).isEqualTo("Junior");

            Underwriter midLevel = Underwriter.create(
                "UW002", "Mid", "Writer", "mid@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 3, new BigDecimal("50000"),
                LocalDate.now().minusYears(3)
            );
            assertThat(midLevel.getExperienceLevel()).isEqualTo("Mid-Level");

            Underwriter senior = createSampleUnderwriter(); // 5 years
            assertThat(senior.getExperienceLevel()).isEqualTo("Senior");

            Underwriter expert = Underwriter.create(
                "UW003", "Expert", "Writer", "expert@bank.com", null,
                UnderwriterSpecialization.MORTGAGES, 15, new BigDecimal("5000000"),
                LocalDate.now().minusYears(15)
            );
            assertThat(expert.getExperienceLevel()).isEqualTo("Expert");
        }

        @Test
        @DisplayName("Should validate approval limit for specialization")
        void shouldValidateApprovalLimitForSpecialization() {
            // Test valid limits
            Underwriter personalLoansUW = Underwriter.create(
                "UW001", "John", "Smith", "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("75000"),
                LocalDate.now().minusYears(5)
            );
            assertThat(personalLoansUW.hasValidApprovalLimitForSpecialization()).isTrue();

            Underwriter businessLoansUW = Underwriter.create(
                "UW002", "Jane", "Doe", "jane@bank.com", null,
                UnderwriterSpecialization.BUSINESS_LOANS, 8, new BigDecimal("3000000"),
                LocalDate.now().minusYears(8)
            );
            assertThat(businessLoansUW.hasValidApprovalLimitForSpecialization()).isTrue();

            Underwriter mortgageUW = Underwriter.create(
                "UW003", "Bob", "Johnson", "bob@bank.com", null,
                UnderwriterSpecialization.MORTGAGES, 10, new BigDecimal("8000000"),
                LocalDate.now().minusYears(10)
            );
            assertThat(mortgageUW.hasValidApprovalLimitForSpecialization()).isTrue();

            // Test invalid limits
            Underwriter invalidPersonalUW = Underwriter.create(
                "UW004", "Invalid", "Personal", "invalid@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("150000"), // Too high
                LocalDate.now().minusYears(5)
            );
            assertThat(invalidPersonalUW.hasValidApprovalLimitForSpecialization()).isFalse();
        }

        @Test
        @DisplayName("Should update status correctly")
        void shouldUpdateStatusCorrectly() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();
            assertThat(underwriter.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

            // When
            underwriter.updateStatus(EmployeeStatus.ON_LEAVE);

            // Then
            assertThat(underwriter.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
        }

        @Test
        @DisplayName("Should not update status if same")
        void shouldNotUpdateStatusIfSame() {
            // Given
            Underwriter underwriter = createSampleUnderwriter();
            java.time.LocalDateTime originalUpdatedAt = underwriter.getUpdatedAt();

            // When - Set same status
            underwriter.updateStatus(EmployeeStatus.ACTIVE);

            // Then - Updated time should not change
            assertThat(underwriter.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should check if available for new loans")
        void shouldCheckIfAvailableForNewLoans() {
            // Given
            Underwriter activeUW = createSampleUnderwriter();
            Underwriter inactiveUW = createSampleUnderwriter();
            inactiveUW.updateStatus(EmployeeStatus.INACTIVE);

            // When & Then
            assertThat(activeUW.isAvailableForNewLoans()).isTrue();
            assertThat(inactiveUW.isAvailableForNewLoans()).isFalse();
        }

        @Test
        @DisplayName("Should check if can handle urgent cases")
        void shouldCheckIfCanHandleUrgentCases() {
            // Given
            Underwriter seniorActiveUW = createSampleUnderwriter(); // 5 years, active
            Underwriter juniorActiveUW = Underwriter.create(
                "UW002", "Junior", "Writer", "junior@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 3, new BigDecimal("50000"), // 3 years
                LocalDate.now().minusYears(3)
            );
            Underwriter seniorInactiveUW = createSampleUnderwriter();
            seniorInactiveUW.updateStatus(EmployeeStatus.INACTIVE);

            // When & Then
            assertThat(seniorActiveUW.canHandleUrgentCases()).isTrue();
            assertThat(juniorActiveUW.canHandleUrgentCases()).isFalse(); // Not senior
            assertThat(seniorInactiveUW.canHandleUrgentCases()).isFalse(); // Not active
        }
    }

    @Nested
    @DisplayName("Reconstruction Tests")
    class ReconstructionTests {

        @Test
        @DisplayName("Should reconstruct underwriter from infrastructure data")
        void shouldReconstructUnderwriterFromInfrastructureData() {
            // Given - Infrastructure data
            String underwriterId = "UW001";
            String firstName = "John";
            String lastName = "Smith";
            String email = "john.smith@bank.com";
            String phone = "+1234567890";
            UnderwriterSpecialization specialization = UnderwriterSpecialization.MORTGAGES;
            Integer yearsExperience = 10;
            BigDecimal approvalLimit = new BigDecimal("5000000");
            EmployeeStatus status = EmployeeStatus.ACTIVE;
            LocalDate hireDate = LocalDate.now().minusYears(10);
            java.time.LocalDateTime createdAt = java.time.LocalDateTime.now().minusYears(10);
            java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now().minusMonths(6);
            Integer version = 3;

            // When
            Underwriter underwriter = Underwriter.reconstruct(
                underwriterId, firstName, lastName, email, phone,
                specialization, yearsExperience, approvalLimit, status,
                hireDate, createdAt, updatedAt, version
            );

            // Then
            assertThat(underwriter.getUnderwriterId()).isEqualTo(underwriterId);
            assertThat(underwriter.getFirstName()).isEqualTo(firstName);
            assertThat(underwriter.getLastName()).isEqualTo(lastName);
            assertThat(underwriter.getEmail()).isEqualTo(email);
            assertThat(underwriter.getPhone()).isEqualTo(phone);
            assertThat(underwriter.getSpecialization()).isEqualTo(specialization);
            assertThat(underwriter.getYearsExperience()).isEqualTo(yearsExperience);
            assertThat(underwriter.getApprovalLimit()).isEqualTo(approvalLimit);
            assertThat(underwriter.getStatus()).isEqualTo(status);
            assertThat(underwriter.getHireDate()).isEqualTo(hireDate);
            assertThat(underwriter.getCreatedAt()).isEqualTo(createdAt);
            assertThat(underwriter.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(underwriter.getVersion()).isEqualTo(version);
        }
    }

    @Nested
    @DisplayName("Equality and Identity Tests")
    class EqualityAndIdentityTests {

        @Test
        @DisplayName("Should implement equality based on underwriter ID")
        void shouldImplementEqualityBasedOnUnderwriterId() {
            // Given
            Underwriter uw1 = Underwriter.create(
                "UW001", "John", "Smith", "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("100000"),
                LocalDate.now().minusYears(5)
            );
            
            Underwriter uw2 = Underwriter.create(
                "UW001", "Jane", "Doe", "jane@bank.com", null, // Different details
                UnderwriterSpecialization.MORTGAGES, 10, new BigDecimal("5000000"),
                LocalDate.now().minusYears(10)
            );
            
            Underwriter uw3 = Underwriter.create(
                "UW002", "John", "Smith", "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("100000"),
                LocalDate.now().minusYears(5)
            );

            // When & Then
            assertThat(uw1).isEqualTo(uw2); // Same underwriter ID
            assertThat(uw1).isNotEqualTo(uw3); // Different underwriter ID
            assertThat(uw1.hashCode()).isEqualTo(uw2.hashCode());
            assertThat(uw1.hashCode()).isNotEqualTo(uw3.hashCode());
        }

        @Test
        @DisplayName("Should implement getId method for aggregate root")
        void shouldImplementGetIdMethodForAggregateRoot() {
            // Given
            Underwriter underwriter = Underwriter.create(
                "UW001", "John", "Smith", "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("100000"),
                LocalDate.now().minusYears(5)
            );

            // When & Then
            assertThat(underwriter.getId()).isEqualTo("UW001");
        }

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            // Given
            Underwriter underwriter = Underwriter.create(
                "UW001", "John", "Smith", "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("100000"),
                LocalDate.now().minusYears(5)
            );

            // When
            String toString = underwriter.toString();

            // Then
            assertThat(toString).contains("UW001");
            assertThat(toString).contains("John Smith");
            assertThat(toString).contains("PERSONAL_LOANS");
            assertThat(toString).contains("100000");
            assertThat(toString).contains("ACTIVE");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null values gracefully in business methods")
        void shouldHandleNullValuesGracefullyInBusinessMethods() {
            // Given
            Underwriter underwriter = Underwriter.create(
                "UW001", "John", "Smith", "john@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 5, new BigDecimal("100000"),
                LocalDate.now().minusYears(5)
            );

            // When & Then
            assertThat(underwriter.specializesIn(null)).isFalse();
            assertThat(underwriter.canApprove(null)).isFalse();
        }

        @Test
        @DisplayName("Should handle zero years experience")
        void shouldHandleZeroYearsExperience() {
            // Given & When
            Underwriter newHire = Underwriter.create(
                "UW001", "New", "Hire", "new@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 0, new BigDecimal("25000"),
                LocalDate.now()
            );

            // Then
            assertThat(newHire.getYearsExperience()).isEqualTo(0);
            assertThat(newHire.isSenior()).isFalse();
            assertThat(newHire.getExperienceLevel()).isEqualTo("Junior");
            assertThat(newHire.canHandleUrgentCases()).isFalse();
        }

        @Test
        @DisplayName("Should handle minimum approval limit")
        void shouldHandleMinimumApprovalLimit() {
            // Given & When
            Underwriter minLimitUW = Underwriter.create(
                "UW001", "Min", "Limit", "min@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 1, new BigDecimal("1000"),
                LocalDate.now().minusYears(1)
            );

            // Then
            assertThat(minLimitUW.getApprovalLimit()).isEqualTo(new BigDecimal("1000"));
            assertThat(minLimitUW.canApprove(new BigDecimal("1000"))).isTrue();
            assertThat(minLimitUW.canApprove(new BigDecimal("1001"))).isFalse();
        }
    }
}