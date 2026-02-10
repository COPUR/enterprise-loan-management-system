package com.enterprise.openfinance.businessfinancialdata.application;

import com.enterprise.openfinance.businessfinancialdata.domain.exception.ForbiddenException;
import com.enterprise.openfinance.businessfinancialdata.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateConsentContext;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTreasurySettings;
import com.enterprise.openfinance.businessfinancialdata.domain.port.in.CorporateTreasuryUseCase;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateAccountReadPort;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateBalanceReadPort;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateCachePort;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateConsentPort;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateTransactionReadPort;
import com.enterprise.openfinance.businessfinancialdata.domain.query.GetCorporateBalancesQuery;
import com.enterprise.openfinance.businessfinancialdata.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.businessfinancialdata.domain.query.ListCorporateAccountsQuery;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CorporateTreasuryService implements CorporateTreasuryUseCase {

    private final CorporateConsentPort consentPort;
    private final CorporateAccountReadPort accountReadPort;
    private final CorporateBalanceReadPort balanceReadPort;
    private final CorporateTransactionReadPort transactionReadPort;
    private final CorporateCachePort cachePort;
    private final CorporateTreasurySettings settings;
    private final Clock clock;

    public CorporateTreasuryService(
            CorporateConsentPort consentPort,
            CorporateAccountReadPort accountReadPort,
            CorporateBalanceReadPort balanceReadPort,
            CorporateTransactionReadPort transactionReadPort,
            CorporateCachePort cachePort,
            CorporateTreasurySettings settings,
            Clock clock
    ) {
        this.consentPort = consentPort;
        this.accountReadPort = accountReadPort;
        this.balanceReadPort = balanceReadPort;
        this.transactionReadPort = transactionReadPort;
        this.cachePort = cachePort;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public CorporateAccountListResult listAccounts(ListCorporateAccountsQuery query) {
        CorporateConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadAccounts");
        Instant now = Instant.now(clock);
        String cacheKey = "accounts:" + query.consentId() + ':' + query.resolveIncludeVirtual() + ':' + query.masterAccountId();

        var cached = cachePort.getAccounts(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<CorporateAccountSnapshot> accounts = accountReadPort.findByCorporateId(consent.corporateId()).stream()
                .filter(account -> consent.allowsAccount(account.accountId()))
                .toList();

        String masterAccountId = query.masterAccountId();
        if (masterAccountId != null) {
            ensureAccountAccess(consent, masterAccountId);
            accounts = accounts.stream()
                    .filter(account -> masterAccountId.equals(account.accountId()) || masterAccountId.equals(account.masterAccountId()))
                    .toList();
        }

        if (!query.resolveIncludeVirtual()) {
            accounts = accounts.stream().filter(account -> !account.virtual()).toList();
        }

        CorporateAccountListResult result = new CorporateAccountListResult(accounts, false);
        cachePort.putAccounts(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }

    @Override
    public CorporateBalanceListResult getBalances(GetCorporateBalancesQuery query) {
        CorporateConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadBalances");
        ensureAccountAccess(consent, query.masterAccountId());

        Instant now = Instant.now(clock);
        String cacheKey = "balances:" + query.consentId() + ':' + query.masterAccountId();

        var cached = cachePort.getBalances(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<CorporateBalanceSnapshot> balances = balanceReadPort.findByMasterAccountId(query.masterAccountId());
        if (balances.isEmpty()) {
            throw new ResourceNotFoundException("Balances not found");
        }

        CorporateBalanceListResult result = new CorporateBalanceListResult(balances, false, consent.isRestricted());
        cachePort.putBalances(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }

    @Override
    public CorporatePagedResult<CorporateTransactionSnapshot> getTransactions(GetCorporateTransactionsQuery query) {
        CorporateConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadTransactions");

        Set<String> accountIds = resolveTargetAccounts(consent, query.accountId());
        int page = query.resolvePage();
        int pageSize = query.resolvePageSize(settings.defaultPageSize(), settings.maxPageSize());

        Instant now = Instant.now(clock);
        String cacheKey = "transactions:" + query.consentId() + ':' + query.accountId() + ':' + query.fromBookingDateTime()
                + ':' + query.toBookingDateTime() + ':' + page + ':' + pageSize;

        var cached = cachePort.getTransactions(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<CorporateTransactionSnapshot> filtered = transactionReadPort.findByAccountIds(accountIds).stream()
                .filter(tx -> query.fromBookingDateTime() == null || !tx.bookingDateTime().isBefore(query.fromBookingDateTime()))
                .filter(tx -> query.toBookingDateTime() == null || !tx.bookingDateTime().isAfter(query.toBookingDateTime()))
                .sorted(Comparator.comparing(CorporateTransactionSnapshot::bookingDateTime).reversed())
                .toList();

        CorporatePagedResult<CorporateTransactionSnapshot> result = paginate(filtered, page, pageSize).withCacheHit(false);
        cachePort.putTransactions(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }

    private CorporateConsentContext validateConsent(String consentId, String tppId, String requiredScope) {
        Instant now = Instant.now(clock);
        CorporateConsentContext consent = consentPort.findById(consentId)
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

    private static void ensureAccountAccess(CorporateConsentContext consent, String accountId) {
        if (!consent.allowsAccount(accountId)) {
            throw new ForbiddenException("Resource not linked to consent");
        }
    }

    private static Set<String> resolveTargetAccounts(CorporateConsentContext consent, String accountId) {
        if (accountId != null) {
            ensureAccountAccess(consent, accountId);
            return Set.of(accountId);
        }
        return new LinkedHashSet<>(consent.accountIds());
    }

    private static <T> CorporatePagedResult<T> paginate(List<T> source, int page, int pageSize) {
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= source.size()) {
            return new CorporatePagedResult<>(List.of(), page, pageSize, source.size(), false);
        }

        int toIndex = Math.min(source.size(), fromIndex + pageSize);
        return new CorporatePagedResult<>(source.subList(fromIndex, toIndex), page, pageSize, source.size(), false);
    }
}
