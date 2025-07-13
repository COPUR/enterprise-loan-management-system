package com.bank.customer.infrastructure.web.enhanced;

import com.bank.customer.application.CustomerManagementService;
import com.bank.customer.application.dto.CreateCustomerRequest;
import com.bank.customer.application.dto.CustomerResponse;
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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Currency;
import java.util.concurrent.CompletableFuture;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Enhanced Customer Management API Controller
 * 
 * Implements OpenAPI 3.1+, FAPI2 compliance, and modern financial platform standards
 * Features: Idempotency, HATEOAS, SSE, Async processing, OpenTelemetry
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Customer lifecycle and credit management operations")
@SecurityRequirement(name = "oauth2", scopes = {"customer:read", "customer:write"})
public class CustomerApiController {
    
    private final CustomerManagementService customerService;
    
    public CustomerApiController(CustomerManagementService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * Create new customer with idempotency support
     */
    @PostMapping(
        produces = {MediaType.APPLICATION_JSON_VALUE, "application/hal+json"},
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Create Customer",
        description = "Creates a new customer account with comprehensive KYC validation and credit assessment",
        operationId = "createCustomer"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Customer created successfully",
            content = @Content(schema = @Schema(implementation = CustomerHalResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Customer already exists",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "Business validation failed",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<CustomerResponse>> createCustomer(
            @Parameter(description = "Idempotency key for duplicate request prevention", 
                      required = true, example = "cust-2024-001-abc123")
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            
            @Parameter(description = "Financial institution identifier", 
                      example = "GB-FCA-123456")
            @RequestHeader(value = "X-FAPI-Financial-Id", required = false) String financialId,
            
            @Parameter(description = "Customer creation request")
            @Valid @RequestBody CreateCustomerRequest request) {
        
        CustomerResponse response = customerService.createCustomer(request);
        
        // HATEOAS implementation
        EntityModel<CustomerResponse> customerModel = EntityModel.of(response)
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomer(response.customerId())).withSelfRel())
            .add(linkTo(methodOn(CustomerApiController.class)
                .updateCreditLimit(response.customerId(), null)).withRel("update-credit-limit"))
            .add(linkTo(methodOn(CustomerApiController.class)
                .reserveCredit(response.customerId(), null)).withRel("reserve-credit"))
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomerEvents(response.customerId())).withRel("events"))
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomerMetrics(response.customerId())).withRel("metrics"));
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Resource-Id", response.customerId())
            .header("X-Idempotency-Key", idempotencyKey)
            .body(customerModel);
    }
    
    /**
     * Get customer with HATEOAS links
     */
    @GetMapping("/{customerId}")
    @Operation(
        summary = "Get Customer",
        description = "Retrieves customer information with hypermedia controls",
        operationId = "getCustomer"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Customer found",
            content = @Content(schema = @Schema(implementation = CustomerHalResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN') and " +
                 "(#customerId == authentication.name or hasRole('BANKER') or hasRole('ADMIN'))")
    @TracingHeaders
    public ResponseEntity<EntityModel<CustomerResponse>> getCustomer(
            @Parameter(description = "Customer identifier", example = "CUST-12345678")
            @PathVariable @NotBlank String customerId) {
        
        CustomerResponse response = customerService.findCustomerById(customerId);
        
        EntityModel<CustomerResponse> customerModel = EntityModel.of(response)
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomer(customerId)).withSelfRel())
            .add(linkTo(methodOn(CustomerApiController.class)
                .updateCreditLimit(customerId, null)).withRel("update-credit-limit"))
            .add(linkTo(methodOn(CustomerApiController.class)
                .reserveCredit(customerId, null)).withRel("reserve-credit"))
            .add(linkTo(methodOn(CustomerApiController.class)
                .releaseCredit(customerId, null)).withRel("release-credit"));
        
        return ResponseEntity.ok()
            .header("X-Resource-Version", response.lastModifiedAt().toString())
            .body(customerModel);
    }
    
    /**
     * Update customer credit limit with optimistic locking
     */
    @PutMapping("/{customerId}/credit-limit")
    @Operation(
        summary = "Update Credit Limit",
        description = "Updates customer credit limit with business rule validation",
        operationId = "updateCreditLimit"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Credit limit updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "422",
            description = "Business rule validation failed"
        )
    })
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<CustomerResponse>> updateCreditLimit(
            @PathVariable String customerId,
            @Parameter(description = "Idempotency key for duplicate request prevention")
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody UpdateCreditLimitRequest request) {
        
        Money newLimit = Money.of(request.amount(), Currency.getInstance(request.currency()));
        CustomerResponse response = customerService.updateCreditLimit(customerId, newLimit);
        
        EntityModel<CustomerResponse> customerModel = EntityModel.of(response)
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomer(customerId)).withRel(IanaLinkRelations.SELF))
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomerEvents(customerId)).withRel("events"));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(customerModel);
    }
    
    /**
     * Server-Sent Events for real-time customer updates
     */
    @GetMapping("/{customerId}/events")
    @Operation(
        summary = "Customer Event Stream",
        description = "Real-time stream of customer events using Server-Sent Events",
        operationId = "getCustomerEvents"
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public SseEmitter getCustomerEvents(
            @PathVariable String customerId,
            @Parameter(description = "Event stream timeout in seconds", example = "300")
            @RequestParam(defaultValue = "300") int timeoutSeconds) {
        
        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(timeoutSeconds).toMillis());
        
        // Implement SSE logic for customer events
        CompletableFuture.runAsync(() -> {
            try {
                // Send initial connection event
                emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to customer event stream for: " + customerId));
                
                // Subscribe to customer domain events and forward via SSE
                // This would integrate with the existing event system
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Get customer metrics and analytics
     */
    @GetMapping("/{customerId}/metrics")
    @Operation(
        summary = "Customer Metrics",
        description = "Retrieve customer analytics and performance metrics",
        operationId = "getCustomerMetrics"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<CustomerMetricsResponse> getCustomerMetrics(
            @PathVariable String customerId) {
        
        // Implementation would gather customer metrics
        CustomerMetricsResponse metrics = CustomerMetricsResponse.builder()
            .customerId(customerId)
            .totalTransactions(0L)
            .totalVolume(java.math.BigDecimal.ZERO)
            .averageTransactionValue(java.math.BigDecimal.ZERO)
            .riskScore(0.0)
            .creditUtilization(0.0)
            .build();
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Reserve credit with idempotency
     */
    @PostMapping("/{customerId}/credit/reserve")
    @Operation(
        summary = "Reserve Credit",
        description = "Reserve credit amount for pending transactions",
        operationId = "reserveCredit"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<CustomerResponse>> reserveCredit(
            @PathVariable String customerId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody ReserveCreditRequest request) {
        
        Money amount = Money.of(request.amount(), Currency.getInstance(request.currency()));
        CustomerResponse response = customerService.reserveCredit(customerId, amount);
        
        EntityModel<CustomerResponse> customerModel = EntityModel.of(response)
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomer(customerId)).withRel(IanaLinkRelations.SELF))
            .add(linkTo(methodOn(CustomerApiController.class)
                .releaseCredit(customerId, null)).withRel("release-credit"));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(customerModel);
    }
    
    /**
     * Release reserved credit
     */
    @PostMapping("/{customerId}/credit/release")
    @Operation(
        summary = "Release Credit",
        description = "Release previously reserved credit amount",
        operationId = "releaseCredit"
    )
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    @TracingHeaders
    public ResponseEntity<EntityModel<CustomerResponse>> releaseCredit(
            @PathVariable String customerId,
            @RequestHeader("Idempotency-Key") @IdempotencyKey String idempotencyKey,
            @RequestBody ReleaseCreditRequest request) {
        
        Money amount = Money.of(request.amount(), Currency.getInstance(request.currency()));
        CustomerResponse response = customerService.releaseCredit(customerId, amount);
        
        EntityModel<CustomerResponse> customerModel = EntityModel.of(response)
            .add(linkTo(methodOn(CustomerApiController.class)
                .getCustomer(customerId)).withRel(IanaLinkRelations.SELF));
        
        return ResponseEntity.ok()
            .header("X-Idempotency-Key", idempotencyKey)
            .body(customerModel);
    }
    
    // Request/Response DTOs
    public record UpdateCreditLimitRequest(
        @Schema(description = "New credit limit amount", example = "50000.00")
        java.math.BigDecimal amount,
        
        @Schema(description = "Currency code", example = "USD")
        String currency,
        
        @Schema(description = "Reason for credit limit change")
        String reason
    ) {}
    
    public record ReserveCreditRequest(
        @Schema(description = "Amount to reserve", example = "1000.00")
        java.math.BigDecimal amount,
        
        @Schema(description = "Currency code", example = "USD") 
        String currency,
        
        @Schema(description = "Reason for reservation")
        String reason
    ) {}
    
    public record ReleaseCreditRequest(
        @Schema(description = "Amount to release", example = "1000.00")
        java.math.BigDecimal amount,
        
        @Schema(description = "Currency code", example = "USD")
        String currency,
        
        @Schema(description = "Reason for release")
        String reason
    ) {}
    
    @Schema(description = "Customer response with HATEOAS links")
    public static class CustomerHalResponse extends CustomerResponse {
        // Extends CustomerResponse with HAL JSON format
    }
    
    @lombok.Builder
    @Schema(description = "Customer analytics and metrics")
    public record CustomerMetricsResponse(
        String customerId,
        Long totalTransactions,
        java.math.BigDecimal totalVolume,
        java.math.BigDecimal averageTransactionValue,
        Double riskScore,
        Double creditUtilization
    ) {}
}