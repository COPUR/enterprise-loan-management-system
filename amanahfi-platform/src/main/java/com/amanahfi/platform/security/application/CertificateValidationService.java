package com.amanahfi.platform.security.application;

import com.amanahfi.platform.security.domain.ClientCertificate;
import com.amanahfi.platform.security.port.out.CertificateRevocationClient;
import com.amanahfi.platform.security.port.out.TrustedCertificateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for validating client certificates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateValidationService {
    
    private final TrustedCertificateStore trustedCertificateStore;
    private final CertificateRevocationClient revocationClient;
    
    /**
     * Validate client certificate
     */
    public void validateCertificate(ClientCertificate certificate) {
        log.debug("Validating certificate: {}", certificate.getSerialNumber());
        
        try {
            // Basic certificate validation
            certificate.validate();
            
            // Check if certificate is trusted
            validateTrust(certificate);
            
            // Check certificate revocation status
            validateRevocationStatus(certificate);
            
            // Check certificate usage
            validateUsage(certificate);
            
            // Check certificate chain
            validateCertificateChain(certificate);
            
            log.debug("Certificate validation successful: {}", certificate.getSerialNumber());
            
        } catch (Exception e) {
            log.error("Certificate validation failed: {} - Error: {}", 
                certificate.getSerialNumber(), e.getMessage());
            throw new CertificateValidationException("Certificate validation failed", e);
        }
    }
    
    /**
     * Validate certificate trust
     */
    private void validateTrust(ClientCertificate certificate) {
        log.debug("Validating certificate trust: {}", certificate.getSerialNumber());
        
        // Check if issuer is trusted
        if (!trustedCertificateStore.isTrustedIssuer(certificate.getIssuerDN())) {
            throw new CertificateValidationException("Certificate issuer is not trusted: " + certificate.getIssuerDN());
        }
        
        // Check if certificate is in trusted store
        if (!trustedCertificateStore.isTrustedCertificate(certificate.getFingerprint())) {
            log.warn("Certificate not found in trusted store: {}", certificate.getSerialNumber());
            // This might be okay if issuer is trusted, depending on policy
        }
        
        log.debug("Certificate trust validation successful: {}", certificate.getSerialNumber());
    }
    
    /**
     * Validate certificate revocation status
     */
    private void validateRevocationStatus(ClientCertificate certificate) {
        log.debug("Validating certificate revocation status: {}", certificate.getSerialNumber());
        
        try {
            // Check OCSP (Online Certificate Status Protocol)
            boolean isRevoked = revocationClient.isRevokedOCSP(certificate.getSerialNumber(), certificate.getIssuerDN());
            if (isRevoked) {
                throw new CertificateValidationException("Certificate is revoked (OCSP): " + certificate.getSerialNumber());
            }
            
            // Check CRL (Certificate Revocation List) as fallback
            if (!revocationClient.isOCSPAvailable(certificate.getIssuerDN())) {
                isRevoked = revocationClient.isRevokedCRL(certificate.getSerialNumber(), certificate.getIssuerDN());
                if (isRevoked) {
                    throw new CertificateValidationException("Certificate is revoked (CRL): " + certificate.getSerialNumber());
                }
            }
            
            log.debug("Certificate revocation status validation successful: {}", certificate.getSerialNumber());
            
        } catch (CertificateValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Certificate revocation status check failed: {} - Error: {}", 
                certificate.getSerialNumber(), e.getMessage());
            // Depending on policy, this might be acceptable or might require rejection
            throw new CertificateValidationException("Certificate revocation status unavailable", e);
        }
    }
    
    /**
     * Validate certificate usage
     */
    private void validateUsage(ClientCertificate certificate) {
        log.debug("Validating certificate usage: {}", certificate.getSerialNumber());
        
        // Check if certificate supports client authentication
        if (!certificate.supportsClientAuthentication()) {
            throw new CertificateValidationException("Certificate does not support client authentication: " + certificate.getSerialNumber());
        }
        
        // Check if certificate supports digital signature
        if (!certificate.supportsDigitalSignature()) {
            throw new CertificateValidationException("Certificate does not support digital signature: " + certificate.getSerialNumber());
        }
        
        log.debug("Certificate usage validation successful: {}", certificate.getSerialNumber());
    }
    
    /**
     * Validate certificate chain
     */
    private void validateCertificateChain(ClientCertificate certificate) {
        log.debug("Validating certificate chain: {}", certificate.getSerialNumber());
        
        try {
            // Validate certificate chain up to trusted root
            boolean chainValid = trustedCertificateStore.validateCertificateChain(
                certificate.getSubjectDN(), certificate.getIssuerDN());
            
            if (!chainValid) {
                throw new CertificateValidationException("Certificate chain validation failed: " + certificate.getSerialNumber());
            }
            
            log.debug("Certificate chain validation successful: {}", certificate.getSerialNumber());
            
        } catch (CertificateValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Certificate chain validation error: {} - Error: {}", 
                certificate.getSerialNumber(), e.getMessage());
            throw new CertificateValidationException("Certificate chain validation failed", e);
        }
    }
    
    /**
     * Check if certificate is near expiration
     */
    public boolean isCertificateNearExpiration(ClientCertificate certificate) {
        return certificate.isNearExpiration();
    }
    
    /**
     * Get certificate expiration warning
     */
    public String getCertificateExpirationWarning(ClientCertificate certificate) {
        if (certificate.isExpired()) {
            return "Certificate has expired on " + certificate.getNotAfter();
        } else if (certificate.isNearExpiration()) {
            return "Certificate will expire in " + certificate.getDaysUntilExpiration() + " days";
        }
        return null;
    }
    
    /**
     * Exception for certificate validation failures
     */
    public static class CertificateValidationException extends RuntimeException {
        public CertificateValidationException(String message) {
            super(message);
        }
        
        public CertificateValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}