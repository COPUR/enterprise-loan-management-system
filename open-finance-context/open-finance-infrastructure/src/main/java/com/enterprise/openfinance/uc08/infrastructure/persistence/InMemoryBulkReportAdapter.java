package com.enterprise.openfinance.uc08.infrastructure.persistence;

import com.enterprise.openfinance.uc08.domain.model.BulkFileReport;
import com.enterprise.openfinance.uc08.domain.port.out.BulkReportPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBulkReportAdapter implements BulkReportPort {

    private final Map<String, BulkFileReport> data = new ConcurrentHashMap<>();

    @Override
    public BulkFileReport save(BulkFileReport report) {
        data.put(report.fileId(), report);
        return report;
    }

    @Override
    public Optional<BulkFileReport> findByFileId(String fileId) {
        return Optional.ofNullable(data.get(fileId));
    }
}
