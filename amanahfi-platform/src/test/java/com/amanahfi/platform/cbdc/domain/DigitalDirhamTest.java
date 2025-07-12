package com.amanahfi.platform.cbdc.domain;

import com.amanahfi.platform.cbdc.domain.events.*;
import com.amanahfi.platform.shared.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for Digital Dirham CBDC aggregate
 */
@DisplayName("Digital Dirham CBDC Tests")
class DigitalDirhamTest {
    
    @Nested
    @DisplayName("Creating Digital Dirham Wallets")
    class CreatingWallets {
        
        @Test
        @DisplayName("Should create Central Bank wallet with initial balance")
        void shouldCreateCentralBankWalletWithInitialBalance() {
            // Given
            DigitalDirhamId id = DigitalDirhamId.generate();
            String walletId = "CB-WALLET-001";
            String ownerId = "CENTRAL-BANK-UAE";
            Money initialBalance = Money.aed(new BigDecimal("1000000000")); // 1 billion AED
            String cordaNodeId = "CORDA-NODE-001";
            String notaryId = "NOTARY-001";
            
            // When
            DigitalDirham digitalDirham = DigitalDirham.createWallet(
                id, walletId, WalletType.CENTRAL_BANK, ownerId, 
                initialBalance, cordaNodeId, notaryId
            );
            
            // Then
            assertThat(digitalDirham.getId()).isEqualTo(id);
            assertThat(digitalDirham.getWalletId()).isEqualTo(walletId);
            assertThat(digitalDirham.getWalletType()).isEqualTo(WalletType.CENTRAL_BANK);
            assertThat(digitalDirham.getOwnerId()).isEqualTo(ownerId);
            assertThat(digitalDirham.getBalance()).isEqualTo(initialBalance);
            assertThat(digitalDirham.getStatus()).isEqualTo(DirhamStatus.ACTIVE);
            assertThat(digitalDirham.getCordaNodeId()).isEqualTo(cordaNodeId);
            assertThat(digitalDirham.getNotaryId()).isEqualTo(notaryId);
            
            // Should have creation event
            assertThat(digitalDirham.getUncommittedEvents()).hasSize(1);
            assertThat(digitalDirham.getUncommittedEvents().get(0))
                .isInstanceOf(DigitalDirhamCreatedEvent.class);
        }
        
        @Test
        @DisplayName("Should create retail wallet with zero balance")
        void shouldCreateRetailWalletWithZeroBalance() {
            // Given
            DigitalDirhamId id = DigitalDirhamId.generate();
            String walletId = "RETAIL-WALLET-001";
            String ownerId = "CUSTOMER-001";
            Money initialBalance = Money.aed(BigDecimal.ZERO);
            
            // When
            DigitalDirham digitalDirham = DigitalDirham.createWallet(
                id, walletId, WalletType.RETAIL, ownerId, 
                initialBalance, "CORDA-NODE-001", "NOTARY-001"
            );
            
            // Then
            assertThat(digitalDirham.getWalletType()).isEqualTo(WalletType.RETAIL);
            assertThat(digitalDirham.getBalance().getAmount()).isEqualTo(BigDecimal.ZERO);
        }
        
