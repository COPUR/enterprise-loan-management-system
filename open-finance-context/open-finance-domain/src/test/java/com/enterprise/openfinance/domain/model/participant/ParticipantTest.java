package com.enterprise.openfinance.domain.model.participant;

import com.enterprise.openfinance.domain.event.ParticipantOnboardedEvent;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for Participant entity following property-based testing approach.
 */
@Tag("unit")
@Tag("property-based")
class ParticipantTest {

    @Test
    @DisplayName("Given valid participant data, When creating participant, Then should create successfully")
    void should_create_participant_successfully() {
        // Given: Valid participant data
        var id = ParticipantId.of("PART-ABC123");
        var legalName = "Test Financial Services LLC";
        var role = ParticipantRole.DATA_RECIPIENT;
        var registration = CBUAERegistration.builder()
                .registrationId("REG-123456")
                .registeredAt(LocalDateTime.now())
                .status(CBUAERegistrationStatus.ACTIVE)
                .build();

        // When: Creating participant
        var participant = Participant.builder()
                .id(id)
                .legalName(legalName)
                .role(role)
                .registration(registration)
                .build();

        // Then: Should be created successfully
        assertThat(participant.getId()).isEqualTo(id);
        assertThat(participant.getLegalName()).isEqualTo(legalName);
        assertThat(participant.getRole()).isEqualTo(role);
        assertThat(participant.getRegistration()).isEqualTo(registration);
        assertThat(participant.isActive()).isTrue();
        
        // And: Should emit onboarded event
        assertThat(participant.getDomainEvents()).hasSize(1);
        assertThat(participant.getDomainEvents().get(0))
                .isInstanceOf(ParticipantOnboardedEvent.class);
    }

    @Test
    @DisplayName("Given null required fields, When creating participant, Then should throw exception")
    void should_reject_participant_with_null_required_fields() {
        // Given & When & Then: Each required field validation
        assertThatThrownBy(() -> 
            Participant.builder()
                .id(null)
                .legalName("Test Company")
                .role(ParticipantRole.DATA_HOLDER)
                .registration(validRegistration())
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Participant ID cannot be null");
    }

    @Test
    @DisplayName("Given valid participant, When adding certificate, Then should store certificate")
    void should_add_certificate_successfully() {
        // Given: Valid participant
        var participant = createValidParticipant();
        var certificate = createMockCertificate();

        // When: Adding certificate
        participant.addCertificate(certificate);

        // Then: Should have certificate
        assertThat(participant.getCertificates()).contains(certificate);
        assertThat(participant.hasCertificate(certificate)).isTrue();
    }

    @Test
    @DisplayName("Given participant with certificate, When removing certificate, Then should remove successfully")
    void should_remove_certificate_successfully() {
        // Given: Participant with certificate
        var participant = createValidParticipant();
        var certificate = createMockCertificate();
        participant.addCertificate(certificate);

        // When: Removing certificate
        participant.removeCertificate(certificate);

        // Then: Should not have certificate
        assertThat(participant.getCertificates()).doesNotContain(certificate);
        assertThat(participant.hasCertificate(certificate)).isFalse();
    }

    @Test
    @DisplayName("Given active participant, When deactivating, Then should be inactive")
    void should_deactivate_participant() {
        // Given: Active participant
        var participant = createValidParticipant();
        assertThat(participant.isActive()).isTrue();

        var reason = "Regulatory violation";

        // When: Deactivating
        participant.deactivate(reason);

        // Then: Should be inactive
        assertThat(participant.isActive()).isFalse();
        assertThat(participant.getRegistration().getStatus())
                .isEqualTo(CBUAERegistrationStatus.SUSPENDED);
    }

    @Property(tries = 50)
    @DisplayName("Property: Participant ID should always be valid format")
    void participant_id_should_be_valid_format(
            @ForAll("validParticipantIds") String participantId) {
        
        // When: Creating participant ID
        var id = ParticipantId.of(participantId);

        // Then: Should be valid
        assertThat(id.getValue()).isEqualTo(participantId);
        assertThat(id.getValue()).matches("^[A-Z0-9\\-]+$");
        assertThat(id.getValue().length()).isBetween(8, 20);
    }

    @Property(tries = 30)
    @DisplayName("Property: Participant with DATA_HOLDER role should allow data sharing")
    void data_holder_should_allow_data_sharing(
            @ForAll("validParticipantNames") String legalName) {
        
        // Given: Data holder participant
        var participant = Participant.builder()
                .id(ParticipantId.of("HOLDER-123"))
                .legalName(legalName)
                .role(ParticipantRole.DATA_HOLDER)
                .registration(validRegistration())
                .build();

        // When & Then: Should allow data sharing
        assertThat(participant.canShareData()).isTrue();
        assertThat(participant.canReceiveData()).isFalse();
    }

    @Property(tries = 30)
    @DisplayName("Property: Participant with DATA_RECIPIENT role should allow data receiving")
    void data_recipient_should_allow_data_receiving(
            @ForAll("validParticipantNames") String legalName) {
        
        // Given: Data recipient participant
        var participant = Participant.builder()
                .id(ParticipantId.of("RECIPIENT-123"))
                .legalName(legalName)
                .role(ParticipantRole.DATA_RECIPIENT)
                .registration(validRegistration())
                .build();

        // When & Then: Should allow data receiving
        assertThat(participant.canReceiveData()).isTrue();
        assertThat(participant.canShareData()).isFalse();
    }

    // Property providers
    @Provide
    Arbitrary<String> validParticipantIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(4)
                .ofMaxLength(12)
                .map(s -> "PART-" + s);
    }

    @Provide
    Arbitrary<String> validParticipantNames() {
        return Arbitraries.of(
                "Emirates NBD Bank",
                "First Abu Dhabi Bank",
                "Dubai Islamic Bank",
                "Abu Dhabi Commercial Bank",
                "Mashreq Bank",
                "RAKBANK",
                "Commercial Bank of Dubai",
                "Union National Bank"
        ).map(name -> name + " PJSC");
    }

    // Helper methods
    private Participant createValidParticipant() {
        return Participant.builder()
                .id(ParticipantId.of("PART-TEST123"))
                .legalName("Test Financial Services LLC")
                .role(ParticipantRole.DATA_RECIPIENT)
                .registration(validRegistration())
                .build();
    }

    private CBUAERegistration validRegistration() {
        return CBUAERegistration.builder()
                .registrationId("REG-123456")
                .registeredAt(LocalDateTime.now().minusDays(30))
                .status(CBUAERegistrationStatus.ACTIVE)
                .build();
    }

    private ParticipantCertificate createMockCertificate() {
        return ParticipantCertificate.builder()
                .serialNumber("123456789")
                .issuer("CBUAE Test CA")
                .subject("CN=Test Participant")
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(365))
                .status(CertificateStatus.ACTIVE)
                .build();
    }
}