package com.amanahfi.platform.islamicfinance.infrastructure.adapter.in.web;

import com.amanahfi.platform.islamicfinance.application.service.EnhancedIslamicFinanceService;
import com.amanahfi.platform.islamicfinance.infrastructure.adapter.in.web.dto.*;
import com.amanahfi.platform.islamicfinance.port.in.*;
import com.amanahfi.platform.shared.exception.ValidationException;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/islamic-finance")
@Validated
@Tag(name = "Islamic Finance", description = "Sharia-compliant financial products and services")
@SecurityRequirement(name = "oauth2")
@SecurityRequirement(name = "dpop")
public class IslamicFinanceController {

    private final EnhancedIslamicFinanceService islamicFinanceService;
    private final IslamicFinanceMapper mapper;

    public IslamicFinanceController(
            EnhancedIslamicFinanceService islamicFinanceService,
            IslamicFinanceMapper mapper) {
        this.islamicFinanceService = islamicFinanceService;
        this.mapper = mapper;
    }

    @PostMapping("/murabaha")
    @Operation(
        summary = "Create Murabaha Financing",
        description = "Creates a new Sharia-compliant Murabaha cost-plus financing arrangement. " +
                     "Validates asset permissibility, profit margin compliance, and customer eligibility.",
        tags = {"Murabaha"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Murabaha financing created successfully",
            content = @Content(schema = @Schema(implementation = MurabahaFinancingResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or business rule violation",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Sharia compliance violation",
            content = @Content(schema = @Schema(implementation = ShariaViolationResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions"
        )
    })
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MurabahaFinancingResponse>> createMurabaha(
            @Valid @RequestBody CreateMurabahaRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        CreateMurabahaCommand command = mapper.toCreateMurabahaCommand(request);
        MurabahaFinancingResponse response = islamicFinanceService.createMurabaha(command);

        EntityModel<MurabahaFinancingResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMurabaha(UUID.fromString(response.getFinancingId()))).withSelfRel())
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .approveMurabaha(UUID.fromString(response.getFinancingId()), null)).withRel("approve"))
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getPaymentSchedule(UUID.fromString(response.getFinancingId()))).withRel("payment-schedule"));

        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @GetMapping("/murabaha/{financingId}")
    @Operation(
        summary = "Get Murabaha Financing Details",
        description = "Retrieves detailed information about a specific Murabaha financing arrangement",
        tags = {"Murabaha"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Murabaha financing details retrieved successfully",
            content = @Content(schema = @Schema(implementation = MurabahaFinancingResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Murabaha financing not found"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - customer can only view their own financings"
        )
    })
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MurabahaFinancingResponse>> getMurabaha(
            @PathVariable @NotNull @Parameter(description = "Unique identifier of the Murabaha financing") 
            UUID financingId,
            Authentication authentication) {

        GetMurabahaQuery query = new GetMurabahaQuery(financingId);
        MurabahaFinancingResponse response = islamicFinanceService.getMurabaha(query);

        EntityModel<MurabahaFinancingResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMurabaha(financingId)).withSelfRel());

        if ("PENDING_APPROVAL".equals(response.getStatus())) {
            entityModel.add(linkTo(methodOn(IslamicFinanceController.class)
                .approveMurabaha(financingId, null)).withRel("approve"));
        }

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/murabaha/{financingId}/approve")
    @Operation(
        summary = "Approve Murabaha Financing",
        description = "Approves a Murabaha financing after Sharia board review and validation",
        tags = {"Murabaha"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Murabaha financing approved successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Murabaha financing not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Financing cannot be approved in current status"
        )
    })
    @PreAuthorize("hasRole('SHARIA_BOARD_MEMBER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MurabahaFinancingResponse>> approveMurabaha(
            @PathVariable @NotNull UUID financingId,
            @Valid @RequestBody ApproveMurabahaRequest request) {

        ApproveMurabahaCommand command = mapper.toApproveMurabahaCommand(financingId, request);
        MurabahaFinancingResponse response = islamicFinanceService.approveMurabaha(command);

        EntityModel<MurabahaFinancingResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMurabaha(financingId)).withSelfRel())
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getPaymentSchedule(financingId)).withRel("payment-schedule"));

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/musharakah")
    @Operation(
        summary = "Create Musharakah Partnership",
        description = "Creates a new Sharia-compliant Musharakah partnership with profit/loss sharing",
        tags = {"Musharakah"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Musharakah partnership created successfully",
            content = @Content(schema = @Schema(implementation = MusharakahPartnershipResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid partnership terms or structure"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Sharia compliance violation in partnership structure"
        )
    })
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MusharakahPartnershipResponse>> createMusharakah(
            @Valid @RequestBody CreateMusharakahRequest request,
            Authentication authentication) {

        CreateMusharakahCommand command = mapper.toCreateMusharakahCommand(request);
        MusharakahPartnershipResponse response = islamicFinanceService.createMusharakah(command);

        EntityModel<MusharakahPartnershipResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMusharakah(UUID.fromString(response.getPartnershipId()))).withSelfRel())
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .updatePartnershipContribution(UUID.fromString(response.getPartnershipId()), null)).withRel("update-contribution"));

        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @GetMapping("/musharakah/{partnershipId}")
    @Operation(
        summary = "Get Musharakah Partnership Details",
        description = "Retrieves detailed information about a specific Musharakah partnership",
        tags = {"Musharakah"}
    )
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MusharakahPartnershipResponse>> getMusharakah(
            @PathVariable @NotNull UUID partnershipId,
            Authentication authentication) {

        GetMusharakahQuery query = new GetMusharakahQuery(partnershipId);
        MusharakahPartnershipResponse response = islamicFinanceService.getMusharakah(query);

        EntityModel<MusharakahPartnershipResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMusharakah(partnershipId)).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/ijarah")
    @Operation(
        summary = "Create Ijarah Lease Agreement",
        description = "Creates a new Sharia-compliant Ijarah lease agreement with optional ownership transfer",
        tags = {"Ijarah"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Ijarah lease agreement created successfully",
            content = @Content(schema = @Schema(implementation = IjarahLeaseResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid lease terms or asset details"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Asset not suitable for Islamic leasing"
        )
    })
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<IjarahLeaseResponse>> createIjarah(
            @Valid @RequestBody CreateIjarahRequest request,
            Authentication authentication) {

        CreateIjarahCommand command = mapper.toCreateIjarahCommand(request);
        IjarahLeaseResponse response = islamicFinanceService.createIjarah(command);

        EntityModel<IjarahLeaseResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getIjarah(UUID.fromString(response.getLeaseId()))).withSelfRel())
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .processRentalPayment(UUID.fromString(response.getLeaseId()), null)).withRel("rental-payment"));

        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @GetMapping("/ijarah/{leaseId}")
    @Operation(
        summary = "Get Ijarah Lease Details",
        description = "Retrieves detailed information about a specific Ijarah lease agreement",
        tags = {"Ijarah"}
    )
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<IjarahLeaseResponse>> getIjarah(
            @PathVariable @NotNull UUID leaseId,
            Authentication authentication) {

        GetIjarahQuery query = new GetIjarahQuery(leaseId);
        IjarahLeaseResponse response = islamicFinanceService.getIjarah(query);

        EntityModel<IjarahLeaseResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getIjarah(leaseId)).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/products")
    @Operation(
        summary = "List Islamic Finance Products",
        description = "Retrieves a paginated list of Islamic finance products with filtering options",
        tags = {"Products"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Islamic finance products retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedIslamicFinanceProductsResponse.class))
        )
    })
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PagedModel<EntityModel<IslamicFinanceProductResponse>>> getProducts(
            @RequestParam(required = false) 
            @Parameter(description = "Filter by product type") String productType,
            @RequestParam(required = false) 
            @Parameter(description = "Filter by customer ID") UUID customerId,
            @RequestParam(required = false) 
            @Parameter(description = "Filter by status") String status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        GetIslamicFinanceProductsQuery query = new GetIslamicFinanceProductsQuery(
            productType, customerId, status, pageable
        );
        
        Page<IslamicFinanceProductResponse> products = islamicFinanceService.getProducts(query);
        
        // Convert to PagedModel with HATEOAS links
        PagedModel<EntityModel<IslamicFinanceProductResponse>> pagedModel = 
            mapper.toPagedModel(products);

        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/murabaha/{financingId}/payment-schedule")
    @Operation(
        summary = "Get Murabaha Payment Schedule",
        description = "Retrieves the payment schedule for a Murabaha financing arrangement",
        tags = {"Murabaha", "Payments"}
    )
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<PaymentScheduleResponse>> getPaymentSchedule(
            @PathVariable @NotNull UUID financingId,
            Authentication authentication) {

        GetPaymentScheduleQuery query = new GetPaymentScheduleQuery(financingId);
        PaymentScheduleResponse response = islamicFinanceService.getPaymentSchedule(query);

        EntityModel<PaymentScheduleResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getPaymentSchedule(financingId)).withSelfRel())
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMurabaha(financingId)).withRel("financing"));

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/musharakah/{partnershipId}/contribution")
    @Operation(
        summary = "Update Partnership Contribution",
        description = "Updates capital contribution in a Musharakah partnership",
        tags = {"Musharakah"}
    )
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<MusharakahPartnershipResponse>> updatePartnershipContribution(
            @PathVariable @NotNull UUID partnershipId,
            @Valid @RequestBody UpdateContributionRequest request) {

        UpdateContributionCommand command = mapper.toUpdateContributionCommand(partnershipId, request);
        MusharakahPartnershipResponse response = islamicFinanceService.updateContribution(command);

        EntityModel<MusharakahPartnershipResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getMusharakah(partnershipId)).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping("/ijarah/{leaseId}/rental-payment")
    @Operation(
        summary = "Process Rental Payment",
        description = "Processes a rental payment for an Ijarah lease agreement",
        tags = {"Ijarah", "Payments"}
    )
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<RentalPaymentResponse>> processRentalPayment(
            @PathVariable @NotNull UUID leaseId,
            @Valid @RequestBody ProcessRentalPaymentRequest request) {

        ProcessRentalPaymentCommand command = mapper.toProcessRentalPaymentCommand(leaseId, request);
        RentalPaymentResponse response = islamicFinanceService.processRentalPayment(command);

        EntityModel<RentalPaymentResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getIjarah(leaseId)).withRel("lease"))
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .getRentalPaymentHistory(leaseId)).withRel("payment-history"));

        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @GetMapping("/ijarah/{leaseId}/payment-history")
    @Operation(
        summary = "Get Rental Payment History",
        description = "Retrieves the rental payment history for an Ijarah lease",
        tags = {"Ijarah", "Payments"}
    )
    @PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER') or hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PagedModel<EntityModel<RentalPaymentResponse>>> getRentalPaymentHistory(
            @PathVariable @NotNull UUID leaseId,
            @PageableDefault(size = 20) Pageable pageable) {

        GetRentalPaymentHistoryQuery query = new GetRentalPaymentHistoryQuery(leaseId, pageable);
        Page<RentalPaymentResponse> payments = islamicFinanceService.getRentalPaymentHistory(query);
        
        PagedModel<EntityModel<RentalPaymentResponse>> pagedModel = 
            mapper.toRentalPaymentPagedModel(payments);

        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/compliance/validate")
    @Operation(
        summary = "Validate Sharia Compliance",
        description = "Validates Sharia compliance for Islamic finance products and transactions",
        tags = {"Compliance"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Compliance validation completed",
            content = @Content(schema = @Schema(implementation = ComplianceValidationResponse.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Compliance validation failed",
            content = @Content(schema = @Schema(implementation = ShariaViolationResponse.class))
        )
    })
    @PreAuthorize("hasRole('SHARIA_BOARD_MEMBER') or hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ComplianceValidationResponse>> validateCompliance(
            @RequestParam @NotNull @Parameter(description = "Product ID to validate") UUID productId,
            @RequestParam @Parameter(description = "Validation type") String validationType) {

        ValidateComplianceQuery query = new ValidateComplianceQuery(productId, validationType);
        ComplianceValidationResponse response = islamicFinanceService.validateCompliance(query);

        EntityModel<ComplianceValidationResponse> entityModel = EntityModel.of(response)
            .add(linkTo(methodOn(IslamicFinanceController.class)
                .validateCompliance(productId, validationType)).withSelfRel());

        return ResponseEntity.ok(entityModel);
    }
}