package com.amanahfi.platform.cbdc.application;

import com.amanahfi.platform.cbdc.domain.*;
import com.amanahfi.platform.cbdc.domain.events.*;
import com.amanahfi.platform.cbdc.port.in.*;
import com.amanahfi.platform.cbdc.port.out.CordaNetworkClient;
import com.amanahfi.platform.cbdc.port.out.DigitalDirhamRepository;
import com.amanahfi.platform.shared.domain.DomainEventPublisher;
import com.amanahfi.platform.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for Digital Dirham CBDC operations
 * Integrates with R3 Corda network for distributed ledger functionality
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DigitalDirhamService implements DigitalDirhamUseCase {
    
    private final DigitalDirhamRepository digitalDirhamRepository;
    private final CordaNetworkClient cordaNetworkClient;
    private final DomainEventPublisher eventPublisher;
    
    @Override
    public DigitalDirhamId createWallet(CreateWalletCommand command) {
        log.info("Creating Digital Dirham wallet for owner: {} with type: {}", 
            command.getOwnerId(), command.getWalletType());
        
        // Validate command
        command.validate();
        
        // Check for existing wallet
        if (digitalDirhamRepository.existsByOwnerIdAndWalletType(
                command.getOwnerId(), command.getWalletType())) {
            throw new IllegalStateException(
                "Wallet already exists for owner: " + command.getOwnerId() + 
                " and type: " + command.getWalletType()
            );
        }
        
        // Create wallet on Corda network first
        String cordaWalletId = cordaNetworkClient.createWallet(
            command.getOwnerId(),
            command.getWalletType(),
            command.getInitialBalance()
        );
        
        // Create Digital Dirham aggregate
        DigitalDirhamId digitalDirhamId = DigitalDirhamId.fromWalletId(cordaWalletId);
        DigitalDirham digitalDirham = DigitalDirham.createWallet(
            digitalDirhamId,
            cordaWalletId,
            command.getWalletType(),
            command.getOwnerId(),
            command.getInitialBalance(),
            command.getCordaNodeId(),
            command.getNotaryId()
        );
        
        // Save to repository
        digitalDirhamRepository.save(digitalDirham);
        
        // Publish events
        eventPublisher.publishAll(digitalDirham.getUncommittedEvents());
        digitalDirham.markEventsAsCommitted();
        
        log.info("Created Digital Dirham wallet with ID: {}", digitalDirhamId.getValue());
        return digitalDirhamId;
    }
    
    @Override
    public void transferFunds(TransferFundsCommand command) {
        log.info("Transferring {} AED from wallet {} to wallet {}", 
            command.getAmount().getAmount(), command.getFromWalletId(), command.getToWalletId());
        
        // Validate command
        command.validate();
        
        // Load source wallet
        DigitalDirham sourceWallet = digitalDirhamRepository.findByWalletId(command.getFromWalletId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Source wallet not found: " + command.getFromWalletId()
            ));
        
        // Load destination wallet
        DigitalDirham destinationWallet = digitalDirhamRepository.findByWalletId(command.getToWalletId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Destination wallet not found: " + command.getToWalletId()
            ));
        
        // Validate transfer capability
        if (!sourceWallet.canSendFunds()) {
            throw new IllegalStateException("Source wallet cannot send funds");
        }
        
        if (!destinationWallet.canReceiveFunds()) {
            throw new IllegalStateException("Destination wallet cannot receive funds");
        }
        
        // Execute transfer on Corda network
        String cordaTransactionHash = cordaNetworkClient.transferFunds(
            command.getFromWalletId(),
            command.getToWalletId(),
            command.getAmount(),
            command.getReference(),
            command.getTransferType()
        );
        
        // Update source wallet
        sourceWallet.transfer(
            command.getToWalletId(),
            command.getAmount(),
            command.getReference(),
            command.getTransferType()
        );
        
        // Update destination wallet
        destinationWallet.receive(
            command.getFromWalletId(),
            command.getAmount(),
            command.getReference(),
            sourceWallet.getTransactions().get(sourceWallet.getTransactions().size() - 1).getTransactionId()
        );
        
        // Confirm transaction on both wallets
        String transactionId = sourceWallet.getTransactions().get(sourceWallet.getTransactions().size() - 1).getTransactionId();
        sourceWallet.confirmTransaction(transactionId, cordaTransactionHash);
        destinationWallet.confirmTransaction(transactionId, cordaTransactionHash);
        
        // Save both wallets
        digitalDirhamRepository.save(sourceWallet);
        digitalDirhamRepository.save(destinationWallet);
        
        // Publish events
        eventPublisher.publishAll(sourceWallet.getUncommittedEvents());
        eventPublisher.publishAll(destinationWallet.getUncommittedEvents());
        sourceWallet.markEventsAsCommitted();
        destinationWallet.markEventsAsCommitted();
        
        log.info("Transfer completed with Corda transaction hash: {}", cordaTransactionHash);
    }
    
    @Override
    public void mintDigitalDirham(MintDigitalDirhamCommand command) {
        log.info("Minting {} AED to Central Bank wallet: {}", 
            command.getAmount().getAmount(), command.getCentralBankWalletId());
        
        // Validate command
        command.validate();
        
        // Load Central Bank wallet
        DigitalDirham centralBankWallet = digitalDirhamRepository.findByWalletId(command.getCentralBankWalletId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Central Bank wallet not found: " + command.getCentralBankWalletId()
            ));
        
        // Validate wallet type
        if (centralBankWallet.getWalletType() != WalletType.CENTRAL_BANK) {
            throw new IllegalStateException("Only Central Bank wallets can mint Digital Dirham");
        }
        
        // Execute minting on Corda network
        String cordaTransactionHash = cordaNetworkClient.mintDigitalDirham(
            command.getCentralBankWalletId(),
            command.getAmount(),
            command.getAuthorization()
        );
        
        // Update wallet
        centralBankWallet.mint(command.getAmount(), command.getAuthorization());
        
        // Save wallet
        digitalDirhamRepository.save(centralBankWallet);
        
        // Publish events
        eventPublisher.publishAll(centralBankWallet.getUncommittedEvents());
        centralBankWallet.markEventsAsCommitted();
        
        log.info("Minting completed with Corda transaction hash: {}", cordaTransactionHash);
    }
    
    @Override
    public void burnDigitalDirham(BurnDigitalDirhamCommand command) {
        log.info("Burning {} AED from Central Bank wallet: {}", 
            command.getAmount().getAmount(), command.getCentralBankWalletId());
        
        // Validate command
        command.validate();
        
        // Load Central Bank wallet
        DigitalDirham centralBankWallet = digitalDirhamRepository.findByWalletId(command.getCentralBankWalletId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Central Bank wallet not found: " + command.getCentralBankWalletId()
            ));
        
        // Validate wallet type
        if (centralBankWallet.getWalletType() != WalletType.CENTRAL_BANK) {
            throw new IllegalStateException("Only Central Bank wallets can burn Digital Dirham");
        }
        
        // Execute burning on Corda network
        String cordaTransactionHash = cordaNetworkClient.burnDigitalDirham(
            command.getCentralBankWalletId(),
            command.getAmount(),
            command.getAuthorization()
        );
        
        // Update wallet
        centralBankWallet.burn(command.getAmount(), command.getAuthorization());
        
        // Save wallet
        digitalDirhamRepository.save(centralBankWallet);
        
        // Publish events
        eventPublisher.publishAll(centralBankWallet.getUncommittedEvents());
        centralBankWallet.markEventsAsCommitted();
        
        log.info("Burning completed with Corda transaction hash: {}", cordaTransactionHash);
    }
    
    @Override
    public void freezeWallet(FreezeWalletCommand command) {
        log.info("Freezing Digital Dirham wallet: {} for reason: {}", 
            command.getWalletId(), command.getReason());
        
        // Validate command
        command.validate();
        
        // Load wallet
        DigitalDirham wallet = digitalDirhamRepository.findByWalletId(command.getWalletId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Wallet not found: " + command.getWalletId()
            ));
        
        // Freeze on Corda network
        cordaNetworkClient.freezeWallet(command.getWalletId(), command.getReason());
        
        // Update wallet
        wallet.freeze(command.getReason(), command.getAuthorizedBy());
        
        // Save wallet
        digitalDirhamRepository.save(wallet);
        
        // Publish events
        eventPublisher.publishAll(wallet.getUncommittedEvents());
        wallet.markEventsAsCommitted();
        
        log.info("Wallet frozen: {}", command.getWalletId());
    }
    
    @Override
    public void unfreezeWallet(UnfreezeWalletCommand command) {
        log.info("Unfreezing Digital Dirham wallet: {} for reason: {}", 
            command.getWalletId(), command.getReason());
        
        // Validate command
        command.validate();
        
        // Load wallet
        DigitalDirham wallet = digitalDirhamRepository.findByWalletId(command.getWalletId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Wallet not found: " + command.getWalletId()
            ));
        
        // Unfreeze on Corda network
        cordaNetworkClient.unfreezeWallet(command.getWalletId(), command.getReason());
        
        // Update wallet
        wallet.unfreeze(command.getReason(), command.getAuthorizedBy());
        
        // Save wallet
        digitalDirhamRepository.save(wallet);
        
        // Publish events
        eventPublisher.publishAll(wallet.getUncommittedEvents());
        wallet.markEventsAsCommitted();
        
        log.info("Wallet unfrozen: {}", command.getWalletId());
    }
    
    @Override
    public BalanceSummary getBalance(String walletId) {
        log.debug("Getting balance for wallet: {}", walletId);
        
        DigitalDirham wallet = digitalDirhamRepository.findByWalletId(walletId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Wallet not found: " + walletId
            ));
        
        return wallet.getBalanceSummary();
    }
    
    @Override
    public List<Transaction> getTransactionHistory(String walletId) {
        log.debug("Getting transaction history for wallet: {}", walletId);
        
        DigitalDirham wallet = digitalDirhamRepository.findByWalletId(walletId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Wallet not found: " + walletId
            ));
        
        return wallet.getTransactionHistory();
    }
    
    @Override
    public Optional<Transaction> getTransaction(String walletId, String transactionId) {
        log.debug("Getting transaction {} for wallet: {}", transactionId, walletId);
        
        DigitalDirham wallet = digitalDirhamRepository.findByWalletId(walletId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Wallet not found: " + walletId
            ));
        
        return wallet.getTransaction(transactionId);
    }
    
    @Override
    public List<DigitalDirham> getWalletsByOwner(String ownerId) {
        log.debug("Getting wallets for owner: {}", ownerId);
        
        return digitalDirhamRepository.findByOwnerId(ownerId);
    }
    
    @Override
    public Money getTotalSupply() {
        log.debug("Getting total Digital Dirham supply");
        
        return digitalDirhamRepository.getTotalSupply();
    }
    
    @Override
    public List<DigitalDirham> getWalletsByType(WalletType walletType) {
        log.debug("Getting wallets by type: {}", walletType);
        
        return digitalDirhamRepository.findByWalletType(walletType);
    }
    
    @Override
    public boolean validateTransaction(String transactionId, String cordaTransactionHash) {
        log.debug("Validating transaction: {} with Corda hash: {}", transactionId, cordaTransactionHash);
        
        return cordaNetworkClient.validateTransaction(transactionId, cordaTransactionHash);
    }
    
    @Override
    public void processIslamicFinanceTransfer(IslamicFinanceTransferCommand command) {
        log.info("Processing Islamic finance transfer from {} to {} for {} AED", 
            command.getFromWalletId(), command.getToWalletId(), command.getAmount().getAmount());
        
        // Validate command
        command.validate();
        
        // Validate Sharia compliance
        validateShariaCompliance(command);
        
        // Execute as special Islamic finance transfer
        TransferFundsCommand transferCommand = TransferFundsCommand.builder()
            .idempotencyKey(command.getIdempotencyKey())
            .metadata(command.getMetadata())
            .fromWalletId(command.getFromWalletId())
            .toWalletId(command.getToWalletId())
            .amount(command.getAmount())
            .reference("ISLAMIC_FINANCE: " + command.getReference())
            .transferType(TransferType.ISLAMIC_FINANCE)
            .build();
        
        transferFunds(transferCommand);
        
        log.info("Islamic finance transfer completed");
    }
    
    // Helper methods
    
    private void validateShariaCompliance(IslamicFinanceTransferCommand command) {
        // Validate Sharia compliance for Islamic finance transfers
        if (command.getAmount().getAmount().compareTo(new java.math.BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Islamic finance transfers above 1M AED require Sharia board approval");
        }
        
        // Additional Sharia compliance checks would be implemented here
        log.debug("Sharia compliance validated for Islamic finance transfer");
    }
}