package com.bank.loan.infrastructure.web.enhanced;

import com.bank.loan.application.LoanManagementService;
import com.bank.loan.application.dto.CreateLoanRequest;
import com.bank.loan.application.dto.LoanResponse;
import com.bank.shared.kernel.domain.Money;
import com.bank.shared.kernel.web.ApiResponse;
import com.bank.shared.kernel.web.IdempotencyKey;
import com.bank.shared.kernel.web.TracingHeaders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Enhanced Loan Management API Controller
 * 
 * Implements OpenAPI 3.1+, FAPI2 compliance, and modern financial platform standards
 * Features: Idempotency, HATEOAS, SSE, Async processing, OpenTelemetry
 */
@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loan Management", description = "Loan origination, underwriting, and lifecycle management operations")
@SecurityRequirement(name = "oauth2", scopes = {"loan:read", "loan:write"})
public class LoanApiController {
    
    private final LoanManagementService loanService;
    
    public LoanApiController(LoanManagementService loanService) {
        this.loanService = loanService;
    }
    
    /**
     * Create new loan application with idempotency support
     */
    @PostMapping(
        produces = {MediaType.APPLICATION_JSON_VALUE, "application/hal+json"},
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Create Loan Application",
        description = "Initiates a new loan application with comprehensive credit assessment and risk evaluation",
        operationId = "createLoanApplication"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Loan application created successfully",
            content = @Content(schema = @Schema(implementation = LoanResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Duplicate loan application",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "Credit assessment failed or business validation error",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<LoanResponse>> createLoanApplication(
            @Parameter(description = "Idempotency key for duplicate request prevention", 
                      required = true, example = "loan-2024-001-xyz789")
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            
            @Parameter(description = "Financial institution identifier", 
                      example = "GB-FCA-123456")
            @RequestHeader(value = "X-FAPI-Financial-Id", required = false) String financialId,
            
            @Parameter(description = "Loan application request")
            @Valid @RequestBody CreateLoanRequest request) {
        
        LoanResponse response = loanService.createLoanApplication(request);
        
        // HATEOAS implementation
        EntityModel<LoanResponse> loanModel = EntityModel.of(response)
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoan(response.loanId())).withSelfRel())
            .add(linkTo(methodOn(LoanApiController.class)
                .approveLoan(response.loanId(), null, null)).withRel("approve"))
            .add(linkTo(methodOn(LoanApiController.class)
                .rejectLoan(response.loanId(), null, null)).withRel("reject"))
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoanEvents(response.loanId(), 300)).withRel("events"))
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoanDocuments(response.loanId())).withRel("documents"));

        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Resource-Id", response.loanId())
            .header("X-Idempotency-Key", idempotencyKey)
            .body(loanModel);
    }
    
    /**
     * Get loan with HATEOAS links
     */
    @GetMapping("/{loanId}")
    @Operation(
        summary = "Get Loan Details",
        description = "Retrieves comprehensive loan information with hypermedia controls",
        operationId = "getLoan"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Loan found",
            content = @Content(schema = @Schema(implementation = LoanResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Loan not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN') and " +
                 "(authentication.name == @loanService.getCustomerIdForLoan(#loanId) or hasRole('BANKER') or hasRole('ADMIN'))")
    @TracingHeaders
    public ResponseEntity<EntityModel<LoanResponse>> getLoan(
            @Parameter(description = "Loan identifier", example = "LOAN-12345678")
            @PathVariable @NotBlank String loanId) {
        
        LoanResponse response = loanService.findLoanById(loanId);

        EntityModel<LoanResponse> loanModel = EntityModel.of(response)
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoan(loanId)).withSelfRel());

        // Add conditional links based on loan status
        if ("PENDING_APPROVAL".equals(response.status())) {
            loanModel.add(linkTo(methodOn(LoanApiController.class)
                .approveLoan(loanId, null, null)).withRel("approve"));
            loanModel.add(linkTo(methodOn(LoanApiController.class)
                .rejectLoan(loanId, null, null)).withRel("reject"));
        }

        if ("APPROVED".equals(response.status())) {
            loanModel.add(linkTo(methodOn(LoanApiController.class)
                .disburseLoan(loanId, null, null)).withRel("disburse"));
        }

        if ("ACTIVE".equals(response.status())) {
            loanModel.add(linkTo(methodOn(LoanApiController.class)
                .makePayment(loanId, null, null)).withRel("payment"));
            loanModel.add(linkTo(methodOn(LoanApiController.class)
                .getAmortizationSchedule(loanId)).withRel("amortization-schedule"));
        }

        loanModel.add(linkTo(methodOn(LoanApiController.class)
            .getLoanEvents(loanId, 300)).withRel("events"));
        loanModel.add(linkTo(methodOn(LoanApiController.class)
            .getLoanDocuments(loanId)).withRel("documents"));

        return ResponseEntity.ok()
            .header("X-Resource-Version", response.lastModifiedAt().toString())
            .body(loanModel);
    }
    
    /**
     * Search loans with pagination
     */
    @GetMapping
    @Operation(
        summary = "Search Loans",
        description = "Search and filter loans with pagination support",
        operationId = "searchLoans"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<PagedModel<EntityModel<LoanResponse>>> searchLoans(
            @Parameter(description = "Customer ID filter") 
            @RequestParam(required = false) String customerId,
            
            @Parameter(description = "Loan status filter") 
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Minimum amount filter") 
            @RequestParam(required = false) BigDecimal minAmount,
            
            @Parameter(description = "Maximum amount filter") 
            @RequestParam(required = false) BigDecimal maxAmount,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        // TODO: Implement search in application layer; returning empty result for now
        Page<LoanResponse> loans = Page.empty(pageable);
        
        PagedModel<EntityModel<LoanResponse>> pagedModel = PagedModel.of(
            loans.getContent().stream()
                .map(loan -> EntityModel.of(loan)
                    .add(linkTo(methodOn(LoanApiController.class)
                        .getLoan(loan.loanId())).withSelfRel()))
                .toList(),
            new PagedModel.PageMetadata(loans.getSize(), loans.getNumber(), loans.getTotalElements())
        );
        
        return ResponseEntity.ok(pagedModel);
    }
    
    /**
     * Approve loan application
     */
    @PutMapping("/{loanId}/approve")
    @Operation(
        summary = "Approve Loan",
        description = "Approve a pending loan application after underwriting review",
        operationId = "approveLoan"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<LoanResponse>> approveLoan(
            @PathVariable String loanId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody(required = false) LoanApprovalRequest request) {
        
        LoanResponse response = loanService.approveLoan(loanId);
        
        EntityModel<LoanResponse> loanModel = EntityModel.of(response)
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoan(loanId)).withRel(IanaLinkRelations.SELF))
            .add(linkTo(methodOn(LoanApiController.class)
                .disburseLoan(loanId, null, null)).withRel("disburse"));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(loanModel);
    }
    
    /**
     * Disburse approved loan
     */
    @PutMapping("/{loanId}/disburse")
    @Operation(
        summary = "Disburse Loan",
        description = "Disburse funds for an approved loan",
        operationId = "disburseLoan"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<LoanResponse>> disburseLoan(
            @PathVariable String loanId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody(required = false) LoanDisbursementRequest request) {
        
        LoanResponse response = loanService.disburseLoan(loanId);
        
        EntityModel<LoanResponse> loanModel = EntityModel.of(response)
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoan(loanId)).withRel(IanaLinkRelations.SELF))
            .add(linkTo(methodOn(LoanApiController.class)
                .makePayment(loanId, null, null)).withRel("payment"));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(loanModel);
    }
    
    /**
     * Make loan payment
     */
    @PostMapping("/{loanId}/payments")
    @Operation(
        summary = "Make Loan Payment",
        description = "Process a payment towards the loan principal and interest",
        operationId = "makePayment"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<LoanPaymentResponse>> makePayment(
            @PathVariable String loanId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody LoanPaymentRequest request) {
        
        Currency currency = request.currency() == null || request.currency().isBlank()
            ? Currency.getInstance("AED")
            : Currency.getInstance(request.currency());
        Money paymentAmount = Money.of(request.amount(), currency);
        LoanResponse loanResponse = loanService.makePayment(loanId, paymentAmount);

        LoanPaymentResponse response = new LoanPaymentResponse(
            UUID.randomUUID().toString(),
            loanId,
            paymentAmount.getAmount(),
            paymentAmount.getCurrency().getCurrencyCode(),
            request.paymentMethod(),
            loanResponse.status(),
            LocalDate.now(),
            paymentAmount.getAmount(),
            BigDecimal.ZERO,
            loanResponse.outstandingBalance()
        );
        
        EntityModel<LoanPaymentResponse> paymentModel = EntityModel.of(response)
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoan(loanId)).withRel("loan"))
            .add(linkTo(methodOn(LoanApiController.class)
                .getPaymentHistory(loanId, null)).withRel("payment-history"));

        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(paymentModel);
    }
    
    /**
     * Server-Sent Events for real-time loan updates
     */
    @GetMapping("/{loanId}/events")
    @Operation(
        summary = "Loan Event Stream",
        description = "Real-time stream of loan status changes and events using Server-Sent Events",
        operationId = "getLoanEvents"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public SseEmitter getLoanEvents(
            @PathVariable String loanId,
            @Parameter(description = "Event stream timeout in seconds", example = "300")
            @RequestParam(defaultValue = "300") int timeoutSeconds) {
        
        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(timeoutSeconds).toMillis());
        
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to loan event stream for: " + loanId));
                
                // Subscribe to loan domain events and forward via SSE
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Get loan amortization schedule
     */
    @GetMapping("/{loanId}/amortization-schedule")
    @Operation(
        summary = "Get Amortization Schedule",
        description = "Retrieve detailed payment schedule for the loan",
        operationId = "getAmortizationSchedule"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<LoanAmortizationResponse> getAmortizationSchedule(
            @PathVariable String loanId) {
        
        LoanAmortizationResponse schedule = new LoanAmortizationResponse(loanId, List.of());
        return ResponseEntity.ok(schedule);
    }
    
    /**
     * Get loan documents
     */
    @GetMapping("/{loanId}/documents")
    @Operation(
        summary = "Get Loan Documents",
        description = "Retrieve all documents associated with the loan",
        operationId = "getLoanDocuments"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<LoanDocumentsResponse> getLoanDocuments(
            @PathVariable String loanId) {
        
        LoanDocumentsResponse documents = new LoanDocumentsResponse(loanId, List.of());
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get payment history
     */
    @GetMapping("/{loanId}/payments")
    @Operation(
        summary = "Get Payment History",
        description = "Retrieve payment history for the loan",
        operationId = "getPaymentHistory"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<PagedModel<EntityModel<LoanPaymentResponse>>> getPaymentHistory(
            @PathVariable String loanId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        // TODO: Implement payment history retrieval in application layer
        Page<LoanPaymentResponse> payments = Page.empty(pageable);
        
        PagedModel<EntityModel<LoanPaymentResponse>> pagedModel = PagedModel.of(
            payments.getContent().stream()
                .map(payment -> EntityModel.of(payment)
                    .add(linkTo(methodOn(LoanApiController.class)
                        .getLoan(loanId)).withRel("loan")))
                .toList(),
            new PagedModel.PageMetadata(payments.getSize(), payments.getNumber(), payments.getTotalElements())
        );
        
        return ResponseEntity.ok(pagedModel);
    }
    
    /**
     * Reject loan application
     */
    @PutMapping("/{loanId}/reject")
    @Operation(
        summary = "Reject Loan",
        description = "Reject a pending loan application",
        operationId = "rejectLoan"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<LoanResponse>> rejectLoan(
            @PathVariable String loanId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody LoanRejectionRequest request) {
        
        LoanResponse response = loanService.rejectLoan(loanId, request.reason());
        
        EntityModel<LoanResponse> loanModel = EntityModel.of(response)
            .add(linkTo(methodOn(LoanApiController.class)
                .getLoan(loanId)).withRel(IanaLinkRelations.SELF));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(loanModel);
    }
    
    // Request/Response DTOs
    public record LoanApprovalRequest(
        @Schema(description = "Approval notes") String notes,
        @Schema(description = "Approved amount if different from requested") BigDecimal approvedAmount,
        @Schema(description = "Approved terms if different from requested") Integer approvedTermMonths
    ) {}
    
    public record LoanDisbursementRequest(
        @Schema(description = "Account number for disbursement") String accountNumber,
        @Schema(description = "Disbursement notes") String notes
    ) {}
    
    public record LoanPaymentRequest(
        @Schema(description = "Payment amount", example = "1250.00") BigDecimal amount,
        @Schema(description = "Currency code", example = "USD") String currency,
        @Schema(description = "Payment method") String paymentMethod,
        @Schema(description = "Account number for payment") String accountNumber
    ) {}
    
    public record LoanRejectionRequest(
        @Schema(description = "Reason for rejection") String reason
    ) {}
    
    @Schema(description = "Loan payment response")
    public record LoanPaymentResponse(
        String paymentId,
        String loanId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        LocalDate paymentDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal remainingBalance
    ) {}
    
    @Schema(description = "Loan amortization schedule")
    public record LoanAmortizationResponse(
        String loanId,
        java.util.List<AmortizationEntry> schedule
    ) {
        public record AmortizationEntry(
            int paymentNumber,
            LocalDate paymentDate,
            BigDecimal paymentAmount,
            BigDecimal principalAmount,
            BigDecimal interestAmount,
            BigDecimal remainingBalance
        ) {}
    }
    
    @Schema(description = "Loan documents response")
    public record LoanDocumentsResponse(
        String loanId,
        java.util.List<LoanDocument> documents
    ) {
        public record LoanDocument(
            String documentId,
            String documentType,
            String fileName,
            String downloadUrl,
            LocalDate uploadDate
        ) {}
    }
}
