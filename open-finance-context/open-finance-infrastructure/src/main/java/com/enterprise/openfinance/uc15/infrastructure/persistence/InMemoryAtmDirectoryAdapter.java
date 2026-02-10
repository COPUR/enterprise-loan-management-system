package com.enterprise.openfinance.uc15.infrastructure.persistence;

import com.enterprise.openfinance.uc15.domain.model.AtmLocation;
import com.enterprise.openfinance.uc15.domain.model.AtmStatus;
import com.enterprise.openfinance.uc15.domain.port.out.AtmDirectoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class InMemoryAtmDirectoryAdapter implements AtmDirectoryPort {

    private final List<AtmLocation> seed = List.of(
            new AtmLocation(
                    "ATM-001",
                    "Downtown ATM",
                    AtmStatus.IN_SERVICE,
                    25.2048,
                    55.2708,
                    "Sheikh Zayed Road",
                    "Dubai",
                    "AE",
                    "Full",
                    List.of("CashWithdrawal", "BalanceInquiry"),
                    Instant.parse("2026-02-10T00:00:00Z")
            ),
            new AtmLocation(
                    "ATM-002",
                    "Marina ATM",
                    AtmStatus.IN_SERVICE,
                    25.0865,
                    55.1390,
                    "Dubai Marina",
                    "Dubai",
                    "AE",
                    "Full",
                    List.of("CashWithdrawal"),
                    Instant.parse("2026-02-10T00:00:00Z")
            ),
            new AtmLocation(
                    "ATM-003",
                    "Abu Dhabi Mall ATM",
                    AtmStatus.OUT_OF_SERVICE,
                    24.4539,
                    54.3773,
                    "Abu Dhabi Mall",
                    "Abu Dhabi",
                    "AE",
                    "Limited",
                    List.of("CashWithdrawal"),
                    Instant.parse("2026-02-10T00:00:00Z")
            )
    );

    @Override
    public List<AtmLocation> listAtms() {
        return seed;
    }
}
