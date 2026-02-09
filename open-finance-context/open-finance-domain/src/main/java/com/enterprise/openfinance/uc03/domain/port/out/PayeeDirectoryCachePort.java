package com.enterprise.openfinance.uc03.domain.port.out;

import com.enterprise.openfinance.uc03.domain.model.DirectoryEntry;

import java.time.Instant;
import java.util.Optional;

public interface PayeeDirectoryCachePort {

    Optional<DirectoryEntry> get(String key, Instant now);

    void put(String key, DirectoryEntry entry, Instant expiresAt);
}
