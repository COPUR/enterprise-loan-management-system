package com.bank.loanmanagement.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import com.bank.loanmanagement.domain.Customer;
import com.bank.loanmanagement.domain.Loan;
import com.bank.loanmanagement.domain.Payment;
import com.bank.loanmanagement.security.FAPISecurityValidator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OpenBanking and OpenFinance FAPI-compliant API Gateway
 * Implements FAPI 1.0 Advanced security requirements for financial services
 * Supports OpenBanking Account Information and Payment Initiation APIs
 * 
 * FAPI Compliance Features:
 * - OAuth2 PKCE with S256 code challenge
 * - Request signing with JWS
 * - Response signing and encryption
 * - MTLS for client authentication
 * - Structured error responses per FAPI spec
 */
@RestController
@RequestMapping("/fapi/v1")
@Validated
@CrossOrigin(origins = {"https://*.openbanking.org.uk", "https://*.openfinance.org"})
public class OpenBankingAPIGateway {

    @Autowired
    private FAPISecurityValidator fapiValidator;

    /**
     * FAPI Account Information API - Get Account Details
     * OpenBanking AISP (Account Information Service Provider) endpoint
     */
    @GetMapping("/accounts/{AccountId}")
    @PreAuthorize("hasAuthority('SCOPE_accounts') and hasAuthority('SCOPE_openbanking_aisp')")
    public ResponseEntity<?> getAccount(
            @PathVariable @NotNull String AccountId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-customer-ip-address") String customerIP,
            @RequestHeader("x-fapi-interaction-id") String interactionId) {
        
        try {
            // FAPI security validation
            fapiValidator.validateFAPIHeaders(authDate, customerIP, interactionId);
            
            // Get customer account information
            Customer customer = customerService.findById(Long.valueOf(AccountId));
            
            // Map to OpenBanking account structure
            var response = createAccountResponse(customer, AccountId);
            
            return ResponseEntity.ok()
                    .header("x-fapi-interaction-id", interactionId)
                    .header("x-jws-signature", fapiValidator.signResponse(response))
                    .body(response);
                    
        } catch (Exception e) {
            return handleFAPIError(e, interactionId);
        }
    }

    /**
     * FAPI Account Information API - Get Account Balances
     */
    @GetMapping("/accounts/{AccountId}/balances")
    @PreAuthorize("hasAuthority('SCOPE_accounts') and hasAuthority('SCOPE_openbanking_aisp')")
    public ResponseEntity<?> getAccountBalances(
            @PathVariable @NotNull String AccountId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-customer-ip-address") String customerIP,
            @RequestHeader("x-fapi-interaction-id") String interactionId) {
        
        try {
            fapiValidator.validateFAPIHeaders(authDate, customerIP, interactionId);
            
            Customer customer = customerService.findById(Long.valueOf(AccountId));
            List<Loan> customerLoans = loanService.findByCustomerId(customer.getId());
            
            var response = createBalanceResponse(customer, customerLoans, AccountId);
            
            return ResponseEntity.ok()
                    .header("x-fapi-interaction-id", interactionId)
                    .header("x-jws-signature", fapiValidator.signResponse(response))
                    .body(response);
                    
        } catch (Exception e) {
            return handleFAPIError(e, interactionId);
        }
    }

