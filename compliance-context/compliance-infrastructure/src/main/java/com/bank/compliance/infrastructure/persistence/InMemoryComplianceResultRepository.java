package com.bank.compliance.infrastructure.persistence;

import com.bank.compliance.domain.ComplianceResult;
import com.bank.compliance.domain.port.out.ComplianceResultRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryComplianceResultRepository implements ComplianceResultRepository {
    private final Map<String, ComplianceResult> byTransactionId = new ConcurrentHashMap<>();

    @Override
    public ComplianceResult save(ComplianceResult result) {
        byTransactionId.put(result.getTransactionId(), result);
        return result;
    }

    @Override
    public Optional<ComplianceResult> findByTransactionId(String transactionId) {
        return Optional.ofNullable(byTransactionId.get(transactionId));
    }
}
