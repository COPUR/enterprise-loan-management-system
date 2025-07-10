package com.masrufi.sharia.controller;

import com.masrufi.sharia.dto.*;
import com.masrufi.sharia.service.ShariaFinancingService;
import com.masrufi.sharia.service.UAECryptoIntegrationService;
import com.masrufi.sharia.service.ShariaComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * UAE Sharia Financing Controller
 * REST API for UAE Government-driven crypto-based Islamic financing
 * 
 * Endpoints for:
 * - UAE CBDC financing applications
 * - UAE bank digital currency payments
 * - Sharia compliance validation
 * - UAE blockchain integration
 */
@RestController
@RequestMapping("/api/v1/uae-sharia")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "UAE Sharia Financing", description = "UAE Government-driven crypto Islamic financing APIs")
public class UAEShariaFinancingController {
    
    private final ShariaFinancingService financingService;
    private final UAECryptoIntegrationService uaeCryptoService;
    private final ShariaComplianceService complianceService;
    
    @Operation(summary = "Apply for UAE crypto-based Islamic financing")
    @PostMapping("/financing/apply")
    public ResponseEntity<FinancingResponse> applyForFinancing(
            @Valid @RequestBody UAEFinancingApplicationRequest request) {
        
        log.info("UAE Islamic financing application received for customer: {} using currency: {}", 
            request.getCustomerId(), request.getUaeCurrency());
        
        // Convert UAE-specific request to standard financing request
        var standardRequest = convertToStandardRequest(request);
        var response = financingService.applyForFinancing(standardRequest);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Process payment using UAE digital currency")
    @PostMapping("/payment/uae-crypto")
    public ResponseEntity<UAECryptoPaymentResponse> processUAECryptoPayment(
            @Valid @RequestBody UAECryptoPaymentRequest request) {
        
        log.info("Processing UAE crypto payment for financing: {}", request.getFinancingReference());
        
        var response = uaeCryptoService.processUAECryptoPayment(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get UAE digital currency exchange rates")
    @GetMapping("/rates/uae-crypto")
    public ResponseEntity<Map<String, BigDecimal>> getUAECryptoRates() {
        var rates = uaeCryptoService.getUAECryptoRates();
        return ResponseEntity.ok(rates);
    }
    
    @Operation(summary = "Get supported UAE digital currencies")
    @GetMapping("/currencies/uae")
    public ResponseEntity<UAEDigitalCurrenciesResponse> getSupportedUAECurrencies() {
        var currencies = uaeCryptoService.getSupportedUAECurrencies();
        
        var response = UAEDigitalCurrenciesResponse.builder()
            .currencies(currencies)
            .totalCount(currencies.size())
            .lastUpdated(java.time.LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Create UAE CBDC wallet for customer")
    @PostMapping("/wallet/cbdc/create")
    public ResponseEntity<UAEWalletResponse> createCBDCWallet(
            @Valid @RequestBody UAEWalletRequest request) {
        
        log.info("Creating UAE CBDC wallet for customer: {}", request.getCustomerId());
        
        var walletAddress = uaeCryptoService.createUAECBDCWallet(
            request.getCustomerId(), 
            request.getEmiratesId()
        );
        
        var response = UAEWalletResponse.builder()
            .customerId(request.getCustomerId())
            .walletAddress(walletAddress)
            .walletType("UAE-CBDC")
            .status("ACTIVE")
            .createdAt(java.time.LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Validate UAE bank crypto wallet")
    @PostMapping("/wallet/validate")
    public ResponseEntity<UAEWalletValidationResponse> validateUAEBankWallet(
            @Valid @RequestBody UAEWalletValidationRequest request) {
        
        log.info("Validating UAE bank wallet: {} for bank: {}", 
            request.getWalletAddress(), request.getBankCode());
        
        var isValid = uaeCryptoService.validateUAEBankWallet(
            request.getWalletAddress(), 
            request.getBankCode()
        );
        
        var response = UAEWalletValidationResponse.builder()
            .walletAddress(request.getWalletAddress())
            .bankCode(request.getBankCode())
            .isValid(isValid)
            .isShariaCompliant(isValid) // If valid, then it's Sharia compliant
            .validatedAt(java.time.LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get financing details")
    @GetMapping("/financing/{financingReference}")
    public ResponseEntity<FinancingResponse> getFinancing(
            @Parameter(description = "Financing reference number")
            @PathVariable String financingReference) {
        
        var response = financingService.getFinancing(financingReference);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get customer's UAE financings")
    @GetMapping("/financing/customer/{customerId}")
    public ResponseEntity<List<FinancingResponse>> getCustomerFinancings(
            @Parameter(description = "Customer ID")
            @PathVariable String customerId) {
        
        var financings = financingService.getCustomerFinancings(customerId);
        return ResponseEntity.ok(financings);
    }
    
    @Operation(summary = "Get payment schedule for financing")
    @GetMapping("/financing/{financingReference}/schedule")
    public ResponseEntity<PaymentScheduleResponse> getPaymentSchedule(
            @Parameter(description = "Financing reference number")
            @PathVariable String financingReference) {
        
        var schedule = financingService.getPaymentSchedule(financingReference);
        
        var response = PaymentScheduleResponse.builder()
            .financingReference(financingReference)
            .payments(schedule)
            .totalPayments(schedule.size())
            .paidPayments((int) schedule.stream().filter(p -> p.isPaid()).count())
            .nextPaymentDue(schedule.stream()
                .filter(p -> !p.isPaid())
                .findFirst()
                .map(p -> p.getDueDate())
                .orElse(null))
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get Sharia compliance status")
    @GetMapping("/compliance/{financingReference}")
    public ResponseEntity<ShariaComplianceResponse> getComplianceStatus(
            @Parameter(description = "Financing reference number")
            @PathVariable String financingReference) {
        
        var complianceStatus = complianceService.getComplianceStatus(financingReference);
        return ResponseEntity.ok(complianceStatus);
    }
    
    @Operation(summary = "Submit financing for Sharia board review")
    @PostMapping("/compliance/submit/{financingReference}")
    public ResponseEntity<ShariaReviewResponse> submitForShariaReview(
            @Parameter(description = "Financing reference number")
            @PathVariable String financingReference,
            @Valid @RequestBody ShariaReviewRequest request) {
        
        log.info("Submitting financing {} for Sharia board review", financingReference);
        
        var reviewResponse = complianceService.submitForShariaReview(financingReference, request);
        return ResponseEntity.ok(reviewResponse);
    }
    
    @Operation(summary = "Get UAE banking partners")
    @GetMapping("/partners/banks")
    public ResponseEntity<UAEBankingPartnersResponse> getUAEBankingPartners() {
        var partners = List.of(
            UAEBankPartner.builder()
                .bankCode("ADIB")
                .bankName("Abu Dhabi Islamic Bank")
                .digitalCurrency("ADIB-DD")
                .islamicBanking(true)
                .cbdcSupport(true)
                .build(),
            UAEBankPartner.builder()
                .bankCode("ENBD")
                .bankName("Emirates NBD")
                .digitalCurrency("ENBD-DC")
                .islamicBanking(true)
                .cbdcSupport(true)
                .build(),
            UAEBankPartner.builder()
                .bankCode("FAB")
                .bankName("First Abu Dhabi Bank")
                .digitalCurrency("FAB-DT")
                .islamicBanking(true)
                .cbdcSupport(true)
                .build(),
            UAEBankPartner.builder()
                .bankCode("CBD")
                .bankName("Commercial Bank of Dubai")
                .digitalCurrency("CBD-DD")
                .islamicBanking(true)
                .cbdcSupport(true)
                .build(),
            UAEBankPartner.builder()
                .bankCode("RAKBANK")
                .bankName("The National Bank of Ras Al-Khaimah")
                .digitalCurrency("RAK-DC")
                .islamicBanking(true)
                .cbdcSupport(true)
                .build(),
            UAEBankPartner.builder()
                .bankCode("MASHREQ")
                .bankName("Mashreq Bank")
                .digitalCurrency("MASHREQ-DC")
                .islamicBanking(true)
                .cbdcSupport(true)
                .build()
        );
        
        var response = UAEBankingPartnersResponse.builder()
            .partners(partners)
            .totalPartners(partners.size())
            .lastUpdated(java.time.LocalDateTime.now())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get UAE government blockchain status")
    @GetMapping("/blockchain/status")
    public ResponseEntity<UAEBlockchainStatusResponse> getBlockchainStatus() {
        var response = UAEBlockchainStatusResponse.builder()
            .network("UAE Government Blockchain")
            .status("OPERATIONAL")
            .blockHeight(12345678L)
            .lastBlockTime(java.time.LocalDateTime.now().minusMinutes(2))
            .cbdcSupport(true)
            .shariaCompliant(true)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    // Private helper methods
    
    private FinancingApplicationRequest convertToStandardRequest(UAEFinancingApplicationRequest uaeRequest) {
        return FinancingApplicationRequest.builder()
            .customerId(uaeRequest.getCustomerId())
            .financingType(uaeRequest.getFinancingType())
            .principalAmount(uaeRequest.getPrincipalAmount())
            .paymentFrequency(uaeRequest.getPaymentFrequency())
            .startDate(uaeRequest.getStartDate())
            .termInDays(uaeRequest.getTermInDays())
            .isAssetBacked(uaeRequest.getIsAssetBacked())
            .assetDetails(uaeRequest.getAssetDetails())
            .blockchainNetwork("UAE-GOVERNMENT")
            .cryptoCurrency(uaeRequest.getUaeCurrency())
            .cryptoAmount(uaeRequest.getCryptoAmount())
            .useSmartContract(true)
            .applicantId(uaeRequest.getApplicantId())
            .emiratesId(uaeRequest.getEmiratesId())
            .build();
    }
}