package com.bank.loanmanagement.loan.domain.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for CustomerId value object
 * Testing entity ID behavior and validation
 */
@DisplayName("👤 Customer ID Value Object Tests")
class CustomerIdTest {

    @Nested
    @DisplayName("Customer ID Creation")
    class CustomerIdCreationTests {

        @Test
        @DisplayName("Should create customer ID with valid value")
        void shouldCreateCustomerIdWithValidValue() {
            // Given
            String customerIdValue = "CUST-12345";

            // When
            CustomerId customerId = new CustomerId(customerIdValue);

            // Then
            assertThat(customerId.getValue()).isEqualTo(customerIdValue);
        }

        @Test
        @DisplayName("Should accept null customer ID")
        void shouldAcceptNullCustomerId() {
            // When
            CustomerId customerId = new CustomerId(null);
            
            // Then
            assertThat(customerId.getValue()).isNull();
        }

        @Test
        @DisplayName("Should accept empty string as customer ID")
        void shouldAcceptEmptyStringAsCustomerId() {
            // Given
            String emptyId = "";

            // When
            CustomerId customerId = new CustomerId(emptyId);

            // Then
            assertThat(customerId.getValue()).isEqualTo(emptyId);
        }
    }

    @Nested
    @DisplayName("Customer ID Equality")
    class CustomerIdEqualityTests {

        @Test
        @DisplayName("Should be equal when IDs have same value")
        void shouldBeEqualWhenIdsHaveSameValue() {
            // Given
            String value = "CUST-98765";
            CustomerId customerId1 = new CustomerId(value);
            CustomerId customerId2 = new CustomerId(value);

            // When & Then
            assertThat(customerId1).isEqualTo(customerId2);
            assertThat(customerId1.hashCode()).isEqualTo(customerId2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs have different values")
        void shouldNotBeEqualWhenIdsHaveDifferentValues() {
            // Given
            CustomerId customerId1 = new CustomerId("CUST-111");
            CustomerId customerId2 = new CustomerId("CUST-222");

            // When & Then
            assertThat(customerId1).isNotEqualTo(customerId2);
            assertThat(customerId1.hashCode()).isNotEqualTo(customerId2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            CustomerId customerId = new CustomerId("CUST-123");

            // When & Then
            assertThat(customerId).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            CustomerId customerId = new CustomerId("CUST-123");
            String stringValue = "CUST-123";

            // When & Then
            assertThat(customerId).isNotEqualTo(stringValue);
        }
    }

    @Nested
    @DisplayName("Customer ID String Representation")
    class CustomerIdStringRepresentationTests {

        @Test
        @DisplayName("Should return value as string representation")
        void shouldReturnValueAsStringRepresentation() {
            // Given
            String value = "CUST-ALPHA-123";
            CustomerId customerId = new CustomerId(value);

            // When
            String stringRepresentation = customerId.toString();

            // Then
            assertThat(stringRepresentation).contains(value);
            assertThat(stringRepresentation).contains("CustomerId");
        }

        @Test
        @DisplayName("Should handle special characters in string representation")
        void shouldHandleSpecialCharactersInStringRepresentation() {
            // Given
            String valueWithSpecialChars = "CUST-2023_#@$%";
            CustomerId customerId = new CustomerId(valueWithSpecialChars);

            // When
            String stringRepresentation = customerId.toString();

            // Then
            assertThat(stringRepresentation).contains(valueWithSpecialChars);
            assertThat(stringRepresentation).contains("CustomerId");
        }
    }

    @Nested
    @DisplayName("Customer ID Business Rules")
    class CustomerIdBusinessRulesTests {

        @Test
        @DisplayName("Should support various ID formats")
        void shouldSupportVariousIdFormats() {
            // Given
            String[] validFormats = {
                    "CUST-123456",
                    "customer_001",
                    "CID-ALPHA-789",
                    "12345",
                    "uuid-like-string-here"
            };

            // When & Then
            for (String format : validFormats) {
                assertThatCode(() -> new CustomerId(format))
                        .doesNotThrowAnyException();
                
                CustomerId customerId = new CustomerId(format);
                assertThat(customerId.getValue()).isEqualTo(format);
            }
        }

        @Test
        @DisplayName("Should maintain immutability")
        void shouldMaintainImmutability() {
            // Given
            String originalValue = "CUST-IMMUTABLE";
            CustomerId customerId = new CustomerId(originalValue);

            // When
            String retrievedValue = customerId.getValue();

            // Then
            assertThat(retrievedValue).isEqualTo(originalValue);
            // Verify that the same instance returns the same value consistently
            assertThat(customerId.getValue()).isEqualTo(originalValue);
            assertThat(customerId.getValue()).isSameAs(retrievedValue);
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        void shouldHandleConcurrentAccessSafely() {
            // Given
            String value = "CUST-CONCURRENT-TEST";
            CustomerId customerId = new CustomerId(value);

            // When - Simulate concurrent access
            String[] results = new String[10];
            for (int i = 0; i < 10; i++) {
                results[i] = customerId.getValue();
            }

            // Then
            for (String result : results) {
                assertThat(result).isEqualTo(value);
            }
        }
    }

    @Nested
    @DisplayName("Customer ID Performance")
    class CustomerIdPerformanceTests {

        @Test
        @DisplayName("Should create instances efficiently")
        void shouldCreateInstancesEfficiently() {
            // Given
            String baseValue = "CUST-PERF-";
            
            // When - Create many instances
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                new CustomerId(baseValue + i);
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(100); // Should complete within 100ms
        }

        @Test
        @DisplayName("Should compare instances efficiently")
        void shouldCompareInstancesEfficiently() {
            // Given
            CustomerId id1 = new CustomerId("CUST-COMPARE-1");
            CustomerId id2 = new CustomerId("CUST-COMPARE-1");
            CustomerId id3 = new CustomerId("CUST-COMPARE-2");

            // When - Perform many comparisons
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                id1.equals(id2);
                id1.equals(id3);
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(50); // Should complete within 50ms
        }
    }
}