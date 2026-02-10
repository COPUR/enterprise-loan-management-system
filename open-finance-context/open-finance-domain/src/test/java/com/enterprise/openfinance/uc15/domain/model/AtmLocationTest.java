package com.enterprise.openfinance.uc15.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtmLocationTest {

    @Test
    void shouldDetermineRadiusMatch() {
        AtmLocation atm = sample("ATM-001", 25.2048, 55.2708);

        assertThat(atm.isWithinRadiusKm(25.2050, 55.2709, 2.0)).isTrue();
        assertThat(atm.isWithinRadiusKm(24.4539, 54.3773, 5.0)).isFalse();
    }

    @Test
    void shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new AtmLocation(
                " ",
                "Downtown ATM",
                AtmStatus.IN_SERVICE,
                25.2048,
                55.2708,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal"),
                Instant.parse("2026-02-10T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("atmId");

        assertThatThrownBy(() -> new AtmLocation(
                "ATM-001",
                "Downtown ATM",
                AtmStatus.IN_SERVICE,
                120.0,
                55.2708,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal"),
                Instant.parse("2026-02-10T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");

        assertThatThrownBy(() -> new AtmLocation(
                "ATM-001",
                "Downtown ATM",
                AtmStatus.IN_SERVICE,
                25.2048,
                55.2708,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of(),
                Instant.parse("2026-02-10T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("services");
    }

    private static AtmLocation sample(String atmId, double lat, double lon) {
        return new AtmLocation(
                atmId,
                "Downtown ATM",
                AtmStatus.IN_SERVICE,
                lat,
                lon,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal", "BalanceInquiry"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
