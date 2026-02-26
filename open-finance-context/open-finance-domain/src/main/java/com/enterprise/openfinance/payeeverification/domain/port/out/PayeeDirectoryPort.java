package com.enterprise.openfinance.payeeverification.domain.port.out;

import com.enterprise.openfinance.payeeverification.domain.model.DirectoryEntry;

import java.util.Optional;

public interface PayeeDirectoryPort {

    Optional<DirectoryEntry> findBySchemeAndIdentification(String schemeName, String identification);
}
