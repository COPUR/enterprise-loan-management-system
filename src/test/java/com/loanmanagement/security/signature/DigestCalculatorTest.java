package com.loanmanagement.security.signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DigestCalculator
 * Tests digest calculation and validation functionality
 */
@DisplayName("Digest Calculator Tests")
class DigestCalculatorTest {

    private DigestCalculator digestCalculator;

    @BeforeEach
    void setUp() {
        digestCalculator = new DigestCalculator();
    }

    @Test
    @DisplayName("Should calculate SHA-256 digest correctly")
    void shouldCalculateSha256DigestCorrectly() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        
        // When
        String digest = digestCalculator.calculateSha256Digest(requestBody);
        
        // Then
        assertNotNull(digest);
        assertTrue(digest.startsWith("SHA-256="));
        
        // The digest should be consistent
        String secondDigest = digestCalculator.calculateSha256Digest(requestBody);
        assertEquals(digest, secondDigest);
    }

    @Test
    @DisplayName("Should calculate SHA-512 digest correctly")
    void shouldCalculateSha512DigestCorrectly() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        
        // When
        String digest = digestCalculator.calculateSha512Digest(requestBody);
        
        // Then
        assertNotNull(digest);
        assertTrue(digest.startsWith("SHA-512="));
        
        // SHA-512 digest should be different from SHA-256
        String sha256Digest = digestCalculator.calculateSha256Digest(requestBody);
        assertNotEquals(digest, sha256Digest);
    }

    @Test
    @DisplayName("Should calculate different digests for different content")
    void shouldCalculateDifferentDigestsForDifferentContent() {
        // Given
        String requestBody1 = "{\"amount\":10000,\"currency\":\"USD\"}";
        String requestBody2 = "{\"amount\":20000,\"currency\":\"EUR\"}";
        
        // When
        String digest1 = digestCalculator.calculateSha256Digest(requestBody1);
        String digest2 = digestCalculator.calculateSha256Digest(requestBody2);
        
        // Then
        assertNotEquals(digest1, digest2);
    }

    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() {
        // Given
        String emptyBody = "";
        
        // When
        String digest = digestCalculator.calculateSha256Digest(emptyBody);
        
        // Then
        assertNotNull(digest);
        assertTrue(digest.startsWith("SHA-256="));
        
        // Empty body should produce a specific known hash
        String expectedEmptyHash = "SHA-256=47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=";
        assertEquals(expectedEmptyHash, digest);
    }

    @Test
    @DisplayName("Should validate correct SHA-256 digest")
    void shouldValidateCorrectSha256Digest() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String correctDigest = digestCalculator.calculateSha256Digest(requestBody);
        
        // When
        boolean isValid = digestCalculator.validateDigest(correctDigest, requestBody);
        
        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject incorrect digest")
    void shouldRejectIncorrectDigest() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String incorrectDigest = "SHA-256=invalidhash";
        
        // When
        boolean isValid = digestCalculator.validateDigest(incorrectDigest, requestBody);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject digest for different content")
    void shouldRejectDigestForDifferentContent() {
        // Given
        String originalBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String modifiedBody = "{\"amount\":20000,\"currency\":\"USD\"}";
        String digestForOriginal = digestCalculator.calculateSha256Digest(originalBody);
        
        // When
        boolean isValid = digestCalculator.validateDigest(digestForOriginal, modifiedBody);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle malformed digest header")
    void shouldHandleMalformedDigestHeader() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String malformedDigest = "invalidformat";
        
        // When
        boolean isValid = digestCalculator.validateDigest(malformedDigest, requestBody);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null digest header")
    void shouldHandleNullDigestHeader() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        
        // When
        boolean isValid = digestCalculator.validateDigest(null, requestBody);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty digest header")
    void shouldHandleEmptyDigestHeader() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        
        // When
        boolean isValid = digestCalculator.validateDigest("", requestBody);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject unsupported digest algorithm")
    void shouldRejectUnsupportedDigestAlgorithm() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String unsupportedDigest = "MD5=somehash";
        
        // When
        boolean isValid = digestCalculator.validateDigest(unsupportedDigest, requestBody);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract algorithm from digest header")
    void shouldExtractAlgorithmFromDigestHeader() {
        // Given
        String digestHeader = "SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=";
        
        // When
        String algorithm = digestCalculator.extractAlgorithm(digestHeader);
        
        // Then
        assertEquals("SHA-256", algorithm);
    }

    @Test
    @DisplayName("Should extract hash from digest header")
    void shouldExtractHashFromDigestHeader() {
        // Given
        String digestHeader = "SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=";
        
        // When
        String hash = digestCalculator.extractHash(digestHeader);
        
        // Then
        assertEquals("X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=", hash);
    }

    @Test
    @DisplayName("Should handle null input when extracting algorithm")
    void shouldHandleNullInputWhenExtractingAlgorithm() {
        // When
        String algorithm = digestCalculator.extractAlgorithm(null);
        
        // Then
        assertNull(algorithm);
    }

    @Test
    @DisplayName("Should handle invalid format when extracting algorithm")
    void shouldHandleInvalidFormatWhenExtractingAlgorithm() {
        // Given
        String invalidHeader = "invalidformat";
        
        // When
        String algorithm = digestCalculator.extractAlgorithm(invalidHeader);
        
        // Then
        assertNull(algorithm);
    }

    @Test
    @DisplayName("Should validate SHA-512 digest correctly")
    void shouldValidateSha512DigestCorrectly() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String correctDigest = digestCalculator.calculateSha512Digest(requestBody);
        
        // When
        boolean isValid = digestCalculator.validateDigest(correctDigest, requestBody);
        
        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle unicode content correctly")
    void shouldHandleUnicodeContentCorrectly() {
        // Given
        String unicodeBody = "{\"message\":\"Hello 世界\",\"emoji\":\"🏦💰\"}";
        
        // When
        String digest = digestCalculator.calculateSha256Digest(unicodeBody);
        boolean isValid = digestCalculator.validateDigest(digest, unicodeBody);
        
        // Then
        assertNotNull(digest);
        assertTrue(digest.startsWith("SHA-256="));
        assertTrue(isValid);
    }
}