package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.MetadataConsentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MetadataConsentMongoRepository extends MongoRepository<MetadataConsentDocument, String> {
}

