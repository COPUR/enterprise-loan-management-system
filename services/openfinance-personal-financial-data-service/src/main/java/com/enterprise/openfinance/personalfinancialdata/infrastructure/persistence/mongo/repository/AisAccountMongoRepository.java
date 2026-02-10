package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository;

import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisAccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AisAccountMongoRepository extends MongoRepository<AisAccountDocument, String> {

    List<AisAccountDocument> findByPsuId(String psuId);
}

