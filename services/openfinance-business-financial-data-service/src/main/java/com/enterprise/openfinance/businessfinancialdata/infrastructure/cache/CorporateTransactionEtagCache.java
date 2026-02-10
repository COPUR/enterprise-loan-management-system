package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import java.util.Optional;

public interface CorporateTransactionEtagCache {

    Optional<String> get(String requestSignature);

    void put(String requestSignature, String etag);
}
