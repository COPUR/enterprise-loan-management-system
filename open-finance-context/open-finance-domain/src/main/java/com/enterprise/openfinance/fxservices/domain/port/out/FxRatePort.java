package com.enterprise.openfinance.fxservices.domain.port.out;

import com.enterprise.openfinance.fxservices.domain.model.FxRateSnapshot;

import java.time.Instant;
import java.util.Optional;

public interface FxRatePort {

    Optional<FxRateSnapshot> findRate(String pair, Instant now);
}