        @Test
        @DisplayName("Should reject non-AED currency")
        void shouldRejectNonAedCurrency() {
            // Given
            DigitalDirhamId id = DigitalDirhamId.generate();
            Money usdBalance = new Money(new BigDecimal("1000"), Currency.getInstance("USD"));
            
            // When & Then
            assertThatThrownBy(() -> DigitalDirham.createWallet(
                id, "WALLET-001", WalletType.RETAIL, "OWNER-001", 
                usdBalance, "CORDA-NODE-001", "NOTARY-001"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Digital Dirham must be in AED currency");
        }
        
        @Test
        @DisplayName("Should reject negative initial balance")
        void shouldRejectNegativeInitialBalance() {
            // Given
            DigitalDirhamId id = DigitalDirhamId.generate();
            Money negativeBalance = Money.aed(new BigDecimal("-100"));
            
            // When & Then
            assertThatThrownBy(() -> DigitalDirham.createWallet(
                id, "WALLET-001", WalletType.RETAIL, "OWNER-001", 
                negativeBalance, "CORDA-NODE-001", "NOTARY-001"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Initial balance cannot be negative");
        }
    }
    
    @Nested
    @DisplayName("Transferring Digital Dirham")
    class TransferringFunds {
        
        @Test
        @DisplayName("Should transfer funds successfully")
        void shouldTransferFundsSuccessfully() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            String toWalletId = "TO-WALLET-001";
            Money transferAmount = Money.aed(new BigDecimal("500"));
            String reference = "Test transfer";
            
            // When
            wallet.transfer(toWalletId, transferAmount, reference, TransferType.SEND);
            
            // Then
            assertThat(wallet.getBalance().getAmount()).isEqualTo(new BigDecimal("500"));
            assertThat(wallet.getTransactions()).hasSize(1);
            
            Transaction transaction = wallet.getTransactions().get(0);
            assertThat(transaction.getFromWalletId()).isEqualTo(wallet.getWalletId());
            assertThat(transaction.getToWalletId()).isEqualTo(toWalletId);
            assertThat(transaction.getAmount()).isEqualTo(transferAmount);
            assertThat(transaction.getReference()).isEqualTo(reference);
            assertThat(transaction.getTransferType()).isEqualTo(TransferType.SEND);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
            
            // Should have transfer event
            assertThat(wallet.getUncommittedEvents()).hasSize(2); // Creation + Transfer
            assertThat(wallet.getUncommittedEvents().get(1))
                .isInstanceOf(DigitalDirhamTransferInitiatedEvent.class);
        }
        
        @Test
        @DisplayName("Should reject transfer with insufficient balance")
        void shouldRejectTransferWithInsufficientBalance() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("100")));
            Money transferAmount = Money.aed(new BigDecimal("500"));
            
            // When & Then
            assertThatThrownBy(() -> wallet.transfer(
                "TO-WALLET-001", transferAmount, "Test", TransferType.SEND
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Insufficient balance for transfer");
        }
        
        @Test
        @DisplayName("Should reject transfer when wallet is frozen")
        void shouldRejectTransferWhenWalletIsFrozen() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            wallet.freeze("Regulatory compliance", "ADMIN-001");
            
            // When & Then
            assertThatThrownBy(() -> wallet.transfer(
                "TO-WALLET-001", Money.aed(new BigDecimal("100")), "Test", TransferType.SEND
            )).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Cannot transfer - wallet is FROZEN");
        }
        
        @Test
        @DisplayName("Should process Islamic finance transfer with validation")
        void shouldProcessIslamicFinanceTransferWithValidation() {
            // Given
            DigitalDirham wallet = createIslamicBankWallet(Money.aed(new BigDecimal("500000")));
            Money transferAmount = Money.aed(new BigDecimal("100000"));
            
            // When
            wallet.transfer("ISLAMIC-WALLET-001", transferAmount, "Murabaha payment", TransferType.ISLAMIC_FINANCE);
            
            // Then
            assertThat(wallet.getBalance().getAmount()).isEqualTo(new BigDecimal("400000"));
            
            Transaction transaction = wallet.getTransactions().get(0);
            assertThat(transaction.getTransferType()).isEqualTo(TransferType.ISLAMIC_FINANCE);
        }
        
        @Test
        @DisplayName("Should reject large Islamic finance transfer without approval")
        void shouldRejectLargeIslamicFinanceTransferWithoutApproval() {
            // Given
            DigitalDirham wallet = createIslamicBankWallet(Money.aed(new BigDecimal("2000000")));
            Money largeAmount = Money.aed(new BigDecimal("1500000")); // Above 1M AED limit
            
            // When & Then
            assertThatThrownBy(() -> wallet.transfer(
                "ISLAMIC-WALLET-001", largeAmount, "Large transfer", TransferType.ISLAMIC_FINANCE
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Islamic finance transfers above 1M AED require additional approval");
        }
    }
    
    @Nested
    @DisplayName("Receiving Digital Dirham")
    class ReceivingFunds {
        
        @Test
        @DisplayName("Should receive funds successfully")
        void shouldReceiveFundsSuccessfully() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("100")));
            String fromWalletId = "FROM-WALLET-001";
            Money receivedAmount = Money.aed(new BigDecimal("500"));
            String reference = "Received payment";
            String transactionId = "TX-001";
            
            // When
            wallet.receive(fromWalletId, receivedAmount, reference, transactionId);
            
            // Then
            assertThat(wallet.getBalance().getAmount()).isEqualTo(new BigDecimal("600"));
            assertThat(wallet.getTransactions()).hasSize(1);
            
            Transaction transaction = wallet.getTransactions().get(0);
            assertThat(transaction.getFromWalletId()).isEqualTo(fromWalletId);
            assertThat(transaction.getToWalletId()).isEqualTo(wallet.getWalletId());
            assertThat(transaction.getAmount()).isEqualTo(receivedAmount);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            
            // Should have receive event
            assertThat(wallet.getUncommittedEvents()).hasSize(2); // Creation + Receive
            assertThat(wallet.getUncommittedEvents().get(1))
                .isInstanceOf(DigitalDirhamReceivedEvent.class);
        }
        
