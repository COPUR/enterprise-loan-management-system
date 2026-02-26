package com.bank.compliance.domain.port.out;

import com.bank.compliance.domain.ComplianceResult;

import java.util.Optional;

public interface ComplianceResultRepository {
    ComplianceResult save(ComplianceResult result);
    Optional<ComplianceResult> findByTransactionId(String transactionId);
}
