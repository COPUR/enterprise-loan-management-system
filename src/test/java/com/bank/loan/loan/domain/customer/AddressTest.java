package com.bank.loanmanagement.loan.domain.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for Address entity
 * Testing address creation, validation, and business rules
 */
@DisplayName("🏠 Address Entity Tests")
class AddressTest {

    @Nested
    @DisplayName("Address Creation")
    class AddressCreationTests {

        @Test
        @DisplayName("Should create address with all required fields using constructor")
        void shouldCreateAddressWithAllRequiredFieldsUsingConstructor() {
            // Given
            String street = "123 Main Street";
            String city = "New York";
            String state = "NY";
            String zipCode = "10001";
            String country = "USA";
            AddressType type = AddressType.HOME;
            Boolean isPrimary = true;

            // When
            Address address = new Address(null, street, city, state, zipCode, country, type, isPrimary, null);

            // Then
            assertThat(address.getStreet()).isEqualTo(street);
            assertThat(address.getCity()).isEqualTo(city);
            assertThat(address.getState()).isEqualTo(state);
            assertThat(address.getZipCode()).isEqualTo(zipCode);
            assertThat(address.getCountry()).isEqualTo(country);
            assertThat(address.getType()).isEqualTo(type);
            assertThat(address.getIsPrimary()).isEqualTo(isPrimary);
        }

        @Test
        @DisplayName("Should create address using builder pattern")
        void shouldCreateAddressUsingBuilderPattern() {
            // When
            Address address = Address.builder()
                    .street("456 Oak Avenue")
                    .city("Los Angeles")
                    .state("CA")
                    .zipCode("90210")
                    .country("USA")
                    .type(AddressType.BUSINESS)
                    .isPrimary(false)
                    .build();

            // Then
            assertThat(address.getStreet()).isEqualTo("456 Oak Avenue");
            assertThat(address.getCity()).isEqualTo("Los Angeles");
            assertThat(address.getState()).isEqualTo("CA");
            assertThat(address.getZipCode()).isEqualTo("90210");
            assertThat(address.getCountry()).isEqualTo("USA");
            assertThat(address.getType()).isEqualTo(AddressType.BUSINESS);
            assertThat(address.getIsPrimary()).isFalse();
        }

        @Test
        @DisplayName("Should create address using no-args constructor")
        void shouldCreateAddressUsingNoArgsConstructor() {
            // When
            Address address = new Address();

            // Then
            assertThat(address.getStreet()).isNull();
            assertThat(address.getCity()).isNull();
            assertThat(address.getState()).isNull();
            assertThat(address.getZipCode()).isNull();
            assertThat(address.getCountry()).isNull();
            assertThat(address.getType()).isNull();
            assertThat(address.getIsPrimary()).isNull();
        }
    }

    @Nested
    @DisplayName("Address Type Validation")
    class AddressTypeValidationTests {

