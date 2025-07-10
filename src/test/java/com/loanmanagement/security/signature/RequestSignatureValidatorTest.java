package com.loanmanagement.security.signature;

import com.loanmanagement.security.signature.exception.InvalidSignatureException;
import com.loanmanagement.security.signature.exception.MissingSignatureHeaderException;
import com.loanmanagement.security.signature.exception.SignatureVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-driven development for Request Signature Validation
 * Following HTTP Message Signatures RFC for FAPI compliance
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Request Signature Validator Tests")
class RequestSignatureValidatorTest {

    @Mock
    private SignatureKeyResolver keyResolver;

    @Mock
    private SignatureComponentsExtractor componentsExtractor;

    private RequestSignatureValidator validator;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        validator = new RequestSignatureValidator(keyResolver, componentsExtractor);
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/loans");
        request.setContent("test request body".getBytes());
    }

    @Nested
    @DisplayName("When validating HTTP signature")
    class HttpSignatureValidation {

        @Test
        @DisplayName("Should fail when signature header is missing")
        void shouldFailWhenSignatureHeaderIsMissing() {
            // Given - no signature header

            // When & Then
            assertThrows(MissingSignatureHeaderException.class,
                    () -> validator.validateSignature(request),
                    "Request must include a valid signature header");
        }

        @Test
        @DisplayName("Should fail when signature is malformed")
        void shouldFailWhenSignatureIsMalformed() {
            // Given
            request.addHeader("Signature", "malformed-signature");

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Signature header must be properly formatted");
        }

        @Test
        @DisplayName("Should fail when key cannot be resolved")
        void shouldFailWhenKeyCannotBeResolved() {
            // Given
            String signature = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"base64signature\"";
            request.addHeader("Signature", signature);
            request.addHeader("Host", "api.loanmanagement.com");
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");

            when(keyResolver.resolvePublicKey("test-key")).thenReturn(Optional.empty());

            // When & Then
            assertThrows(SignatureVerificationException.class,
                    () -> validator.validateSignature(request),
                    "Cannot resolve public key for keyId: test-key");
        }

        @Test
        @DisplayName("Should validate signature successfully with valid components")
        void shouldValidateSignatureSuccessfullyWithValidComponents() {
            // Given
            String keyId = "test-key-123";
            String algorithm = "rsa-sha256";
            String headers = "(request-target) host date content-length digest";
            String signatureValue = "validBase64SignatureValue==";

            String signatureHeader = String.format(
                    "keyId=\"%s\",algorithm=\"%s\",headers=\"%s\",signature=\"%s\"",
                    keyId, algorithm, headers, signatureValue
            );

            request.addHeader("Signature", signatureHeader);
            request.addHeader("Host", "api.loanmanagement.com");
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            request.addHeader("Content-Length", "17");
            request.addHeader("Digest", "SHA-256=abc123def456");

            PublicKey mockPublicKey = mock(PublicKey.class);
            when(keyResolver.resolvePublicKey(keyId)).thenReturn(Optional.of(mockPublicKey));

            SignatureComponents components = SignatureComponents.builder()
                    .keyId(keyId)
                    .algorithm(algorithm)
                    .headers(headers.split(" "))
                    .signature(signatureValue)
                    .build();

            when(componentsExtractor.extractComponents(signatureHeader))
                    .thenReturn(components);

            when(componentsExtractor.buildSignatureString(eq(request), eq(components)))
                    .thenReturn("(request-target): post /api/v1/loans\nhost: api.loanmanagement.com\ndate: Tue, 07 Jun 2024 20:51:35 GMT\ncontent-length: 17\ndigest: SHA-256=abc123def456");

            // Mock signature verification to return true
            when(componentsExtractor.verifySignature(any(), any(), any())).thenReturn(true);

            // When
            boolean result = validator.validateSignature(request);

            // Then
            assertTrue(result);
            verify(keyResolver).resolvePublicKey(keyId);
            verify(componentsExtractor).extractComponents(signatureHeader);
            verify(componentsExtractor).buildSignatureString(request, components);
        }

        @Test
        @DisplayName("Should fail when signature verification fails")
        void shouldFailWhenSignatureVerificationFails() {
            // Given
            String keyId = "test-key-123";
            String signatureHeader = "keyId=\"test-key-123\",algorithm=\"rsa-sha256\",headers=\"date host\",signature=\"invalidSignature\"";
            
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Host", "api.loanmanagement.com");
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");

            PublicKey mockPublicKey = mock(PublicKey.class);
            when(keyResolver.resolvePublicKey(keyId)).thenReturn(Optional.of(mockPublicKey));

            SignatureComponents components = SignatureComponents.builder()
                    .keyId(keyId)
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"date", "host"})
                    .signature("invalidSignature")
                    .build();

            when(componentsExtractor.extractComponents(signatureHeader)).thenReturn(components);
            when(componentsExtractor.buildSignatureString(any(), any())).thenReturn("test signature string");
            when(componentsExtractor.verifySignature(any(), any(), any())).thenReturn(false);

            // When & Then
            assertThrows(SignatureVerificationException.class,
                    () -> validator.validateSignature(request),
                    "Signature verification failed");
        }
    }

    @Nested
    @DisplayName("When validating required headers")
    class RequiredHeadersValidation {

        @Test
        @DisplayName("Should require Date header")
        void shouldRequireDateHeader() {
            // Given
            String signatureHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"host\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Host", "api.loanmanagement.com");
            // Missing Date header

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Date header is required for signature validation");
        }

        @Test
        @DisplayName("Should require Host header")
        void shouldRequireHostHeader() {
            // Given
            String signatureHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"date\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            // Missing Host header

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Host header is required for signature validation");
        }

        @Test
        @DisplayName("Should require request-target in signature")
        void shouldRequireRequestTargetInSignature() {
            // Given
            String signatureHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"date host\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            request.addHeader("Host", "api.loanmanagement.com");

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "(request-target) must be included in signature headers");
        }

        @Test
        @DisplayName("Should require Digest header for POST requests with body")
        void shouldRequireDigestHeaderForPostRequestsWithBody() {
            // Given
            String signatureHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"(request-target) date host\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            request.addHeader("Host", "api.loanmanagement.com");
            request.setContent("request body".getBytes());
            // Missing Digest header

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Digest header is required for requests with body content");
        }
    }

    @Nested
    @DisplayName("When validating timestamp freshness")
    class TimestampFreshnessValidation {

        @Test
        @DisplayName("Should reject requests with old timestamps")
        void shouldRejectRequestsWithOldTimestamps() {
            // Given - Date header is 10 minutes old
            Instant tenMinutesAgo = Instant.now().minusSeconds(600);
            String oldDate = "Tue, 07 Jun 2024 20:41:35 GMT"; // 10 minutes ago format

            String signatureHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"(request-target) date host\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", oldDate);
            request.addHeader("Host", "api.loanmanagement.com");

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Request timestamp is too old - potential replay attack");
        }

        @Test
        @DisplayName("Should reject requests with future timestamps")
        void shouldRejectRequestsWithFutureTimestamps() {
            // Given - Date header is 10 minutes in the future
            String futureDate = "Tue, 07 Jun 2024 21:01:35 GMT"; // 10 minutes in future

            String signatureHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\",headers=\"(request-target) date host\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", futureDate);
            request.addHeader("Host", "api.loanmanagement.com");

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Request timestamp is in the future - invalid date");
        }

        @Test
        @DisplayName("Should accept requests with recent valid timestamps")
        void shouldAcceptRequestsWithRecentValidTimestamps() {
            // Given - Current timestamp
            String currentDate = "Tue, 07 Jun 2024 20:51:35 GMT";
            String keyId = "test-key";

            String signatureHeader = String.format(
                    "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) date host\",signature=\"validSig\"",
                    keyId
            );

            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", currentDate);
            request.addHeader("Host", "api.loanmanagement.com");

            PublicKey mockPublicKey = mock(PublicKey.class);
            when(keyResolver.resolvePublicKey(keyId)).thenReturn(Optional.of(mockPublicKey));

            SignatureComponents components = SignatureComponents.builder()
                    .keyId(keyId)
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)", "date", "host"})
                    .signature("validSig")
                    .build();

            when(componentsExtractor.extractComponents(signatureHeader)).thenReturn(components);
            when(componentsExtractor.buildSignatureString(any(), any())).thenReturn("signature string");
            when(componentsExtractor.verifySignature(any(), any(), any())).thenReturn(true);

            // When
            boolean result = validator.validateSignature(request);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("When handling different signature algorithms")
    class SignatureAlgorithmHandling {

        @Test
        @DisplayName("Should support RSA-SHA256 algorithm")
        void shouldSupportRsaSha256Algorithm() {
            // Given
            String signatureHeader = "keyId=\"rsa-key\",algorithm=\"rsa-sha256\",headers=\"(request-target) date host\",signature=\"rsaSig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            request.addHeader("Host", "api.loanmanagement.com");

            PublicKey mockPublicKey = mock(PublicKey.class);
            when(keyResolver.resolvePublicKey("rsa-key")).thenReturn(Optional.of(mockPublicKey));

            SignatureComponents components = SignatureComponents.builder()
                    .keyId("rsa-key")
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)", "date", "host"})
                    .signature("rsaSig")
                    .build();

            when(componentsExtractor.extractComponents(signatureHeader)).thenReturn(components);
            when(componentsExtractor.buildSignatureString(any(), any())).thenReturn("signature string");
            when(componentsExtractor.verifySignature(any(), any(), any())).thenReturn(true);

            // When
            boolean result = validator.validateSignature(request);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should support ECDSA-SHA256 algorithm")
        void shouldSupportEcdsaSha256Algorithm() {
            // Given
            String signatureHeader = "keyId=\"ecdsa-key\",algorithm=\"ecdsa-sha256\",headers=\"(request-target) date host\",signature=\"ecdsaSig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            request.addHeader("Host", "api.loanmanagement.com");

            PublicKey mockPublicKey = mock(PublicKey.class);
            when(keyResolver.resolvePublicKey("ecdsa-key")).thenReturn(Optional.of(mockPublicKey));

            SignatureComponents components = SignatureComponents.builder()
                    .keyId("ecdsa-key")
                    .algorithm("ecdsa-sha256")
                    .headers(new String[]{"(request-target)", "date", "host"})
                    .signature("ecdsaSig")
                    .build();

            when(componentsExtractor.extractComponents(signatureHeader)).thenReturn(components);
            when(componentsExtractor.buildSignatureString(any(), any())).thenReturn("signature string");
            when(componentsExtractor.verifySignature(any(), any(), any())).thenReturn(true);

            // When
            boolean result = validator.validateSignature(request);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should reject unsupported algorithms")
        void shouldRejectUnsupportedAlgorithms() {
            // Given
            String signatureHeader = "keyId=\"test-key\",algorithm=\"unsupported-algo\",headers=\"(request-target) date host\",signature=\"sig\"";
            request.addHeader("Signature", signatureHeader);
            request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
            request.addHeader("Host", "api.loanmanagement.com");

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> validator.validateSignature(request),
                    "Unsupported signature algorithm: unsupported-algo");
        }
    }

    @Nested
    @DisplayName("When extracting signature components")
    class SignatureComponentsExtraction {

        @Test
        @DisplayName("Should extract all signature components correctly")
        void shouldExtractAllSignatureComponentsCorrectly() {
            // Given
            String signatureHeader = "keyId=\"test-key-123\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date digest\",signature=\"SGVsbG8gV29ybGQ=\"";
            request.addHeader("Signature", signatureHeader);

            // When
            SignatureComponents components = componentsExtractor.extractComponents(signatureHeader);

            // Then - This will fail initially, driving us to implement the extractor
            assertNotNull(components);
            assertEquals("test-key-123", components.getKeyId());
            assertEquals("rsa-sha256", components.getAlgorithm());
            assertArrayEquals(new String[]{"(request-target)", "host", "date", "digest"}, components.getHeaders());
            assertEquals("SGVsbG8gV29ybGQ=", components.getSignature());
        }
    }
}