package com.amanahfi.platform.cbdc.infrastructure.adapter.in.web;

import com.amanahfi.platform.cbdc.application.DigitalDirhamService;
import com.amanahfi.platform.cbdc.infrastructure.adapter.in.web.dto.*;
import com.amanahfi.platform.cbdc.port.in.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/cbdc")
@Validated
@Tag(name = "CBDC", description = "Central Bank Digital Currency operations and Digital Dirham transactions")
@SecurityRequirement(name = "oauth2")
@SecurityRequirement(name = "dpop")
@SecurityRequirement(name = "mtls")
public class CBDCController {

    private final DigitalDirhamService digitalDirhamService;
    private final CBDCMapper mapper;

    public CBDCController(
            DigitalDirhamService digitalDirhamService,
            CBDCMapper mapper) {
        this.digitalDirhamService = digitalDirhamService;
        this.mapper = mapper;
    }

    @PostMapping("/digital-dirham/create-wallet")
    @Operation(
        summary = "Create Digital Dirham Wallet",
        description = "Creates a new Digital Dirham wallet for CBDC transactions on the R3 Corda network",
        tags = {"Digital Dirham", "Wallet Management"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Digital Dirham wallet created successfully",
            content = @Content(schema = @Schema(implementation = DigitalDirhamWalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid wallet creation parameters"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Wallet already exists for the customer"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "R3 Corda network unavailable"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<DigitalDirhamWalletResponse>> createWallet(
            @Valid @RequestBody CreateWalletRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        CreateWalletCommand command = mapper.toCreateWalletCommand(request);
        DigitalDirhamWalletResponse response = digitalDirhamService.createWallet(command);

        EntityModel<DigitalDirhamWalletResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getWallet(UUID.fromString(response.getWalletId()))).withSelfRel())
            .add(linkTo(methodOn(CBDCController.class)
                .getWalletBalance(UUID.fromString(response.getWalletId()))).withRel("balance"))
            .add(linkTo(methodOn(CBDCController.class)
                .getTransactionHistory(UUID.fromString(response.getWalletId()), null)).withRel("transactions"));

        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @GetMapping("/digital-dirham/wallet/{walletId}")
    @Operation(
        summary = "Get Digital Dirham Wallet",
        description = "Retrieves Digital Dirham wallet information and current status",
        tags = {"Digital Dirham", "Wallet Management"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Wallet information retrieved successfully",
            content = @Content(schema = @Schema(implementation = DigitalDirhamWalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Wallet not found"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - customers can only view their own wallets"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<DigitalDirhamWalletResponse>> getWallet(
            @PathVariable @NotNull @Parameter(description = "Unique identifier of the Digital Dirham wallet") 
            UUID walletId,
            Authentication authentication) {

        GetWalletQuery query = new GetWalletQuery(walletId);
        DigitalDirhamWalletResponse response = digitalDirhamService.getWallet(query);

        EntityModel<DigitalDirhamWalletResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getWallet(walletId)).withSelfRel())
            .add(linkTo(methodOn(CBDCController.class)
                .getWalletBalance(walletId)).withRel("balance"));

        if ("ACTIVE".equals(response.getStatus())) {
            entityModel.add(linkTo(methodOn(CBDCController.class)
                .transferFunds(null)).withRel("transfer"));
        }

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/digital-dirham/wallet/{walletId}/balance")
    @Operation(
        summary = "Get Digital Dirham Balance",
        description = "Retrieves the current Digital Dirham balance for a specific wallet",
        tags = {"Digital Dirham", "Balance"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Wallet balance retrieved successfully",
            content = @Content(schema = @Schema(implementation = DigitalDirhamBalanceResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Wallet not found"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<DigitalDirhamBalanceResponse>> getWalletBalance(
            @PathVariable @NotNull UUID walletId,
            Authentication authentication) {

        GetWalletBalanceQuery query = new GetWalletBalanceQuery(walletId);
        DigitalDirhamBalanceResponse response = digitalDirhamService.getWalletBalance(query);

        EntityModel<DigitalDirhamBalanceResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getWalletBalance(walletId)).withSelfRel())
            .add(linkTo(methodOn(CBDCController.class)
                .getWallet(walletId)).withRel("wallet"));

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/digital-dirham/transfer")
    @Operation(
        summary = "Transfer Digital Dirham",
        description = "Initiates a Digital Dirham transfer between wallets on the R3 Corda network. " +
                     "Supports both domestic and cross-border CBDC transactions.",
        tags = {"Digital Dirham", "Transfers"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "202",
            description = "Transfer initiated successfully - processing asynchronously",
            content = @Content(schema = @Schema(implementation = DigitalDirhamTransferResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid transfer parameters or insufficient balance"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Transfer violates regulatory compliance rules"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "R3 Corda network temporarily unavailable"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<DigitalDirhamTransferResponse>> transferFunds(
            @Valid @RequestBody TransferFundsRequest request,
            Authentication authentication) {

        TransferFundsCommand command = mapper.toTransferFundsCommand(request);
        
        // Initiate async transfer
        CompletableFuture<DigitalDirhamTransferResponse> futureResponse = 
            digitalDirhamService.transferFundsAsync(command);

        // Return immediate response with tracking info
        DigitalDirhamTransferResponse response = DigitalDirhamTransferResponse.builder()
            .transferId(command.getTransferId().toString())
            .status("INITIATED")
            .message("Transfer initiated successfully and is being processed")
            .build();

        EntityModel<DigitalDirhamTransferResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferStatus(UUID.fromString(response.getTransferId()))).withSelfRel())
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferStatus(UUID.fromString(response.getTransferId()))).withRel("status"));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(entityModel);
    }

    @GetMapping("/digital-dirham/transfer/{transferId}/status")
    @Operation(
        summary = "Get Transfer Status",
        description = "Retrieves the current status of a Digital Dirham transfer including blockchain confirmation details",
        tags = {"Digital Dirham", "Transfers"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transfer status retrieved successfully",
            content = @Content(schema = @Schema(implementation = TransferStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transfer not found"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<TransferStatusResponse>> getTransferStatus(
            @PathVariable @NotNull @Parameter(description = "Unique identifier of the transfer") 
            UUID transferId,
            Authentication authentication) {

        GetTransferStatusQuery query = new GetTransferStatusQuery(transferId);
        TransferStatusResponse response = digitalDirhamService.getTransferStatus(query);

        EntityModel<TransferStatusResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferStatus(transferId)).withSelfRel());

        if ("COMPLETED".equals(response.getStatus()) || "CONFIRMED".equals(response.getStatus())) {
            entityModel.add(linkTo(methodOn(CBDCController.class)
                .getTransferDetails(transferId)).withRel("details"));
        }

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/digital-dirham/transfer/{transferId}")
    @Operation(
        summary = "Get Transfer Details",
        description = "Retrieves comprehensive details of a Digital Dirham transfer including blockchain transaction info",
        tags = {"Digital Dirham", "Transfers"}
    )
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<DigitalDirhamTransferDetailsResponse>> getTransferDetails(
            @PathVariable @NotNull UUID transferId,
            Authentication authentication) {

        GetTransferDetailsQuery query = new GetTransferDetailsQuery(transferId);
        DigitalDirhamTransferDetailsResponse response = digitalDirhamService.getTransferDetails(query);

        EntityModel<DigitalDirhamTransferDetailsResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferDetails(transferId)).withSelfRel())
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferStatus(transferId)).withRel("status"));

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/digital-dirham/mint")
    @Operation(
        summary = "Mint Digital Dirham",
        description = "Mints new Digital Dirham tokens - restricted to central bank operations",
        tags = {"Digital Dirham", "Central Bank Operations"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Digital Dirham minted successfully",
            content = @Content(schema = @Schema(implementation = MintDigitalDirhamResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient privileges for minting operation"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Minting violates monetary policy constraints"
        )
    })
    @PreAuthorize("hasRole('CENTRAL_BANK_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MintDigitalDirhamResponse>> mintDigitalDirham(
            @Valid @RequestBody MintDigitalDirhamRequest request,
            Authentication authentication) {

        MintDigitalDirhamCommand command = mapper.toMintDigitalDirhamCommand(request);
        MintDigitalDirhamResponse response = digitalDirhamService.mintDigitalDirham(command);

        EntityModel<MintDigitalDirhamResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getMintingTransaction(UUID.fromString(response.getTransactionId()))).withSelfRel());

        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @PostMapping("/digital-dirham/burn")
    @Operation(
        summary = "Burn Digital Dirham",
        description = "Burns existing Digital Dirham tokens - restricted to central bank operations",
        tags = {"Digital Dirham", "Central Bank Operations"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Digital Dirham burned successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient privileges for burning operation"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Burning violates monetary policy constraints"
        )
    })
    @PreAuthorize("hasRole('CENTRAL_BANK_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<BurnDigitalDirhamResponse>> burnDigitalDirham(
            @Valid @RequestBody BurnDigitalDirhamRequest request,
            Authentication authentication) {

        BurnDigitalDirhamCommand command = mapper.toBurnDigitalDirhamCommand(request);
        BurnDigitalDirhamResponse response = digitalDirhamService.burnDigitalDirham(command);

        EntityModel<BurnDigitalDirhamResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getBurningTransaction(UUID.fromString(response.getTransactionId()))).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/digital-dirham/wallet/{walletId}/freeze")
    @Operation(
        summary = "Freeze Digital Dirham Wallet",
        description = "Freezes a Digital Dirham wallet to prevent transactions - used for compliance and security",
        tags = {"Digital Dirham", "Compliance"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Wallet frozen successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Wallet not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Wallet already frozen"
        )
    })
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('CBDC_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<FreezeWalletResponse>> freezeWallet(
            @PathVariable @NotNull UUID walletId,
            @Valid @RequestBody FreezeWalletRequest request,
            Authentication authentication) {

        FreezeWalletCommand command = mapper.toFreezeWalletCommand(walletId, request);
        FreezeWalletResponse response = digitalDirhamService.freezeWallet(command);

        EntityModel<FreezeWalletResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getWallet(walletId)).withRel("wallet"))
            .add(linkTo(methodOn(CBDCController.class)
                .unfreezeWallet(walletId, null)).withRel("unfreeze"));

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/digital-dirham/wallet/{walletId}/unfreeze")
    @Operation(
        summary = "Unfreeze Digital Dirham Wallet",
        description = "Unfreezes a previously frozen Digital Dirham wallet to restore transaction capabilities",
        tags = {"Digital Dirham", "Compliance"}
    )
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('CBDC_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<UnfreezeWalletResponse>> unfreezeWallet(
            @PathVariable @NotNull UUID walletId,
            @Valid @RequestBody UnfreezeWalletRequest request,
            Authentication authentication) {

        UnfreezeWalletCommand command = mapper.toUnfreezeWalletCommand(walletId, request);
        UnfreezeWalletResponse response = digitalDirhamService.unfreezeWallet(command);

        EntityModel<UnfreezeWalletResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getWallet(walletId)).withRel("wallet"));

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/digital-dirham/wallet/{walletId}/transactions")
    @Operation(
        summary = "Get Transaction History",
        description = "Retrieves paginated transaction history for a Digital Dirham wallet",
        tags = {"Digital Dirham", "Transactions"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transaction history retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedTransactionHistoryResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Wallet not found"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PagedModel<EntityModel<TransactionHistoryResponse>>> getTransactionHistory(
            @PathVariable @NotNull UUID walletId,
            @RequestParam(required = false) 
            @Parameter(description = "Filter by transaction type") String transactionType,
            @RequestParam(required = false) 
            @Parameter(description = "Start date for filtering (ISO format)") String startDate,
            @RequestParam(required = false) 
            @Parameter(description = "End date for filtering (ISO format)") String endDate,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            walletId, transactionType, startDate, endDate, pageable
        );
        
        Page<TransactionHistoryResponse> transactions = digitalDirhamService.getTransactionHistory(query);
        
        PagedModel<EntityModel<TransactionHistoryResponse>> pagedModel = 
            mapper.toTransactionHistoryPagedModel(transactions);

        return ResponseEntity.ok(pagedModel);
    }

    @PostMapping("/islamic-finance-transfer")
    @Operation(
        summary = "Islamic Finance CBDC Transfer",
        description = "Initiates a Sharia-compliant CBDC transfer for Islamic finance operations",
        tags = {"Islamic Finance", "CBDC Integration"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "202",
            description = "Islamic finance transfer initiated successfully"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Transfer violates Sharia compliance rules"
        )
    })
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CBDC_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<IslamicFinanceTransferResponse>> islamicFinanceTransfer(
            @Valid @RequestBody IslamicFinanceTransferRequest request,
            Authentication authentication) {

        IslamicFinanceTransferCommand command = mapper.toIslamicFinanceTransferCommand(request);
        IslamicFinanceTransferResponse response = digitalDirhamService.islamicFinanceTransfer(command);

        EntityModel<IslamicFinanceTransferResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferStatus(UUID.fromString(response.getTransferId()))).withRel("status"))
            .add(linkTo(methodOn(CBDCController.class)
                .validateShariaCompliance(UUID.fromString(response.getTransferId()))).withRel("sharia-validation"));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(entityModel);
    }

