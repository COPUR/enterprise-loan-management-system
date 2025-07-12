package com.amanahfi.platform.cbdc.domain;

import com.amanahfi.platform.shared.domain.AggregateRoot;
import com.amanahfi.platform.shared.domain.Money;
import com.amanahfi.platform.cbdc.domain.events.*;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Digital Dirham CBDC Aggregate Root
 * Represents UAE Central Bank Digital Currency operations
 */
@Getter
@ToString
public class DigitalDirham extends AggregateRoot<DigitalDirhamId> {
    
    private final String walletId;
    private final WalletType walletType;
    private final String ownerId;
    private Money balance;
    private final String cordaNodeId;
    private final String notaryId;
    private DirhamStatus status;
    private final List<Transaction> transactions;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private Instant lastUpdated;
    
    // Private constructor for aggregate creation
    private DigitalDirham(DigitalDirhamId digitalDirhamId,
                         String walletId,
                         WalletType walletType,
                         String ownerId,
                         Money initialBalance,
                         String cordaNodeId,
                         String notaryId) {
        super(digitalDirhamId);
        this.walletId = Objects.requireNonNull(walletId, "Wallet ID cannot be null");
        this.walletType = Objects.requireNonNull(walletType, "Wallet type cannot be null");
        this.ownerId = Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        this.balance = Objects.requireNonNull(initialBalance, "Initial balance cannot be null");
        this.cordaNodeId = Objects.requireNonNull(cordaNodeId, "Corda node ID cannot be null");
        this.notaryId = Objects.requireNonNull(notaryId, "Notary ID cannot be null");
        this.status = DirhamStatus.ACTIVE;
        this.transactions = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        
        validateInitialBalance();
    }
    
    // Factory method for creating new Digital Dirham wallet
    public static DigitalDirham createWallet(
            DigitalDirhamId digitalDirhamId,
            String walletId,
            WalletType walletType,
            String ownerId,
            Money initialBalance,
            String cordaNodeId,
            String notaryId) {
        
        DigitalDirham digitalDirham = new DigitalDirham(
            digitalDirhamId, walletId, walletType, ownerId, 
            initialBalance, cordaNodeId, notaryId
        );
        
        digitalDirham.raiseEvent(DigitalDirhamCreatedEvent.builder()
            .digitalDirhamId(digitalDirhamId.getValue())
            .walletId(walletId)
            .walletType(walletType)
            .ownerId(ownerId)
            .initialBalance(initialBalance)
            .cordaNodeId(cordaNodeId)
            .notaryId(notaryId)
            .createdAt(digitalDirham.createdAt)
            .build());
            
        return digitalDirham;
    }
    
    // Transfer Digital Dirham to another wallet
    public void transfer(String toWalletId, Money amount, String reference, TransferType transferType) {
        Objects.requireNonNull(toWalletId, "To wallet ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(reference, "Reference cannot be null");
        Objects.requireNonNull(transferType, "Transfer type cannot be null");
        
        validateTransfer(amount, transferType);
        
        // Create transaction
        Transaction transaction = Transaction.builder()
            .transactionId(UUID.randomUUID().toString())
            .fromWalletId(this.walletId)
            .toWalletId(toWalletId)
            .amount(amount)
            .reference(reference)
            .transferType(transferType)
            .status(TransactionStatus.PENDING)
            .createdAt(Instant.now())
            .build();
        
        // Update balance (for outgoing transfer)
        this.balance = this.balance.subtract(amount);
        this.transactions.add(transaction);
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamTransferInitiatedEvent.builder()
            .digitalDirhamId(id.getValue())
            .transactionId(transaction.getTransactionId())
            .fromWalletId(this.walletId)
            .toWalletId(toWalletId)
            .amount(amount)
            .reference(reference)
            .transferType(transferType)
            .newBalance(this.balance)
            .initiatedAt(Instant.now())
            .build());
    }
    
    // Receive Digital Dirham from another wallet
    public void receive(String fromWalletId, Money amount, String reference, String transactionId) {
        Objects.requireNonNull(fromWalletId, "From wallet ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(reference, "Reference cannot be null");
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        
        if (status != DirhamStatus.ACTIVE) {
            throw new IllegalStateException("Cannot receive funds - wallet is " + status);
        }
        
        // Create transaction
        Transaction transaction = Transaction.builder()
            .transactionId(transactionId)
            .fromWalletId(fromWalletId)
            .toWalletId(this.walletId)
            .amount(amount)
            .reference(reference)
            .transferType(TransferType.RECEIVE)
            .status(TransactionStatus.COMPLETED)
            .createdAt(Instant.now())
            .completedAt(Instant.now())
            .build();
        
        // Update balance (for incoming transfer)
        this.balance = this.balance.add(amount);
        this.transactions.add(transaction);
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamReceivedEvent.builder()
            .digitalDirhamId(id.getValue())
            .transactionId(transactionId)
            .fromWalletId(fromWalletId)
            .toWalletId(this.walletId)
            .amount(amount)
            .reference(reference)
            .newBalance(this.balance)
            .receivedAt(Instant.now())
            .build());
    }
    
