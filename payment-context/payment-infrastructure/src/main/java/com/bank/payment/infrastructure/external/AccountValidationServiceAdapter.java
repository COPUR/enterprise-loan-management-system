package com.bank.payment.infrastructure.external;

import com.bank.payment.application.AccountValidationService;
import com.bank.payment.domain.AccountId;
import com.bank.shared.kernel.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for account validation and balance checking
 * 
 * Implements Hexagonal Architecture - Infrastructure adapter for account services
 * In production, this would integrate with core banking systems
 */
@Component
public class AccountValidationServiceAdapter implements AccountValidationService {
    
    // Mock account database for demonstration
    private final Map<String, AccountInfo> mockAccounts = new HashMap<>();
    
    public AccountValidationServiceAdapter() {
        // Initialize mock accounts for testing
        mockAccounts.put("ACC-11111111", new AccountInfo("ACC-11111111", BigDecimal.valueOf(25000), true, true, true));
        mockAccounts.put("ACC-22222222", new AccountInfo("ACC-22222222", BigDecimal.valueOf(15000), true, true, true));
        mockAccounts.put("ACC-33333333", new AccountInfo("ACC-33333333", BigDecimal.valueOf(5000), true, false, true));
        mockAccounts.put("ACC-INVALID", new AccountInfo("ACC-INVALID", BigDecimal.ZERO, false, false, false));
    }
    
    @Override
    public boolean validateAccount(AccountId accountId) {
        AccountInfo account = mockAccounts.get(accountId.getValue());
        return account != null && account.isActive();
    }
    
    @Override
    public boolean hasBalance(AccountId accountId, Money amount) {
        AccountInfo account = mockAccounts.get(accountId.getValue());
        if (account == null || !account.isActive()) {
            return false;
        }
        
        return account.getBalance().compareTo(amount.getAmount()) >= 0;
    }
    
    @Override
    public Money getAccountBalance(AccountId accountId) {
        AccountInfo account = mockAccounts.get(accountId.getValue());
        if (account == null) {
            return Money.zero(Currency.getInstance("USD"));
        }
        
        return Money.usd(account.getBalance());
    }
    
    @Override
    public boolean canReceivePayments(AccountId accountId) {
        AccountInfo account = mockAccounts.get(accountId.getValue());
        return account != null && account.isActive() && account.canReceive();
    }
    
    @Override
    public boolean canSendPayments(AccountId accountId) {
        AccountInfo account = mockAccounts.get(accountId.getValue());
        return account != null && account.isActive() && account.canSend();
    }
    
    /**
     * Mock account information for demonstration
     * In production, this would be replaced by actual account entities
     */
    private static class AccountInfo {
        private final String accountId;
        private final BigDecimal balance;
        private final boolean active;
        private final boolean canSend;
        private final boolean canReceive;
        
        public AccountInfo(String accountId, BigDecimal balance, boolean active, boolean canSend, boolean canReceive) {
            this.accountId = accountId;
            this.balance = balance;
            this.active = active;
            this.canSend = canSend;
            this.canReceive = canReceive;
        }
        
        public String getAccountId() { return accountId; }
        public BigDecimal getBalance() { return balance; }
        public boolean isActive() { return active; }
        public boolean canSend() { return canSend; }
        public boolean canReceive() { return canReceive; }
    }
}