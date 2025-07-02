package com.bank.loanmanagement.loan.infrastructure.anticorruption;

import com.bank.customer.domain.model.CustomerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Unit Tests for CustomerContextAdapter
 * 
 * Tests the anti-corruption layer that protects our domain from external Customer context.
 * Validates translation, error handling, and business rule application.
 * 
 * Architecture Compliance Testing:
 * ✅ Hexagonal Architecture: Tests anti-corruption layer pattern
 * ✅ Clean Code: Tests single responsibility for external context translation
 * ✅ DDD: Tests bounded context protection
 * ✅ Type Safety: Tests proper error handling and null safety
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerContextAdapter Tests")
class CustomerContextAdapterTest {

    @Mock
    private ExternalCustomerService externalCustomerService;

    private CustomerContextAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CustomerContextAdapter(externalCustomerService);
    }

    @Nested
    @DisplayName("Customer Profile Translation Tests")
    class CustomerProfileTranslationTests {

        @Test
        @DisplayName("Should translate complete external customer to domain profile")
        void shouldTranslateCompleteExternalCustomerToDomainProfile() {
            // Given - Complete external customer
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(123L);

            // Then
            assertThat(profile).isPresent();
            CustomerProfile customerProfile = profile.get();
            
            assertThat(customerProfile.getCustomerId()).isEqualTo(123L);
            assertThat(customerProfile.getFirstName()).isEqualTo("John");
            assertThat(customerProfile.getLastName()).isEqualTo("Doe");
            assertThat(customerProfile.getEmail()).isEqualTo("john.doe@email.com");
            assertThat(customerProfile.getPhone()).isEqualTo("+1234567890");
            assertThat(customerProfile.getMonthlyIncome()).isEqualTo(new BigDecimal("8000"));
            assertThat(customerProfile.getEmploymentStatus()).isEqualTo(CustomerProfile.EmploymentStatus.EMPLOYED);
            assertThat(customerProfile.getCreditScore()).isEqualTo(750);
            assertThat(customerProfile.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 6, 15));
            assertThat(customerProfile.getAddress()).contains("123 Main St, Anytown, ST 12345");
            assertThat(customerProfile.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should handle missing external customer")
        void shouldHandleMissingExternalCustomer() {
            // Given
            when(externalCustomerService.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(999L);

            // Then
            assertThat(profile).isEmpty();
        }

        @Test
        @DisplayName("Should handle external service exception")
        void shouldHandleExternalServiceException() {
            // Given
            when(externalCustomerService.findById(any())).thenThrow(new RuntimeException("External service error"));

            // When
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(123L);

            // Then
            assertThat(profile).isEmpty();
        }

        @Test
        @DisplayName("Should translate employment status correctly")
        void shouldTranslateEmploymentStatusCorrectly() {
            // Test different employment statuses
            testEmploymentStatusTranslation("EMPLOYED", CustomerProfile.EmploymentStatus.EMPLOYED);
            testEmploymentStatusTranslation("FULL_TIME", CustomerProfile.EmploymentStatus.EMPLOYED);
            testEmploymentStatusTranslation("SELF_EMPLOYED", CustomerProfile.EmploymentStatus.SELF_EMPLOYED);
            testEmploymentStatusTranslation("UNEMPLOYED", CustomerProfile.EmploymentStatus.UNEMPLOYED);
            testEmploymentStatusTranslation("RETIRED", CustomerProfile.EmploymentStatus.RETIRED);
            testEmploymentStatusTranslation("STUDENT", CustomerProfile.EmploymentStatus.STUDENT);
            testEmploymentStatusTranslation("UNKNOWN", CustomerProfile.EmploymentStatus.OTHER);
        }

        private void testEmploymentStatusTranslation(String externalStatus, CustomerProfile.EmploymentStatus expectedStatus) {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            externalCustomer.getEmploymentInfo().setStatus(externalStatus);
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(123L);

            // Then
            assertThat(profile).isPresent();
            assertThat(profile.get().getEmploymentStatus()).isEqualTo(expectedStatus);
        }

        @Test
        @DisplayName("Should format address correctly")
        void shouldFormatAddressCorrectly() {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            ExternalAddress address = externalCustomer.getContactInfo().getAddress();
            address.setStreet("456 Oak Ave");
            address.setCity("Springfield");
            address.setState("IL");
            address.setZipCode("62701");
            
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(123L);

            // Then
            assertThat(profile).isPresent();
            assertThat(profile.get().getAddress()).isEqualTo("456 Oak Ave, Springfield, IL 62701");
        }

        @Test
        @DisplayName("Should handle null address")
        void shouldHandleNullAddress() {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            externalCustomer.getContactInfo().setAddress(null);
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(123L);

            // Then
            assertThat(profile).isPresent();
            assertThat(profile.get().getAddress()).isNull();
        }
    }

    @Nested
    @DisplayName("Customer Validation Tests")
    class CustomerValidationTests {

        @Test
        @DisplayName("Should validate active customer with complete data")
        void shouldValidateActiveCustomerWithCompleteData() {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            boolean isValid = adapter.isCustomerActiveAndValid(123L);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject inactive customer")
        void shouldRejectInactiveCustomer() {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            externalCustomer.setStatus("INACTIVE");
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            boolean isValid = adapter.isCustomerActiveAndValid(123L);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject customer with missing credit info")
        void shouldRejectCustomerWithMissingCreditInfo() {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            externalCustomer.setCreditInfo(null);
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            boolean isValid = adapter.isCustomerActiveAndValid(123L);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject customer with zero credit score")
        void shouldRejectCustomerWithZeroCreditScore() {
            // Given
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            externalCustomer.getCreditInfo().setScore(0);
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));

            // When
            boolean isValid = adapter.isCustomerActiveAndValid(123L);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should handle validation exception")
        void shouldHandleValidationException() {
            // Given
            when(externalCustomerService.findById(any())).thenThrow(new RuntimeException("Validation error"));

            // When
            boolean isValid = adapter.isCustomerActiveAndValid(123L);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject non-existent customer")
        void shouldRejectNonExistentCustomer() {
            // Given
            when(externalCustomerService.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean isValid = adapter.isCustomerActiveAndValid(999L);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Credit Score Tests")
    class CreditScoreTests {

        @Test
        @DisplayName("Should retrieve customer credit score")
        void shouldRetrieveCustomerCreditScore() {
            // Given
            when(externalCustomerService.getCreditScore(123L)).thenReturn(Optional.of(780));

            // When
            Optional<Integer> creditScore = adapter.getCustomerCreditScore(123L);

            // Then
            assertThat(creditScore).isPresent();
            assertThat(creditScore.get()).isEqualTo(780);
        }

        @Test
        @DisplayName("Should handle missing credit score")
        void shouldHandleMissingCreditScore() {
            // Given
            when(externalCustomerService.getCreditScore(123L)).thenReturn(Optional.empty());

            // When
            Optional<Integer> creditScore = adapter.getCustomerCreditScore(123L);

            // Then
            assertThat(creditScore).isEmpty();
        }

        @Test
        @DisplayName("Should handle credit score service exception")
        void shouldHandleCreditScoreServiceException() {
            // Given
            when(externalCustomerService.getCreditScore(any())).thenThrow(new RuntimeException("Credit service error"));

            // When
            Optional<Integer> creditScore = adapter.getCustomerCreditScore(123L);

            // Then
            assertThat(creditScore).isEmpty();
        }
    }

    @Nested
    @DisplayName("Income Sufficiency Tests")
    class IncomeSufficiencyTests {

        @Test
        @DisplayName("Should confirm sufficient income for loan")
        void shouldConfirmSufficientIncomeForLoan() {
            // Given - Customer with $8000 monthly income
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));
            
            BigDecimal loanAmount = new BigDecimal("180000"); // Requires $5000/month (180000/36)

            // When
            boolean hasSufficientIncome = adapter.hassufficientIncome(123L, loanAmount);

            // Then
            assertThat(hasSufficientIncome).isTrue();
        }

        @Test
        @DisplayName("Should reject insufficient income for loan")
        void shouldRejectInsufficientIncomeForLoan() {
            // Given - Customer with $4000 monthly income
            ExternalCustomer externalCustomer = createCompleteExternalCustomer();
            externalCustomer.getFinancialInfo().setMonthlyIncome(new BigDecimal("4000"));
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(externalCustomer));
            
            BigDecimal loanAmount = new BigDecimal("360000"); // Requires $10000/month (360000/36)

            // When
            boolean hasSufficientIncome = adapter.hassufficientIncome(123L, loanAmount);

            // Then
            assertThat(hasSufficientIncome).isFalse();
        }

        @Test
        @DisplayName("Should handle missing customer profile")
        void shouldHandleMissingCustomerProfile() {
            // Given
            when(externalCustomerService.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean hasSufficientIncome = adapter.hassufficientIncome(999L, new BigDecimal("100000"));

            // Then
            assertThat(hasSufficientIncome).isFalse();
        }

        @Test
        @DisplayName("Should handle income calculation exception")
        void shouldHandleIncomeCalculationException() {
            // Given
            when(externalCustomerService.findById(any())).thenThrow(new RuntimeException("Calculation error"));

            // When
            boolean hasSufficientIncome = adapter.hassufficientIncome(123L, new BigDecimal("100000"));

            // Then
            assertThat(hasSufficientIncome).isFalse();
        }

        @Test
        @DisplayName("Should calculate income requirement correctly")
        void shouldCalculateIncomeRequirementCorrectly() {
            // Given - Test the 1/36 rule (loan amount / 36 months)
            ExternalCustomer customer1 = createCustomerWithIncome(new BigDecimal("3000")); // $3000/month
            ExternalCustomer customer2 = createCustomerWithIncome(new BigDecimal("2777")); // $2777/month
            
            when(externalCustomerService.findById(1L)).thenReturn(Optional.of(customer1));
            when(externalCustomerService.findById(2L)).thenReturn(Optional.of(customer2));
            
            BigDecimal loanAmount = new BigDecimal("100000"); // Requires $2777.78/month

            // When & Then
            assertThat(adapter.hassufficientIncome(1L, loanAmount)).isTrue(); // $3000 > $2777.78
            assertThat(adapter.hassufficientIncome(2L, loanAmount)).isFalse(); // $2777 < $2777.78
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle all null inputs gracefully")
        void shouldHandleAllNullInputsGracefully() {
            // When & Then - Should not throw exceptions
            assertThatCode(() -> adapter.getCustomerProfile(null)).doesNotThrowAnyException();
            assertThatCode(() -> adapter.isCustomerActiveAndValid(null)).doesNotThrowAnyException();
            assertThatCode(() -> adapter.getCustomerCreditScore(null)).doesNotThrowAnyException();
            assertThatCode(() -> adapter.hassufficientIncome(null, new BigDecimal("100000"))).doesNotThrowAnyException();
            assertThatCode(() -> adapter.hassufficientIncome(123L, null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle incomplete external customer data")
        void shouldHandleIncompleteExternalCustomerData() {
            // Given - Customer with missing personal info
            ExternalCustomer incompleteCustomer = new ExternalCustomer();
            incompleteCustomer.setId(123L);
            incompleteCustomer.setStatus("ACTIVE");
            // Missing personalInfo, contactInfo, etc.
            
            when(externalCustomerService.findById(123L)).thenReturn(Optional.of(incompleteCustomer));

            // When & Then - Should handle gracefully
            assertThat(adapter.isCustomerActiveAndValid(123L)).isFalse();
            
            assertThatCode(() -> adapter.getCustomerProfile(123L)).doesNotThrowAnyException();
            Optional<CustomerProfile> profile = adapter.getCustomerProfile(123L);
            assertThat(profile).isEmpty(); // Should fail translation gracefully
        }
    }

    // Helper methods
    private ExternalCustomer createCompleteExternalCustomer() {
        ExternalCustomer customer = new ExternalCustomer();
        customer.setId(123L);
        customer.setStatus("ACTIVE");
        
        // Personal info
        ExternalPersonalInfo personalInfo = new ExternalPersonalInfo();
        personalInfo.setFirstName("John");
        personalInfo.setLastName("Doe");
        personalInfo.setDateOfBirth(LocalDate.of(1985, 6, 15));
        customer.setPersonalInfo(personalInfo);
        
        // Contact info
        ExternalContactInfo contactInfo = new ExternalContactInfo();
        contactInfo.setEmail("john.doe@email.com");
        contactInfo.setPhone("+1234567890");
        
        ExternalAddress address = new ExternalAddress();
        address.setStreet("123 Main St");
        address.setCity("Anytown");
        address.setState("ST");
        address.setZipCode("12345");
        contactInfo.setAddress(address);
        customer.setContactInfo(contactInfo);
        
        // Financial info
        ExternalFinancialInfo financialInfo = new ExternalFinancialInfo();
        financialInfo.setMonthlyIncome(new BigDecimal("8000"));
        financialInfo.setTotalAssets(new BigDecimal("250000"));
        financialInfo.setTotalLiabilities(new BigDecimal("150000"));
        customer.setFinancialInfo(financialInfo);
        
        // Employment info
        ExternalEmploymentInfo employmentInfo = new ExternalEmploymentInfo();
        employmentInfo.setStatus("EMPLOYED");
        employmentInfo.setEmployer("Tech Corp");
        employmentInfo.setYearsEmployed(5);
        customer.setEmploymentInfo(employmentInfo);
        
        // Credit info
        ExternalCreditInfo creditInfo = new ExternalCreditInfo();
        creditInfo.setScore(750);
        creditInfo.setRating("EXCELLENT");
        customer.setCreditInfo(creditInfo);
        
        return customer;
    }
    
    private ExternalCustomer createCustomerWithIncome(BigDecimal monthlyIncome) {
        ExternalCustomer customer = createCompleteExternalCustomer();
        customer.getFinancialInfo().setMonthlyIncome(monthlyIncome);
        return customer;
    }
}