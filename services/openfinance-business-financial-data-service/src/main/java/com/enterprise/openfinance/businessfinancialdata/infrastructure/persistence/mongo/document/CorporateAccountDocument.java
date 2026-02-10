package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("corporate_accounts")
public record CorporateAccountDocument(
        @Id String id,
        String corporateId,
        String masterAccountId,
        String iban,
        String currency,
        String status,
        String accountType,
        boolean virtual
) {
}
