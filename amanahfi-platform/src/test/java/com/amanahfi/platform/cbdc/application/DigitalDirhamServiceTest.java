package com.amanahfi.platform.cbdc.application;

import com.amanahfi.platform.cbdc.domain.*;
import com.amanahfi.platform.cbdc.port.in.*;
import com.amanahfi.platform.cbdc.port.out.CordaNetworkClient;
import com.amanahfi.platform.cbdc.port.out.DigitalDirhamRepository;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.domain.DomainEventPublisher;
import com.amanahfi.platform.shared.domain.Money;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for Digital Dirham application service
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Digital Dirham Service Tests")
class DigitalDirhamServiceTest {
    
    @Mock
    private DigitalDirhamRepository digitalDirhamRepository;
    
    @Mock
    private CordaNetworkClient cordaNetworkClient;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    private DigitalDirhamService service;
    
    @BeforeEach
    void setUp() {
        service = new DigitalDirhamService(digitalDirhamRepository, cordaNetworkClient, eventPublisher);
    }
    
    @Nested
    @DisplayName("Creating Wallets")
    class CreatingWallets {
        
        @Test
        @DisplayName("Should create Central Bank wallet successfully")
        void shouldCreateCentralBankWalletSuccessfully() {
            // Given
            CreateWalletCommand command = CreateWalletCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-001"))
                .metadata(CommandMetadata.create())
                .ownerId("CENTRAL-BANK-UAE")
                .walletType(WalletType.CENTRAL_BANK)
                .initialBalance(Money.aed(new BigDecimal("1000000000")))
                .cordaNodeId("CORDA-NODE-001")
                .notaryId("NOTARY-001")
                .build();
            
            when(digitalDirhamRepository.existsByOwnerIdAndWalletType(
                "CENTRAL-BANK-UAE", WalletType.CENTRAL_BANK))
                .thenReturn(false);
            when(cordaNetworkClient.createWallet(any(), any(), any()))
                .thenReturn("CORDA-WALLET-001");
            
            // When
            DigitalDirhamId result = service.createWallet(command);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getValue()).startsWith("DD-WALLET-");
            
            verify(cordaNetworkClient).createWallet(
                "CENTRAL-BANK-UAE", WalletType.CENTRAL_BANK, command.getInitialBalance()
            );
            verify(digitalDirhamRepository).save(any(DigitalDirham.class));
            verify(eventPublisher).publishAll(anyList());
        }
        
        @Test
        @DisplayName("Should create retail wallet with zero balance")
        void shouldCreateRetailWalletWithZeroBalance() {
            // Given
            CreateWalletCommand command = CreateWalletCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-002"))
                .metadata(CommandMetadata.create())
                .ownerId("CUSTOMER-001")
                .walletType(WalletType.RETAIL)
                .initialBalance(Money.aed(BigDecimal.ZERO))
                .cordaNodeId("CORDA-NODE-001")
                .notaryId("NOTARY-001")
                .build();
            
            when(digitalDirhamRepository.existsByOwnerIdAndWalletType(
                "CUSTOMER-001", WalletType.RETAIL))
                .thenReturn(false);
            when(cordaNetworkClient.createWallet(any(), any(), any()))
                .thenReturn("CORDA-WALLET-002");
            
            // When
            DigitalDirhamId result = service.createWallet(command);
            
            // Then
            assertThat(result).isNotNull();
            verify(cordaNetworkClient).createWallet(
                "CUSTOMER-001", WalletType.RETAIL, Money.aed(BigDecimal.ZERO)
            );
        }
        
