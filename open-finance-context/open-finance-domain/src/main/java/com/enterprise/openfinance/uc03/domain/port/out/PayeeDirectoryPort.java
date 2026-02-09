package com.enterprise.openfinance.uc03.domain.port.out;

import com.enterprise.openfinance.uc03.domain.model.DirectoryEntry;

import java.util.Optional;

public interface PayeeDirectoryPort {

    Optional<DirectoryEntry> findBySchemeAndIdentification(String schemeName, String identification);
}