        @Test
        @DisplayName("Should support all address types")
        void shouldSupportAllAddressTypes() {
            // Given & When & Then
            for (AddressType type : AddressType.values()) {
                Address address = Address.builder()
                        .street("123 Test Street")
                        .city("Test City")
                        .state("TS")
                        .zipCode("12345")
                        .country("USA")
                        .type(type)
                        .isPrimary(true)
                        .build();

                assertThat(address.getType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("Should create HOME address correctly")
        void shouldCreateHomeAddressCorrectly() {
            // When
            Address homeAddress = createTestAddress(AddressType.HOME);

            // Then
            assertThat(homeAddress.getType()).isEqualTo(AddressType.HOME);
        }

        @Test
        @DisplayName("Should create BUSINESS address correctly")
        void shouldCreateBusinessAddressCorrectly() {
            // When
            Address businessAddress = createTestAddress(AddressType.BUSINESS);

            // Then
            assertThat(businessAddress.getType()).isEqualTo(AddressType.BUSINESS);
        }

        @Test
        @DisplayName("Should create MAILING address correctly")
        void shouldCreateMailingAddressCorrectly() {
            // When
            Address mailingAddress = createTestAddress(AddressType.MAILING);

            // Then
            assertThat(mailingAddress.getType()).isEqualTo(AddressType.MAILING);
        }
    }

    @Nested
    @DisplayName("Full Address Formatting")
    class FullAddressFormattingTests {

        @Test
        @DisplayName("Should format complete US address correctly")
        void shouldFormatCompleteUSAddressCorrectly() {
            // Given
            Address address = Address.builder()
                    .street("1600 Pennsylvania Avenue NW")
                    .city("Washington")
                    .state("DC")
                    .zipCode("20500")
                    .country("USA")
                    .type(AddressType.HOME)
                    .isPrimary(true)
                    .build();

            // When
            String fullAddress = address.getFullAddress();

            // Then
            assertThat(fullAddress).isEqualTo("1600 Pennsylvania Avenue NW, Washington, DC 20500, USA");
        }

        @Test
        @DisplayName("Should format international address correctly")
        void shouldFormatInternationalAddressCorrectly() {
            // Given
            Address address = Address.builder()
                    .street("10 Downing Street")
                    .city("London")
                    .state("England")
                    .zipCode("SW1A 2AA")
                    .country("United Kingdom")
                    .type(AddressType.HOME)
                    .isPrimary(true)
                    .build();

            // When
            String fullAddress = address.getFullAddress();

            // Then
            assertThat(fullAddress).isEqualTo("10 Downing Street, London, England SW1A 2AA, United Kingdom");
        }

        @Test
        @DisplayName("Should handle addresses with special characters")
        void shouldHandleAddressesWithSpecialCharacters() {
            // Given
            Address address = Address.builder()
                    .street("123 O'Malley St. Apt #4B")
                    .city("Saint-Jean-sur-Richelieu")
                    .state("Québec")
                    .zipCode("J3A 1B2")
                    .country("Canada")
                    .type(AddressType.HOME)
                    .isPrimary(true)
                    .build();

            // When
            String fullAddress = address.getFullAddress();

            // Then
            assertThat(fullAddress).isEqualTo("123 O'Malley St. Apt #4B, Saint-Jean-sur-Richelieu, Québec J3A 1B2, Canada");
        }

        @Test
        @DisplayName("Should handle null address components gracefully")
        void shouldHandleNullAddressComponentsGracefully() {
            // Given
            Address address = Address.builder()
                    .street(null)
                    .city("Test City")
                    .state(null)
                    .zipCode("12345")
                    .country("USA")
                    .build();

            // When
            String fullAddress = address.getFullAddress();

            // Then
            assertThat(fullAddress).isEqualTo("null, Test City, null 12345, USA");
        }
    }

    @Nested
    @DisplayName("Primary Address Management")
    class PrimaryAddressManagementTests {

        @Test
        @DisplayName("Should mark address as primary")
        void shouldMarkAddressAsPrimary() {
            // Given
            Address address = createTestAddress(AddressType.HOME);
            address.setIsPrimary(true);

            // When & Then
            assertThat(address.getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("Should mark address as non-primary")
        void shouldMarkAddressAsNonPrimary() {
            // Given
            Address address = createTestAddress(AddressType.BUSINESS);
            address.setIsPrimary(false);

            // When & Then
            assertThat(address.getIsPrimary()).isFalse();
        }

        @Test
        @DisplayName("Should handle null primary flag")
        void shouldHandleNullPrimaryFlag() {
            // Given
            Address address = createTestAddress(AddressType.MAILING);
            address.setIsPrimary(null);

            // When & Then
            assertThat(address.getIsPrimary()).isNull();
        }
    }

    @Nested
    @DisplayName("Address Equality")
    class AddressEqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            Address address1 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.HOME, true);
            Address address2 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.HOME, true);

            // When & Then
            assertThat(address1).isEqualTo(address2);
            assertThat(address1.hashCode()).isEqualTo(address2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when streets differ")
        void shouldNotBeEqualWhenStreetsDiffer() {
            // Given
            Address address1 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.HOME, true);
            Address address2 = createTestAddress("456 Oak St", "Anytown", "NY", "12345", "USA", AddressType.HOME, true);

            // When & Then
            assertThat(address1).isNotEqualTo(address2);
        }

        @Test
        @DisplayName("Should not be equal when types differ")
        void shouldNotBeEqualWhenTypesDiffer() {
            // Given
            Address address1 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.HOME, true);
            Address address2 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.BUSINESS, true);

            // When & Then
            assertThat(address1).isNotEqualTo(address2);
        }

