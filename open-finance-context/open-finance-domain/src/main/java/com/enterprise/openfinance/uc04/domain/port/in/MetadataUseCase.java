package com.enterprise.openfinance.uc04.domain.port.in;

import com.enterprise.openfinance.uc04.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.uc04.domain.model.MetadataItemResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataListResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.uc04.domain.model.PartyMetadata;
import com.enterprise.openfinance.uc04.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.uc04.domain.model.TransactionMetadata;
import com.enterprise.openfinance.uc04.domain.query.GetAccountMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetPartiesMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetStandingOrdersMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetTransactionMetadataQuery;

public interface MetadataUseCase {

    MetadataQueryResult<TransactionMetadata> getTransactions(GetTransactionMetadataQuery query);

    MetadataListResult<PartyMetadata> getParties(GetPartiesMetadataQuery query);

    MetadataItemResult<AccountSchemeMetadata> getAccountMetadata(GetAccountMetadataQuery query);

    MetadataQueryResult<StandingOrderMetadata> getStandingOrders(GetStandingOrdersMetadataQuery query);
}