        @Test
        @DisplayName("Should reject receive when wallet is frozen")
        void shouldRejectReceiveWhenWalletIsFrozen() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(BigDecimal.ZERO));
            wallet.freeze("Regulatory compliance", "ADMIN-001");
            
            // When & Then
            assertThatThrownBy(() -> wallet.receive(
                "FROM-WALLET-001", Money.aed(new BigDecimal("100")), "Test", "TX-001"
            )).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Cannot receive funds - wallet is FROZEN");
        }
    }
    
    @Nested
    @DisplayName("Central Bank Operations")
    class CentralBankOperations {
        
        @Test
        @DisplayName("Should mint Digital Dirham successfully")
        void shouldMintDigitalDirhamSuccessfully() {
            // Given
            DigitalDirham centralBankWallet = createCentralBankWallet(Money.aed(BigDecimal.ZERO));
            Money mintAmount = Money.aed(new BigDecimal("1000000"));
            String authorization = "CBUAE-MINT-AUTH-001";
            
            // When
            centralBankWallet.mint(mintAmount, authorization);
            
            // Then
            assertThat(centralBankWallet.getBalance().getAmount()).isEqualTo(new BigDecimal("1000000"));
            
            // Should have minting event
            assertThat(centralBankWallet.getUncommittedEvents()).hasSize(2); // Creation + Mint
            assertThat(centralBankWallet.getUncommittedEvents().get(1))
                .isInstanceOf(DigitalDirhamMintedEvent.class);
        }
        
        @Test
        @DisplayName("Should burn Digital Dirham successfully")
        void shouldBurnDigitalDirhamSuccessfully() {
            // Given
            DigitalDirham centralBankWallet = createCentralBankWallet(Money.aed(new BigDecimal("1000000")));
            Money burnAmount = Money.aed(new BigDecimal("500000"));
            String authorization = "CBUAE-BURN-AUTH-001";
            
            // When
            centralBankWallet.burn(burnAmount, authorization);
            
            // Then
            assertThat(centralBankWallet.getBalance().getAmount()).isEqualTo(new BigDecimal("500000"));
            
            // Should have burning event
            assertThat(centralBankWallet.getUncommittedEvents()).hasSize(2); // Creation + Burn
            assertThat(centralBankWallet.getUncommittedEvents().get(1))
                .isInstanceOf(DigitalDirhamBurnedEvent.class);
        }
        
        @Test
        @DisplayName("Should reject minting by non-Central Bank wallet")
        void shouldRejectMintingByNonCentralBankWallet() {
            // Given
            DigitalDirham retailWallet = createRetailWallet(Money.aed(BigDecimal.ZERO));
            Money mintAmount = Money.aed(new BigDecimal("1000"));
            
            // When & Then
            assertThatThrownBy(() -> retailWallet.mint(mintAmount, "UNAUTHORIZED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only Central Bank can mint Digital Dirham");
        }
        
        @Test
        @DisplayName("Should reject burning with insufficient balance")
        void shouldRejectBurningWithInsufficientBalance() {
            // Given
            DigitalDirham centralBankWallet = createCentralBankWallet(Money.aed(new BigDecimal("100")));
            Money burnAmount = Money.aed(new BigDecimal("500"));
            
            // When & Then
            assertThatThrownBy(() -> centralBankWallet.burn(burnAmount, "AUTH-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance for burn operation");
        }
    }
    
    @Nested
    @DisplayName("Wallet Freeze/Unfreeze Operations")
    class FreezeUnfreezeOperations {
        
        @Test
        @DisplayName("Should freeze wallet successfully")
        void shouldFreezeWalletSuccessfully() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            String reason = "Regulatory compliance investigation";
            String authorizedBy = "VARA-ADMIN-001";
            
            // When
            wallet.freeze(reason, authorizedBy);
            
            // Then
            assertThat(wallet.getStatus()).isEqualTo(DirhamStatus.FROZEN);
            assertThat(wallet.canSendFunds()).isFalse();
            assertThat(wallet.canReceiveFunds()).isFalse();
            
            // Should have freeze event
            assertThat(wallet.getUncommittedEvents()).hasSize(2); // Creation + Freeze
            assertThat(wallet.getUncommittedEvents().get(1))
                .isInstanceOf(DigitalDirhamFrozenEvent.class);
        }
        
        @Test
        @DisplayName("Should unfreeze wallet successfully")
        void shouldUnfreezeWalletSuccessfully() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            wallet.freeze("Investigation", "ADMIN-001");
            
            String reason = "Investigation completed";
            String authorizedBy = "VARA-ADMIN-001";
            
            // When
            wallet.unfreeze(reason, authorizedBy);
            
            // Then
            assertThat(wallet.getStatus()).isEqualTo(DirhamStatus.ACTIVE);
            assertThat(wallet.canSendFunds()).isTrue();
            assertThat(wallet.canReceiveFunds()).isTrue();
            
            // Should have unfreeze event
            assertThat(wallet.getUncommittedEvents()).hasSize(3); // Creation + Freeze + Unfreeze
            assertThat(wallet.getUncommittedEvents().get(2))
                .isInstanceOf(DigitalDirhamUnfrozenEvent.class);
        }
        
        @Test
        @DisplayName("Should reject double freeze")
        void shouldRejectDoubleFreeze() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            wallet.freeze("Investigation", "ADMIN-001");
            
            // When & Then
            assertThatThrownBy(() -> wallet.freeze("Another reason", "ADMIN-002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wallet is already frozen");
        }
        
        @Test
        @DisplayName("Should reject unfreeze of non-frozen wallet")
        void shouldRejectUnfreezeOfNonFrozenWallet() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            
            // When & Then
            assertThatThrownBy(() -> wallet.unfreeze("No reason", "ADMIN-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wallet is not frozen");
        }
    }
    
    @Nested
    @DisplayName("Transaction Confirmation")
    class TransactionConfirmation {
        
        @Test
        @DisplayName("Should confirm transaction successfully")
        void shouldConfirmTransactionSuccessfully() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            wallet.transfer("TO-WALLET-001", Money.aed(new BigDecimal("100")), "Test", TransferType.SEND);
            
            String transactionId = wallet.getTransactions().get(0).getTransactionId();
            String cordaHash = "CORDA-HASH-001";
            
            // When
            wallet.confirmTransaction(transactionId, cordaHash);
            
            // Then
            Transaction transaction = wallet.getTransactions().get(0);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
            assertThat(transaction.getCordaTransactionHash()).isEqualTo(cordaHash);
            
            // Should have confirmation event
            assertThat(wallet.getUncommittedEvents()).hasSize(3); // Creation + Transfer + Confirmation
            assertThat(wallet.getUncommittedEvents().get(2))
                .isInstanceOf(DigitalDirhamTransactionConfirmedEvent.class);
        }
        
        @Test
        @DisplayName("Should reject confirmation of non-existent transaction")
        void shouldRejectConfirmationOfNonExistentTransaction() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            
            // When & Then
            assertThatThrownBy(() -> wallet.confirmTransaction("NON-EXISTENT", "CORDA-HASH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction not found");
        }
    }
    
    @Nested
    @DisplayName("Balance Summary")
    class BalanceSummary {
        
        @Test
        @DisplayName("Should provide accurate balance summary")
        void shouldProvideAccurateBalanceSummary() {
            // Given
            DigitalDirham wallet = createRetailWallet(Money.aed(new BigDecimal("1000")));
            wallet.transfer("TO-WALLET-001", Money.aed(new BigDecimal("100")), "Test", TransferType.SEND);
            
            // When
            com.amanahfi.platform.cbdc.domain.BalanceSummary summary = wallet.getBalanceSummary();
            
            // Then
            assertThat(summary.getWalletId()).isEqualTo(wallet.getWalletId());
            assertThat(summary.getOwnerId()).isEqualTo(wallet.getOwnerId());
            assertThat(summary.getCurrentBalance()).isEqualTo(wallet.getBalance());
            assertThat(summary.getTotalTransactions()).isEqualTo(1);
            assertThat(summary.getPendingTransactions()).isEqualTo(1);
        }
    }
    
    // Helper methods
    
    private DigitalDirham createRetailWallet(Money initialBalance) {
        return DigitalDirham.createWallet(
            DigitalDirhamId.generate(),
            "RETAIL-WALLET-001",
            WalletType.RETAIL,
            "CUSTOMER-001",
            initialBalance,
            "CORDA-NODE-001",
            "NOTARY-001"
        );
    }
    
    private DigitalDirham createCentralBankWallet(Money initialBalance) {
        return DigitalDirham.createWallet(
            DigitalDirhamId.generate(),
            "CB-WALLET-001",
            WalletType.CENTRAL_BANK,
            "CENTRAL-BANK-UAE",
            initialBalance,
            "CORDA-NODE-001",
            "NOTARY-001"
        );
    }
    
    private DigitalDirham createIslamicBankWallet(Money initialBalance) {
        return DigitalDirham.createWallet(
            DigitalDirhamId.generate(),
            "ISLAMIC-WALLET-001",
            WalletType.ISLAMIC_BANK,
            "EMIRATES-ISLAMIC-BANK",
            initialBalance,
            "CORDA-NODE-001",
            "NOTARY-001"
        );
    }
}