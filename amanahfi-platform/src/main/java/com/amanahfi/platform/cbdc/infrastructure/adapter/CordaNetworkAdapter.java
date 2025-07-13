package com.amanahfi.platform.cbdc.infrastructure.adapter;

import com.amanahfi.platform.cbdc.domain.TransferType;
import com.amanahfi.platform.cbdc.domain.WalletType;
import com.amanahfi.platform.cbdc.domain.states.TransferState;
import com.amanahfi.platform.cbdc.domain.states.WalletState;
import com.amanahfi.platform.cbdc.port.out.CordaNetworkClient;
import com.amanahfi.platform.cbdc.port.out.CordaTransactionDetails;
import com.amanahfi.platform.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.FlowLogic;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter for R3 Corda network integration
 * Handles Digital Dirham operations on the distributed ledger
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CordaNetworkAdapter implements CordaNetworkClient {
    
    private final CordaRPCOps cordaRPCOps;
    
    @Value("${amanahfi.platform.cbdc.corda.node-name}")
    private String cordaNodeName;
    
    @Value("${amanahfi.platform.cbdc.corda.notary-name}")
    private String notaryName;
    
    @Value("${amanahfi.platform.cbdc.corda.timeout-seconds:30}")
    private int timeoutSeconds;
    
    @Override
    public String createWallet(String ownerId, WalletType walletType, Money initialBalance) {
        log.info("Creating Digital Dirham wallet on Corda for owner: {} with type: {}", 
            ownerId, walletType);
        
        try {
            // Start CreateWalletFlow on Corda
            FlowLogic<?> createWalletFlow = new CreateWalletFlow(ownerId, walletType, initialBalance);
            SignedTransaction txResult = (SignedTransaction) cordaRPCOps
                .startFlowDynamic(createWalletFlow.getClass(), ownerId, walletType, initialBalance)
                .getReturnValue()
                .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            String walletId = "DD-WALLET-" + txResult.getId().toString();
            
            log.info("Created Digital Dirham wallet on Corda: {} with transaction: {}", 
                walletId, txResult.getId());
            return walletId;
            
        } catch (Exception e) {
            log.error("Failed to create wallet on Corda for owner: {}", ownerId, e);
            throw new RuntimeException("Failed to create wallet on Corda network", e);
        }
    }
    
    @Override
    public String transferFunds(String fromWalletId, String toWalletId, Money amount, 
                              String reference, TransferType transferType) {
        log.info("Transferring {} AED from {} to {} on Corda network", 
            amount.getAmount(), fromWalletId, toWalletId);
        
        try {
            // Execute transfer flow on Corda
            String transactionHash = executeTransferFlow(fromWalletId, toWalletId, amount, reference, transferType);
            
            log.info("Transfer completed on Corda with transaction hash: {}", transactionHash);
            return transactionHash;
            
        } catch (Exception e) {
            log.error("Failed to transfer funds on Corda from {} to {}", fromWalletId, toWalletId, e);
            throw new RuntimeException("Failed to transfer funds on Corda network", e);
        }
    }
    
    @Override
    public String mintDigitalDirham(String centralBankWalletId, Money amount, String authorization) {
        log.info("Minting {} AED to Central Bank wallet {} on Corda", 
            amount.getAmount(), centralBankWalletId);
        
        try {
            // Execute minting flow on Corda
            String transactionHash = executeMintingFlow(centralBankWalletId, amount, authorization);
            
            log.info("Minting completed on Corda with transaction hash: {}", transactionHash);
            return transactionHash;
            
        } catch (Exception e) {
            log.error("Failed to mint Digital Dirham on Corda for wallet: {}", centralBankWalletId, e);
            throw new RuntimeException("Failed to mint Digital Dirham on Corda network", e);
        }
    }
    
    @Override
    public String burnDigitalDirham(String centralBankWalletId, Money amount, String authorization) {
        log.info("Burning {} AED from Central Bank wallet {} on Corda", 
            amount.getAmount(), centralBankWalletId);
        
        try {
            // Execute burning flow on Corda
            String transactionHash = executeBurningFlow(centralBankWalletId, amount, authorization);
            
            log.info("Burning completed on Corda with transaction hash: {}", transactionHash);
            return transactionHash;
            
        } catch (Exception e) {
            log.error("Failed to burn Digital Dirham on Corda for wallet: {}", centralBankWalletId, e);
            throw new RuntimeException("Failed to burn Digital Dirham on Corda network", e);
        }
    }
    
    @Override
    public void freezeWallet(String walletId, String reason) {
        log.info("Freezing wallet {} on Corda for reason: {}", walletId, reason);
        
        try {
            // Execute freeze flow on Corda
            executeFreezeFlow(walletId, reason);
            
            log.info("Wallet frozen on Corda: {}", walletId);
            
        } catch (Exception e) {
            log.error("Failed to freeze wallet on Corda: {}", walletId, e);
            throw new RuntimeException("Failed to freeze wallet on Corda network", e);
        }
    }
    
    @Override
    public void unfreezeWallet(String walletId, String reason) {
        log.info("Unfreezing wallet {} on Corda for reason: {}", walletId, reason);
        
        try {
            // Execute unfreeze flow on Corda
            executeUnfreezeFlow(walletId, reason);
            
            log.info("Wallet unfrozen on Corda: {}", walletId);
            
        } catch (Exception e) {
            log.error("Failed to unfreeze wallet on Corda: {}", walletId, e);
            throw new RuntimeException("Failed to unfreeze wallet on Corda network", e);
        }
    }
    
    @Override
    public boolean validateTransaction(String transactionId, String cordaTransactionHash) {
        log.debug("Validating transaction {} with Corda hash: {}", transactionId, cordaTransactionHash);
        
        try {
            // Query Corda for transaction details
            CordaTransactionDetails details = getTransactionDetails(cordaTransactionHash);
            
            // Validate transaction exists and is confirmed
            return details != null && "CONFIRMED".equals(details.getStatus());
            
        } catch (Exception e) {
            log.error("Failed to validate transaction on Corda: {}", cordaTransactionHash, e);
            return false;
        }
    }
    
    @Override
    public Money getWalletBalance(String walletId) {
        log.debug("Getting wallet balance from Corda for wallet: {}", walletId);
        
        try {
            // Query the vault for wallet states
            Vault.Page<Object> walletStates = cordaRPCOps.vaultQuery(WalletState.class);
            
            // Find the wallet with matching ID
            for (Object state : walletStates.getStates()) {
                WalletState walletState = (WalletState) ((StateAndRef<?>) state).getState().getData();
                if (walletId.equals(walletState.getWalletId())) {
                    return Money.aed(walletState.getBalance());
                }
            }
            
            // Wallet not found
            throw new RuntimeException("Wallet not found: " + walletId);
            
        } catch (Exception e) {
            log.error("Failed to get wallet balance from Corda for wallet: {}", walletId, e);
            throw new RuntimeException("Failed to get wallet balance from Corda network", e);
        }
    }
    
    @Override
    public boolean isNetworkHealthy() {
        try {
            // Check Corda network connectivity
            cordaRPCOps.networkMapSnapshot();
            return true;
            
        } catch (Exception e) {
            log.error("Corda network health check failed", e);
            return false;
        }
    }
    
    @Override
    public long getCurrentBlockHeight() {
        try {
            // Get network parameters which contain the current block height equivalent
            var networkParameters = cordaRPCOps.networkParameters();
            var networkMap = cordaRPCOps.networkMapSnapshot();
            
            // In Corda, we use the network map update ID as a proxy for block height
            return networkMap.stream()
                .mapToLong(nodeInfo -> nodeInfo.getSerial())
                .max()
                .orElse(0L);
            
        } catch (Exception e) {
            log.error("Failed to get current block height from Corda", e);
            throw new RuntimeException("Failed to get block height from Corda network", e);
        }
    }
    
    @Override
    public CordaTransactionDetails getTransactionDetails(String cordaTransactionHash) {
        log.debug("Getting transaction details from Corda for hash: {}", cordaTransactionHash);
        
        try {
            // Parse transaction hash to SecureHash
            net.corda.core.crypto.SecureHash txHash = 
                net.corda.core.crypto.SecureHash.parse(cordaTransactionHash);
            
            // Get transaction from the vault
            SignedTransaction signedTx = cordaRPCOps.internalFindVerifiedTransaction(txHash);
            
            if (signedTx == null) {
                throw new RuntimeException("Transaction not found: " + cordaTransactionHash);
            }
            
            // Extract transaction details
            var coreTransaction = signedTx.getCoreTransaction();
            var inputs = coreTransaction.getInputs();
            var outputs = coreTransaction.getOutputs();
            
            // Determine transaction type and extract wallet information
            String transactionType = determineTransactionType(outputs);
            String fromWalletId = extractFromWalletId(inputs);
            String toWalletId = extractToWalletId(outputs);
            Money amount = extractTransactionAmount(outputs);
            String reference = extractTransactionReference(coreTransaction);
            
            return CordaTransactionDetails.builder()
                .transactionHash(cordaTransactionHash)
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(amount)
                .reference(reference)
                .transactionType(transactionType)
                .timestamp(Instant.now()) // Use current time as proxy
                .status("CONFIRMED")
                .notarySignatures(signedTx.getSigs().stream()
                    .map(sig -> sig.toString())
                    .collect(java.util.stream.Collectors.toList()))
                .blockHeight(getCurrentBlockHeight())
                .blockHash(txHash.toString())
                .build();
            
        } catch (Exception e) {
            log.error("Failed to get transaction details from Corda: {}", cordaTransactionHash, e);
            throw new RuntimeException("Failed to get transaction details from Corda network", e);
        }
    }
    
    // Private helper methods for Corda flows
    
    private String executeTransferFlow(String fromWalletId, String toWalletId, Money amount, 
                                     String reference, TransferType transferType) {
        try {
            // Start TransferFlow on Corda
            FlowLogic<?> transferFlow = new TransferFlow(fromWalletId, toWalletId, amount, reference, transferType);
            SignedTransaction txResult = (SignedTransaction) cordaRPCOps
                .startFlowDynamic(transferFlow.getClass(), fromWalletId, toWalletId, amount, reference, transferType)
                .getReturnValue()
                .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            return txResult.getId().toString();
            
        } catch (Exception e) {
            log.error("Failed to execute transfer flow on Corda", e);
            throw new RuntimeException("Transfer flow execution failed", e);
        }
    }
    
    private String executeMintingFlow(String centralBankWalletId, Money amount, String authorization) {
        try {
            // Start MintingFlow on Corda
            FlowLogic<?> mintingFlow = new MintingFlow(centralBankWalletId, amount, authorization);
            SignedTransaction txResult = (SignedTransaction) cordaRPCOps
                .startFlowDynamic(mintingFlow.getClass(), centralBankWalletId, amount, authorization)
                .getReturnValue()
                .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            return txResult.getId().toString();
            
        } catch (Exception e) {
            log.error("Failed to execute minting flow on Corda", e);
            throw new RuntimeException("Minting flow execution failed", e);
        }
    }
    
    private String executeBurningFlow(String centralBankWalletId, Money amount, String authorization) {
        try {
            // Start BurningFlow on Corda
            FlowLogic<?> burningFlow = new BurningFlow(centralBankWalletId, amount, authorization);
            SignedTransaction txResult = (SignedTransaction) cordaRPCOps
                .startFlowDynamic(burningFlow.getClass(), centralBankWalletId, amount, authorization)
                .getReturnValue()
                .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            return txResult.getId().toString();
            
        } catch (Exception e) {
            log.error("Failed to execute burning flow on Corda", e);
            throw new RuntimeException("Burning flow execution failed", e);
        }
    }
    
    private void executeFreezeFlow(String walletId, String reason) {
        try {
            // Start FreezeWalletFlow on Corda
            FlowLogic<?> freezeFlow = new FreezeWalletFlow(walletId, reason);
            cordaRPCOps
                .startFlowDynamic(freezeFlow.getClass(), walletId, reason)
                .getReturnValue()
                .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            log.debug("Freeze flow executed successfully for wallet: {}", walletId);
            
        } catch (Exception e) {
            log.error("Failed to execute freeze flow on Corda for wallet: {}", walletId, e);
            throw new RuntimeException("Freeze flow execution failed", e);
        }
    }
    
    private void executeUnfreezeFlow(String walletId, String reason) {
        try {
            // Start UnfreezeWalletFlow on Corda
            FlowLogic<?> unfreezeFlow = new UnfreezeWalletFlow(walletId, reason);
            cordaRPCOps
                .startFlowDynamic(unfreezeFlow.getClass(), walletId, reason)
                .getReturnValue()
                .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            log.debug("Unfreeze flow executed successfully for wallet: {}", walletId);
            
        } catch (Exception e) {
            log.error("Failed to execute unfreeze flow on Corda for wallet: {}", walletId, e);
            throw new RuntimeException("Unfreeze flow execution failed", e);
        }
    }
    
    // Helper methods for transaction parsing
    
    private String determineTransactionType(List<?> outputs) {
        // Analyze outputs to determine transaction type based on state types
        for (Object output : outputs) {
            if (output instanceof StateAndRef) {
                StateAndRef<?> stateAndRef = (StateAndRef<?>) output;
                ContractState state = stateAndRef.getState().getData();
                
                if (state instanceof TransferState) {
                    TransferState transferState = (TransferState) state;
                    return transferState.getTransferType().toString();
                } else if (state instanceof WalletState) {
                    // Wallet state changes might indicate minting/burning
                    return "WALLET_UPDATE";
                }
            }
        }
        return "TRANSFER"; // Default
    }
    
    private String extractFromWalletId(List<?> inputs) {
        // Extract source wallet ID from transaction inputs
        for (Object input : inputs) {
            if (input instanceof StateRef) {
                StateRef stateRef = (StateRef) input;
                // Query the vault for the input state
                try {
                    StateAndRef<?> stateAndRef = cordaRPCOps.internalFindVerifiedTransaction(
                        stateRef.getTxhash()).getTx().getOutput(stateRef.getIndex());
                    
                    ContractState state = stateAndRef.getState().getData();
                    if (state instanceof WalletState) {
                        return ((WalletState) state).getWalletId();
                    } else if (state instanceof TransferState) {
                        return ((TransferState) state).getFromWalletId();
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract from wallet ID from input: {}", e.getMessage());
                }
            }
        }
        return "UNKNOWN-FROM-WALLET";
    }
    
    private String extractToWalletId(List<?> outputs) {
        // Extract destination wallet ID from transaction outputs
        for (Object output : outputs) {
            if (output instanceof StateAndRef) {
                StateAndRef<?> stateAndRef = (StateAndRef<?>) output;
                ContractState state = stateAndRef.getState().getData();
                
                if (state instanceof TransferState) {
                    return ((TransferState) state).getToWalletId();
                } else if (state instanceof WalletState) {
                    return ((WalletState) state).getWalletId();
                }
            }
        }
        return "UNKNOWN-TO-WALLET";
    }
    
    private Money extractTransactionAmount(List<?> outputs) {
        // Extract transaction amount from outputs
        for (Object output : outputs) {
            if (output instanceof StateAndRef) {
                StateAndRef<?> stateAndRef = (StateAndRef<?>) output;
                ContractState state = stateAndRef.getState().getData();
                
                if (state instanceof TransferState) {
                    TransferState transferState = (TransferState) state;
                    return Money.aed(transferState.getAmount());
                } else if (state instanceof WalletState) {
                    // For wallet updates, the amount might be the balance difference
                    WalletState walletState = (WalletState) state;
                    return Money.aed(walletState.getBalance());
                }
            }
        }
        return Money.aed(java.math.BigDecimal.ZERO);
    }
    
    private String extractTransactionReference(Object coreTransaction) {
        // Extract reference from transaction metadata
        if (coreTransaction instanceof net.corda.core.transactions.CoreTransaction) {
            net.corda.core.transactions.CoreTransaction coreTx = 
                (net.corda.core.transactions.CoreTransaction) coreTransaction;
            
            // Check outputs for TransferState which contains reference
            for (Object output : coreTx.getOutputs()) {
                if (output instanceof net.corda.core.contracts.TransactionState) {
                    net.corda.core.contracts.TransactionState<?> txState = 
                        (net.corda.core.contracts.TransactionState<?>) output;
                    
                    if (txState.getData() instanceof TransferState) {
                        return ((TransferState) txState.getData()).getReference();
                    }
                }
            }
            
            // Fallback to transaction ID
            return "TX-REF-" + coreTx.getId().toString().substring(0, 8);
        }
        return "UNKNOWN-REFERENCE";
    }
}