package com.enterprise.openfinance.uc11.domain.port.out;

import com.enterprise.openfinance.uc11.domain.model.FxRateSnapshot;

import java.time.Instant;
import java.util.Optional;

public interface FxRatePort {

    Optional<FxRateSnapshot> findRate(String pair, Instant now);
}