    /**
     * FAPI Payment Initiation API - Create Payment Consent
     */
    @PostMapping("/domestic-payment-consents")
    @PreAuthorize("hasAuthority('SCOPE_payments') and hasAuthority('SCOPE_openbanking_pisp')")
    public ResponseEntity<?> createPaymentConsent(
            @Valid @RequestBody Object request,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-customer-ip-address") String customerIP,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("x-jws-signature") String jwsSignature) {
        
        try {
            fapiValidator.validateFAPIHeaders(authDate, customerIP, interactionId);
            fapiValidator.validateRequestSignature(request, jwsSignature);
            
            String consentId = UUID.randomUUID().toString();
            var response = createPaymentConsentResponse(consentId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("x-fapi-interaction-id", interactionId)
                    .header("x-jws-signature", fapiValidator.signResponse(response))
                    .body(response);
                    
        } catch (Exception e) {
            return handleFAPIError(e, interactionId);
        }
    }

    /**
     * OpenFinance Credit Information API - Get Credit Offers
     */
    @GetMapping("/credit-offers")
    @PreAuthorize("hasAuthority('SCOPE_credit_offers') and hasAuthority('SCOPE_openfinance')")
    public ResponseEntity<?> getCreditOffers(
            @RequestParam String customerId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-customer-ip-address") String customerIP,
            @RequestHeader("x-fapi-interaction-id") String interactionId) {
        
        try {
            fapiValidator.validateFAPIHeaders(authDate, customerIP, interactionId);
            
            Customer customer = customerService.findById(Long.valueOf(customerId));
            var response = createCreditOfferResponse(customer);
            
            return ResponseEntity.ok()
                    .header("x-fapi-interaction-id", interactionId)
                    .header("x-jws-signature", fapiValidator.signResponse(response))
                    .body(response);
                    
        } catch (Exception e) {
            return handleFAPIError(e, interactionId);
        }
    }

    private Object createAccountResponse(Customer customer, String accountId) {
        return Map.of(
            "Data", Map.of(
                "Account", Map.of(
                    "AccountId", accountId,
                    "Currency", "USD",
                    "AccountType", "Loan",
                    "AccountSubType", "CreditCard",
                    "Nickname", customer.getName() + " Loan Account",
                    "OpeningDate", customer.getCreatedAt().toLocalDate().toString()
                )
            ),
            "Links", Map.of("Self", "/fapi/v1/accounts/" + accountId),
            "Meta", Map.of("TotalPages", 1)
        );
    }

    private Object createBalanceResponse(Customer customer, List<Loan> loans, String accountId) {
        double outstandingBalance = loans.stream()
                .filter(loan -> "APPROVED".equals(loan.getStatus()))
                .mapToDouble(loan -> loan.getAmount() - (loan.getPaidAmount() != null ? loan.getPaidAmount() : 0))
                .sum();
        
        return Map.of(
            "Data", Map.of(
                "Balance", List.of(
                    Map.of(
                        "AccountId", accountId,
                        "CreditDebitIndicator", "Debit",
                        "Type", "OpeningAvailable",
                        "DateTime", OffsetDateTime.now().toString(),
                        "Amount", Map.of(
                            "Amount", String.format("%.2f", outstandingBalance),
                            "Currency", "USD"
                        )
                    )
                )
            ),
            "Links", Map.of("Self", "/fapi/v1/accounts/" + accountId + "/balances"),
            "Meta", Map.of("TotalPages", 1)
        );
    }

    private Object createPaymentConsentResponse(String consentId) {
        return Map.of(
            "Data", Map.of(
                "ConsentId", consentId,
                "Status", "AwaitingAuthorisation",
                "CreationDateTime", OffsetDateTime.now().toString(),
                "StatusUpdateDateTime", OffsetDateTime.now().toString()
            ),
            "Links", Map.of("Self", "/fapi/v1/domestic-payment-consents/" + consentId),
            "Meta", Map.of("TotalPages", 1)
        );
    }

    private Object createCreditOfferResponse(Customer customer) {
        List<Map<String, Object>> offers = List.of(
            Map.of(
                "OfferId", UUID.randomUUID().toString(),
                "ProductType", "PERSONAL_LOAN",
                "InterestRate", calculatePersonalizedRate(customer),
                "MaxAmount", calculateMaxLoanAmount(customer),
                "TermMonths", List.of(12, 24, 36, 48),
                "ValidUntil", OffsetDateTime.now().plusDays(30).toString()
            )
        );
        
        return Map.of(
            "Data", Map.of("Offers", offers),
            "Links", Map.of("Self", "/fapi/v1/credit-offers"),
            "Meta", Map.of("TotalPages", 1)
        );
    }

    private ResponseEntity<?> handleFAPIError(Exception e, String interactionId) {
        Map<String, Object> error = Map.of(
            "Code", "500",
            "Id", interactionId,
            "Message", "Internal Server Error",
            "Errors", List.of(Map.of(
                "ErrorCode", "UK.OBIE.UnexpectedError",
                "Message", "An unexpected error occurred",
                "Path", "/fapi/v1"
            ))
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("x-fapi-interaction-id", interactionId)
                .body(error);
    }

    private double calculatePersonalizedRate(Customer customer) {
        if (customer.getCreditScore() >= 750) return 0.08;
        if (customer.getCreditScore() >= 700) return 0.12;
        if (customer.getCreditScore() >= 650) return 0.16;
        return 0.20;
    }

    private double calculateMaxLoanAmount(Customer customer) {
        return customer.getCreditScore() * 1000;
    }
}