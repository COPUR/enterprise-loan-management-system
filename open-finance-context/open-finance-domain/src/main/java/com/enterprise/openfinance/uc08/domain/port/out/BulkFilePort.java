package com.enterprise.openfinance.uc08.domain.port.out;

import com.enterprise.openfinance.uc08.domain.model.BulkFile;

import java.util.Optional;

public interface BulkFilePort {

    BulkFile save(BulkFile file);

    Optional<BulkFile> findById(String fileId);
}
