package com.bank.payment.infrastructure.web.enhanced;

import com.bank.payment.application.PaymentProcessingService;
import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
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
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.concurrent.CompletableFuture;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Enhanced Payment Processing API Controller
 * 
 * Implements OpenAPI 3.1+, FAPI2 compliance, and modern financial platform standards
 * Features: Idempotency, HATEOAS, SSE, Async processing, OpenTelemetry
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Processing", description = "Real-time payment processing, validation, and settlement operations")
@SecurityRequirement(name = "oauth2", scopes = {"payment:read", "payment:write"})
public class PaymentApiController {
    
    private final PaymentProcessingService paymentService;
    
    public PaymentApiController(PaymentProcessingService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Process new payment with idempotency support
     */
    @PostMapping(
        produces = {MediaType.APPLICATION_JSON_VALUE, "application/hal+json"},
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Process Payment",
        description = "Initiates a new payment with comprehensive validation, fraud detection, and real-time settlement",
        operationId = "processPayment"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment initiated successfully",
            content = @Content(schema = @Schema(implementation = PaymentHalResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "402",
            description = "Insufficient funds",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Duplicate payment request",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "Fraud detection alert or compliance violation",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<PaymentResponse>> processPayment(
            @Parameter(description = "Idempotency key for duplicate request prevention", 
                      required = true, example = "pay-2024-001-uuid123")
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            
            @Parameter(description = "Financial institution identifier", 
                      example = "GB-FCA-123456")
            @RequestHeader(value = "X-FAPI-Financial-Id", required = false) String financialId,
            
            @Parameter(description = "Client interaction ID for audit trails", 
                      example = "client-interaction-001")
            @RequestHeader(value = "X-FAPI-Interaction-Id", required = false) String interactionId,
            
            @Parameter(description = "Payment processing request")
            @Valid @RequestBody CreatePaymentRequest request) {
        
        PaymentResponse response = paymentService.processPayment(request);
        
        // HATEOAS implementation
        EntityModel<PaymentResponse> paymentModel = EntityModel.of(response)
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPayment(response.paymentId())).withSelfRel())
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPaymentEvents(response.paymentId())).withRel("events"));
        
