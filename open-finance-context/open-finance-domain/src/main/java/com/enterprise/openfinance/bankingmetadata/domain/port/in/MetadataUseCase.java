package com.enterprise.openfinance.bankingmetadata.domain.port.in;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataItemResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataListResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetAccountMetadataQuery;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetPartiesMetadataQuery;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetStandingOrdersMetadataQuery;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetTransactionMetadataQuery;

public interface MetadataUseCase {

    MetadataQueryResult<TransactionMetadata> getTransactions(GetTransactionMetadataQuery query);

    MetadataListResult<PartyMetadata> getParties(GetPartiesMetadataQuery query);

    MetadataItemResult<AccountSchemeMetadata> getAccountMetadata(GetAccountMetadataQuery query);

    MetadataQueryResult<StandingOrderMetadata> getStandingOrders(GetStandingOrdersMetadataQuery query);
}