    // Confirm transaction completion
    public void confirmTransaction(String transactionId, String cordaTransactionHash) {
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        Objects.requireNonNull(cordaTransactionHash, "Corda transaction hash cannot be null");
        
        Transaction transaction = findTransaction(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not pending: " + transactionId);
        }
        
        // Update transaction status
        transaction.confirm(cordaTransactionHash);
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamTransactionConfirmedEvent.builder()
            .digitalDirhamId(id.getValue())
            .transactionId(transactionId)
            .cordaTransactionHash(cordaTransactionHash)
            .finalStatus(transaction.getStatus())
            .confirmedAt(Instant.now())
            .build());
    }
    
    // Freeze wallet (regulatory compliance)
    public void freeze(String reason, String authorizedBy) {
        Objects.requireNonNull(reason, "Freeze reason cannot be null");
        Objects.requireNonNull(authorizedBy, "Authorized by cannot be null");
        
        if (status == DirhamStatus.FROZEN) {
            throw new IllegalStateException("Wallet is already frozen");
        }
        
        this.status = DirhamStatus.FROZEN;
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamFrozenEvent.builder()
            .digitalDirhamId(id.getValue())
            .reason(reason)
            .authorizedBy(authorizedBy)
            .frozenAt(Instant.now())
            .build());
    }
    
    // Unfreeze wallet
    public void unfreeze(String reason, String authorizedBy) {
        Objects.requireNonNull(reason, "Unfreeze reason cannot be null");
        Objects.requireNonNull(authorizedBy, "Authorized by cannot be null");
        
        if (status != DirhamStatus.FROZEN) {
            throw new IllegalStateException("Wallet is not frozen");
        }
        
        this.status = DirhamStatus.ACTIVE;
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamUnfrozenEvent.builder()
            .digitalDirhamId(id.getValue())
            .reason(reason)
            .authorizedBy(authorizedBy)
            .unfrozenAt(Instant.now())
            .build());
    }
    
    // Mint new Digital Dirham (Central Bank only)
    public void mint(Money amount, String authorization) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(authorization, "Authorization cannot be null");
        
        if (walletType != WalletType.CENTRAL_BANK) {
            throw new IllegalStateException("Only Central Bank can mint Digital Dirham");
        }
        
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mint amount must be positive");
        }
        
        this.balance = this.balance.add(amount);
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamMintedEvent.builder()
            .digitalDirhamId(id.getValue())
            .amount(amount)
            .authorization(authorization)
            .newBalance(this.balance)
            .mintedAt(Instant.now())
            .build());
    }
    
    // Burn Digital Dirham (Central Bank only)
    public void burn(Money amount, String authorization) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(authorization, "Authorization cannot be null");
        
        if (walletType != WalletType.CENTRAL_BANK) {
            throw new IllegalStateException("Only Central Bank can burn Digital Dirham");
        }
        
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Burn amount must be positive");
        }
        
        if (balance.getAmount().compareTo(amount.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance for burn operation");
        }
        
        this.balance = this.balance.subtract(amount);
        this.lastUpdated = Instant.now();
        
        raiseEvent(DigitalDirhamBurnedEvent.builder()
            .digitalDirhamId(id.getValue())
            .amount(amount)
            .authorization(authorization)
            .newBalance(this.balance)
            .burnedAt(Instant.now())
            .build());
    }
    
    // Get transaction history
    public List<Transaction> getTransactionHistory() {
        return new ArrayList<>(transactions);
    }
    
    // Get transaction by ID
    public Optional<Transaction> getTransaction(String transactionId) {
        return transactions.stream()
            .filter(t -> t.getTransactionId().equals(transactionId))
            .findFirst();
    }
    
    // Get balance summary
    public BalanceSummary getBalanceSummary() {
        return BalanceSummary.builder()
            .walletId(walletId)
            .ownerId(ownerId)
            .currentBalance(balance)
            .totalTransactions(transactions.size())
            .pendingTransactions(getPendingTransactionCount())
            .lastUpdated(lastUpdated)
            .build();
    }
    
    // Private helper methods
    
    private void validateInitialBalance() {
        if (!balance.getCurrency().getCurrencyCode().equals("AED")) {
            throw new IllegalArgumentException("Digital Dirham must be in AED currency");
        }
        
        if (balance.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
    }
    
    private void validateTransfer(Money amount, TransferType transferType) {
        if (status != DirhamStatus.ACTIVE) {
            throw new IllegalStateException("Cannot transfer - wallet is " + status);
        }
        
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        if (!amount.getCurrency().getCurrencyCode().equals("AED")) {
            throw new IllegalArgumentException("Digital Dirham transfers must be in AED");
        }
        
        if (transferType == TransferType.SEND || transferType == TransferType.ISLAMIC_FINANCE) {
            if (balance.getAmount().compareTo(amount.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient balance for transfer");
            }
        }
        
        // Additional validation for Islamic finance transfers
        if (transferType == TransferType.ISLAMIC_FINANCE) {
            validateIslamicFinanceTransfer(amount);
        }
    }
    
    private void validateIslamicFinanceTransfer(Money amount) {
        // Islamic finance specific validations
        if (amount.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Islamic finance transfers above 1M AED require additional approval");
        }
    }
    
    private Transaction findTransaction(String transactionId) {
        return transactions.stream()
            .filter(t -> t.getTransactionId().equals(transactionId))
            .findFirst()
            .orElse(null);
    }
    
    private long getPendingTransactionCount() {
        return transactions.stream()
            .filter(t -> t.getStatus() == TransactionStatus.PENDING)
            .count();
    }
    
    // Check if wallet can receive funds
    public boolean canReceiveFunds() {
        return status == DirhamStatus.ACTIVE;
    }
    
    // Check if wallet can send funds
    public boolean canSendFunds() {
        return status == DirhamStatus.ACTIVE && balance.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }
    
    // Get wallet metadata
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    // Add metadata
    public void addMetadata(String key, String value) {
        Objects.requireNonNull(key, "Metadata key cannot be null");
        Objects.requireNonNull(value, "Metadata value cannot be null");
        
        this.metadata.put(key, value);
        this.lastUpdated = Instant.now();
    }
}