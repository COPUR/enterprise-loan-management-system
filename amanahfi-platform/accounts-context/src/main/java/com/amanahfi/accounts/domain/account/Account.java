package com.amanahfi.accounts.domain.account;

import com.amanahfi.shared.domain.money.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Account Aggregate Root
 * 
 * Represents a financial account in the AmanahFi platform supporting:
 * - Multi-currency wallets (AED, USD, EUR, etc.)
 * - CBDC (Central Bank Digital Currency) - UAE Digital Dirham
 * - Stablecoins (USDC, USDT, etc.)
 * - Islamic banking compliance
 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    private String accountId;

    @NotBlank
    private String customerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @NotBlank
    private String currency;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "balance_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "balance_currency"))
    })
    private Money balance;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Islamic Banking Properties
    private boolean islamicCompliant = false;

    // Digital Currency Properties
    private boolean cbdcEnabled = false;
    private boolean stablecoinEnabled = false;
    private String stablecoinType;

    // Account Control
    private String freezeReason;

    // Transactions
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Domain Events
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Creates a new account with specified type and currency
     */
    public static Account create(String customerId, AccountType accountType, String currency) {
        Account account = new Account();
        account.accountId = generateAccountId();
        account.customerId = validateCustomerId(customerId);
        account.accountType = accountType;
        account.currency = validateCurrency(currency);
        account.balance = Money.zero(currency);
        account.status = AccountStatus.ACTIVE;
        account.createdAt = LocalDateTime.now();
        account.updatedAt = LocalDateTime.now();
        
        // Add domain event
        account.addDomainEvent(new AccountCreatedEvent(account.accountId, customerId, accountType, currency));
        
        return account;
    }

    /**
     * Creates a CBDC wallet for UAE Digital Dirham
     */
    public static Account createCbdcWallet(String customerId) {
        Account account = create(customerId, AccountType.CBDC_WALLET, "AED");
        account.cbdcEnabled = true;
        
        account.addDomainEvent(new CbdcWalletCreatedEvent(account.accountId, customerId));
        
        return account;
    }

    /**
     * Creates a stablecoin wallet
     */
    public static Account createStablecoinWallet(String customerId, String stablecoinType) {
        Account account = create(customerId, AccountType.STABLECOIN_WALLET, "USD"); // Most stablecoins are USD-pegged
        account.stablecoinEnabled = true;
        account.stablecoinType = stablecoinType;
        
        account.addDomainEvent(new StablecoinWalletCreatedEvent(account.accountId, customerId, stablecoinType));
        
        return account;
    }

    /**
     * Sets Islamic banking compliance
     */
    public Account withIslamicCompliance(boolean compliant) {
        this.islamicCompliant = compliant;
        this.updatedAt = LocalDateTime.now();
        
        if (compliant) {
            addDomainEvent(new AccountMarkedIslamicCompliantEvent(accountId));
        }
        
        return this;
    }

    /**
     * Deposits money into the account
     */
    public void deposit(Money amount, String description) {
        validateAccountOperational();
        validateCurrencyMatch(amount);
        
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
        
        Transaction transaction = new Transaction(TransactionType.DEPOSIT, amount, description);
        transaction.assignToAccount(this);
        this.transactions.add(transaction);
        
        addDomainEvent(new MoneyDepositedEvent(accountId, amount, description));
    }

    /**
     * Withdraws money from the account
     */
    public void withdraw(Money amount, String description) {
        validateAccountOperational();
        validateCurrencyMatch(amount);
        validateSufficientFunds(amount);
        
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
        
        Transaction transaction = new Transaction(TransactionType.WITHDRAWAL, amount, description);
        transaction.assignToAccount(this);
        this.transactions.add(transaction);
        
        addDomainEvent(new MoneyWithdrawnEvent(accountId, amount, description));
    }

    /**
     * Transfers money to another account
     */
    public void transferTo(Account targetAccount, Money amount, String description) {
        validateAccountOperational();
        validateCurrencyMatch(amount);
        validateSufficientFunds(amount);
        
        if (!this.customerId.equals(targetAccount.customerId)) {
            throw new IllegalArgumentException("Transfers only allowed between accounts of the same customer");
        }
        
        if (!this.currency.equals(targetAccount.currency)) {
            throw new IllegalArgumentException("Currency mismatch between source and target accounts");
        }
        
        // Debit from source
        this.balance = this.balance.subtract(amount);
        Transaction debitTransaction = new Transaction(TransactionType.TRANSFER_OUT, amount, description);
        debitTransaction.assignToAccount(this);
        this.transactions.add(debitTransaction);
        
        // Credit to target
        targetAccount.balance = targetAccount.balance.add(amount);
        Transaction creditTransaction = new Transaction(TransactionType.TRANSFER_IN, amount, description);
        creditTransaction.assignToAccount(targetAccount);
        targetAccount.transactions.add(creditTransaction);
        
        this.updatedAt = LocalDateTime.now();
        targetAccount.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new MoneyTransferredEvent(accountId, targetAccount.accountId, amount, description));
    }

    /**
     * Freezes the account
     */
    public void freeze(String reason) {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot freeze a closed account");
        }
        
        this.status = AccountStatus.FROZEN;
        this.freezeReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new AccountFrozenEvent(accountId, reason));
    }

    /**
     * Unfreezes the account
     */
    public void unfreeze(String reason) {
        if (status != AccountStatus.FROZEN) {
            throw new IllegalStateException("Only frozen accounts can be unfrozen");
        }
        
        this.status = AccountStatus.ACTIVE;
        this.freezeReason = null;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new AccountUnfrozenEvent(accountId, reason));
    }

    // Business Logic Methods

    public boolean isCbdcEnabled() {
        return cbdcEnabled;
    }

    public boolean isStablecoinEnabled() {
        return stablecoinEnabled;
    }

    public boolean canSettleInstantly() {
        return cbdcEnabled && status == AccountStatus.ACTIVE;
    }

    public boolean isIslamicCompliant() {
        return islamicCompliant;
    }

    public boolean canEarnInterest() {
        return !islamicCompliant;
    }

    public boolean canParticipateProfitSharing() {
        return islamicCompliant;
    }

    public boolean canInvestInConventionalProducts() {
        return !islamicCompliant;
    }

    public boolean canInvestInShariahCompliantProducts() {
        return islamicCompliant;
    }

    // Validation Methods

    private void validateAccountOperational() {
        if (status == AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account is frozen: " + freezeReason);
        }
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is closed");
        }
    }

    private void validateCurrencyMatch(Money amount) {
        if (!this.currency.equals(amount.getCurrency())) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: account currency %s, transaction currency %s", 
                    this.currency, amount.getCurrency())
            );
        }
    }

    private void validateSufficientFunds(Money amount) {
        if (this.balance.isLessThan(amount)) {
            throw new InsufficientFundsException(
                String.format("Insufficient funds: available %s, requested %s", 
                    this.balance, amount)
            );
        }
    }

    private static String validateCustomerId(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        return customerId;
    }

    private static String validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter ISO code");
        }
        return currency.toUpperCase();
    }

    private static String generateAccountId() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}