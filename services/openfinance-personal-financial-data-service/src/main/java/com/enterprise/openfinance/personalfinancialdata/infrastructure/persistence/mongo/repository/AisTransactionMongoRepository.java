package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisTransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AisTransactionMongoRepository extends MongoRepository<AisTransactionDocument, String> {

    List<AisTransactionDocument> findByAccountId(String accountId);
}

