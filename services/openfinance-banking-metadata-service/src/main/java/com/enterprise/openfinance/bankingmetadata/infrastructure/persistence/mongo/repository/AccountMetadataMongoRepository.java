package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.AccountMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountMetadataMongoRepository extends MongoRepository<AccountMetadataDocument, String> {
}