        @Test
        @DisplayName("Should reject duplicate wallet creation")
        void shouldRejectDuplicateWalletCreation() {
            // Given
            CreateWalletCommand command = CreateWalletCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-003"))
                .metadata(CommandMetadata.create())
                .ownerId("CUSTOMER-001")
                .walletType(WalletType.RETAIL)
                .initialBalance(Money.aed(BigDecimal.ZERO))
                .cordaNodeId("CORDA-NODE-001")
                .notaryId("NOTARY-001")
                .build();
            
            when(digitalDirhamRepository.existsByOwnerIdAndWalletType(
                "CUSTOMER-001", WalletType.RETAIL))
                .thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> service.createWallet(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wallet already exists");
            
            verify(cordaNetworkClient, never()).createWallet(any(), any(), any());
            verify(digitalDirhamRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("Transferring Funds")
    class TransferringFunds {
        
        @Test
        @DisplayName("Should transfer funds successfully")
        void shouldTransferFundsSuccessfully() {
            // Given
            TransferFundsCommand command = createTransferFundsCommand();
            
            DigitalDirham sourceWallet = createMockWallet("FROM-WALLET-001", Money.aed(new BigDecimal("1000")));
            DigitalDirham destinationWallet = createMockWallet("TO-WALLET-001", Money.aed(BigDecimal.ZERO));
            
            when(digitalDirhamRepository.findByWalletId("FROM-WALLET-001"))
                .thenReturn(Optional.of(sourceWallet));
            when(digitalDirhamRepository.findByWalletId("TO-WALLET-001"))
                .thenReturn(Optional.of(destinationWallet));
            when(cordaNetworkClient.transferFunds(any(), any(), any(), any(), any()))
                .thenReturn("CORDA-TX-001");
            
            // When
            service.transferFunds(command);
            
            // Then
            verify(cordaNetworkClient).transferFunds(
                "FROM-WALLET-001", "TO-WALLET-001", 
                command.getAmount(), command.getReference(), command.getTransferType()
            );
            verify(digitalDirhamRepository, times(2)).save(any(DigitalDirham.class));
            verify(eventPublisher, times(2)).publishAll(anyList());
        }
        
        @Test
        @DisplayName("Should reject transfer from non-existent wallet")
        void shouldRejectTransferFromNonExistentWallet() {
            // Given
            TransferFundsCommand command = createTransferFundsCommand();
            
            when(digitalDirhamRepository.findByWalletId("FROM-WALLET-001"))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> service.transferFunds(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source wallet not found");
        }
        
        @Test
        @DisplayName("Should reject transfer to non-existent wallet")
        void shouldRejectTransferToNonExistentWallet() {
            // Given
            TransferFundsCommand command = createTransferFundsCommand();
            DigitalDirham sourceWallet = createMockWallet("FROM-WALLET-001", Money.aed(new BigDecimal("1000")));
            
            when(digitalDirhamRepository.findByWalletId("FROM-WALLET-001"))
                .thenReturn(Optional.of(sourceWallet));
            when(digitalDirhamRepository.findByWalletId("TO-WALLET-001"))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> service.transferFunds(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination wallet not found");
        }
    }
    
    @Nested
    @DisplayName("Minting Digital Dirham")
    class MintingDigitalDirham {
        
        @Test
        @DisplayName("Should mint Digital Dirham successfully")
        void shouldMintDigitalDirhamSuccessfully() {
            // Given
            MintDigitalDirhamCommand command = createMintDigitalDirhamCommand();
            DigitalDirham centralBankWallet = createCentralBankWallet();
            
            when(digitalDirhamRepository.findByWalletId("CB-WALLET-001"))
                .thenReturn(Optional.of(centralBankWallet));
            when(cordaNetworkClient.mintDigitalDirham(any(), any(), any()))
                .thenReturn("CORDA-MINT-001");
            
            // When
            service.mintDigitalDirham(command);
            
            // Then
            verify(cordaNetworkClient).mintDigitalDirham(
                "CB-WALLET-001", command.getAmount(), command.getAuthorization()
            );
            verify(digitalDirhamRepository).save(centralBankWallet);
            verify(eventPublisher).publishAll(anyList());
        }
        
        @Test
        @DisplayName("Should reject minting from non-Central Bank wallet")
        void shouldRejectMintingFromNonCentralBankWallet() {
            // Given
            MintDigitalDirhamCommand command = createMintDigitalDirhamCommand();
            DigitalDirham retailWallet = createMockWallet("RETAIL-WALLET-001", Money.aed(BigDecimal.ZERO));
            
            when(digitalDirhamRepository.findByWalletId("CB-WALLET-001"))
                .thenReturn(Optional.of(retailWallet));
            
            // When & Then
            assertThatThrownBy(() -> service.mintDigitalDirham(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only Central Bank wallets can mint");
        }
    }
    
    @Nested
    @DisplayName("Burning Digital Dirham")
    class BurningDigitalDirham {
        
        @Test
        @DisplayName("Should burn Digital Dirham successfully")
        void shouldBurnDigitalDirhamSuccessfully() {
            // Given
            BurnDigitalDirhamCommand command = createBurnDigitalDirhamCommand();
            DigitalDirham centralBankWallet = createCentralBankWallet();
            
            when(digitalDirhamRepository.findByWalletId("CB-WALLET-001"))
                .thenReturn(Optional.of(centralBankWallet));
            when(cordaNetworkClient.burnDigitalDirham(any(), any(), any()))
                .thenReturn("CORDA-BURN-001");
            
            // When
            service.burnDigitalDirham(command);
            
            // Then
            verify(cordaNetworkClient).burnDigitalDirham(
                "CB-WALLET-001", command.getAmount(), command.getAuthorization()
            );
            verify(digitalDirhamRepository).save(centralBankWallet);
            verify(eventPublisher).publishAll(anyList());
        }
    }
    
    @Nested
    @DisplayName("Freezing and Unfreezing Wallets")
    class FreezingUnfreezingWallets {
        
        @Test
        @DisplayName("Should freeze wallet successfully")
        void shouldFreezeWalletSuccessfully() {
            // Given
            FreezeWalletCommand command = createFreezeWalletCommand();
            DigitalDirham wallet = createMockWallet("WALLET-001", Money.aed(new BigDecimal("1000")));
            
            when(digitalDirhamRepository.findByWalletId("WALLET-001"))
                .thenReturn(Optional.of(wallet));
            
            // When
            service.freezeWallet(command);
            
            // Then
            verify(cordaNetworkClient).freezeWallet("WALLET-001", command.getReason());
            verify(digitalDirhamRepository).save(wallet);
            verify(eventPublisher).publishAll(anyList());
        }
        
        @Test
        @DisplayName("Should unfreeze wallet successfully")
        void shouldUnfreezeWalletSuccessfully() {
            // Given
            UnfreezeWalletCommand command = createUnfreezeWalletCommand();
            DigitalDirham wallet = createMockWallet("WALLET-001", Money.aed(new BigDecimal("1000")));
            
            when(digitalDirhamRepository.findByWalletId("WALLET-001"))
                .thenReturn(Optional.of(wallet));
            
            // When
            service.unfreezeWallet(command);
            
            // Then
            verify(cordaNetworkClient).unfreezeWallet("WALLET-001", command.getReason());
            verify(digitalDirhamRepository).save(wallet);
            verify(eventPublisher).publishAll(anyList());
        }
    }
    
    @Nested
    @DisplayName("Islamic Finance Operations")
    class IslamicFinanceOperations {
        
        @Test
        @DisplayName("Should process Islamic finance transfer successfully")
        void shouldProcessIslamicFinanceTransferSuccessfully() {
            // Given
            IslamicFinanceTransferCommand command = createIslamicFinanceTransferCommand();
            
            DigitalDirham sourceWallet = createMockWallet("FROM-WALLET-001", Money.aed(new BigDecimal("100000")));
            DigitalDirham destinationWallet = createMockWallet("TO-WALLET-001", Money.aed(BigDecimal.ZERO));
            
            when(digitalDirhamRepository.findByWalletId("FROM-WALLET-001"))
                .thenReturn(Optional.of(sourceWallet));
            when(digitalDirhamRepository.findByWalletId("TO-WALLET-001"))
                .thenReturn(Optional.of(destinationWallet));
            when(cordaNetworkClient.transferFunds(any(), any(), any(), any(), any()))
                .thenReturn("CORDA-TX-001");
            
            // When
            service.processIslamicFinanceTransfer(command);
            
            // Then
            verify(cordaNetworkClient).transferFunds(
                eq("FROM-WALLET-001"), eq("TO-WALLET-001"), 
                eq(command.getAmount()), 
                eq("ISLAMIC_FINANCE: " + command.getReference()),
                eq(TransferType.ISLAMIC_FINANCE)
            );
        }
        
        @Test
        @DisplayName("Should reject large Islamic finance transfer without Sharia approval")
        void shouldRejectLargeIslamicFinanceTransferWithoutShariaApproval() {
            // Given
            IslamicFinanceTransferCommand command = IslamicFinanceTransferCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-001"))
                .metadata(CommandMetadata.create())
                .fromWalletId("FROM-WALLET-001")
                .toWalletId("TO-WALLET-001")
                .amount(Money.aed(new BigDecimal("1500000"))) // Above 1M AED limit
                .reference("Large Islamic finance transfer")
                .islamicFinanceProductId("MURABAHA-001")
                .build(); // No Sharia reference number
            
            // When & Then
            assertThatThrownBy(() -> service.processIslamicFinanceTransfer(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sharia reference number required");
        }
    }
    
    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {
        
        @Test
        @DisplayName("Should get wallet balance successfully")
        void shouldGetWalletBalanceSuccessfully() {
            // Given
            String walletId = "WALLET-001";
            DigitalDirham wallet = createMockWallet(walletId, Money.aed(new BigDecimal("1000")));
            
            when(digitalDirhamRepository.findByWalletId(walletId))
                .thenReturn(Optional.of(wallet));
            
            // When
            BalanceSummary result = service.getBalance(walletId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWalletId()).isEqualTo(walletId);
            assertThat(result.getCurrentBalance()).isEqualTo(Money.aed(new BigDecimal("1000")));
        }
        
        @Test
        @DisplayName("Should get wallets by owner successfully")
        void shouldGetWalletsByOwnerSuccessfully() {
            // Given
            String ownerId = "CUSTOMER-001";
            List<DigitalDirham> wallets = List.of(
                createMockWallet("WALLET-001", Money.aed(new BigDecimal("1000"))),
                createMockWallet("WALLET-002", Money.aed(new BigDecimal("2000")))
            );
            
            when(digitalDirhamRepository.findByOwnerId(ownerId))
                .thenReturn(wallets);
            
            // When
            List<DigitalDirham> result = service.getWalletsByOwner(ownerId);
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(wallets);
        }
        
        @Test
        @DisplayName("Should validate transaction successfully")
        void shouldValidateTransactionSuccessfully() {
            // Given
            String transactionId = "TX-001";
            String cordaHash = "CORDA-HASH-001";
            
            when(cordaNetworkClient.validateTransaction(transactionId, cordaHash))
                .thenReturn(true);
            
            // When
            boolean result = service.validateTransaction(transactionId, cordaHash);
            
            // Then
            assertThat(result).isTrue();
            verify(cordaNetworkClient).validateTransaction(transactionId, cordaHash);
        }
    }
    
    // Helper methods
    
    private TransferFundsCommand createTransferFundsCommand() {
        return TransferFundsCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .fromWalletId("FROM-WALLET-001")
            .toWalletId("TO-WALLET-001")
            .amount(Money.aed(new BigDecimal("500")))
            .reference("Test transfer")
            .transferType(TransferType.SEND)
            .build();
    }
    
    private MintDigitalDirhamCommand createMintDigitalDirhamCommand() {
        return MintDigitalDirhamCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .centralBankWalletId("CB-WALLET-001")
            .amount(Money.aed(new BigDecimal("1000000")))
            .authorization("CBUAE-MINT-AUTH-001")
            .build();
    }
    
    private BurnDigitalDirhamCommand createBurnDigitalDirhamCommand() {
        return BurnDigitalDirhamCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .centralBankWalletId("CB-WALLET-001")
            .amount(Money.aed(new BigDecimal("500000")))
            .authorization("CBUAE-BURN-AUTH-001")
            .build();
    }
    
    private FreezeWalletCommand createFreezeWalletCommand() {
        return FreezeWalletCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .walletId("WALLET-001")
            .reason("Regulatory compliance")
            .authorizedBy("VARA-ADMIN-001")
            .build();
    }
    
    private UnfreezeWalletCommand createUnfreezeWalletCommand() {
        return UnfreezeWalletCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .walletId("WALLET-001")
            .reason("Investigation completed")
            .authorizedBy("VARA-ADMIN-001")
            .build();
    }
    
    private IslamicFinanceTransferCommand createIslamicFinanceTransferCommand() {
        return IslamicFinanceTransferCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .fromWalletId("FROM-WALLET-001")
            .toWalletId("TO-WALLET-001")
            .amount(Money.aed(new BigDecimal("50000")))
            .reference("Murabaha payment")
            .islamicFinanceProductId("MURABAHA-001")
            .build();
    }
    
    private DigitalDirham createMockWallet(String walletId, Money balance) {
        return DigitalDirham.createWallet(
            DigitalDirhamId.fromWalletId(walletId),
            walletId,
            WalletType.RETAIL,
            "CUSTOMER-001",
            balance,
            "CORDA-NODE-001",
            "NOTARY-001"
        );
    }
    
    private DigitalDirham createCentralBankWallet() {
        return DigitalDirham.createWallet(
            DigitalDirhamId.fromWalletId("CB-WALLET-001"),
            "CB-WALLET-001",
            WalletType.CENTRAL_BANK,
            "CENTRAL-BANK-UAE",
            Money.aed(new BigDecimal("1000000000")),
            "CORDA-NODE-001",
            "NOTARY-001"
        );
    }
}