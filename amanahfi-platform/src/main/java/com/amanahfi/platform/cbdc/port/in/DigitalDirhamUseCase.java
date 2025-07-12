package com.amanahfi.platform.cbdc.port.in;

import com.amanahfi.platform.cbdc.domain.*;
import com.amanahfi.platform.shared.domain.Money;

import java.util.List;
import java.util.Optional;

/**
 * Input port for Digital Dirham CBDC use cases
 */
public interface DigitalDirhamUseCase {
    
    // Wallet management
    DigitalDirhamId createWallet(CreateWalletCommand command);
    void freezeWallet(FreezeWalletCommand command);
    void unfreezeWallet(UnfreezeWalletCommand command);
    
    // Transfer operations
    void transferFunds(TransferFundsCommand command);
    void processIslamicFinanceTransfer(IslamicFinanceTransferCommand command);
    
    // Central Bank operations
    void mintDigitalDirham(MintDigitalDirhamCommand command);
    void burnDigitalDirham(BurnDigitalDirhamCommand command);
    
    // Query operations
    BalanceSummary getBalance(String walletId);
    List<Transaction> getTransactionHistory(String walletId);
    Optional<Transaction> getTransaction(String walletId, String transactionId);
    List<DigitalDirham> getWalletsByOwner(String ownerId);
    List<DigitalDirham> getWalletsByType(WalletType walletType);
    Money getTotalSupply();
    
    // Validation
    boolean validateTransaction(String transactionId, String cordaTransactionHash);
}