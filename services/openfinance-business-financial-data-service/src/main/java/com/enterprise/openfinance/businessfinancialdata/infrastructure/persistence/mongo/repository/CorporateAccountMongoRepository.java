package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateAccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CorporateAccountMongoRepository extends MongoRepository<CorporateAccountDocument, String> {

    List<CorporateAccountDocument> findByCorporateId(String corporateId);
}
