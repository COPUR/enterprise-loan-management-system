package com.enterprise.openfinance.uc08.infrastructure.persistence;

import com.enterprise.openfinance.uc08.domain.model.BulkFile;
import com.enterprise.openfinance.uc08.domain.port.out.BulkFilePort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBulkFileAdapter implements BulkFilePort {

    private final Map<String, BulkFile> data = new ConcurrentHashMap<>();

    @Override
    public BulkFile save(BulkFile file) {
        data.put(file.fileId(), file);
        return file;
    }

    @Override
    public Optional<BulkFile> findById(String fileId) {
        return Optional.ofNullable(data.get(fileId));
    }
}
