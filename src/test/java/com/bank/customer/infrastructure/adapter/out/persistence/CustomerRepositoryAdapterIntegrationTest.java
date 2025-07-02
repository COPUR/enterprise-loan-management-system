package com.bank.customer.infrastructure.adapter.out.persistence;

import com.bank.loan.loan.domain.customer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Tests for Customer Repository Adapter
 * 
 * Tests the infrastructure adapter's integration with the database.
 * Validates that domain operations translate correctly to persistence operations.
 * 
 * Architecture Validation:
 * ✅ Hexagonal Architecture: Infrastructure adapter integration testing
 * ✅ DDD: Domain-infrastructure boundary testing
 * ✅ Repository Pattern: CRUD and query operations validation
 * ✅ Data Integrity: Round-trip conversion and persistence validation
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "logging.level.org.hibernate.SQL=DEBUG"
})
@DisplayName("Customer Repository Adapter Integration Tests")
class CustomerRepositoryAdapterIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerJpaRepository jpaRepository;

    private CustomerRepositoryAdapter repository;
    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
        repository = new CustomerRepositoryAdapter(jpaRepository, mapper);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save and retrieve customer successfully")
        void shouldSaveAndRetrieveCustomer() {
            // Given
            Customer customer = createTestCustomer("CUST001", "John Doe", "john.doe@email.com");

            // When
            Customer savedCustomer = repository.save(customer);
            Optional<Customer> retrievedCustomer = repository.findById(CustomerId.of("CUST001"));

            // Then
            assertThat(savedCustomer).isNotNull();
            assertThat(savedCustomer.getId().getValue()).isEqualTo("CUST001");
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedCustomer.get().getId().getValue()).isEqualTo("CUST001");
            assertThat(retrievedCustomer.get().getName().getFullName()).isEqualTo("John Doe");
            assertThat(retrievedCustomer.get().getEmailAddress().getValue()).isEqualTo("john.doe@email.com");
        }

        @Test
        @DisplayName("Should update existing customer")
        void shouldUpdateExistingCustomer() {
            // Given
            Customer customer = createTestCustomer("CUST002", "Jane Smith", "jane.smith@email.com");
            repository.save(customer);

            // Modify the customer
            Customer updatedCustomer = Customer.builder()
                .id(customer.getId())
                .name(PersonalName.of("Jane", "Johnson")) // Changed last name
                .emailAddress(customer.getEmailAddress())
                .phoneNumber(customer.getPhoneNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .creditLimit(CreditLimit.of(new BigDecimal("15000"))) // Increased credit limit
                .status(customer.getStatus())
                .build();

            // When
            Customer saved = repository.save(updatedCustomer);
            Optional<Customer> retrieved = repository.findById(CustomerId.of("CUST002"));

            // Then
            assertThat(saved.getName().getFullName()).isEqualTo("Jane Johnson");
            assertThat(saved.getCreditLimit().getValue()).isEqualTo(new BigDecimal("15000"));
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getName().getFullName()).isEqualTo("Jane Johnson");
            assertThat(retrieved.get().getCreditLimit().getValue()).isEqualTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("Should delete customer by ID")
        void shouldDeleteCustomerById() {
            // Given
            Customer customer = createTestCustomer("CUST003", "Bob Wilson", "bob.wilson@email.com");
            repository.save(customer);
            assertThat(repository.existsById(CustomerId.of("CUST003"))).isTrue();

            // When
            repository.delete(customer);

            // Then
            assertThat(repository.existsById(CustomerId.of("CUST003"))).isFalse();
            assertThat(repository.findById(CustomerId.of("CUST003"))).isEmpty();
        }

        @Test
        @DisplayName("Should return empty optional for non-existent ID")
        void shouldReturnEmptyOptionalForNonExistentId() {
            // When
            Optional<Customer> result = repository.findById(CustomerId.of("NONEXISTENT"));

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @BeforeEach
        void setupTestData() {
            // Create multiple test customers with different characteristics
            Customer customer1 = createTestCustomer("CUST100", "Alice Brown", "alice.brown@email.com");
            repository.save(customer1);

            Customer customer2 = createTestCustomer("CUST101", "Charlie Davis", "charlie.davis@email.com");
            repository.save(customer2);

            Customer customer3 = createTestCustomer("CUST102", "Diana Evans", "diana.evans@email.com");
            Customer suspendedCustomer = Customer.builder()
                .id(customer3.getId())
                .name(customer3.getName())
                .emailAddress(customer3.getEmailAddress())
                .phoneNumber(customer3.getPhoneNumber())
                .dateOfBirth(customer3.getDateOfBirth())
                .creditLimit(customer3.getCreditLimit())
                .status(CustomerStatus.SUSPENDED)
                .build();
            repository.save(suspendedCustomer);

            Customer customer4 = createTestCustomer("CUST103", "Frank Wilson", "frank.wilson@email.com");
            Customer highCreditCustomer = Customer.builder()
                .id(customer4.getId())
                .name(customer4.getName())
                .emailAddress(customer4.getEmailAddress())
                .phoneNumber(customer4.getPhoneNumber())
                .dateOfBirth(customer4.getDateOfBirth())
                .creditLimit(CreditLimit.of(new BigDecimal("50000")))
                .status(customer4.getStatus())
                .build();
            repository.save(highCreditCustomer);

            // Force flush to ensure data is persisted
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find customers by email address")
        void shouldFindCustomersByEmailAddress() {
            // When
            Optional<Customer> customer = repository.findByEmailAddress(EmailAddress.of("alice.brown@email.com"));

            // Then
            assertThat(customer).isPresent();
            assertThat(customer.get().getId().getValue()).isEqualTo("CUST100");
            assertThat(customer.get().getName().getFullName()).isEqualTo("Alice Brown");
        }

        @Test
        @DisplayName("Should find customers by status")
        void shouldFindCustomersByStatus() {
            // When
            List<Customer> activeCustomers = repository.findByStatus(CustomerStatus.ACTIVE);
            List<Customer> suspendedCustomers = repository.findByStatus(CustomerStatus.SUSPENDED);

            // Then
            assertThat(activeCustomers).hasSize(3);
            assertThat(suspendedCustomers).hasSize(1);
            assertThat(suspendedCustomers.get(0).getId().getValue()).isEqualTo("CUST102");
        }

        @Test
        @DisplayName("Should find customers with credit limit greater than amount")
        void shouldFindCustomersWithCreditLimitGreaterThanAmount() {
            // When
            List<Customer> highCreditCustomers = repository.findByCreditLimitGreaterThan(new BigDecimal("20000"));

            // Then
            assertThat(highCreditCustomers).hasSize(1);
            assertThat(highCreditCustomers.get(0).getId().getValue()).isEqualTo("CUST103");
            assertThat(highCreditCustomers.get(0).getCreditLimit().getValue()).isEqualTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("Should find customers by date of birth range")
        void shouldFindCustomersByDateOfBirthRange() {
            // Given
            LocalDate startDate = LocalDate.now().minusYears(40);
            LocalDate endDate = LocalDate.now().minusYears(20);

            // When
            List<Customer> customersInAgeRange = repository.findByDateOfBirthBetween(startDate, endDate);

            // Then
            assertThat(customersInAgeRange).hasSize(4); // All test customers are in this age range
        }
    }

    @Nested
    @DisplayName("Pagination Operations")
    class PaginationOperations {

        @BeforeEach
        void setupPaginationTestData() {
            // Create multiple customers for pagination testing
            for (int i = 1; i <= 15; i++) {
                Customer customer = createTestCustomer(
                    String.format("PAGE%03d", i),
                    String.format("Customer %d", i),
                    String.format("customer%d@email.com", i)
                );
                repository.save(customer);
            }

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should support pagination for findAll")
        void shouldSupportPaginationForFindAll() {
            // When
            Page<Customer> firstPage = repository.findAll(PageRequest.of(0, 5));
            Page<Customer> secondPage = repository.findAll(PageRequest.of(1, 5));

            // Then
            assertThat(firstPage.getContent()).hasSize(5);
            assertThat(firstPage.getTotalElements()).isEqualTo(15);
            assertThat(firstPage.getTotalPages()).isEqualTo(3);
            assertThat(firstPage.isFirst()).isTrue();
            assertThat(firstPage.isLast()).isFalse();

            assertThat(secondPage.getContent()).hasSize(5);
            assertThat(secondPage.getTotalElements()).isEqualTo(15);
            assertThat(secondPage.isFirst()).isFalse();
            assertThat(secondPage.isLast()).isFalse();
        }

        @Test
        @DisplayName("Should support pagination for customers by status")
        void shouldSupportPaginationForCustomersByStatus() {
            // When
            Page<Customer> activeCustomersPage = repository.findByStatus(
                CustomerStatus.ACTIVE, PageRequest.of(0, 10)
            );

            // Then
            assertThat(activeCustomersPage.getContent()).hasSize(10);
            assertThat(activeCustomersPage.getTotalElements()).isEqualTo(15);
            assertThat(activeCustomersPage.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Business Logic Operations")
    class BusinessLogicOperations {

        @BeforeEach
        void setupBusinessTestData() {
            // Create customers with specific business characteristics
            Customer eligibleCustomer = Customer.builder()
                .id(CustomerId.of("ELIGIBLE001"))
                .name(PersonalName.of("Eligible", "Customer"))
                .emailAddress(EmailAddress.of("eligible@email.com"))
                .phoneNumber(PhoneNumber.of("+1234567890"))
                .dateOfBirth(LocalDate.now().minusYears(35))
                .creditLimit(CreditLimit.of(new BigDecimal("25000")))
                .status(CustomerStatus.ACTIVE)
                .build();
            repository.save(eligibleCustomer);

            Customer ineligibleCustomer = Customer.builder()
                .id(CustomerId.of("INELIGIBLE001"))
                .name(PersonalName.of("Ineligible", "Customer"))
                .emailAddress(EmailAddress.of("ineligible@email.com"))
                .phoneNumber(PhoneNumber.of("+1234567891"))
                .dateOfBirth(LocalDate.now().minusYears(17)) // Too young
                .creditLimit(CreditLimit.of(new BigDecimal("5000")))
                .status(CustomerStatus.ACTIVE)
                .build();
            repository.save(ineligibleCustomer);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find loan eligible customers")
        void shouldFindLoanEligibleCustomers() {
            // When
            List<Customer> eligibleCustomers = repository.findLoanEligibleCustomers();

            // Then
            assertThat(eligibleCustomers).hasSize(1);
            assertThat(eligibleCustomers.get(0).getId().getValue()).isEqualTo("ELIGIBLE001");
        }

        @Test
        @DisplayName("Should count customers by status")
        void shouldCountCustomersByStatus() {
            // When
            long activeCount = repository.countByStatus(CustomerStatus.ACTIVE);
            long suspendedCount = repository.countByStatus(CustomerStatus.SUSPENDED);

            // Then
            assertThat(activeCount).isEqualTo(2L);
            assertThat(suspendedCount).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should check customer existence")
        void shouldCheckCustomerExistence() {
            // When
            boolean existsEligible = repository.existsById(CustomerId.of("ELIGIBLE001"));
            boolean existsNonExistent = repository.existsById(CustomerId.of("NONEXISTENT"));

            // Then
            assertThat(existsEligible).isTrue();
            assertThat(existsNonExistent).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Integrity and Mapping")
    class DataIntegrityAndMapping {

        @Test
        @DisplayName("Should preserve all domain data through persistence round-trip")
        void shouldPreserveAllDomainDataThroughPersistenceRoundTrip() {
            // Given
            Customer originalCustomer = Customer.builder()
                .id(CustomerId.of("INTEGRITY001"))
                .name(PersonalName.of("Test", "Customer"))
                .emailAddress(EmailAddress.of("test.customer@email.com"))
                .phoneNumber(PhoneNumber.of("+1987654321"))
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .creditLimit(CreditLimit.of(new BigDecimal("35000.50")))
                .status(CustomerStatus.ACTIVE)
                .build();

            // When
            Customer savedCustomer = repository.save(originalCustomer);
            Optional<Customer> retrievedCustomer = repository.findById(CustomerId.of("INTEGRITY001"));

            // Then
            assertThat(retrievedCustomer).isPresent();
            Customer retrieved = retrievedCustomer.get();

            // Verify all fields are preserved
            assertThat(retrieved.getId().getValue()).isEqualTo(originalCustomer.getId().getValue());
            assertThat(retrieved.getName().getFullName()).isEqualTo(originalCustomer.getName().getFullName());
            assertThat(retrieved.getEmailAddress().getValue()).isEqualTo(originalCustomer.getEmailAddress().getValue());
            assertThat(retrieved.getPhoneNumber().getValue()).isEqualTo(originalCustomer.getPhoneNumber().getValue());
            assertThat(retrieved.getDateOfBirth()).isEqualTo(originalCustomer.getDateOfBirth());
            assertThat(retrieved.getCreditLimit().getValue()).isEqualTo(originalCustomer.getCreditLimit().getValue());
            assertThat(retrieved.getStatus()).isEqualTo(originalCustomer.getStatus());
        }

        @Test
        @DisplayName("Should handle domain business rules correctly after persistence")
        void shouldHandleDomainBusinessRulesCorrectlyAfterPersistence() {
            // Given
            Customer customer = createTestCustomer("BUSINESS001", "Business Test", "business@email.com");
            repository.save(customer);

            // When
            Optional<Customer> retrievedCustomer = repository.findById(CustomerId.of("BUSINESS001"));

            // Then
            assertThat(retrievedCustomer).isPresent();
            Customer retrieved = retrievedCustomer.get();

            // Verify domain business rules work correctly after persistence
            assertThat(retrieved.isAdult()).isTrue();
            assertThat(retrieved.isActive()).isTrue();
            assertThat(retrieved.hasAvailableCredit(new BigDecimal("5000"))).isTrue();
            assertThat(retrieved.hasAvailableCredit(new BigDecimal("15000"))).isFalse();
        }

        @Test
        @DisplayName("Should handle value object equality correctly")
        void shouldHandleValueObjectEqualityCorrectly() {
            // Given
            Customer customer1 = createTestCustomer("EQUAL001", "Equal Test", "equal@email.com");
            Customer customer2 = createTestCustomer("EQUAL001", "Equal Test", "equal@email.com");

            // When
            repository.save(customer1);
            Optional<Customer> retrievedCustomer = repository.findById(CustomerId.of("EQUAL001"));

            // Then
            assertThat(retrievedCustomer).isPresent();
            Customer retrieved = retrievedCustomer.get();

            // Value objects should be equal
            assertThat(retrieved.getId()).isEqualTo(customer2.getId());
            assertThat(retrieved.getEmailAddress()).isEqualTo(customer2.getEmailAddress());
            assertThat(retrieved.getPhoneNumber()).isEqualTo(customer2.getPhoneNumber());
        }
    }

    // Helper method to create test customers
    private Customer createTestCustomer(String customerId, String fullName, String email) {
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "Unknown";

        return Customer.builder()
            .id(CustomerId.of(customerId))
            .name(PersonalName.of(firstName, lastName))
            .emailAddress(EmailAddress.of(email))
            .phoneNumber(PhoneNumber.of("+1234567890"))
            .dateOfBirth(LocalDate.now().minusYears(30))
            .creditLimit(CreditLimit.of(new BigDecimal("10000")))
            .status(CustomerStatus.ACTIVE)
            .build();
    }
}