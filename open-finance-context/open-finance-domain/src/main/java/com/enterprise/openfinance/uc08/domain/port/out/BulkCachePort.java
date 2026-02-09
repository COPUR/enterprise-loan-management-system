package com.enterprise.openfinance.uc08.domain.port.out;

import com.enterprise.openfinance.uc08.domain.model.BulkFileReport;

import java.time.Instant;
import java.util.Optional;

public interface BulkCachePort {

    Optional<BulkFileReport> getReport(String key, Instant now);

    void putReport(String key, BulkFileReport report, Instant expiresAt);
}
