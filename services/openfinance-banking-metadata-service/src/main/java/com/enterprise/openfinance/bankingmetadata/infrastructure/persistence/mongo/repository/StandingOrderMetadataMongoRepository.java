package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.StandingOrderMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StandingOrderMetadataMongoRepository extends MongoRepository<StandingOrderMetadataDocument, String> {

    List<StandingOrderMetadataDocument> findByAccountId(String accountId);
}

