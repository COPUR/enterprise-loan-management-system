package com.amanahfi.platform.security.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;

/**
 * Client certificate for mTLS authentication
 */
@Value
@Builder
public class ClientCertificate {
    
    /**
     * Certificate serial number
     */
    String serialNumber;
    
    /**
     * Certificate subject DN
     */
    String subjectDN;
    
    /**
     * Certificate issuer DN
     */
    String issuerDN;
    
    /**
     * Certificate fingerprint (SHA-256)
     */
    String fingerprint;
    
    /**
     * Certificate not before date
     */
    Instant notBefore;
    
    /**
     * Certificate not after date
     */
    Instant notAfter;
    
    /**
     * Certificate key usage
     */
    Set<String> keyUsage;
    
    /**
     * Certificate extended key usage
     */
    Set<String> extendedKeyUsage;
    
    /**
     * Certificate common name
     */
    String commonName;
    
    /**
     * Certificate organization
     */
    String organization;
    
    /**
     * Certificate organizational unit
     */
    String organizationalUnit;
    
    /**
     * Certificate country
     */
    String country;
    
    /**
     * Whether certificate is trusted
     */
    boolean trusted;
    
    /**
     * Whether certificate is revoked
     */
    boolean revoked;
    
    /**
     * Certificate validation timestamp
     */
    Instant validatedAt;
    
    /**
     * Check if certificate is valid
     */
    public boolean isValid() {
        Instant now = Instant.now();
        return trusted && 
               !revoked && 
               notBefore != null && 
               notAfter != null && 
               !now.isBefore(notBefore) && 
               !now.isAfter(notAfter);
    }
    
    /**
     * Check if certificate is expired
     */
    public boolean isExpired() {
        return notAfter != null && Instant.now().isAfter(notAfter);
    }
    
    /**
     * Check if certificate is not yet valid
     */
    public boolean isNotYetValid() {
        return notBefore != null && Instant.now().isBefore(notBefore);
    }
    
    /**
     * Check if certificate supports client authentication
     */
    public boolean supportsClientAuthentication() {
        return extendedKeyUsage != null && 
               extendedKeyUsage.contains("clientAuth");
    }
    
    /**
     * Check if certificate supports digital signature
     */
    public boolean supportsDigitalSignature() {
        return keyUsage != null && 
               keyUsage.contains("digitalSignature");
    }
    
    /**
     * Get certificate validity period in days
     */
    public long getValidityPeriodDays() {
        if (notBefore == null || notAfter == null) {
            return 0;
        }
        return java.time.Duration.between(notBefore, notAfter).toDays();
    }
    
    /**
     * Get days until expiration
     */
    public long getDaysUntilExpiration() {
        if (notAfter == null) {
            return 0;
        }
        return java.time.Duration.between(Instant.now(), notAfter).toDays();
    }
    
    /**
     * Check if certificate is near expiration (within 30 days)
     */
    public boolean isNearExpiration() {
        return getDaysUntilExpiration() <= 30;
    }
    
    /**
     * Validate client certificate
     */
    public void validate() {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate serial number cannot be null or empty");
        }
        
        if (subjectDN == null || subjectDN.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate subject DN cannot be null or empty");
        }
        
        if (issuerDN == null || issuerDN.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate issuer DN cannot be null or empty");
        }
        
        if (fingerprint == null || fingerprint.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate fingerprint cannot be null or empty");
        }
        
        if (notBefore == null) {
            throw new IllegalArgumentException("Certificate not before date cannot be null");
        }
        
        if (notAfter == null) {
            throw new IllegalArgumentException("Certificate not after date cannot be null");
        }
        
        if (notBefore.isAfter(notAfter)) {
            throw new IllegalArgumentException("Certificate not before date cannot be after not after date");
        }
        
        if (revoked) {
            throw new SecurityException("Certificate is revoked");
        }
        
        if (!trusted) {
            throw new SecurityException("Certificate is not trusted");
        }
        
        if (isExpired()) {
            throw new SecurityException("Certificate is expired");
        }
        
        if (isNotYetValid()) {
            throw new SecurityException("Certificate is not yet valid");
        }
    }
}