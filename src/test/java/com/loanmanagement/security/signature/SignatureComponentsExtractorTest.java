package com.loanmanagement.security.signature;

import com.loanmanagement.security.signature.exception.InvalidSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test-driven development for Signature Components Extractor
 * Tests HTTP Message Signature parsing and validation
 */
@DisplayName("Signature Components Extractor Tests")
class SignatureComponentsExtractorTest {

    private SignatureComponentsExtractor extractor;
    private MockHttpServletRequest request;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        extractor = new SignatureComponentsExtractor();
        request = new MockHttpServletRequest();
        
        // Generate test key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
        
        setupMockRequest();
    }

    private void setupMockRequest() {
        request.setMethod("POST");
        request.setRequestURI("/api/v1/loans");
        request.setServerName("api.loanmanagement.com");
        request.addHeader("Host", "api.loanmanagement.com");
        request.addHeader("Date", "Tue, 07 Jun 2024 20:51:35 GMT");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Content-Length", "123");
        request.addHeader("Digest", "SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=");
        request.setContent("{\"amount\":10000,\"currency\":\"USD\"}".getBytes());
    }

    @Nested
    @DisplayName("When parsing signature header")
    class SignatureHeaderParsing {

        @Test
        @DisplayName("Should parse valid signature header with all components")
        void shouldParseValidSignatureHeaderWithAllComponents() {
            // Given
            String signatureHeader = "keyId=\"test-key-123\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date content-length digest\",signature=\"SGVsbG8gV29ybGQ=\"";

            // When
            SignatureComponents components = extractor.extractComponents(signatureHeader);

            // Then
            assertNotNull(components);
            assertEquals("test-key-123", components.getKeyId());
            assertEquals("rsa-sha256", components.getAlgorithm());
            assertArrayEquals(
                new String[]{"(request-target)", "host", "date", "content-length", "digest"}, 
                components.getHeaders()
            );
            assertEquals("SGVsbG8gV29ybGQ=", components.getSignature());
        }

        @Test
        @DisplayName("Should parse signature header with quoted values containing spaces")
        void shouldParseSignatureHeaderWithQuotedValuesContainingSpaces() {
            // Given
            String signatureHeader = "keyId=\"my test key with spaces\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"long base64 signature value\"";

            // When
            SignatureComponents components = extractor.extractComponents(signatureHeader);

            // Then
            assertEquals("my test key with spaces", components.getKeyId());
            assertEquals("long base64 signature value", components.getSignature());
        }

        @Test
        @DisplayName("Should fail when signature header is malformed")
        void shouldFailWhenSignatureHeaderIsMalformed() {
            // Given
            String malformedHeader = "keyId=test-key,algorithm=rsa-sha256"; // Missing quotes

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> extractor.extractComponents(malformedHeader),
                    "Malformed signature header");
        }

        @Test
        @DisplayName("Should fail when required component is missing")
        void shouldFailWhenRequiredComponentIsMissing() {
            // Given
            String incompleteHeader = "keyId=\"test-key\",algorithm=\"rsa-sha256\""; // Missing headers and signature

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> extractor.extractComponents(incompleteHeader),
                    "Missing required signature components");
        }

        @Test
        @DisplayName("Should handle signature header with different component order")
        void shouldHandleSignatureHeaderWithDifferentComponentOrder() {
            // Given
            String reorderedHeader = "signature=\"SGVsbG8gV29ybGQ=\",headers=\"(request-target) host date\",algorithm=\"rsa-sha256\",keyId=\"test-key\"";

            // When
            SignatureComponents components = extractor.extractComponents(reorderedHeader);

            // Then
            assertEquals("test-key", components.getKeyId());
            assertEquals("rsa-sha256", components.getAlgorithm());
            assertEquals("SGVsbG8gV29ybGQ=", components.getSignature());
        }
    }

    @Nested
    @DisplayName("When building signature string")
    class SignatureStringBuilding {

        @Test
        @DisplayName("Should build signature string with request-target")
        void shouldBuildSignatureStringWithRequestTarget() {
            // Given
            SignatureComponents components = SignatureComponents.builder()
                    .keyId("test-key")
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)", "host", "date"})
                    .signature("testSig")
                    .build();

            // When
            String signatureString = extractor.buildSignatureString(request, components);

            // Then
            String expected = "(request-target): post /api/v1/loans\n" +
                             "host: api.loanmanagement.com\n" +
                             "date: Tue, 07 Jun 2024 20:51:35 GMT";
            assertEquals(expected, signatureString);
        }

        @Test
        @DisplayName("Should build signature string with all standard headers")
        void shouldBuildSignatureStringWithAllStandardHeaders() {
            // Given
            SignatureComponents components = SignatureComponents.builder()
                    .keyId("test-key")
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)", "host", "date", "content-length", "content-type", "digest"})
                    .signature("testSig")
                    .build();

            // When
            String signatureString = extractor.buildSignatureString(request, components);

            // Then
            String expected = "(request-target): post /api/v1/loans\n" +
                             "host: api.loanmanagement.com\n" +
                             "date: Tue, 07 Jun 2024 20:51:35 GMT\n" +
                             "content-length: 123\n" +
                             "content-type: application/json\n" +
                             "digest: SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=";
            assertEquals(expected, signatureString);
        }

        @Test
        @DisplayName("Should handle missing header gracefully")
        void shouldHandleMissingHeaderGracefully() {
            // Given
            SignatureComponents components = SignatureComponents.builder()
                    .keyId("test-key")
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)", "host", "missing-header"})
                    .signature("testSig")
                    .build();

            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> extractor.buildSignatureString(request, components),
                    "Required header 'missing-header' not found in request");
        }

        @Test
        @DisplayName("Should build signature string for GET request")
        void shouldBuildSignatureStringForGetRequest() {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/v1/customers/123");
            
            SignatureComponents components = SignatureComponents.builder()
                    .keyId("test-key")
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)", "host", "date"})
                    .signature("testSig")
                    .build();

            // When
            String signatureString = extractor.buildSignatureString(request, components);

            // Then
            String expected = "(request-target): get /api/v1/customers/123\n" +
                             "host: api.loanmanagement.com\n" +
                             "date: Tue, 07 Jun 2024 20:51:35 GMT";
            assertEquals(expected, signatureString);
        }

        @Test
        @DisplayName("Should handle query parameters in request target")
        void shouldHandleQueryParametersInRequestTarget() {
            // Given
            request.setRequestURI("/api/v1/loans");
            request.setQueryString("status=active&limit=10");
            
            SignatureComponents components = SignatureComponents.builder()
                    .keyId("test-key")
                    .algorithm("rsa-sha256")
                    .headers(new String[]{"(request-target)"})
                    .signature("testSig")
                    .build();

            // When
            String signatureString = extractor.buildSignatureString(request, components);

            // Then
            assertEquals("(request-target): post /api/v1/loans?status=active&limit=10", signatureString);
        }
    }

    @Nested
    @DisplayName("When verifying signatures")
    class SignatureVerification {

        @Test
        @DisplayName("Should verify valid RSA signature")
        void shouldVerifyValidRsaSignature() throws Exception {
            // Given
            String signatureString = "(request-target): post /api/v1/loans\nhost: api.loanmanagement.com\ndate: Tue, 07 Jun 2024 20:51:35 GMT";
            
            // Create actual signature using private key
            String actualSignature = extractor.createSignature(signatureString, keyPair.getPrivate(), "SHA256withRSA");

            // When
            boolean isValid = extractor.verifySignature(signatureString, actualSignature, keyPair.getPublic());

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should reject invalid signature")
        void shouldRejectInvalidSignature() {
            // Given
            String signatureString = "(request-target): post /api/v1/loans\nhost: api.loanmanagement.com\ndate: Tue, 07 Jun 2024 20:51:35 GMT";
            String invalidSignature = "invalidSignatureValue";

            // When
            boolean isValid = extractor.verifySignature(signatureString, invalidSignature, keyPair.getPublic());

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject signature for different content")
        void shouldRejectSignatureForDifferentContent() throws Exception {
            // Given
            String originalString = "(request-target): post /api/v1/loans\nhost: api.loanmanagement.com\ndate: Tue, 07 Jun 2024 20:51:35 GMT";
            String modifiedString = "(request-target): post /api/v1/loans\nhost: evil.hacker.com\ndate: Tue, 07 Jun 2024 20:51:35 GMT";
            
            // Create signature for original content
            String signature = extractor.createSignature(originalString, keyPair.getPrivate(), "SHA256withRSA");

            // When - verify against modified content
            boolean isValid = extractor.verifySignature(modifiedString, signature, keyPair.getPublic());

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should handle signature verification exceptions gracefully")
        void shouldHandleSignatureVerificationExceptionsGracefully() {
            // Given
            String signatureString = "test content";
            String signature = "not-base64-content!@#$";
            PublicKey publicKey = keyPair.getPublic();

            // When
            boolean isValid = extractor.verifySignature(signatureString, signature, publicKey);

            // Then
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("When handling different algorithms")
    class AlgorithmHandling {

        @Test
        @DisplayName("Should support RSA-SHA256 algorithm")
        void shouldSupportRsaSha256Algorithm() throws Exception {
            // Given
            String content = "test signature content";
            String algorithm = "rsa-sha256";

            // When
            String signature = extractor.createSignature(content, keyPair.getPrivate(), extractor.getJavaAlgorithm(algorithm));
            boolean isValid = extractor.verifySignature(content, signature, keyPair.getPublic());

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should map signature algorithms to Java algorithms correctly")
        void shouldMapSignatureAlgorithmsToJavaAlgorithmsCorrectly() {
            // When & Then
            assertEquals("SHA256withRSA", extractor.getJavaAlgorithm("rsa-sha256"));
            assertEquals("SHA256withECDSA", extractor.getJavaAlgorithm("ecdsa-sha256"));
            assertEquals("HmacSHA256", extractor.getJavaAlgorithm("hmac-sha256"));
        }

        @Test
        @DisplayName("Should reject unsupported algorithms")
        void shouldRejectUnsupportedAlgorithms() {
            // When & Then
            assertThrows(InvalidSignatureException.class,
                    () -> extractor.getJavaAlgorithm("unsupported-algorithm"),
                    "Unsupported signature algorithm");
        }
    }
}