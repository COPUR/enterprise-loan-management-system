package com.amanahfi.accounts.domain.account;

import com.amanahfi.shared.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Account Aggregate
 * Following Islamic banking multi-currency wallet requirements
 */
@DisplayName("Account Aggregate Tests")
class AccountTest {

    @Test
    @DisplayName("Should create new Account with valid customer ID")
    void shouldCreateNewAccountWithValidCustomerId() {
        // Given
        String customerId = "CUST-12345678";
        AccountType accountType = AccountType.SAVINGS;
        String currency = "AED";

        // When
        Account account = Account.create(customerId, accountType, currency);

        // Then
        assertThat(account.getAccountId()).isNotNull();
        assertThat(account.getCustomerId()).isEqualTo(customerId);
        assertThat(account.getAccountType()).isEqualTo(accountType);
        assertThat(account.getCurrency()).isEqualTo(currency);
        assertThat(account.getBalance()).isEqualTo(Money.zero(currency));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.isIslamicCompliant()).isFalse(); // Default
    }

    @Test
    @DisplayName("Should create Islamic-compliant savings account")
    void shouldCreateIslamicCompliantSavingsAccount() {
        // When
        Account account = Account.create("CUST-12345678", AccountType.SAVINGS, "AED")
            .withIslamicCompliance(true);

        // Then
        assertThat(account.isIslamicCompliant()).isTrue();
        assertThat(account.canEarnInterest()).isFalse();
        assertThat(account.canParticipateProfitSharing()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AED", "USD", "EUR", "SAR", "QAR", "KWD", "BHD"})
    @DisplayName("Should support major MENAT currencies")
    void shouldSupportMajorMenatCurrencies(String currency) {
        // When
        Account account = Account.create("CUST-12345678", AccountType.CURRENT, currency);

        // Then
        assertThat(account.getCurrency()).isEqualTo(currency);
        assertThat(account.getBalance().getCurrency()).isEqualTo(currency);
    }

    @Test
    @DisplayName("Should deposit money and update balance")
    void shouldDepositMoneyAndUpdateBalance() {
        // Given
        Account account = Account.create("CUST-12345678", AccountType.SAVINGS, "AED");
        Money depositAmount = Money.of(new BigDecimal("1000.00"), "AED");
        String description = "Initial deposit";

        // When
        account.deposit(depositAmount, description);

        // Then
        assertThat(account.getBalance()).isEqualTo(depositAmount);
        assertThat(account.getTransactions()).hasSize(1);
        
        Transaction transaction = account.getTransactions().get(0);
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.getAmount()).isEqualTo(depositAmount);
        assertThat(transaction.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("Should withdraw money when sufficient balance")
    void shouldWithdrawMoneyWhenSufficientBalance() {
        // Given
        Account account = Account.create("CUST-12345678", AccountType.CURRENT, "AED");
        Money depositAmount = Money.of(new BigDecimal("1000.00"), "AED");
        Money withdrawAmount = Money.of(new BigDecimal("300.00"), "AED");
        
        account.deposit(depositAmount, "Initial deposit");

        // When
        account.withdraw(withdrawAmount, "ATM withdrawal");

        // Then
        Money expectedBalance = Money.of(new BigDecimal("700.00"), "AED");
        assertThat(account.getBalance()).isEqualTo(expectedBalance);
        assertThat(account.getTransactions()).hasSize(2);
        
        Transaction withdrawalTransaction = account.getTransactions().get(1);
        assertThat(withdrawalTransaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(withdrawalTransaction.getAmount()).isEqualTo(withdrawAmount);
    }

    @Test
    @DisplayName("Should reject withdrawal when insufficient balance")
    void shouldRejectWithdrawalWhenInsufficientBalance() {
        // Given
        Account account = Account.create("CUST-12345678", AccountType.SAVINGS, "AED");
        Money depositAmount = Money.of(new BigDecimal("100.00"), "AED");
        Money withdrawAmount = Money.of(new BigDecimal("200.00"), "AED");
        
        account.deposit(depositAmount, "Initial deposit");

        // When & Then
        assertThatThrownBy(() -> account.withdraw(withdrawAmount, "Overdraft attempt"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("Should create CBDC wallet account")
    void shouldCreateCbdcWalletAccount() {
        // When
        Account cbdcAccount = Account.createCbdcWallet("CUST-12345678");

        // Then
        assertThat(cbdcAccount.getAccountType()).isEqualTo(AccountType.CBDC_WALLET);
        assertThat(cbdcAccount.getCurrency()).isEqualTo("AED");
        assertThat(cbdcAccount.isCbdcEnabled()).isTrue();
        assertThat(cbdcAccount.canSettleInstantly()).isTrue();
    }

    @Test
    @DisplayName("Should create stablecoin wallet account")
    void shouldCreateStablecoinWalletAccount() {
        // Given
        String stablecoinType = "USDC";

        // When
        Account stablecoinAccount = Account.createStablecoinWallet("CUST-12345678", stablecoinType);

        // Then
        assertThat(stablecoinAccount.getAccountType()).isEqualTo(AccountType.STABLECOIN_WALLET);
        assertThat(stablecoinAccount.getStablecoinType()).isEqualTo(stablecoinType);
        assertThat(stablecoinAccount.isStablecoinEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should freeze account when suspicious activity detected")
    void shouldFreezeAccountWhenSuspiciousActivityDetected() {
        // Given
        Account account = Account.create("CUST-12345678", AccountType.CURRENT, "AED");
        String freezeReason = "Suspicious large transactions detected";

        // When
        account.freeze(freezeReason);

        // Then
        assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(account.getFreezeReason()).isEqualTo(freezeReason);
    }

    @Test
    @DisplayName("Should reject transactions on frozen account")
    void shouldRejectTransactionsOnFrozenAccount() {
        // Given
        Account account = Account.create("CUST-12345678", AccountType.SAVINGS, "AED");
        Money amount = Money.of(new BigDecimal("100.00"), "AED");
        
        account.freeze("AML investigation");

        // When & Then
        assertThatThrownBy(() -> account.deposit(amount, "Test deposit"))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("Account is frozen");
                
        assertThatThrownBy(() -> account.withdraw(amount, "Test withdrawal"))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("Account is frozen");
    }

    @Test
    @DisplayName("Should unfreeze account after investigation")
    void shouldUnfreezeAccountAfterInvestigation() {
        // Given
        Account account = Account.create("CUST-12345678", AccountType.CURRENT, "AED");
        account.freeze("Investigation required");

        // When
        account.unfreeze("Investigation completed - no issues found");

        // Then
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getFreezeReason()).isNull();
    }

    @Test
    @DisplayName("Should enforce Islamic banking compliance rules")
    void shouldEnforceIslamicBankingComplianceRules() {
        // Given
        Account islamicAccount = Account.create("CUST-12345678", AccountType.SAVINGS, "AED")
            .withIslamicCompliance(true);

        // Then
        assertThat(islamicAccount.isIslamicCompliant()).isTrue();
        assertThat(islamicAccount.canEarnInterest()).isFalse();
        assertThat(islamicAccount.canParticipateProfitSharing()).isTrue();
        assertThat(islamicAccount.canInvestInConventionalProducts()).isFalse();
        assertThat(islamicAccount.canInvestInShariahCompliantProducts()).isTrue();
    }

    @Test
    @DisplayName("Should validate multi-currency wallet creation")
    void shouldValidateMultiCurrencyWalletCreation() {
        // Given
        String customerId = "CUST-12345678";

        // When
        Account aedWallet = Account.create(customerId, AccountType.CURRENT, "AED");
        Account usdWallet = Account.create(customerId, AccountType.CURRENT, "USD");
        Account cbdcWallet = Account.createCbdcWallet(customerId);
        Account stablecoinWallet = Account.createStablecoinWallet(customerId, "USDC");

        // Then
        assertThat(aedWallet.getCustomerId()).isEqualTo(customerId);
        assertThat(usdWallet.getCustomerId()).isEqualTo(customerId);
        assertThat(cbdcWallet.getCustomerId()).isEqualTo(customerId);
        assertThat(stablecoinWallet.getCustomerId()).isEqualTo(customerId);
        
        // Each account should have different currencies/types
        assertThat(aedWallet.getCurrency()).isEqualTo("AED");
        assertThat(usdWallet.getCurrency()).isEqualTo("USD");
        assertThat(cbdcWallet.getCurrency()).isEqualTo("AED");
        assertThat(cbdcWallet.getAccountType()).isEqualTo(AccountType.CBDC_WALLET);
        assertThat(stablecoinWallet.getAccountType()).isEqualTo(AccountType.STABLECOIN_WALLET);
    }

    @Test
    @DisplayName("Should transfer between accounts of same customer")
    void shouldTransferBetweenAccountsOfSameCustomer() {
        // Given
        String customerId = "CUST-12345678";
        Account aedAccount = Account.create(customerId, AccountType.CURRENT, "AED");
        Account savingsAccount = Account.create(customerId, AccountType.SAVINGS, "AED");
        
        Money initialAmount = Money.of(new BigDecimal("1000.00"), "AED");
        Money transferAmount = Money.of(new BigDecimal("300.00"), "AED");
        
        aedAccount.deposit(initialAmount, "Initial deposit");

        // When
        aedAccount.transferTo(savingsAccount, transferAmount, "Transfer to savings");

        // Then
        Money expectedAedBalance = Money.of(new BigDecimal("700.00"), "AED");
        assertThat(aedAccount.getBalance()).isEqualTo(expectedAedBalance);
        assertThat(savingsAccount.getBalance()).isEqualTo(transferAmount);
    }

    @Test
    @DisplayName("Should reject currency mismatch in operations")
    void shouldRejectCurrencyMismatchInOperations() {
        // Given
        Account aedAccount = Account.create("CUST-12345678", AccountType.CURRENT, "AED");
        Money usdAmount = Money.of(new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> aedAccount.deposit(usdAmount, "USD deposit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    private Account createTestAccount() {
        return Account.create("CUST-12345678", AccountType.CURRENT, "AED");
    }
}