    @GetMapping("/transfer/{transferId}/sharia-compliance")
    @Operation(
        summary = "Validate Sharia Compliance for CBDC Transfer",
        description = "Validates Sharia compliance for a CBDC transfer in Islamic finance context",
        tags = {"Compliance", "Islamic Finance"}
    )
    @PreAuthorize("hasRole('SHARIA_BOARD_MEMBER') or hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ShariaComplianceValidationResponse>> validateShariaCompliance(
            @PathVariable @NotNull UUID transferId,
            Authentication authentication) {

        ValidateShariaComplianceQuery query = new ValidateShariaComplianceQuery(transferId);
        ShariaComplianceValidationResponse response = digitalDirhamService.validateShariaCompliance(query);

        EntityModel<ShariaComplianceValidationResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .validateShariaCompliance(transferId)).withSelfRel())
            .add(linkTo(methodOn(CBDCController.class)
                .getTransferDetails(transferId)).withRel("transfer"));

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/network/status")
    @Operation(
        summary = "Get R3 Corda Network Status",
        description = "Retrieves the current status and health of the R3 Corda CBDC network",
        tags = {"Network", "Monitoring"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Network status retrieved successfully",
            content = @Content(schema = @Schema(implementation = NetworkStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Network is experiencing issues"
        )
    })
    @PreAuthorize("hasRole('CBDC_OPERATOR') or hasRole('SYSTEM_MONITOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<NetworkStatusResponse>> getNetworkStatus(
            Authentication authentication) {

        GetNetworkStatusQuery query = new GetNetworkStatusQuery();
        NetworkStatusResponse response = digitalDirhamService.getNetworkStatus(query);

        EntityModel<NetworkStatusResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getNetworkStatus()).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/mint-transaction/{transactionId}")
    @Operation(
        summary = "Get Minting Transaction Details",
        description = "Retrieves details of a Digital Dirham minting transaction",
        tags = {"Central Bank Operations"}
    )
    @PreAuthorize("hasRole('CENTRAL_BANK_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MintTransactionDetailsResponse>> getMintingTransaction(
            @PathVariable @NotNull UUID transactionId) {

        GetMintTransactionQuery query = new GetMintTransactionQuery(transactionId);
        MintTransactionDetailsResponse response = digitalDirhamService.getMintingTransaction(query);

        EntityModel<MintTransactionDetailsResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getMintingTransaction(transactionId)).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/burn-transaction/{transactionId}")
    @Operation(
        summary = "Get Burning Transaction Details",
        description = "Retrieves details of a Digital Dirham burning transaction",
        tags = {"Central Bank Operations"}
    )
    @PreAuthorize("hasRole('CENTRAL_BANK_OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<BurnTransactionDetailsResponse>> getBurningTransaction(
            @PathVariable @NotNull UUID transactionId) {

        GetBurnTransactionQuery query = new GetBurnTransactionQuery(transactionId);
        BurnTransactionDetailsResponse response = digitalDirhamService.getBurningTransaction(query);

        EntityModel<BurnTransactionDetailsResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(CBDCController.class)
                .getBurningTransaction(transactionId)).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }
}