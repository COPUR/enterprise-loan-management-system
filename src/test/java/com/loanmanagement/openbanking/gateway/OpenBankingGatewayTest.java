package com.loanmanagement.openbanking.gateway;

import com.loanmanagement.openbanking.domain.model.*;
import com.loanmanagement.openbanking.gateway.service.OpenBankingGatewayService;
import com.loanmanagement.openbanking.gateway.service.FAPIComplianceService;
import com.loanmanagement.openbanking.gateway.service.ConsentManagementService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Open Banking Gateway
 * Comprehensive testing of FAPI-compliant Open Banking functionality
 */
@DisplayName("Open Banking Gateway Tests")
class OpenBankingGatewayTest {

    private OpenBankingGatewayService openBankingGatewayService;
    private FAPIComplianceService fapiComplianceService;
    private ConsentManagementService consentManagementService;
    private OpenBankingCustomerId testCustomerId;
    private BankAccountId testAccountId;
    private Money testAmount;

    @BeforeEach
    void setUp() {
        openBankingGatewayService = new OpenBankingGatewayService();
        fapiComplianceService = new FAPIComplianceService();
        consentManagementService = new ConsentManagementService();
        testCustomerId = OpenBankingCustomerId.generate();
        testAccountId = BankAccountId.generate();
        testAmount = Money.of("GBP", new BigDecimal("1500.00"));
    }

    @Nested
    @DisplayName("Account Information Service (AIS) Tests")
    class AccountInformationServiceTests {

        @Test
        @DisplayName("Should retrieve account information with valid consent")
        void shouldRetrieveAccountInformationWithValidConsent() {
            // Given
            AccountInformationRequest aisRequest = AccountInformationRequest.builder()
                    .customerId(testCustomerId)
                    .accountId(testAccountId)
                    .permissions(List.of(OpenBankingPermission.READ_ACCOUNTS, 
                                       OpenBankingPermission.READ_BALANCES))
                    .consentId(ConsentId.generate())
                    .requestId(RequestId.generate())
                    .build();

            // When
            AccountInformationResponse response = openBankingGatewayService
                    .getAccountInformation(aisRequest);

            // Then
            assertNotNull(response);
            assertEquals(OpenBankingStatus.SUCCESS, response.getStatus());
            assertNotNull(response.getAccountData());
            assertNotNull(response.getAccountData().getAccountId());
            assertNotNull(response.getAccountData().getBalance());
            assertTrue(response.getAccountData().getBalance().getAmount().compareTo(BigDecimal.ZERO) >= 0);
            assertNotNull(response.getFapiInteractionId());
        }

        @Test
        @DisplayName("Should retrieve account transactions with date range")
        void shouldRetrieveAccountTransactionsWithDateRange() {
            // Given
            TransactionHistoryRequest request = TransactionHistoryRequest.builder()
                    .customerId(testCustomerId)
                    .accountId(testAccountId)
                    .fromBookingDateTime(LocalDateTime.now().minusDays(30))
                    .toBookingDateTime(LocalDateTime.now())
                    .consentId(ConsentId.generate())
                    .permissions(List.of(OpenBankingPermission.READ_TRANSACTIONS))
                    .build();

            // When
            TransactionHistoryResponse response = openBankingGatewayService
                    .getTransactionHistory(request);

            // Then
            assertNotNull(response);
            assertEquals(OpenBankingStatus.SUCCESS, response.getStatus());
            assertNotNull(response.getTransactions());
            assertFalse(response.getTransactions().isEmpty());
            
            // Verify transaction data structure
            BankTransaction firstTransaction = response.getTransactions().get(0);
            assertNotNull(firstTransaction.getTransactionId());
            assertNotNull(firstTransaction.getAmount());
            assertNotNull(firstTransaction.getBookingDateTime());
            assertNotNull(firstTransaction.getTransactionReference());
        }

