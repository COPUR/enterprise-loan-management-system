package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateTransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;

public interface CorporateTransactionMongoRepository extends MongoRepository<CorporateTransactionDocument, String> {

    List<CorporateTransactionDocument> findByAccountIdIn(Set<String> accountIds);
}
