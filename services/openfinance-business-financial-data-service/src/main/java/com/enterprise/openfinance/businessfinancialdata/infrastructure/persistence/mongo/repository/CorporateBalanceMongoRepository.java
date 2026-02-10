package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateBalanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CorporateBalanceMongoRepository extends MongoRepository<CorporateBalanceDocument, String> {

    List<CorporateBalanceDocument> findByMasterAccountId(String masterAccountId);
}