        @Test
        @DisplayName("Should handle account information request with insufficient permissions")
        void shouldHandleAccountInformationRequestWithInsufficientPermissions() {
            // Given
            AccountInformationRequest invalidRequest = AccountInformationRequest.builder()
                    .customerId(testCustomerId)
                    .accountId(testAccountId)
                    .permissions(List.of(OpenBankingPermission.READ_ACCOUNTS)) // Missing READ_BALANCES
                    .consentId(ConsentId.generate())
                    .build();

            // When
            AccountInformationResponse response = openBankingGatewayService
                    .getAccountInformation(invalidRequest);

            // Then
            assertNotNull(response);
            assertEquals(OpenBankingStatus.INSUFFICIENT_PERMISSIONS, response.getStatus());
            assertNotNull(response.getErrorCode());
            assertEquals(OpenBankingErrorCode.INSUFFICIENT_SCOPE, response.getErrorCode());
            assertNotNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("Should validate FAPI compliance for account information requests")
        void shouldValidateFAPIComplianceForAccountInformationRequests() {
            // Given
            AccountInformationRequest request = AccountInformationRequest.builder()
                    .customerId(testCustomerId)
                    .accountId(testAccountId)
                    .permissions(List.of(OpenBankingPermission.READ_ACCOUNTS))
                    .consentId(ConsentId.generate())
                    .fapiAuthDate("Sun, 10 Sep 2017 19:43:31 GMT")
                    .fapiCustomerIpAddress("192.168.1.1")
                    .fapiInteractionId(FAPIInteractionId.generate())
                    .build();

            // When
            FAPIComplianceResult complianceResult = fapiComplianceService
                    .validateAccountInformationRequest(request);

            // Then
            assertNotNull(complianceResult);
            assertTrue(complianceResult.isCompliant());
            assertTrue(complianceResult.getValidationResults().stream()
                    .allMatch(FAPIValidationResult::isValid));
            assertNotNull(complianceResult.getComplianceLevel());
            assertEquals(FAPIComplianceLevel.FAPI_R_RW, complianceResult.getComplianceLevel());
        }
    }

    @Nested
    @DisplayName("Payment Initiation Service (PIS) Tests")
    class PaymentInitiationServiceTests {

        @Test
        @DisplayName("Should initiate domestic payment with valid consent")
        void shouldInitiateDomesticPaymentWithValidConsent() {
            // Given
            DomesticPaymentRequest paymentRequest = DomesticPaymentRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount("12345678", "123456"))
                    .creditorAccount(createTestAccount("87654321", "654321"))
                    .instructedAmount(testAmount)
                    .endToEndIdentification("E2E123456789")
                    .reference("Loan Payment")
                    .consentId(ConsentId.generate())
                    .fapiInteractionId(FAPIInteractionId.generate())
                    .build();

            // When
            PaymentInitiationResponse response = openBankingGatewayService
                    .initiateDomesticPayment(paymentRequest);

            // Then
            assertNotNull(response);
            assertEquals(OpenBankingStatus.SUCCESS, response.getStatus());
            assertNotNull(response.getPaymentId());
            assertNotNull(response.getConsentId());
            assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, response.getPaymentStatus());
            assertNotNull(response.getStatusUpdateDateTime());
            assertNotNull(response.getFapiInteractionId());
        }

        @Test
        @DisplayName("Should initiate international payment with currency conversion")
        void shouldInitiateInternationalPaymentWithCurrencyConversion() {
            // Given
            InternationalPaymentRequest internationalRequest = InternationalPaymentRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount("GB29NWBK60161331926819", "NWBKGB2L"))
                    .creditorAccount(createTestAccount("DE89370400440532013000", "DEUTDEFF"))
                    .instructedAmount(Money.of("EUR", new BigDecimal("1000.00")))
                    .exchangeRateInformation(createExchangeRateInfo())
                    .purpose(PaymentPurpose.LOAN_PAYMENT)
                    .chargeBearer(ChargeBearer.SHARED)
                    .consentId(ConsentId.generate())
                    .fapiInteractionId(FAPIInteractionId.generate())
                    .build();

