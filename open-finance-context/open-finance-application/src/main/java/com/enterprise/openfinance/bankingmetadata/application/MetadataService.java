package com.enterprise.openfinance.bankingmetadata.application;

import com.enterprise.openfinance.bankingmetadata.domain.exception.ForbiddenException;
import com.enterprise.openfinance.bankingmetadata.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataConsentContext;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataItemResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataListResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataSettings;
import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.in.MetadataUseCase;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.AccountMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.MetadataCachePort;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.MetadataConsentPort;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.PartyMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.StandingOrderMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.TransactionMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetAccountMetadataQuery;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetPartiesMetadataQuery;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetStandingOrdersMetadataQuery;
import com.enterprise.openfinance.bankingmetadata.domain.query.GetTransactionMetadataQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MetadataService implements MetadataUseCase {

    private final MetadataConsentPort consentPort;
    private final TransactionMetadataReadPort transactionReadPort;
    private final PartyMetadataReadPort partyReadPort;
    private final AccountMetadataReadPort accountReadPort;
    private final StandingOrderMetadataReadPort standingOrderReadPort;
    private final MetadataCachePort cachePort;
    private final MetadataSettings settings;
    private final Clock clock;

    public MetadataService(
            MetadataConsentPort consentPort,
            TransactionMetadataReadPort transactionReadPort,
            PartyMetadataReadPort partyReadPort,
            AccountMetadataReadPort accountReadPort,
            StandingOrderMetadataReadPort standingOrderReadPort,
            MetadataCachePort cachePort,
            MetadataSettings settings,
            Clock clock
    ) {
        this.consentPort = consentPort;
        this.transactionReadPort = transactionReadPort;
        this.partyReadPort = partyReadPort;
        this.accountReadPort = accountReadPort;
        this.standingOrderReadPort = standingOrderReadPort;
        this.cachePort = cachePort;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public MetadataQueryResult<TransactionMetadata> getTransactions(GetTransactionMetadataQuery query) {
        MetadataConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadTransactions");
        ensureAccountAccess(consent, query.accountId());

        int page = query.resolvePage();
        int pageSize = query.resolvePageSize(settings.defaultPageSize(), settings.maxPageSize());
        Instant now = Instant.now(clock);
        String cacheKey = "transactions:" + query.consentId() + ':' + query.accountId() + ':' + query.fromBookingDateTime()
                + ':' + query.toBookingDateTime() + ':' + page + ':' + pageSize;

        var cached = cachePort.getTransactions(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<TransactionMetadata> filtered = transactionReadPort.findByAccountId(query.accountId()).stream()
                .filter(tx -> query.fromBookingDateTime() == null || !tx.bookingDateTime().isBefore(query.fromBookingDateTime()))
                .filter(tx -> query.toBookingDateTime() == null || !tx.bookingDateTime().isAfter(query.toBookingDateTime()))
                .sorted(Comparator.comparing(TransactionMetadata::bookingDateTime).reversed())
                .toList();

        MetadataQueryResult<TransactionMetadata> pageResult = paginate(filtered, page, pageSize).withCacheHit(false);
        cachePort.putTransactions(cacheKey, pageResult, now.plus(settings.cacheTtl()));
        return pageResult;
    }

    @Override
    public MetadataListResult<PartyMetadata> getParties(GetPartiesMetadataQuery query) {
        MetadataConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadParties");
        ensureAccountAccess(consent, query.accountId());

        Instant now = Instant.now(clock);
        String cacheKey = "parties:" + query.consentId() + ':' + query.accountId();
        var cached = cachePort.getParties(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<PartyMetadata> parties = partyReadPort.findByAccountId(query.accountId());
        if (parties.isEmpty()) {
            throw new ResourceNotFoundException("Parties metadata not found");
        }

        MetadataListResult<PartyMetadata> result = new MetadataListResult<>(parties, false);
        cachePort.putParties(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }

    @Override
    public MetadataItemResult<AccountSchemeMetadata> getAccountMetadata(GetAccountMetadataQuery query) {
        MetadataConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadMetadata");
        ensureAccountAccess(consent, query.accountId());

        Instant now = Instant.now(clock);
        String cacheKey = "account:" + query.consentId() + ':' + query.accountId();
        var cached = cachePort.getAccount(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        AccountSchemeMetadata accountMetadata = accountReadPort.findByAccountId(query.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account metadata not found"));

        MetadataItemResult<AccountSchemeMetadata> result = new MetadataItemResult<>(accountMetadata, false);
        cachePort.putAccount(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }

    @Override
    public MetadataQueryResult<StandingOrderMetadata> getStandingOrders(GetStandingOrdersMetadataQuery query) {
        MetadataConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadStandingOrders");
        if (query.accountId() != null) {
            ensureAccountAccess(consent, query.accountId());
        }

        int page = query.resolvePage();
        int pageSize = query.resolvePageSize(settings.defaultPageSize(), settings.maxPageSize());
        Instant now = Instant.now(clock);
        String cacheKey = "standing-orders:" + query.consentId() + ':' + query.accountId() + ':' + page + ':' + pageSize;

        var cached = cachePort.getStandingOrders(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<StandingOrderMetadata> all = query.accountId() == null
                ? standingOrderReadPort.findAll()
                : standingOrderReadPort.findByAccountId(query.accountId());

        List<StandingOrderMetadata> sorted = all.stream()
                .sorted(Comparator.comparing(StandingOrderMetadata::firstPaymentDate))
                .toList();

        MetadataQueryResult<StandingOrderMetadata> pageResult = paginate(sorted, page, pageSize).withCacheHit(false);
        cachePort.putStandingOrders(cacheKey, pageResult, now.plus(settings.cacheTtl()));
        return pageResult;
    }

    private MetadataConsentContext validateConsent(String consentId, String tppId, String requiredScope) {
        Instant now = Instant.now(clock);
        MetadataConsentContext consent = consentPort.findById(consentId)
                .orElseThrow(() -> new ForbiddenException("Consent not found"));

        if (!consent.belongsToTpp(tppId)) {
            throw new ForbiddenException("Consent participant mismatch");
        }
        if (!consent.isActive(now)) {
            throw new ForbiddenException("Consent expired");
        }
        if (!consent.hasScope(requiredScope)) {
            throw new ForbiddenException("Required scope missing: " + requiredScope);
        }

        return consent;
    }

    private static void ensureAccountAccess(MetadataConsentContext consent, String accountId) {
        if (!consent.allowsAccount(accountId)) {
            throw new ForbiddenException("Resource not linked to consent");
        }
    }

    private static <T> MetadataQueryResult<T> paginate(List<T> source, int page, int pageSize) {
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= source.size()) {
            return new MetadataQueryResult<>(List.of(), page, pageSize, source.size(), false);
        }

        int toIndex = Math.min(source.size(), fromIndex + pageSize);
        return new MetadataQueryResult<>(source.subList(fromIndex, toIndex), page, pageSize, source.size(), false);
    }
}
