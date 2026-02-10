package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisBalanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AisBalanceMongoRepository extends MongoRepository<AisBalanceDocument, String> {

    List<AisBalanceDocument> findByAccountId(String accountId);
}

