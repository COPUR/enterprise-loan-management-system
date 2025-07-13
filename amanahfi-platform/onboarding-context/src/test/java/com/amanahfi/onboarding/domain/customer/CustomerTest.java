package com.amanahfi.onboarding.domain.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Customer Aggregate
 * Following Islamic banking KYC and onboarding requirements
 */
@DisplayName("Customer Aggregate Tests")
class CustomerTest {

    @Test
    @DisplayName("Should create new Customer with valid details")
    void shouldCreateNewCustomerWithValidDetails() {
        // Given
        String emiratesId = "784-1990-1234567-1";
        String fullName = "Ahmed Mohammed Al-Mansouri";
        String email = "ahmed.mansouri@email.ae";
        String mobileNumber = "+971501234567";
        LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);

        // When
        Customer customer = Customer.create(
            emiratesId,
            fullName,
            email,
            mobileNumber,
            dateOfBirth,
            CustomerType.INDIVIDUAL
        );

        // Then
        assertThat(customer.getCustomerId()).isNotNull();
        assertThat(customer.getEmiratesId()).isEqualTo(emiratesId);
        assertThat(customer.getFullName()).isEqualTo(fullName);
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getMobileNumber()).isEqualTo(mobileNumber);
        assertThat(customer.getDateOfBirth()).isEqualTo(dateOfBirth);
        assertThat(customer.getCustomerType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING_KYC);
        assertThat(customer.getRegistrationDate()).isNotNull();
        assertThat(customer.isIslamicBankingPreferred()).isFalse(); // Default
    }

    @Test
    @DisplayName("Should create Customer with Islamic banking preference")
    void shouldCreateCustomerWithIslamicBankingPreference() {
        // When
        Customer customer = Customer.create(
            "784-1990-1234567-1",
            "Fatima Al-Zahra",
            "fatima.zahra@email.ae",
            "+971501234567",
            LocalDate.of(1985, 3, 20),
            CustomerType.INDIVIDUAL
        ).withIslamicBankingPreference(true);

        // Then
        assertThat(customer.isIslamicBankingPreferred()).isTrue();
        assertThat(customer.getCustomerType()).isEqualTo(CustomerType.INDIVIDUAL);
    }

    @Test
    @DisplayName("Should validate Emirates ID format")
    void shouldValidateEmiratesIdFormat() {
        // Given - Invalid Emirates ID formats
        String[] invalidEmiratesIds = {
            null,
            "",
            "123-456-789",
            "784-1990-123456789", // Too long
            "abc-1990-1234567-1",  // Non-numeric
            "784-2030-1234567-1"   // Future birth year
        };

        // When & Then
        for (String invalidId : invalidEmiratesIds) {
            assertThatThrownBy(() -> Customer.create(
                invalidId,
                "Test Name",
                "test@email.ae",
                "+971501234567",
                LocalDate.of(1990, 1, 1),
                CustomerType.INDIVIDUAL
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "784-1990-1234567-1",
        "784-1985-7654321-2", 
        "784-2000-1111111-1"
    })
    @DisplayName("Should accept valid Emirates ID formats")
    void shouldAcceptValidEmiratesIdFormats(String validEmiratesId) {
        // When
        Customer customer = Customer.create(
            validEmiratesId,
            "Test Customer",
            "test@email.ae",
            "+971501234567",
            LocalDate.of(1990, 1, 1),
            CustomerType.INDIVIDUAL
        );

        // Then
        assertThat(customer.getEmiratesId()).isEqualTo(validEmiratesId);
    }

    @Test
    @DisplayName("Should validate UAE mobile number format")
    void shouldValidateUaeMobileNumberFormat() {
        // Given - Invalid mobile numbers
        String[] invalidNumbers = {
            null,
            "",
            "1234567890",        // No country code
            "+1234567890",       // Non-UAE country code
            "+971123456789",     // Invalid UAE format
            "+97150123456789"    // Too long
        };

        // When & Then
        for (String invalidNumber : invalidNumbers) {
            assertThatThrownBy(() -> Customer.create(
                "784-1990-1234567-1",
                "Test Name",
                "test@email.ae",
                invalidNumber,
                LocalDate.of(1990, 1, 1),
                CustomerType.INDIVIDUAL
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+971501234567",   // Etisalat
        "+971521234567",   // Etisalat
        "+971551234567",   // du
        "+971561234567"    // du
    })
    @DisplayName("Should accept valid UAE mobile numbers")
    void shouldAcceptValidUaeMobileNumbers(String validNumber) {
        // When
        Customer customer = Customer.create(
            "784-1990-1234567-1",
            "Test Customer",
            "test@email.ae",
            validNumber,
            LocalDate.of(1990, 1, 1),
            CustomerType.INDIVIDUAL
        );

        // Then
        assertThat(customer.getMobileNumber()).isEqualTo(validNumber);
    }

    @Test
    @DisplayName("Should validate customer age requirements")
    void shouldValidateCustomerAgeRequirements() {
        // Given - Under 18 years old
        LocalDate underage = LocalDate.now().minusYears(17);
        
        // When & Then
        assertThatThrownBy(() -> Customer.create(
            "784-2005-1234567-1",
            "Young Customer",
            "young@email.ae",
            "+971501234567",
            underage,
            CustomerType.INDIVIDUAL
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be at least 18 years old");
    }

    @Test
    @DisplayName("Should complete KYC process successfully")
    void shouldCompleteKycProcessSuccessfully() {
        // Given
        Customer customer = createValidCustomer();
        KycDocument document = new KycDocument(
            DocumentType.EMIRATES_ID,
            "emirates_id_front.jpg",
            "base64encodedcontent..."
        );

        // When
        customer.submitKycDocuments(document);
        customer.approveKyc("KYC-OFFICER-001", "All documents verified");

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getKycCompletionDate()).isNotNull();
        assertThat(customer.isKycCompleted()).isTrue();
    }

    @Test
    @DisplayName("Should reject KYC with proper reason")
    void shouldRejectKycWithProperReason() {
        // Given
        Customer customer = createValidCustomer();
        String rejectionReason = "Document quality insufficient";

        // When
        customer.rejectKyc("KYC-OFFICER-001", rejectionReason);

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.KYC_REJECTED);
        assertThat(customer.getKycRejectionReason()).isEqualTo(rejectionReason);
        assertThat(customer.isKycCompleted()).isFalse();
    }

    @Test
    @DisplayName("Should suspend customer account")
    void shouldSuspendCustomerAccount() {
        // Given
        Customer customer = createValidCustomer();
        customer.approveKyc("KYC-OFFICER-001", "Approved");
        String suspensionReason = "Suspicious activity detected";

        // When
        customer.suspend(suspensionReason);

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(customer.getSuspensionReason()).isEqualTo(suspensionReason);
    }

    @Test
    @DisplayName("Should reactivate suspended customer")
    void shouldReactivateSuspendedCustomer() {
        // Given
        Customer customer = createValidCustomer();
        customer.approveKyc("KYC-OFFICER-001", "Approved");
        customer.suspend("Test suspension");

        // When
        customer.reactivate("Issue resolved");

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getSuspensionReason()).isNull();
    }

    @Test
    @DisplayName("Should update customer preferences")
    void shouldUpdateCustomerPreferences() {
        // Given
        Customer customer = createValidCustomer();
        customer.approveKyc("KYC-OFFICER-001", "Approved");

        // When
        customer.updatePreferences(
            true,  // Islamic banking preferred
            NotificationPreference.EMAIL_AND_SMS,
            "Arabic"
        );

        // Then
        assertThat(customer.isIslamicBankingPreferred()).isTrue();
        assertThat(customer.getNotificationPreference()).isEqualTo(NotificationPreference.EMAIL_AND_SMS);
        assertThat(customer.getPreferredLanguage()).isEqualTo("Arabic");
    }

    @Test
    @DisplayName("Should validate business customer creation")
    void shouldValidateBusinessCustomerCreation() {
        // Given
        String tradeNumber = "CN-1234567";

        // When
        Customer businessCustomer = Customer.createBusiness(
            "784-1980-1234567-1", // Authorized person Emirates ID (valid age)
            "Al-Mansouri Trading LLC",
            "business@almansouri.ae",
            "+971501234567",
            tradeNumber,
            LocalDate.of(1980, 1, 15) // Date of birth of authorized person
        );

        // Then
        assertThat(businessCustomer.getCustomerType()).isEqualTo(CustomerType.BUSINESS);
        assertThat(businessCustomer.getBusinessDetails()).isNotNull();
        assertThat(businessCustomer.getBusinessDetails().getTradeLicenseNumber()).isEqualTo(tradeNumber);
        assertThat(businessCustomer.getStatus()).isEqualTo(CustomerStatus.PENDING_KYC);
    }

    @Test
    @DisplayName("Should enforce Islamic banking compliance rules")
    void shouldEnforceIslamicBankingComplianceRules() {
        // Given
        Customer customer = createValidCustomer().withIslamicBankingPreference(true);
        customer.submitKycDocuments(new KycDocument(DocumentType.EMIRATES_ID, "emirates_id.jpg", "content"));
        customer.approveKyc("KYC-OFFICER-001", "Approved for Islamic banking");

        // When & Then
        assertThat(customer.canAccessConventionalProducts()).isFalse();
        assertThat(customer.canAccessIslamicProducts()).isTrue();
        assertThat(customer.requiresShariaSupervisoryBoardApproval()).isTrue();
    }

    private Customer createValidCustomer() {
        return Customer.create(
            "784-1990-1234567-1",
            "Ahmed Mohammed Al-Mansouri",
            "ahmed.mansouri@email.ae",
            "+971501234567",
            LocalDate.of(1990, 5, 15),
            CustomerType.INDIVIDUAL
        );
    }
}