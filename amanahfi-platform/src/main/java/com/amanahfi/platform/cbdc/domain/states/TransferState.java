package com.amanahfi.platform.cbdc.domain.states;

import com.amanahfi.platform.cbdc.domain.TransferType;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Corda state representing a Digital Dirham transfer on the ledger
 */
public class TransferState implements ContractState {
    
    private final String transferId;
    private final String fromWalletId;
    private final String toWalletId;
    private final Party sender;
    private final Party receiver;
    private final BigDecimal amount;
    private final String currency;
    private final String reference;
    private final TransferType transferType;
    private final Instant timestamp;
    private final Party notary;
    
    public TransferState(String transferId, String fromWalletId, String toWalletId,
                        Party sender, Party receiver, BigDecimal amount, String currency,
                        String reference, TransferType transferType, Instant timestamp, Party notary) {
        this.transferId = transferId;
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.transferType = transferType;
        this.timestamp = timestamp;
        this.notary = notary;
    }
    
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver, notary);
    }
    
    public String getTransferId() {
        return transferId;
    }
    
    public String getFromWalletId() {
        return fromWalletId;
    }
    
    public String getToWalletId() {
        return toWalletId;
    }
    
    public Party getSender() {
        return sender;
    }
    
    public Party getReceiver() {
        return receiver;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public String getReference() {
        return reference;
    }
    
    public TransferType getTransferType() {
        return transferType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Party getNotary() {
        return notary;
    }
}