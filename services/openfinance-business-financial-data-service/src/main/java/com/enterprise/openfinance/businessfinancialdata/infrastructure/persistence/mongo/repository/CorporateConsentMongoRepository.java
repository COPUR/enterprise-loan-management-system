package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateConsentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CorporateConsentMongoRepository extends MongoRepository<CorporateConsentDocument, String> {
}
