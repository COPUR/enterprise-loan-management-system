package com.bank.loanmanagement.domain.loan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for LoanId value object
 * Testing entity ID behavior and validation
 */
@DisplayName("🏦 Loan ID Value Object Tests")
class LoanIdTest {

    @Nested
    @DisplayName("Loan ID Creation")
    class LoanIdCreationTests {

        @Test
        @DisplayName("Should create loan ID with valid value using of() method")
        void shouldCreateLoanIdWithValidValueUsingOfMethod() {
            // Given
            String loanIdValue = "LOAN-12345";

            // When
            LoanId loanId = LoanId.of(loanIdValue);

            // Then
            assertThat(loanId.getValue()).isEqualTo(loanIdValue);
        }

        @Test
        @DisplayName("Should create loan ID with constructor")
        void shouldCreateLoanIdWithConstructor() {
            // Given
            String loanIdValue = "LOAN-CONSTRUCTOR-TEST";

            // When
            LoanId loanId = new LoanId(loanIdValue);

            // Then
            assertThat(loanId.getValue()).isEqualTo(loanIdValue);
        }

        @Test
        @DisplayName("Should accept null loan ID in constructor")
        void shouldAcceptNullLoanIdInConstructor() {
            // When
            LoanId loanId = new LoanId(null);
            
            // Then
            assertThat(loanId.getValue()).isNull();
        }

