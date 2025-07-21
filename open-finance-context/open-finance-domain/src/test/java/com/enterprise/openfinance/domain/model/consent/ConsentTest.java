package com.enterprise.openfinance.domain.model.consent;

import com.enterprise.openfinance.domain.event.ConsentAuthorizedEvent;
import com.enterprise.openfinance.domain.event.ConsentCreatedEvent;
import com.enterprise.openfinance.domain.event.ConsentRevokedEvent;
import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.shared.domain.CustomerId;
import com.enterprise.shared.domain.Money;
import net.jqwik.api.*;
import net.jqwik.time.api.DateTimes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for Consent aggregate following property-based testing approach.
 * Tests cover consent lifecycle, business rules, and invariants.
 */
@Tag("unit")
@Tag("property-based")
class ConsentTest {

    // Test Data Builders (Following project's pattern)
    private static class ConsentTestData {
        
        static Consent.ConsentBuilder validConsentBuilder() {
            return Consent.builder()
                    .id(ConsentId.generate())
                    .customerId(CustomerId.of("CUST-123"))
                    .participantId(ParticipantId.of("PART-456"))
                    .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION, ConsentScope.TRANSACTION_HISTORY))
                    .purpose(ConsentPurpose.LOAN_APPLICATION)
                    .expiryDate(LocalDateTime.now().plusDays(30));
        }
        
        static Consent createValidConsent() {
            return validConsentBuilder().build();
        }
    }

    // === TDD: Red-Green-Refactor Cycle for Consent Creation ===
    
    @Test
    @DisplayName("Given valid consent data, When creating consent, Then should create with PENDING status")
    void should_create_consent_with_pending_status() {
        // Given: Valid consent data
        var customerId = CustomerId.of("CUST-123");
        var participantId = ParticipantId.of("PART-456");
        var scopes = Set.of(ConsentScope.ACCOUNT_INFORMATION);
        var purpose = ConsentPurpose.LOAN_APPLICATION;
        var expiryDate = LocalDateTime.now().plusDays(30);

        // When: Creating consent
        var consent = Consent.builder()
                .id(ConsentId.generate())
                .customerId(customerId)
                .participantId(participantId)
                .scopes(scopes)
                .purpose(purpose)
                .expiryDate(expiryDate)
                .build();

        // Then: Should have correct initial state
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.PENDING);
        assertThat(consent.getCustomerId()).isEqualTo(customerId);
        assertThat(consent.getParticipantId()).isEqualTo(participantId);
        assertThat(consent.getScopes()).containsExactlyInAnyOrderElementsOf(scopes);
        assertThat(consent.getPurpose()).isEqualTo(purpose);
        assertThat(consent.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(consent.isExpired()).isFalse();
        
        // And: Should have domain event
        assertThat(consent.getDomainEvents()).hasSize(1);
        assertThat(consent.getDomainEvents().get(0)).isInstanceOf(ConsentCreatedEvent.class);
    }

    @Test
    @DisplayName("Given consent with null required fields, When creating, Then should throw exception")
    void should_reject_consent_with_null_required_fields() {
        // Given & When & Then: Each required field should be validated
        assertThatThrownBy(() -> 
            Consent.builder()
                .customerId(null)
                .participantId(ParticipantId.of("PART-456"))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .purpose(ConsentPurpose.LOAN_APPLICATION)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer ID cannot be null");
    }

    // === TDD: Consent Authorization Flow ===
    
    @Test
    @DisplayName("Given PENDING consent, When authorizing, Then should change to AUTHORIZED status")
    void should_authorize_pending_consent() {
        // Given: Pending consent
        var consent = ConsentTestData.createValidConsent();
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.PENDING);

        // When: Authorizing consent
        consent.authorize();

        // Then: Should be authorized
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.AUTHORIZED);
        assertThat(consent.getAuthorizedAt()).isNotNull();
        
        // And: Should emit authorization event
        var events = consent.getDomainEvents();
        assertThat(events).hasSize(2); // Created + Authorized
        assertThat(events.get(1)).isInstanceOf(ConsentAuthorizedEvent.class);
    }

    @Test
    @DisplayName("Given AUTHORIZED consent, When authorizing again, Then should throw exception")
    void should_reject_authorization_of_already_authorized_consent() {
        // Given: Already authorized consent
        var consent = ConsentTestData.createValidConsent();
        consent.authorize();

        // When & Then: Should reject re-authorization
        assertThatThrownBy(() -> consent.authorize())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot authorize consent that is not pending");
    }

    // === TDD: Consent Revocation ===
    
    @Test
    @DisplayName("Given AUTHORIZED consent, When revoking, Then should change to REVOKED status")
    void should_revoke_authorized_consent() {
        // Given: Authorized consent
        var consent = ConsentTestData.createValidConsent();
        consent.authorize();
        
        var revocationReason = "Customer requested revocation";

        // When: Revoking consent
        consent.revoke(revocationReason);

        // Then: Should be revoked
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(consent.getRevokedAt()).isNotNull();
        assertThat(consent.getRevocationReason()).isEqualTo(revocationReason);
        
        // And: Should emit revocation event
        var events = consent.getDomainEvents();
        assertThat(events).anyMatch(event -> event instanceof ConsentRevokedEvent);
    }

    @Test
    @DisplayName("Given PENDING consent, When revoking, Then should change to REVOKED status")
    void should_revoke_pending_consent() {
        // Given: Pending consent
        var consent = ConsentTestData.createValidConsent();
        
        // When: Revoking consent
        consent.revoke("Cancelled before authorization");

        // Then: Should be revoked
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
    }

    @Test
    @DisplayName("Given REVOKED consent, When revoking again, Then should throw exception")
    void should_reject_revocation_of_already_revoked_consent() {
        // Given: Already revoked consent
        var consent = ConsentTestData.createValidConsent();
        consent.revoke("First revocation");

        // When & Then: Should reject re-revocation
        assertThatThrownBy(() -> consent.revoke("Second revocation"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Consent is already revoked");
    }

    // === TDD: Consent Expiration ===
    
    @Test
    @DisplayName("Given consent with past expiry date, When checking expiry, Then should be expired")
    void should_detect_expired_consent() {
        // Given: Consent with past expiry date
        var consent = Consent.builder()
                .id(ConsentId.generate())
                .customerId(CustomerId.of("CUST-123"))
                .participantId(ParticipantId.of("PART-456"))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .purpose(ConsentPurpose.LOAN_APPLICATION)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        // When & Then: Should be expired
        assertThat(consent.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Given expired consent, When checking if active, Then should not be active")
    void should_not_be_active_when_expired() {
        // Given: Expired but authorized consent
        var consent = Consent.builder()
                .id(ConsentId.generate())
                .customerId(CustomerId.of("CUST-123"))
                .participantId(ParticipantId.of("PART-456"))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .purpose(ConsentPurpose.LOAN_APPLICATION)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();
        consent.authorize();

        // When & Then: Should not be active despite being authorized
        assertThat(consent.isActive()).isFalse();
    }

    // === TDD: Consent Renewal ===
    
    @Test
    @DisplayName("Given AUTHORIZED consent, When renewing, Then should extend expiry date")
    void should_renew_authorized_consent() {
        // Given: Authorized consent expiring soon
        var consent = ConsentTestData.validConsentBuilder()
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
        consent.authorize();
        
        var newExpiryDate = LocalDateTime.now().plusDays(30);

        // When: Renewing consent
        consent.renew(newExpiryDate);

        // Then: Should have new expiry date
        assertThat(consent.getExpiryDate()).isEqualTo(newExpiryDate);
        assertThat(consent.getRenewedAt()).isNotNull();
        assertThat(consent.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Given REVOKED consent, When renewing, Then should throw exception")
    void should_reject_renewal_of_revoked_consent() {
        // Given: Revoked consent
        var consent = ConsentTestData.createValidConsent();
        consent.revoke("Test revocation");

        // When & Then: Should reject renewal
        assertThatThrownBy(() -> consent.renew(LocalDateTime.now().plusDays(30)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot renew revoked consent");
    }

    // === Property-Based Tests ===
    
    @Property(tries = 100)
    @DisplayName("Property: Consent ID should always be unique")
    void consent_ids_should_be_unique() {
        // Given: Two consent ID generations
        var id1 = ConsentId.generate();
        var id2 = ConsentId.generate();

        // When & Then: Should always be different
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.getValue()).isNotEqualTo(id2.getValue());
    }

    @Property(tries = 100)
    @DisplayName("Property: Consent should remain immutable after creation")
    void consent_should_be_immutable_after_creation(
            @ForAll("validCustomerIds") String customerId,
            @ForAll("validParticipantIds") String participantId,
            @ForAll("validScopes") Set<ConsentScope> scopes,
            @ForAll("futureDates") LocalDateTime expiryDate) {
        
        // Given: Created consent
        var consent = Consent.builder()
                .id(ConsentId.generate())
                .customerId(CustomerId.of(customerId))
                .participantId(ParticipantId.of(participantId))
                .scopes(scopes)
                .purpose(ConsentPurpose.LOAN_APPLICATION)
                .expiryDate(expiryDate)
                .build();

        var originalCustomerId = consent.getCustomerId();
        var originalScopes = consent.getScopes();

        // When: Attempting to modify (should not be possible due to immutability)
        // Then: Original values should remain unchanged
        assertThat(consent.getCustomerId()).isEqualTo(originalCustomerId);
        assertThat(consent.getScopes()).containsExactlyInAnyOrderElementsOf(originalScopes);
    }

    @Property(tries = 50)
    @DisplayName("Property: Authorized consent should always emit ConsentAuthorizedEvent")
    void authorized_consent_should_emit_event(
            @ForAll("validCustomerIds") String customerId,
            @ForAll("validParticipantIds") String participantId) {
        
        // Given: Pending consent
        var consent = Consent.builder()
                .id(ConsentId.generate())
                .customerId(CustomerId.of(customerId))
                .participantId(ParticipantId.of(participantId))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .purpose(ConsentPurpose.LOAN_APPLICATION)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();

        // When: Authorizing consent
        consent.authorize();

        // Then: Should always emit authorization event
        assertThat(consent.getDomainEvents())
                .anyMatch(event -> event instanceof ConsentAuthorizedEvent);
    }

    // === Property Providers ===
    
    @Provide
    Arbitrary<String> validCustomerIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(s -> "CUST-" + s);
    }

    @Provide
    Arbitrary<String> validParticipantIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(s -> "PART-" + s);
    }

    @Provide
    Arbitrary<Set<ConsentScope>> validScopes() {
        return Arbitraries.of(ConsentScope.class)
                .set()
                .ofMinSize(1)
                .ofMaxSize(ConsentScope.values().length);
    }

    @Provide
    Arbitrary<LocalDateTime> futureDates() {
        return DateTimes.dateTimes()
                .between(
                        LocalDateTime.now().plusHours(1),
                        LocalDateTime.now().plusYears(1)
                );
    }

    // === Business Rule Tests ===
    
    @Test
    @DisplayName("Business Rule: Consent for payment initiation should have longer validity")
    void payment_consent_should_have_longer_validity() {
        // Given: Payment initiation purpose
        var paymentConsent = ConsentTestData.validConsentBuilder()
                .purpose(ConsentPurpose.PAYMENT_INITIATION)
                .expiryDate(LocalDateTime.now().plusDays(90))
                .build();

        var informationConsent = ConsentTestData.validConsentBuilder()
                .purpose(ConsentPurpose.ACCOUNT_INFORMATION)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();

        // When & Then: Payment consent should have longer validity
        assertThat(paymentConsent.getExpiryDate())
                .isAfter(informationConsent.getExpiryDate());
    }

    @Test
    @DisplayName("Business Rule: Active consent should be authorized and not expired")
    void active_consent_business_rule() {
        // Given: Various consent states
        var pendingConsent = ConsentTestData.createValidConsent();
        
        var authorizedConsent = ConsentTestData.createValidConsent();
        authorizedConsent.authorize();
        
        var expiredConsent = ConsentTestData.validConsentBuilder()
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();
        expiredConsent.authorize();
        
        var revokedConsent = ConsentTestData.createValidConsent();
        revokedConsent.authorize();
        revokedConsent.revoke("Test revocation");

        // When & Then: Only authorized and non-expired should be active
        assertThat(pendingConsent.isActive()).isFalse();
        assertThat(authorizedConsent.isActive()).isTrue();
        assertThat(expiredConsent.isActive()).isFalse();
        assertThat(revokedConsent.isActive()).isFalse();
    }

    @Test
    @DisplayName("Business Rule: Consent with sensitive scopes should have shorter validity")
    void sensitive_scopes_should_have_shorter_validity() {
        // Given: Consent with sensitive transaction scope
        var sensitiveConsent = ConsentTestData.validConsentBuilder()
                .scopes(Set.of(ConsentScope.TRANSACTION_HISTORY, ConsentScope.ACCOUNT_INFORMATION))
                .expiryDate(LocalDateTime.now().plusDays(7)) // Shorter validity
                .build();

        var basicConsent = ConsentTestData.validConsentBuilder()
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .expiryDate(LocalDateTime.now().plusDays(30)) // Standard validity
                .build();

        // When & Then: Sensitive consent should have shorter validity
        assertThat(sensitiveConsent.getExpiryDate())
                .isBefore(basicConsent.getExpiryDate());
    }
}