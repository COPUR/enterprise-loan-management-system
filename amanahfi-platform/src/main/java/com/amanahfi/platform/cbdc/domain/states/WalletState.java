package com.amanahfi.platform.cbdc.domain.states;

import com.amanahfi.platform.cbdc.domain.WalletType;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Corda state representing a Digital Dirham wallet on the ledger
 */
public class WalletState implements ContractState {
    
    private final String walletId;
    private final Party owner;
    private final BigDecimal balance;
    private final WalletType walletType;
    private final boolean frozen;
    private final Party issuer; // Central Bank for CBDC
    
    public WalletState(String walletId, Party owner, BigDecimal balance, 
                      WalletType walletType, boolean frozen, Party issuer) {
        this.walletId = walletId;
        this.owner = owner;
        this.balance = balance;
        this.walletType = walletType;
        this.frozen = frozen;
        this.issuer = issuer;
    }
    
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner, issuer);
    }
    
    public String getWalletId() {
        return walletId;
    }
    
    public Party getOwner() {
        return owner;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public WalletType getWalletType() {
        return walletType;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    public Party getIssuer() {
        return issuer;
    }
    
    public WalletState withNewBalance(BigDecimal newBalance) {
        return new WalletState(walletId, owner, newBalance, walletType, frozen, issuer);
    }
    
    public WalletState freeze() {
        return new WalletState(walletId, owner, balance, walletType, true, issuer);
    }
    
    public WalletState unfreeze() {
        return new WalletState(walletId, owner, balance, walletType, false, issuer);
    }
}