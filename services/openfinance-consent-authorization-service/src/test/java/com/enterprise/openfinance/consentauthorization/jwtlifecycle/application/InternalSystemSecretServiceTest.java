package com.enterprise.openfinance.consentauthorization.jwtlifecycle.application;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.UpsertInternalSystemSecretCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalSystemSecretPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalSystemSecretServiceTest {

    @Mock
    private InternalSystemSecretPort secretPort;

    private InternalSystemSecretService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-02-25T10:00:00Z"), ZoneOffset.UTC);
        service = new InternalSystemSecretService(secretPort, clock);
    }

    @Test
    void upsertShouldCreateVersionOneRecordWithMaskedValue() {
        UpsertInternalSystemSecretCommand command = new UpsertInternalSystemSecretCommand(
                "internal.jwt_hmac_secret",
                "super-sensitive-secret-value",
                null
        );
        when(secretPort.findBySecretKey("INTERNAL.JWT_HMAC_SECRET")).thenReturn(Optional.empty());
        when(secretPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0, InternalSystemSecretRecord.class));

        var response = service.upsert(command);

        assertThat(response.secretKey()).isEqualTo("INTERNAL.JWT_HMAC_SECRET");
        assertThat(response.maskedValue()).isEqualTo("su****ue");
        assertThat(response.classification()).isEqualTo("INTERNAL");
        assertThat(response.version()).isEqualTo(1);

        ArgumentCaptor<InternalSystemSecretRecord> captor = ArgumentCaptor.forClass(InternalSystemSecretRecord.class);
        verify(secretPort).save(captor.capture());
        InternalSystemSecretRecord saved = captor.getValue();
        assertThat(saved.secretHash()).isNotBlank();
        assertThat(saved.secretHash()).doesNotContain("super-sensitive-secret-value");
        assertThat(saved.hashSalt()).isNotBlank();
    }

    @Test
    void upsertShouldIncrementVersionAndPreserveCreationTimestamp() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        InternalSystemSecretRecord existing = new InternalSystemSecretRecord(
                "PAYMENT.API.KEY",
                "ab****yz",
                "hash",
                "salt",
                "INTERNAL",
                4,
                createdAt,
                Instant.parse("2026-02-01T00:00:00Z")
        );
        when(secretPort.findBySecretKey("PAYMENT.API.KEY")).thenReturn(Optional.of(existing));
        when(secretPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0, InternalSystemSecretRecord.class));

        var response = service.upsert(new UpsertInternalSystemSecretCommand(
                "payment.api.key",
                "next-secret",
                "payment"
        ));

        assertThat(response.version()).isEqualTo(5);
        assertThat(response.classification()).isEqualTo("PAYMENT");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void getMetadataShouldReturnOptionalView() {
        InternalSystemSecretRecord existing = new InternalSystemSecretRecord(
                "RISK.ENGINE.TOKEN",
                "to****en",
                "hash",
                "salt",
                "INTERNAL",
                2,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z")
        );
        when(secretPort.findBySecretKey("RISK.ENGINE.TOKEN")).thenReturn(Optional.of(existing));

        var metadata = service.getMetadata("risk.engine.token");

        assertThat(metadata).isPresent();
        assertThat(metadata.orElseThrow().secretKey()).isEqualTo("RISK.ENGINE.TOKEN");
    }

    @Test
    void helperMethodsShouldValidateInput() {
        assertThatThrownBy(() -> InternalSystemSecretService.normalizeKey(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secret key");

        assertThatThrownBy(() -> InternalSystemSecretService.mask("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secret value");

        assertThatThrownBy(() -> InternalSystemSecretService.hash("value", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Salt");
    }
}