        @Test
        @DisplayName("Should not be equal when primary flags differ")
        void shouldNotBeEqualWhenPrimaryFlagsDiffer() {
            // Given
            Address address1 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.HOME, true);
            Address address2 = createTestAddress("123 Main St", "Anytown", "NY", "12345", "USA", AddressType.HOME, false);

            // When & Then
            assertThat(address1).isNotEqualTo(address2);
        }
    }

    @Nested
    @DisplayName("Address Business Rules")
    class AddressBusinessRulesTests {

        @Test
        @DisplayName("Should support various zip code formats")
        void shouldSupportVariousZipCodeFormats() {
            // Given
            String[] zipCodes = {
                    "12345",           // Standard US 5-digit
                    "12345-6789",      // US ZIP+4
                    "K1A 0A6",         // Canadian postal code
                    "SW1A 1AA",        // UK postal code
                    "75001",           // French postal code
                    "10115"            // German postal code
            };

            // When & Then
            for (String zipCode : zipCodes) {
                Address address = Address.builder()
                        .street("Test Street")
                        .city("Test City")
                        .state("Test State")
                        .zipCode(zipCode)
                        .country("Test Country")
                        .type(AddressType.HOME)
                        .isPrimary(true)
                        .build();

                assertThat(address.getZipCode()).isEqualTo(zipCode);
                assertThat(address.getFullAddress()).contains(zipCode);
            }
        }

        @Test
        @DisplayName("Should handle long street addresses")
        void shouldHandleLongStreetAddresses() {
            // Given
            String longStreet = "1234 Very Long Street Name That Goes On For A While Apartment Complex Building A Unit 123B Suite 456";
            Address address = Address.builder()
                    .street(longStreet)
                    .city("Long City Name")
                    .state("Long State Name")
                    .zipCode("12345")
                    .country("USA")
                    .type(AddressType.HOME)
                    .isPrimary(true)
                    .build();

            // When & Then
            assertThat(address.getStreet()).isEqualTo(longStreet);
            assertThat(address.getFullAddress()).startsWith(longStreet);
        }

        @Test
        @DisplayName("Should maintain consistency across different countries")
        void shouldMaintainConsistencyAcrossDifferentCountries() {
            // Given
            String[] countries = {"USA", "Canada", "United Kingdom", "France", "Germany", "Japan", "Australia"};

            // When & Then
            for (String country : countries) {
                Address address = Address.builder()
                        .street("123 Test Street")
                        .city("Test City")
                        .state("Test State")
                        .zipCode("12345")
                        .country(country)
                        .type(AddressType.HOME)
                        .isPrimary(true)
                        .build();

                assertThat(address.getCountry()).isEqualTo(country);
                assertThat(address.getFullAddress()).endsWith(country);
            }
        }
    }

    @Nested
    @DisplayName("Address Customer Relationship")
    class AddressCustomerRelationshipTests {

        @Test
        @DisplayName("Should allow setting customer relationship")
        void shouldAllowSettingCustomerRelationship() {
            // Given
            Address address = createTestAddress(AddressType.HOME);
            Customer customer = new Customer(); // Assuming Customer exists

            // When
            address.setCustomer(customer);

            // Then
            assertThat(address.getCustomer()).isEqualTo(customer);
        }

        @Test
        @DisplayName("Should handle null customer relationship")
        void shouldHandleNullCustomerRelationship() {
            // Given
            Address address = createTestAddress(AddressType.HOME);

            // When
            address.setCustomer(null);

            // Then
            assertThat(address.getCustomer()).isNull();
        }
    }

    // Helper methods for test setup
    private Address createTestAddress(AddressType type) {
        return Address.builder()
                .street("123 Test Street")
                .city("Test City")
                .state("TS")
                .zipCode("12345")
                .country("USA")
                .type(type)
                .isPrimary(true)
                .build();
    }

    private Address createTestAddress(String street, String city, String state, String zipCode, 
                                    String country, AddressType type, Boolean isPrimary) {
        return Address.builder()
                .street(street)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .country(country)
                .type(type)
                .isPrimary(isPrimary)
                .build();
    }
}