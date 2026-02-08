package com.amanahfi.platform.cbdc.port.out;

import com.amanahfi.platform.cbdc.domain.TransferType;
import com.amanahfi.platform.cbdc.domain.WalletType;
import com.amanahfi.platform.shared.domain.Money;

/**
 * Output port for R3 Corda network integration
 * Handles distributed ledger operations for Digital Dirham
 */
public interface CordaNetworkClient {
    
    /**
     * Create a new wallet on the Corda network
     * @return Corda wallet ID
     */
    String createWallet(String ownerId, WalletType walletType, Money initialBalance);
    
    /**
     * Transfer funds between wallets on Corda network
     * @return Corda transaction hash
     */
    String transferFunds(String fromWalletId, String toWalletId, Money amount, 
                        String reference, TransferType transferType);
    
    /**
     * Mint new Digital Dirham (Central Bank only)
     * @return Corda transaction hash
     */
    String mintDigitalDirham(String centralBankWalletId, Money amount, String authorization);
    
    /**
     * Burn Digital Dirham (Central Bank only)
     * @return Corda transaction hash
     */
    String burnDigitalDirham(String centralBankWalletId, Money amount, String authorization);
    
    /**
     * Freeze wallet on Corda network
     */
    void freezeWallet(String walletId, String reason);
    
    /**
     * Unfreeze wallet on Corda network
     */
    void unfreezeWallet(String walletId, String reason);
    
    /**
     * Validate transaction on Corda network
     */
    boolean validateTransaction(String transactionId, String cordaTransactionHash);
    
    /**
     * Get wallet balance from Corda network
     */
    Money getWalletBalance(String walletId);
    
    /**
     * Check if Corda network is healthy
     */
    boolean isNetworkHealthy();
    
    /**
     * Get current block height
     */
    long getCurrentBlockHeight();
    
    /**
     * Get transaction details from Corda
     */
    CordaTransactionDetails getTransactionDetails(String cordaTransactionHash);
}