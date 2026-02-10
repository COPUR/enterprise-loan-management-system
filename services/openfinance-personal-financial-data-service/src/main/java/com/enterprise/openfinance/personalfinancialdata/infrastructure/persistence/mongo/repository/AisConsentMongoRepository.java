package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisConsentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AisConsentMongoRepository extends MongoRepository<AisConsentDocument, String> {
}