            // When
            PaymentInitiationResponse response = openBankingGatewayService
                    .initiateInternationalPayment(internationalRequest);

            // Then
            assertNotNull(response);
            assertEquals(OpenBankingStatus.SUCCESS, response.getStatus());
            assertNotNull(response.getPaymentId());
            assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, response.getPaymentStatus());
            assertNotNull(response.getExchangeRateInformation());
            assertTrue(response.getExchangeRateInformation().getExchangeRate()
                    .compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should handle payment initiation with expired consent")
        void shouldHandlePaymentInitiationWithExpiredConsent() {
            // Given
            ConsentId expiredConsentId = ConsentId.generate();
            DomesticPaymentRequest expiredConsentRequest = DomesticPaymentRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount("12345678", "123456"))
                    .creditorAccount(createTestAccount("87654321", "654321"))
                    .instructedAmount(testAmount)
                    .consentId(expiredConsentId)
                    .build();

            // When
            PaymentInitiationResponse response = openBankingGatewayService
                    .initiateDomesticPayment(expiredConsentRequest);

            // Then
            assertNotNull(response);
            assertEquals(OpenBankingStatus.CONSENT_EXPIRED, response.getStatus());
            assertEquals(OpenBankingErrorCode.CONSENT_EXPIRED, response.getErrorCode());
            assertNull(response.getPaymentId());
        }

        @Test
        @DisplayName("Should validate payment amounts against regulatory limits")
        void shouldValidatePaymentAmountsAgainstRegulatoryLimits() {
            // Given
            Money largeAmount = Money.of("GBP", new BigDecimal("100000.00"));
            DomesticPaymentRequest largePaymentRequest = DomesticPaymentRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount("12345678", "123456"))
                    .creditorAccount(createTestAccount("87654321", "654321"))
                    .instructedAmount(largeAmount)
                    .consentId(ConsentId.generate())
                    .build();

            // When
            PaymentValidationResult validationResult = openBankingGatewayService
                    .validatePaymentRequest(largePaymentRequest);

            // Then
            assertNotNull(validationResult);
            assertFalse(validationResult.isValid());
            assertTrue(validationResult.getViolations().stream()
                    .anyMatch(v -> v.getViolationType() == PaymentValidationType.AMOUNT_LIMIT_EXCEEDED));
            assertNotNull(validationResult.getRegulatoryRequirements());
        }
    }

    @Nested
    @DisplayName("Consent Management Tests")
    class ConsentManagementTests {

        @Test
        @DisplayName("Should create account information consent")
        void shouldCreateAccountInformationConsent() {
            // Given
            AccountInformationConsentRequest consentRequest = AccountInformationConsentRequest.builder()
                    .customerId(testCustomerId)
                    .permissions(List.of(
                            OpenBankingPermission.READ_ACCOUNTS,
                            OpenBankingPermission.READ_BALANCES,
                            OpenBankingPermission.READ_TRANSACTIONS
                    ))
                    .expirationDateTime(LocalDateTime.now().plusDays(90))
                    .transactionFromDateTime(LocalDateTime.now().minusDays(90))
                    .transactionToDateTime(LocalDateTime.now())
                    .build();

            // When
            ConsentResponse consentResponse = consentManagementService
                    .createAccountInformationConsent(consentRequest);

            // Then
            assertNotNull(consentResponse);
            assertEquals(ConsentStatus.AWAITING_AUTHORISATION, consentResponse.getStatus());
            assertNotNull(consentResponse.getConsentId());
            assertNotNull(consentResponse.getCreationDateTime());
            assertNotNull(consentResponse.getStatusUpdateDateTime());
            assertEquals(consentRequest.getPermissions(), consentResponse.getPermissions());
        }

        @Test
        @DisplayName("Should create payment consent with SCA requirements")
        void shouldCreatePaymentConsentWithSCARequirements() {
            // Given
            PaymentConsentRequest paymentConsentRequest = PaymentConsentRequest.builder()
                    .customerId(testCustomerId)
                    .paymentType(PaymentType.DOMESTIC_PAYMENT)
                    .instructedAmount(testAmount)
                    .debtorAccount(createTestAccount("12345678", "123456"))
                    .creditorAccount(createTestAccount("87654321", "654321"))
                    .scaMethod(SCAMethod.SMS_OTP)
                    .build();

            // When
            ConsentResponse consentResponse = consentManagementService
                    .createPaymentConsent(paymentConsentRequest);

            // Then
            assertNotNull(consentResponse);
            assertEquals(ConsentStatus.AWAITING_AUTHORISATION, consentResponse.getStatus());
            assertNotNull(consentResponse.getConsentId());
            assertNotNull(consentResponse.getScaDetails());
            assertEquals(SCAStatus.RECEIVED, consentResponse.getScaDetails().getScaStatus());
            assertNotNull(consentResponse.getScaDetails().getChallengeData());
        }

        @Test
        @DisplayName("Should authorize consent with valid SCA")
        void shouldAuthorizeConsentWithValidSCA() {
            // Given
            ConsentId consentId = ConsentId.generate();
            ConsentAuthorisationRequest authRequest = ConsentAuthorisationRequest.builder()
                    .consentId(consentId)
                    .customerId(testCustomerId)
                    .scaAuthenticationData("123456") // SMS OTP
                    .psuAuthenticationMethod(PSUAuthenticationMethod.SMS_OTP)
                    .build();

            // When
            ConsentAuthorisationResponse authResponse = consentManagementService
                    .authoriseConsent(authRequest);

            // Then
            assertNotNull(authResponse);
            assertEquals(ConsentStatus.AUTHORISED, authResponse.getConsentStatus());
            assertEquals(SCAStatus.FINALISED, authResponse.getScaStatus());
            assertNotNull(authResponse.getAuthorisationId());
            assertNotNull(authResponse.getAuthorisationDateTime());
        }

        @Test
        @DisplayName("Should handle consent revocation")
        void shouldHandleConsentRevocation() {
            // Given
            ConsentId consentId = ConsentId.generate();
            ConsentRevocationRequest revocationRequest = ConsentRevocationRequest.builder()
                    .consentId(consentId)
                    .customerId(testCustomerId)
                    .revocationReason(ConsentRevocationReason.CUSTOMER_REQUEST)
                    .build();

            // When
            ConsentRevocationResponse revocationResponse = consentManagementService
                    .revokeConsent(revocationRequest);

            // Then
            assertNotNull(revocationResponse);
            assertEquals(ConsentStatus.REVOKED, revocationResponse.getConsentStatus());
            assertNotNull(revocationResponse.getRevocationDateTime());
            assertEquals(revocationRequest.getRevocationReason(), 
                    revocationResponse.getRevocationReason());
        }

        @Test
        @DisplayName("Should validate consent expiration")
        void shouldValidateConsentExpiration() {
            // Given
            ConsentId consentId = ConsentId.generate();
            
            // When
            ConsentValidationResult validationResult = consentManagementService
                    .validateConsent(consentId);

            // Then
            assertNotNull(validationResult);
            assertTrue(validationResult.isValid());
            assertNotNull(validationResult.getConsentDetails());
            assertTrue(validationResult.getConsentDetails().isActive());
            assertFalse(validationResult.getConsentDetails().isExpired());
        }
    }

    @Nested
    @DisplayName("FAPI Compliance Tests")
    class FAPIComplianceTests {

        @Test
        @DisplayName("Should validate FAPI security headers")
        void shouldValidateFAPISecurityHeaders() {
            // Given
            FAPISecurityHeaders headers = FAPISecurityHeaders.builder()
                    .fapiAuthDate("Sun, 10 Sep 2017 19:43:31 GMT")
                    .fapiCustomerIpAddress("192.168.1.1")
                    .fapiInteractionId(FAPIInteractionId.generate())
                    .fapiCustomerLastLoggedTime("Sun, 10 Sep 2017 19:40:00 GMT")
                    .authorization("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                    .build();

            // When
            FAPIHeaderValidationResult validationResult = fapiComplianceService
                    .validateSecurityHeaders(headers);

            // Then
            assertNotNull(validationResult);
            assertTrue(validationResult.isValid());
            assertTrue(validationResult.getHeaderValidations().stream()
                    .allMatch(FAPIHeaderValidation::isValid));
            assertEquals(FAPIComplianceLevel.FAPI_R_RW, validationResult.getComplianceLevel());
        }

        @Test
        @DisplayName("Should validate JWT tokens for FAPI compliance")
        void shouldValidateJWTTokensForFAPICompliance() {
            // Given
            String jwtToken = "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEyMzQ1Njc4OTAifQ...";
            FAPITokenValidationRequest tokenRequest = FAPITokenValidationRequest.builder()
                    .accessToken(jwtToken)
                    .tokenType(TokenType.BEARER)
                    .scope(List.of("accounts", "payments"))
                    .clientId("test-client-id")
                    .build();

            // When
            FAPITokenValidationResult tokenResult = fapiComplianceService
                    .validateJWTToken(tokenRequest);

            // Then
            assertNotNull(tokenResult);
            assertTrue(tokenResult.isValid());
            assertNotNull(tokenResult.getTokenClaims());
            assertTrue(tokenResult.getTokenClaims().containsKey("iss"));
            assertTrue(tokenResult.getTokenClaims().containsKey("aud"));
            assertTrue(tokenResult.getTokenClaims().containsKey("exp"));
            assertEquals(FAPITokenType.ACCESS_TOKEN, tokenResult.getTokenType());
        }

        @Test
        @DisplayName("Should enforce MTLS certificate validation")
        void shouldEnforceMTLSCertificateValidation() {
            // Given
            MTLSCertificateValidationRequest mtlsRequest = MTLSCertificateValidationRequest.builder()
                    .clientCertificate("-----BEGIN CERTIFICATE-----\nMIIC...")
                    .certificateChain(List.of("-----BEGIN CERTIFICATE-----\nMIID..."))
                    .clientId("test-client-id")
                    .build();

            // When
            MTLSValidationResult mtlsResult = fapiComplianceService
                    .validateMTLSCertificate(mtlsRequest);

            // Then
            assertNotNull(mtlsResult);
            assertTrue(mtlsResult.isValid());
            assertTrue(mtlsResult.isCertificateValid());
            assertTrue(mtlsResult.isChainValid());
            assertFalse(mtlsResult.isExpired());
            assertNotNull(mtlsResult.getCertificateDetails());
        }

        @Test
        @DisplayName("Should validate message signing for FAPI")
        void shouldValidateMessageSigningForFAPI() {
            // Given
            String signedMessage = "{\"amount\":\"1500.00\",\"currency\":\"GBP\"}";
            String signature = "MEYCIQD7..."; // JWS signature
            MessageSigningValidationRequest signingRequest = MessageSigningValidationRequest.builder()
                    .message(signedMessage)
                    .signature(signature)
                    .signingAlgorithm(SigningAlgorithm.PS256)
                    .publicKey("-----BEGIN PUBLIC KEY-----\nMIIBIjANBgk...")
                    .build();

            // When
            MessageSigningValidationResult signingResult = fapiComplianceService
                    .validateMessageSigning(signingRequest);

            // Then
            assertNotNull(signingResult);
            assertTrue(signingResult.isValid());
            assertTrue(signingResult.isSignatureValid());
            assertFalse(signingResult.isMessageTampered());
            assertEquals(SigningAlgorithm.PS256, signingResult.getUsedAlgorithm());
        }
    }

    @Nested
    @DisplayName("Regulatory Compliance Tests")
    class RegulatoryComplianceTests {

        @Test
        @DisplayName("Should enforce PSD2 SCA requirements")
        void shouldEnforcePSD2SCARequirements() {
            // Given
            PSD2ComplianceRequest psd2Request = PSD2ComplianceRequest.builder()
                    .paymentAmount(Money.of("EUR", new BigDecimal("500.00")))
                    .paymentType(PaymentType.DOMESTIC_PAYMENT)
                    .customerId(testCustomerId)
                    .scaExemption(SCAExemption.LOW_VALUE)
                    .build();

            // When
            PSD2ComplianceResult psd2Result = fapiComplianceService
                    .validatePSD2Compliance(psd2Request);

            // Then
            assertNotNull(psd2Result);
            assertTrue(psd2Result.isCompliant());
            assertEquals(SCARequirement.REQUIRED, psd2Result.getScaRequirement());
            assertNotNull(psd2Result.getApplicableExemptions());
            assertTrue(psd2Result.getApplicableExemptions().contains(SCAExemption.LOW_VALUE));
        }

        @Test
        @DisplayName("Should validate anti-money laundering requirements")
        void shouldValidateAntiMoneyLaunderingRequirements() {
            // Given
            AMLValidationRequest amlRequest = AMLValidationRequest.builder()
                    .customerId(testCustomerId)
                    .paymentAmount(Money.of("GBP", new BigDecimal("10000.00")))
                    .creditorAccount(createTestAccount("87654321", "654321"))
                    .paymentPurpose(PaymentPurpose.LOAN_PAYMENT)
                    .build();

            // When
            AMLValidationResult amlResult = fapiComplianceService
                    .validateAMLRequirements(amlRequest);

            // Then
            assertNotNull(amlResult);
            assertTrue(amlResult.isCompliant());
            assertNotNull(amlResult.getRiskScore());
            assertTrue(amlResult.getRiskScore().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(amlResult.getRiskScore().compareTo(new BigDecimal("100")) <= 0);
            assertEquals(AMLRiskLevel.LOW, amlResult.getRiskLevel());
        }

        @Test
        @DisplayName("Should handle high-value transaction reporting")
        void shouldHandleHighValueTransactionReporting() {
            // Given
            Money highValueAmount = Money.of("GBP", new BigDecimal("15000.00"));
            HighValueTransactionRequest hvtRequest = HighValueTransactionRequest.builder()
                    .customerId(testCustomerId)
                    .transactionAmount(highValueAmount)
                    .creditorAccount(createTestAccount("87654321", "654321"))
                    .transactionPurpose(PaymentPurpose.LOAN_PAYMENT)
                    .build();

            // When
            HighValueTransactionResult hvtResult = fapiComplianceService
                    .processHighValueTransaction(hvtRequest);

            // Then
            assertNotNull(hvtResult);
            assertTrue(hvtResult.isReportingRequired());
            assertNotNull(hvtResult.getReportingAuthorities());
            assertFalse(hvtResult.getReportingAuthorities().isEmpty());
            assertEquals(TransactionReportingStatus.REPORTED, hvtResult.getReportingStatus());
            assertNotNull(hvtResult.getReportingReference());
        }
    }

    // Helper methods
    private BankAccount createTestAccount(String accountNumber, String sortCodeOrBIC) {
        return BankAccount.builder()
                .accountId(BankAccountId.generate())
                .accountNumber(accountNumber)
                .sortCode(sortCodeOrBIC)
                .accountName("Test Account")
                .currency("GBP")
                .accountType(AccountType.CURRENT)
                .build();
    }

    private ExchangeRateInformation createExchangeRateInfo() {
        return ExchangeRateInformation.builder()
                .unitCurrency("GBP")
                .exchangeRate(new BigDecimal("1.15"))
                .rateType(ExchangeRateType.AGREED)
                .contractIdentification("FX123456")
                .build();
    }
}