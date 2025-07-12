package com.amanahfi.platform.security.infrastructure.adapter;

import com.amanahfi.platform.security.domain.ClientCertificate;
import com.amanahfi.platform.security.port.out.CertificateValidationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * mTLS certificate validation adapter implementation
 */
@Component
@Slf4j
public class CertificateValidationAdapter implements CertificateValidationPort {
    
    @Value("${amanahfi.security.mtls.trusted-ca-path:/etc/ssl/certs/trusted-ca.pem}")
    private String trustedCaPath;
    
    @Value("${amanahfi.security.mtls.crl-check-enabled:true}")
    private boolean crlCheckEnabled;
    
    @Value("${amanahfi.security.mtls.ocsp-check-enabled:true}")
    private boolean ocspCheckEnabled;
    
    @Value("${amanahfi.security.mtls.require-client-auth:true}")
    private boolean requireClientAuth;
    
    private final CertificateFactory certificateFactory;
    private final List<X509Certificate> trustedCAs;
    
    public CertificateValidationAdapter() {
        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
            this.trustedCAs = loadTrustedCAs();
        } catch (CertificateException e) {
            log.error("Failed to initialize certificate factory", e);
            throw new RuntimeException("Failed to initialize certificate validation", e);
        }
    }
    
    @Override
    public CertificateValidationResult validateCertificateChain(List<X509Certificate> certificateChain) {
        log.debug("Validating certificate chain with {} certificates", certificateChain.size());
        
        List<String> validationErrors = new ArrayList<>();
        
        try {
            // Validate each certificate in the chain
            for (int i = 0; i < certificateChain.size(); i++) {
                X509Certificate cert = certificateChain.get(i);
                
                // Basic certificate validation
                CertificateValidationResult result = validateCertificate(cert);
                if (!result.valid()) {
                    validationErrors.addAll(result.validationErrors());
                    continue;
                }
                
                // Chain validation
                if (i < certificateChain.size() - 1) {
                    X509Certificate issuer = certificateChain.get(i + 1);
                    if (!isValidIssuer(cert, issuer)) {
                        validationErrors.add("Certificate " + i + " is not properly signed by issuer");
                    }
                }
            }
            
            // Validate chain to trusted root
            if (!isChainTrusted(certificateChain)) {
                validationErrors.add("Certificate chain does not lead to a trusted root CA");
            }
            
            // Extract information from leaf certificate
            X509Certificate leafCert = certificateChain.get(0);
            CertificateInfo certInfo = extractCertificateInfo(leafCert);
            
            boolean isValid = validationErrors.isEmpty();
            
            log.debug("Certificate chain validation result: {}", isValid ? "VALID" : "INVALID");
            
            return new CertificateValidationResult(
                isValid,
                isValid ? null : "VALIDATION_FAILED",
                isValid ? null : "Certificate chain validation failed",
                validationErrors,
                certInfo
            );
            
        } catch (Exception e) {
            log.error("Error validating certificate chain", e);
            
            return new CertificateValidationResult(
                false,
                "VALIDATION_ERROR",
                "Certificate chain validation error: " + e.getMessage(),
                List.of(e.getMessage()),
                null
            );
        }
    }
    
    @Override
    public CertificateValidationResult validateCertificate(X509Certificate certificate) {
        log.debug("Validating single certificate: {}", certificate.getSubjectX500Principal().getName());
        
        List<String> validationErrors = new ArrayList<>();
        
        try {
            // Check certificate validity period
            if (!isTemporallyValid(certificate, Instant.now())) {
                validationErrors.add("Certificate is expired or not yet valid");
            }
            
            // Check certificate signature
            try {
                certificate.verify(certificate.getPublicKey());
            } catch (Exception e) {
                // Try to verify with issuer certificate
                boolean signatureValid = false;
                for (X509Certificate trustedCA : trustedCAs) {
                    try {
                        certificate.verify(trustedCA.getPublicKey());
                        signatureValid = true;
                        break;
                    } catch (Exception ignored) {
                        // Continue to next CA
                    }
                }
                
                if (!signatureValid) {
                    validationErrors.add("Certificate signature verification failed");
                }
            }
            
            // Check revocation status
            RevocationStatus revocationStatus = checkRevocationStatus(certificate);
            if (revocationStatus == RevocationStatus.REVOKED) {
                validationErrors.add("Certificate has been revoked");
            } else if (revocationStatus == RevocationStatus.UNKNOWN && crlCheckEnabled) {
                validationErrors.add("Certificate revocation status could not be determined");
            }
            
            // Check key usage for client authentication
            if (requireClientAuth && !hasValidKeyUsage(certificate, KeyUsage.DIGITAL_SIGNATURE)) {
                validationErrors.add("Certificate does not have required key usage for client authentication");
            }
            
            // Extract certificate information
            CertificateInfo certInfo = extractCertificateInfo(certificate);
            
            boolean isValid = validationErrors.isEmpty();
            
            log.debug("Certificate validation result: {}", isValid ? "VALID" : "INVALID");
            
            return new CertificateValidationResult(
                isValid,
                isValid ? null : "VALIDATION_FAILED",
                isValid ? null : "Certificate validation failed",
                validationErrors,
                certInfo
            );
            
        } catch (Exception e) {
            log.error("Error validating certificate", e);
            
            return new CertificateValidationResult(
                false,
                "VALIDATION_ERROR",
                "Certificate validation error: " + e.getMessage(),
                List.of(e.getMessage()),
                null
            );
        }
    }
    
    @Override
    public RevocationStatus checkRevocationStatus(X509Certificate certificate) {
        log.debug("Checking revocation status for certificate: {}", 
            certificate.getSubjectX500Principal().getName());
        
        try {
            // Check CRL first if enabled
            if (crlCheckEnabled) {
                RevocationStatus crlStatus = checkCRLStatus(certificate);
                if (crlStatus != RevocationStatus.UNKNOWN) {
                    return crlStatus;
                }
            }
            
            // Check OCSP if enabled
            if (ocspCheckEnabled) {
                RevocationStatus ocspStatus = checkOCSPStatus(certificate);
                if (ocspStatus != RevocationStatus.UNKNOWN) {
                    return ocspStatus;
                }
            }
            
            log.debug("Revocation status could not be determined, assuming valid");
            return RevocationStatus.UNAVAILABLE;
            
        } catch (Exception e) {
            log.error("Error checking revocation status", e);
            return RevocationStatus.UNKNOWN;
        }
    }
    
    @Override
    public boolean isTrustedCA(X509Certificate certificate) {
        try {
            for (X509Certificate trustedCA : trustedCAs) {
                if (certificate.equals(trustedCA)) {
                    return true;
                }
                
                // Check if certificate is issued by trusted CA
                try {
                    certificate.verify(trustedCA.getPublicKey());
                    return true;
                } catch (Exception ignored) {
                    // Continue to next CA
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error checking trusted CA status", e);
            return false;
        }
    }
    
    @Override
    public ClientCertificate extractCertificateInfo(X509Certificate certificate) {
        try {
            String subject = certificate.getSubjectX500Principal().getName();
            String issuer = certificate.getIssuerX500Principal().getName();
            String serialNumber = certificate.getSerialNumber().toString();
            String fingerprint = getCertificateFingerprint(certificate, "SHA-256");
            
            return ClientCertificate.builder()
                .subject(subject)
                .issuer(issuer)
                .serialNumber(serialNumber)
                .notBefore(certificate.getNotBefore().toInstant())
                .notAfter(certificate.getNotAfter().toInstant())
                .fingerprint(fingerprint)
                .algorithm(certificate.getSigAlgName())
                .keyUsages(extractKeyUsages(certificate))
                .extendedKeyUsages(extractExtendedKeyUsages(certificate))
                .subjectAlternativeNames(extractSubjectAlternativeNames(certificate))
                .build();
            
        } catch (Exception e) {
            log.error("Error extracting certificate information", e);
            throw new RuntimeException("Failed to extract certificate information", e);
        }
    }
    
    @Override
    public boolean isValidForPurpose(X509Certificate certificate, CertificatePurpose purpose) {
        try {
            switch (purpose) {
                case CLIENT_AUTHENTICATION -> {
                    return hasValidKeyUsage(certificate, KeyUsage.DIGITAL_SIGNATURE) &&
                           hasExtendedKeyUsage(certificate, "1.3.6.1.5.5.7.3.2"); // Client Authentication OID
                }
                case SERVER_AUTHENTICATION -> {
                    return hasValidKeyUsage(certificate, KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_ENCIPHERMENT) &&
                           hasExtendedKeyUsage(certificate, "1.3.6.1.5.5.7.3.1"); // Server Authentication OID
                }
                case CODE_SIGNING -> {
                    return hasValidKeyUsage(certificate, KeyUsage.DIGITAL_SIGNATURE) &&
                           hasExtendedKeyUsage(certificate, "1.3.6.1.5.5.7.3.3"); // Code Signing OID
                }
                case EMAIL_PROTECTION -> {
                    return hasValidKeyUsage(certificate, KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_ENCIPHERMENT) &&
                           hasExtendedKeyUsage(certificate, "1.3.6.1.5.5.7.3.4"); // Email Protection OID
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error checking certificate purpose", e);
            return false;
        }
    }
    
    @Override
    public boolean isTemporallyValid(X509Certificate certificate, Instant checkTime) {
        try {
            Date notBefore = certificate.getNotBefore();
            Date notAfter = certificate.getNotAfter();
            Date checkDate = Date.from(checkTime);
            
            return !checkDate.before(notBefore) && !checkDate.after(notAfter);
            
        } catch (Exception e) {
            log.error("Error checking certificate temporal validity", e);
            return false;
        }
    }
    
    @Override
    public boolean isValidSubject(X509Certificate certificate, String expectedSubject) {
        try {
            String actualSubject = certificate.getSubjectX500Principal().getName();
            return actualSubject.equals(expectedSubject);
            
        } catch (Exception e) {
            log.error("Error validating certificate subject", e);
            return false;
        }
    }
    
    @Override
    public String getCertificateFingerprint(X509Certificate certificate, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] fingerprint = digest.digest(certificate.getEncoded());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : fingerprint) {
                sb.append(String.format("%02X", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("Error calculating certificate fingerprint", e);
            throw new RuntimeException("Failed to calculate certificate fingerprint", e);
        }
    }
    
    @Override
    public boolean hasValidKeyUsage(X509Certificate certificate, KeyUsage... requiredUsages) {
        try {
            boolean[] keyUsage = certificate.getKeyUsage();
            if (keyUsage == null) {
                return false;
            }
            
            for (KeyUsage required : requiredUsages) {
                int index = getKeyUsageIndex(required);
                if (index >= keyUsage.length || !keyUsage[index]) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error checking key usage", e);
            return false;
        }
    }
    
    @Override
    public List<X509Certificate> getTrustedCAs() {
        return new ArrayList<>(trustedCAs);
    }
    
    // Helper methods
    
    private List<X509Certificate> loadTrustedCAs() {
        // In a real implementation, this would load trusted CAs from configured path
        // For now, return empty list
        log.warn("Trusted CA loading not implemented, using empty list");
        return new ArrayList<>();
    }
    
    private boolean isValidIssuer(X509Certificate certificate, X509Certificate issuer) {
        try {
            certificate.verify(issuer.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isChainTrusted(List<X509Certificate> certificateChain) {
        if (certificateChain.isEmpty()) {
            return false;
        }
        
        X509Certificate rootCert = certificateChain.get(certificateChain.size() - 1);
        return isTrustedCA(rootCert);
    }
    
    private RevocationStatus checkCRLStatus(X509Certificate certificate) {
        // Implementation would check Certificate Revocation List
        // For now, return UNAVAILABLE
        log.debug("CRL check not implemented");
        return RevocationStatus.UNAVAILABLE;
    }
    
    private RevocationStatus checkOCSPStatus(X509Certificate certificate) {
        // Implementation would check OCSP (Online Certificate Status Protocol)
        // For now, return UNAVAILABLE
        log.debug("OCSP check not implemented");
        return RevocationStatus.UNAVAILABLE;
    }
    
    private CertificateInfo extractCertificateInfo(X509Certificate certificate) {
        String subject = certificate.getSubjectX500Principal().getName();
        String issuer = certificate.getIssuerX500Principal().getName();
        String serialNumber = certificate.getSerialNumber().toString();
        Instant notBefore = certificate.getNotBefore().toInstant();
        Instant notAfter = certificate.getNotAfter().toInstant();
        String fingerprint = getCertificateFingerprint(certificate, "SHA-256");
        String algorithm = certificate.getSigAlgName();
        List<String> keyUsages = extractKeyUsages(certificate);
        List<String> extendedKeyUsages = extractExtendedKeyUsages(certificate);
        List<String> subjectAlternativeNames = extractSubjectAlternativeNames(certificate);
        
        return new CertificateInfo(
            subject, issuer, serialNumber, notBefore, notAfter, 
            fingerprint, algorithm, keyUsages, extendedKeyUsages, subjectAlternativeNames
        );
    }
    
    private List<String> extractKeyUsages(X509Certificate certificate) {
        boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage == null) {
            return new ArrayList<>();
        }
        
        List<String> usages = new ArrayList<>();
        String[] keyUsageNames = {
            "DIGITAL_SIGNATURE", "NON_REPUDIATION", "KEY_ENCIPHERMENT", "DATA_ENCIPHERMENT",
            "KEY_AGREEMENT", "KEY_CERT_SIGN", "CRL_SIGN", "ENCIPHER_ONLY", "DECIPHER_ONLY"
        };
        
        for (int i = 0; i < Math.min(keyUsage.length, keyUsageNames.length); i++) {
            if (keyUsage[i]) {
                usages.add(keyUsageNames[i]);
            }
        }
        
        return usages;
    }
    
    private List<String> extractExtendedKeyUsages(X509Certificate certificate) {
        try {
            List<String> extendedKeyUsage = certificate.getExtendedKeyUsage();
            return extendedKeyUsage != null ? new ArrayList<>(extendedKeyUsage) : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error extracting extended key usage", e);
            return new ArrayList<>();
        }
    }
    
    private List<String> extractSubjectAlternativeNames(X509Certificate certificate) {
        try {
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
            if (sans == null) {
                return new ArrayList<>();
            }
            
            return sans.stream()
                .map(san -> san.get(1).toString())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error extracting subject alternative names", e);
            return new ArrayList<>();
        }
    }
    
    private boolean hasExtendedKeyUsage(X509Certificate certificate, String oid) {
        try {
            List<String> extendedKeyUsage = certificate.getExtendedKeyUsage();
            return extendedKeyUsage != null && extendedKeyUsage.contains(oid);
        } catch (Exception e) {
            log.error("Error checking extended key usage", e);
            return false;
        }
    }
    
    private int getKeyUsageIndex(KeyUsage keyUsage) {
        return switch (keyUsage) {
            case DIGITAL_SIGNATURE -> 0;
            case NON_REPUDIATION -> 1;
            case KEY_ENCIPHERMENT -> 2;
            case DATA_ENCIPHERMENT -> 3;
            case KEY_AGREEMENT -> 4;
            case KEY_CERT_SIGN -> 5;
            case CRL_SIGN -> 6;
            case ENCIPHER_ONLY -> 7;
            case DECIPHER_ONLY -> 8;
        };
    }
}