        // Add conditional links based on payment status
        if ("PENDING".equals(response.status())) {
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .cancelPayment(response.paymentId(), null)).withRel("cancel"));
        }
        
        if ("FAILED".equals(response.status())) {
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .retryPayment(response.paymentId(), null)).withRel("retry"));
        }
        
        if ("COMPLETED".equals(response.status())) {
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .refundPayment(response.paymentId(), null)).withRel("refund"));
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .getPaymentReceipt(response.paymentId())).withRel("receipt"));
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Resource-Id", response.paymentId())
            .header("X-Idempotency-Key", idempotencyKey)
            .header("X-FAPI-Interaction-Id", interactionId)
            .body(paymentModel);
    }
    
    /**
     * Get payment with HATEOAS links
     */
    @GetMapping("/{paymentId}")
    @Operation(
        summary = "Get Payment Details",
        description = "Retrieves comprehensive payment information with hypermedia controls",
        operationId = "getPayment"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentHalResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN') and " +
                 "(#paymentId == @paymentService.getCustomerIdForPayment(#paymentId) or hasRole('BANKER') or hasRole('ADMIN'))")
    @TracingHeaders
    public ResponseEntity<EntityModel<PaymentResponse>> getPayment(
            @Parameter(description = "Payment identifier", example = "PAY-12345678")
            @PathVariable @NotBlank String paymentId) {
        
        PaymentResponse response = paymentService.findPaymentById(paymentId);
        
        EntityModel<PaymentResponse> paymentModel = EntityModel.of(response)
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPayment(paymentId)).withSelfRel())
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPaymentEvents(paymentId)).withRel("events"));
        
        // Add conditional links
        if ("PENDING".equals(response.status())) {
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .cancelPayment(paymentId, null)).withRel("cancel"));
        }
        
        if ("FAILED".equals(response.status())) {
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .retryPayment(paymentId, null)).withRel("retry"));
        }
        
        if ("COMPLETED".equals(response.status())) {
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .refundPayment(paymentId, null)).withRel("refund"));
            paymentModel.add(linkTo(methodOn(PaymentApiController.class)
                .getPaymentReceipt(paymentId)).withRel("receipt"));
        }
        
        return ResponseEntity.ok()
            .header("X-Resource-Version", response.lastModifiedAt().toString())
            .body(paymentModel);
    }
    
    /**
     * Search payments with pagination
     */
    @GetMapping
    @Operation(
        summary = "Search Payments",
        description = "Search and filter payments with pagination support",
        operationId = "searchPayments"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<PagedModel<EntityModel<PaymentResponse>>> searchPayments(
            @Parameter(description = "Customer ID filter") 
            @RequestParam(required = false) String customerId,
            
            @Parameter(description = "Payment status filter") 
            @RequestParam(required = false) String status,
            
            @Parameter(description = "From account filter") 
            @RequestParam(required = false) String fromAccount,
            
            @Parameter(description = "To account filter") 
            @RequestParam(required = false) String toAccount,
            
            @Parameter(description = "Minimum amount filter") 
            @RequestParam(required = false) BigDecimal minAmount,
            
            @Parameter(description = "Maximum amount filter") 
            @RequestParam(required = false) BigDecimal maxAmount,
            
            @Parameter(description = "Start date filter (ISO 8601)") 
            @RequestParam(required = false) LocalDateTime startDate,
            
            @Parameter(description = "End date filter (ISO 8601)") 
            @RequestParam(required = false) LocalDateTime endDate,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<PaymentResponse> payments = paymentService.searchPayments(
            customerId, status, fromAccount, toAccount, minAmount, maxAmount, 
            startDate, endDate, pageable);
        
        PagedModel<EntityModel<PaymentResponse>> pagedModel = PagedModel.of(
            payments.getContent().stream()
                .map(payment -> EntityModel.of(payment)
                    .add(linkTo(methodOn(PaymentApiController.class)
                        .getPayment(payment.paymentId())).withSelfRel()))
                .toList(),
            new PagedModel.PageMetadata(payments.getSize(), payments.getNumber(), payments.getTotalElements())
        );
        
        return ResponseEntity.ok(pagedModel);
    }
    
    /**
     * Cancel pending payment
     */
    @PutMapping("/{paymentId}/cancel")
    @Operation(
        summary = "Cancel Payment",
        description = "Cancel a pending payment before it's processed",
        operationId = "cancelPayment"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<PaymentResponse>> cancelPayment(
            @PathVariable String paymentId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody(required = false) PaymentCancellationRequest request) {
        
        PaymentResponse response = paymentService.cancelPayment(paymentId);
        
        EntityModel<PaymentResponse> paymentModel = EntityModel.of(response)
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPayment(paymentId)).withRel(IanaLinkRelations.SELF));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(paymentModel);
    }
    
    /**
     * Retry failed payment
     */
    @PostMapping("/{paymentId}/retry")
    @Operation(
        summary = "Retry Payment",
        description = "Retry a failed payment with optional modifications",
        operationId = "retryPayment"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<PaymentResponse>> retryPayment(
            @PathVariable String paymentId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody(required = false) PaymentRetryRequest request) {
        
        PaymentResponse response = paymentService.retryPayment(paymentId);
        
        EntityModel<PaymentResponse> paymentModel = EntityModel.of(response)
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPayment(paymentId)).withRel(IanaLinkRelations.SELF))
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPaymentEvents(paymentId)).withRel("events"));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(paymentModel);
    }
    
    /**
     * Refund completed payment
     */
    @PostMapping("/{paymentId}/refund")
    @Operation(
        summary = "Refund Payment",
        description = "Process a full or partial refund for a completed payment",
        operationId = "refundPayment"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<PaymentRefundResponse>> refundPayment(
            @PathVariable String paymentId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody PaymentRefundRequest request) {
        
        PaymentRefundResponse response = paymentService.refundPayment(paymentId, request);
        
        EntityModel<PaymentRefundResponse> refundModel = EntityModel.of(response)
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPayment(paymentId)).withRel("original-payment"))
            .add(linkTo(methodOn(PaymentApiController.class)
                .getPayment(response.refundPaymentId())).withRel("refund-payment"));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(refundModel);
    }
    
    /**
     * Server-Sent Events for real-time payment updates
     */
    @GetMapping("/{paymentId}/events")
    @Operation(
        summary = "Payment Event Stream",
        description = "Real-time stream of payment status changes and events using Server-Sent Events",
        operationId = "getPaymentEvents"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public SseEmitter getPaymentEvents(
            @PathVariable String paymentId,
            @Parameter(description = "Event stream timeout in seconds", example = "300")
            @RequestParam(defaultValue = "300") int timeoutSeconds) {
        
        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(timeoutSeconds).toMillis());
        
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to payment event stream for: " + paymentId));
                
                // Subscribe to payment domain events and forward via SSE
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Get payment receipt
     */
    @GetMapping("/{paymentId}/receipt")
    @Operation(
        summary = "Get Payment Receipt",
        description = "Generate and retrieve payment receipt with transaction details",
        operationId = "getPaymentReceipt"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<PaymentReceiptResponse> getPaymentReceipt(
            @PathVariable String paymentId,
            @Parameter(description = "Receipt format") 
            @RequestParam(defaultValue = "json") String format) {
        
        PaymentReceiptResponse receipt = paymentService.generateReceipt(paymentId);
        
        if ("pdf".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=payment-receipt-" + paymentId + ".pdf")
                .body(receipt);
        }
        
        return ResponseEntity.ok(receipt);
    }
    
    /**
     * Get payment fraud analysis
     */
    @GetMapping("/{paymentId}/fraud-analysis")
    @Operation(
        summary = "Get Fraud Analysis",
        description = "Retrieve fraud detection analysis for the payment",
        operationId = "getFraudAnalysis"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<PaymentFraudAnalysisResponse> getFraudAnalysis(
            @PathVariable String paymentId) {
        
        PaymentFraudAnalysisResponse analysis = paymentService.getFraudAnalysis(paymentId);
        return ResponseEntity.ok(analysis);
    }
    
    /**
     * Get payment compliance report
     */
    @GetMapping("/{paymentId}/compliance")
    @Operation(
        summary = "Get Compliance Report",
        description = "Retrieve compliance checks and AML analysis for the payment",
        operationId = "getComplianceReport"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<PaymentComplianceResponse> getComplianceReport(
            @PathVariable String paymentId) {
        
        PaymentComplianceResponse compliance = paymentService.getComplianceReport(paymentId);
        return ResponseEntity.ok(compliance);
    }
    
    // Request/Response DTOs
    public record PaymentCancellationRequest(
        @Schema(description = "Reason for cancellation") String reason
    ) {}
    
    public record PaymentRetryRequest(
        @Schema(description = "Retry reason") String reason,
        @Schema(description = "Modified amount for retry") BigDecimal newAmount,
        @Schema(description = "Modified account for retry") String newFromAccount
    ) {}
    
    public record PaymentRefundRequest(
        @Schema(description = "Refund amount", example = "100.00") BigDecimal amount,
        @Schema(description = "Refund reason") String reason,
        @Schema(description = "Refund type") String refundType
    ) {}
    
    @Schema(description = "Payment response with HATEOAS links")
    public static class PaymentHalResponse extends PaymentResponse {
        // Extends PaymentResponse with HAL JSON format
    }
    
    @lombok.Builder
    @Schema(description = "Payment refund response")
    public record PaymentRefundResponse(
        String refundId,
        String originalPaymentId,
        String refundPaymentId,
        BigDecimal refundAmount,
        String currency,
        String status,
        String reason,
        LocalDateTime processedAt
    ) {}
    
    @lombok.Builder
    @Schema(description = "Payment receipt")
    public record PaymentReceiptResponse(
        String paymentId,
        String receiptNumber,
        PaymentResponse paymentDetails,
        String merchantInfo,
        String customerInfo,
        LocalDateTime generatedAt,
        String digitalSignature
    ) {}
    
    @lombok.Builder
    @Schema(description = "Payment fraud analysis")
    public record PaymentFraudAnalysisResponse(
        String paymentId,
        Double riskScore,
        String riskLevel,
        java.util.List<FraudIndicator> indicators,
        String analysis,
        LocalDateTime analyzedAt
    ) {
        public record FraudIndicator(
            String type,
            String description,
            Double score,
            String severity
        ) {}
    }
    
    @lombok.Builder
    @Schema(description = "Payment compliance report")
    public record PaymentComplianceResponse(
        String paymentId,
        boolean amlCompliant,
        boolean sanctionScreenPassed,
        boolean kycVerified,
        java.util.List<ComplianceCheck> checks,
        String overallStatus,
        LocalDateTime checkedAt
    ) {
        public record ComplianceCheck(
            String checkType,
            String result,
            String details,
            LocalDateTime performedAt
        ) {}
    }
}