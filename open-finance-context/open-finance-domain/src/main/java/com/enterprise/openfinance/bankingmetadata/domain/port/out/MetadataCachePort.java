package com.enterprise.openfinance.bankingmetadata.domain.port.out;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataItemResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataListResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;

import java.time.Instant;
import java.util.Optional;

public interface MetadataCachePort {

    Optional<MetadataQueryResult<TransactionMetadata>> getTransactions(String key, Instant now);

    void putTransactions(String key, MetadataQueryResult<TransactionMetadata> value, Instant expiresAt);

    Optional<MetadataListResult<PartyMetadata>> getParties(String key, Instant now);

    void putParties(String key, MetadataListResult<PartyMetadata> value, Instant expiresAt);

    Optional<MetadataItemResult<AccountSchemeMetadata>> getAccount(String key, Instant now);

    void putAccount(String key, MetadataItemResult<AccountSchemeMetadata> value, Instant expiresAt);

    Optional<MetadataQueryResult<StandingOrderMetadata>> getStandingOrders(String key, Instant now);

    void putStandingOrders(String key, MetadataQueryResult<StandingOrderMetadata> value, Instant expiresAt);
}
