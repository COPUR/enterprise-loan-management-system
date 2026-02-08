package com.amanahfi.platform.security.port.out;

import com.amanahfi.platform.security.domain.ClientCertificate;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;

/**
 * Port for mTLS certificate validation
 */
public interface CertificateValidationPort {
    
    /**
     * Validate client certificate chain
     */
    CertificateValidationResult validateCertificateChain(List<X509Certificate> certificateChain);
    
    /**
     * Validate single certificate
     */
    CertificateValidationResult validateCertificate(X509Certificate certificate);
    
    /**
     * Check certificate revocation status
     */
    RevocationStatus checkRevocationStatus(X509Certificate certificate);
    
    /**
     * Validate certificate against trusted CAs
     */
    boolean isTrustedCA(X509Certificate certificate);
    
    /**
     * Extract client certificate information
     */
    ClientCertificate extractCertificateInfo(X509Certificate certificate);
    
    /**
     * Validate certificate for specific purpose
     */
    boolean isValidForPurpose(X509Certificate certificate, CertificatePurpose purpose);
    
    /**
     * Check if certificate is expired or not yet valid
     */
    boolean isTemporallyValid(X509Certificate certificate, Instant checkTime);
    
    /**
     * Validate certificate subject
     */
    boolean isValidSubject(X509Certificate certificate, String expectedSubject);
    
    /**
     * Get certificate fingerprint
     */
    String getCertificateFingerprint(X509Certificate certificate, String algorithm);
    
    /**
     * Validate certificate key usage
     */
    boolean hasValidKeyUsage(X509Certificate certificate, KeyUsage... requiredUsages);
    
    /**
     * Get trusted certificate authorities
     */
    List<X509Certificate> getTrustedCAs();
    
    /**
     * Certificate validation result
     */
    record CertificateValidationResult(
        boolean valid,
        String errorCode,
        String errorMessage,
        List<String> validationErrors,
        CertificateInfo certificateInfo
    ) {}
    
    /**
     * Certificate information
     */
    record CertificateInfo(
        String subject,
        String issuer,
        String serialNumber,
        Instant notBefore,
        Instant notAfter,
        String fingerprint,
        String algorithm,
        List<String> keyUsages,
        List<String> extendedKeyUsages,
        List<String> subjectAlternativeNames
    ) {}
    
    /**
     * Revocation status
     */
    enum RevocationStatus {
        VALID,
        REVOKED,
        UNKNOWN,
        UNAVAILABLE
    }
    
    /**
     * Certificate purpose
     */
    enum CertificatePurpose {
        CLIENT_AUTHENTICATION,
        SERVER_AUTHENTICATION,
        CODE_SIGNING,
        EMAIL_PROTECTION,
        TIME_STAMPING,
        OCSP_SIGNING
    }
    
    /**
     * Key usage
     */
    enum KeyUsage {
        DIGITAL_SIGNATURE,
        NON_REPUDIATION,
        KEY_ENCIPHERMENT,
        DATA_ENCIPHERMENT,
        KEY_AGREEMENT,
        KEY_CERT_SIGN,
        CRL_SIGN,
        ENCIPHER_ONLY,
        DECIPHER_ONLY
    }
}