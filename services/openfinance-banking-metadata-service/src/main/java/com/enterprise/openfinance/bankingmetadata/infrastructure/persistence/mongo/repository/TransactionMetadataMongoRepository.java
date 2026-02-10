package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.TransactionMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransactionMetadataMongoRepository extends MongoRepository<TransactionMetadataDocument, String> {

    List<TransactionMetadataDocument> findByAccountId(String accountId);
}

