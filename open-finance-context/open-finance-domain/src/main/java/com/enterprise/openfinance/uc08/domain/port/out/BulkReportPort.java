package com.enterprise.openfinance.uc08.domain.port.out;

import com.enterprise.openfinance.uc08.domain.model.BulkFileReport;

import java.util.Optional;

public interface BulkReportPort {

    BulkFileReport save(BulkFileReport report);

    Optional<BulkFileReport> findByFileId(String fileId);
}
