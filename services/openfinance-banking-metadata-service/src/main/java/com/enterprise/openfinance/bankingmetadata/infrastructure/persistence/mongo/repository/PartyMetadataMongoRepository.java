package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.PartyMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PartyMetadataMongoRepository extends MongoRepository<PartyMetadataDocument, String> {

    List<PartyMetadataDocument> findByAccountId(String accountId);
}

