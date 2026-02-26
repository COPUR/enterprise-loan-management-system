package com.enterprise.openfinance.accountinformation.application;

import com.enterprise.openfinance.accountinformation.domain.exception.ForbiddenException;
import com.enterprise.openfinance.accountinformation.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.accountinformation.domain.model.AccountListResult;
import com.enterprise.openfinance.accountinformation.domain.model.AccountSnapshot;
import com.enterprise.openfinance.accountinformation.domain.model.AisConsentContext;
import com.enterprise.openfinance.accountinformation.domain.model.AisSettings;
import com.enterprise.openfinance.accountinformation.domain.model.BalanceListResult;
import com.enterprise.openfinance.accountinformation.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.accountinformation.domain.model.PagedResult;
import com.enterprise.openfinance.accountinformation.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.accountinformation.domain.port.in.AccountInformationUseCase;
import com.enterprise.openfinance.accountinformation.domain.port.out.AccountReadPort;
import com.enterprise.openfinance.accountinformation.domain.port.out.AisCachePort;
import com.enterprise.openfinance.accountinformation.domain.port.out.AisConsentPort;
import com.enterprise.openfinance.accountinformation.domain.port.out.BalanceReadPort;
import com.enterprise.openfinance.accountinformation.domain.port.out.TransactionReadPort;
import com.enterprise.openfinance.accountinformation.domain.query.GetAccountQuery;
import com.enterprise.openfinance.accountinformation.domain.query.GetBalancesQuery;
import com.enterprise.openfinance.accountinformation.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.accountinformation.domain.query.ListAccountsQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountInformationService implements AccountInformationUseCase {

    private final AisConsentPort consentPort;
    private final AccountReadPort accountReadPort;
    private final BalanceReadPort balanceReadPort;
    private final TransactionReadPort transactionReadPort;
    private final AisCachePort cachePort;
    private final AisSettings settings;
    private final Clock clock;

    public AccountInformationService(
            AisConsentPort consentPort,
            AccountReadPort accountReadPort,
            BalanceReadPort balanceReadPort,
            TransactionReadPort transactionReadPort,
            AisCachePort cachePort,
            AisSettings settings,
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
    public AccountListResult listAccounts(ListAccountsQuery query) {
        AisConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadAccounts");
        Instant now = Instant.now(clock);
        String cacheKey = "accounts:" + query.consentId() + ":" + query.tppId();

        var cached = cachePort.getAccounts(cacheKey, now);
        if (cached.isPresent()) {
            return new AccountListResult(filterAllowedAccounts(cached.orElseThrow(), consent), true);
        }

        List<AccountSnapshot> accounts = filterAllowedAccounts(accountReadPort.findByPsuId(consent.psuId()), consent);
        cachePort.putAccounts(cacheKey, accounts, now.plus(settings.cacheTtl()));
        return new AccountListResult(accounts, false);
    }

    @Override
    public AccountSnapshot getAccount(GetAccountQuery query) {
        AisConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadAccounts");
        ensureAccountAccess(consent, query.accountId());

        AccountSnapshot account = accountReadPort.findById(query.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!consent.psuId().equals(account.psuId())) {
            throw new ForbiddenException("Resource not linked to consent");
        }
        return account;
    }

    @Override
    public BalanceListResult getBalances(GetBalancesQuery query) {
        AisConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadBalances");
        ensureAccountAccess(consent, query.accountId());
        Instant now = Instant.now(clock);
        String cacheKey = "balances:" + query.consentId() + ":" + query.accountId();

        var cached = cachePort.getBalances(cacheKey, now);
        if (cached.isPresent()) {
            return new BalanceListResult(cached.orElseThrow(), true);
        }

        List<BalanceSnapshot> balances = balanceReadPort.findByAccountId(query.accountId());
        cachePort.putBalances(cacheKey, balances, now.plus(settings.cacheTtl()));
        return new BalanceListResult(balances, false);
    }

    @Override
    public PagedResult<TransactionSnapshot> getTransactions(GetTransactionsQuery query) {
        AisConsentContext consent = validateConsent(query.consentId(), query.tppId(), "ReadTransactions");
        ensureAccountAccess(consent, query.accountId());

        List<TransactionSnapshot> filtered = transactionReadPort.findByAccountId(query.accountId()).stream()
                .filter(tx -> query.fromBookingDateTime() == null || !tx.bookingDateTime().isBefore(query.fromBookingDateTime()))
                .filter(tx -> query.toBookingDateTime() == null || !tx.bookingDateTime().isAfter(query.toBookingDateTime()))
                .sorted(Comparator.comparing(TransactionSnapshot::bookingDateTime).reversed())
                .toList();

        int page = query.resolvePage();
        int pageSize = query.resolvePageSize(settings.defaultPageSize(), settings.maxPageSize());
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= filtered.size()) {
            return new PagedResult<>(List.of(), page, pageSize, filtered.size());
        }

        int toIndex = Math.min(filtered.size(), fromIndex + pageSize);
        return new PagedResult<>(filtered.subList(fromIndex, toIndex), page, pageSize, filtered.size());
    }

    private AisConsentContext validateConsent(String consentId, String tppId, String requiredScope) {
        Instant now = Instant.now(clock);
        AisConsentContext consent = consentPort.findById(consentId)
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

    private static void ensureAccountAccess(AisConsentContext consent, String accountId) {
        if (!consent.allowsAccount(accountId)) {
            throw new ForbiddenException("Resource not linked to consent");
        }
    }

    private static List<AccountSnapshot> filterAllowedAccounts(List<AccountSnapshot> accounts, AisConsentContext consent) {
        return accounts.stream()
                .filter(account -> consent.allowsAccount(account.accountId()))
                .toList();
    }
}