        @Test
        @DisplayName("Should accept null loan ID in of() method")
        void shouldAcceptNullLoanIdInOfMethod() {
            // When
            LoanId loanId = LoanId.of(null);
            
            // Then
            assertThat(loanId.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("Loan ID Equality")
    class LoanIdEqualityTests {

        @Test
        @DisplayName("Should be equal when IDs have same value")
        void shouldBeEqualWhenIdsHaveSameValue() {
            // Given
            String value = "LOAN-98765";
            LoanId loanId1 = LoanId.of(value);
            LoanId loanId2 = LoanId.of(value);

            // When & Then
            assertThat(loanId1).isEqualTo(loanId2);
            assertThat(loanId1.hashCode()).isEqualTo(loanId2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs have different values")
        void shouldNotBeEqualWhenIdsHaveDifferentValues() {
            // Given
            LoanId loanId1 = LoanId.of("LOAN-111");
            LoanId loanId2 = LoanId.of("LOAN-222");

            // When & Then
            assertThat(loanId1).isNotEqualTo(loanId2);
        }

        @Test
        @DisplayName("Should be equal regardless of creation method")
        void shouldBeEqualRegardlessOfCreationMethod() {
            // Given
            String value = "LOAN-CREATION-TEST";
            LoanId loanIdFromConstructor = new LoanId(value);
            LoanId loanIdFromOf = LoanId.of(value);

            // When & Then
            assertThat(loanIdFromConstructor).isEqualTo(loanIdFromOf);
            assertThat(loanIdFromConstructor.hashCode()).isEqualTo(loanIdFromOf.hashCode());
        }
    }

    @Nested
    @DisplayName("Loan ID Business Rules")
    class LoanIdBusinessRulesTests {

        @Test
        @DisplayName("Should support standard loan ID formats")
        void shouldSupportStandardLoanIdFormats() {
            // Given
            String[] validFormats = {
                    "LOAN-123456",
                    "L-001",
                    "PERSONAL-LOAN-789",
                    "AUTO-LOAN-ABC123",
                    "MORTGAGE-2023-001"
            };

            // When & Then
            for (String format : validFormats) {
                assertThatCode(() -> LoanId.of(format))
                        .doesNotThrowAnyException();
                
                LoanId loanId = LoanId.of(format);
                assertThat(loanId.getValue()).isEqualTo(format);
            }
        }

        @Test
        @DisplayName("Should handle numeric only IDs")
        void shouldHandleNumericOnlyIds() {
            // Given
            String numericId = "123456789";

            // When
            LoanId loanId = LoanId.of(numericId);

            // Then
            assertThat(loanId.getValue()).isEqualTo(numericId);
        }

        @Test
        @DisplayName("Should handle alphanumeric IDs with special characters")
        void shouldHandleAlphanumericIdsWithSpecialCharacters() {
            // Given
            String specialId = "LOAN-2023_HOME#001";

            // When
            LoanId loanId = LoanId.of(specialId);

            // Then
            assertThat(loanId.getValue()).isEqualTo(specialId);
        }

        @Test
        @DisplayName("Should maintain immutability")
        void shouldMaintainImmutability() {
            // Given
            String originalValue = "LOAN-IMMUTABLE-TEST";
            LoanId loanId = LoanId.of(originalValue);

            // When
            String retrievedValue1 = loanId.getValue();
            String retrievedValue2 = loanId.getValue();

            // Then
            assertThat(retrievedValue1).isEqualTo(originalValue);
            assertThat(retrievedValue2).isEqualTo(originalValue);
            assertThat(retrievedValue1).isSameAs(retrievedValue2);
        }
    }

    @Nested
    @DisplayName("Loan ID Collections")
    class LoanIdCollectionsTests {

        @Test
        @DisplayName("Should work correctly in HashSet")
        void shouldWorkCorrectlyInHashSet() {
            // Given
            java.util.Set<LoanId> loanIdSet = new java.util.HashSet<>();
            LoanId loan1 = LoanId.of("LOAN-001");
            LoanId loan2 = LoanId.of("LOAN-002");
            LoanId loan1Duplicate = LoanId.of("LOAN-001");

            // When
            loanIdSet.add(loan1);
            loanIdSet.add(loan2);
            loanIdSet.add(loan1Duplicate);

            // Then
            assertThat(loanIdSet).hasSize(2);
            assertThat(loanIdSet).contains(loan1);
            assertThat(loanIdSet).contains(loan2);
            assertThat(loanIdSet).contains(loan1Duplicate);
        }

        @Test
        @DisplayName("Should work correctly as HashMap key")
        void shouldWorkCorrectlyAsHashMapKey() {
            // Given
            java.util.Map<LoanId, String> loanMap = new java.util.HashMap<>();
            LoanId loan1 = LoanId.of("LOAN-001");
            LoanId loan2 = LoanId.of("LOAN-002");
            LoanId loan1Duplicate = LoanId.of("LOAN-001");

            // When
            loanMap.put(loan1, "Personal Loan");
            loanMap.put(loan2, "Auto Loan");
            loanMap.put(loan1Duplicate, "Updated Personal Loan");

            // Then
            assertThat(loanMap).hasSize(2);
            assertThat(loanMap.get(loan1)).isEqualTo("Updated Personal Loan");
            assertThat(loanMap.get(loan1Duplicate)).isEqualTo("Updated Personal Loan");
            assertThat(loanMap.get(loan2)).isEqualTo("Auto Loan");
        }
    }

    @Nested
    @DisplayName("Loan ID String Representation")
    class LoanIdStringRepresentationTests {

        @Test
        @DisplayName("Should return value as string representation")
        void shouldReturnValueAsStringRepresentation() {
            // Given
            String value = "LOAN-STRING-REPR-TEST";
            LoanId loanId = LoanId.of(value);

            // When
            String stringRepresentation = loanId.toString();

            // Then
            assertThat(stringRepresentation).contains(value);
            assertThat(stringRepresentation).contains("LoanId");
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            // Given
            String emptyValue = "";
            LoanId loanId = LoanId.of(emptyValue);

            // When
            String stringRepresentation = loanId.toString();

            // Then
            assertThat(stringRepresentation).contains("LoanId");
            assertThat(stringRepresentation).contains("value=");
        }
    }

    @Nested
    @DisplayName("Loan ID Performance")
    class LoanIdPerformanceTests {

        @Test
        @DisplayName("Should create many instances efficiently")
        void shouldCreateManyInstancesEfficiently() {
            // Given
            int instanceCount = 10000;
            
            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < instanceCount; i++) {
                LoanId.of("LOAN-" + i);
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(200); // Should complete within 200ms
        }

        @Test
        @DisplayName("Should handle equality checks efficiently")
        void shouldHandleEqualityChecksEfficiently() {
            // Given
            LoanId id1 = LoanId.of("LOAN-EQUALITY-TEST");
            LoanId id2 = LoanId.of("LOAN-EQUALITY-TEST");
            LoanId id3 = LoanId.of("LOAN-DIFFERENT");

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                id1.equals(id2);
                id1.equals(id3);
                id1.hashCode();
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(100); // Should complete within 100ms
        }
    }
}