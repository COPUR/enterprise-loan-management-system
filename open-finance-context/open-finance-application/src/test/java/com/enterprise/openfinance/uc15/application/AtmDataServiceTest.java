package com.enterprise.openfinance.uc15.application;

import com.enterprise.openfinance.uc15.domain.model.AtmDataSettings;
import com.enterprise.openfinance.uc15.domain.model.AtmListResult;
import com.enterprise.openfinance.uc15.domain.model.AtmLocation;
import com.enterprise.openfinance.uc15.domain.model.AtmStatus;
import com.enterprise.openfinance.uc15.domain.port.out.AtmCachePort;
import com.enterprise.openfinance.uc15.domain.port.out.AtmDirectoryPort;
import com.enterprise.openfinance.uc15.domain.query.GetAtmsQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtmDataServiceTest {

    private final AtmDirectoryPort directoryPort = mock(AtmDirectoryPort.class);
    private final AtmCachePort cachePort = mock(AtmCachePort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-02-10T10:00:00Z"), ZoneOffset.UTC);

    private final AtmDataService service = new AtmDataService(
            directoryPort,
            cachePort,
            new AtmDataSettings(Duration.ofMinutes(5)),
            clock
    );

    @Test
    void shouldReturnCachedResultOnHit() {
        AtmListResult cached = new AtmListResult(List.of(sample("ATM-001", 25.2048, 55.2708)), false);
        when(cachePort.getAtms(eq("atms:ALL"), any())).thenReturn(Optional.of(cached));

        AtmListResult result = service.listAtms(new GetAtmsQuery("ix-1", null, null, null));

        assertThat(result.cacheHit()).isTrue();
        verify(directoryPort, never()).listAtms();
    }

    @Test
    void shouldReadAndCacheOnMiss() {
        when(cachePort.getAtms(eq("atms:ALL"), any())).thenReturn(Optional.empty());
        when(directoryPort.listAtms()).thenReturn(List.of(
                sample("ATM-002", 24.4539, 54.3773),
                sample("ATM-001", 25.2048, 55.2708)
        ));

        AtmListResult result = service.listAtms(new GetAtmsQuery("ix-2", null, null, null));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.atms()).extracting(AtmLocation::atmId)
                .containsExactly("ATM-001", "ATM-002");
        verify(cachePort).putAtms(eq("atms:ALL"), eq(result), eq(Instant.parse("2026-02-10T10:05:00Z")));
    }

    @Test
    void shouldFilterByLocation() {
        when(cachePort.getAtms(any(), any())).thenReturn(Optional.empty());
        when(directoryPort.listAtms()).thenReturn(List.of(
                sample("ATM-001", 25.2048, 55.2708),
                sample("ATM-002", 24.4539, 54.3773)
        ));

        AtmListResult result = service.listAtms(new GetAtmsQuery("ix-3", 25.2048, 55.2708, 2.0));

        assertThat(result.atms()).extracting(AtmLocation::atmId).containsExactly("ATM-001");
    }

    private static AtmLocation sample(String atmId, double lat, double lon) {
        return new AtmLocation(
                atmId,
                "ATM " + atmId,
                AtmStatus.IN_SERVICE,
                lat,
                lon,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
