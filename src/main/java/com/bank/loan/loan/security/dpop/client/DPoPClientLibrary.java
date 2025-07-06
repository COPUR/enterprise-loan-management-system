package com.bank.loan.loan.security.dpop.client;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Client-side DPoP (Demonstrating Proof-of-Possession) Library
 * Provides utilities for generating DPoP proofs and managing DPoP keys
 * 
 * This library can be used by client applications to integrate with FAPI 2.0 + DPoP endpoints
 */
public class DPoPClientLibrary {
    
    /**
     * DPoP Key Manager for generating and managing key pairs
     */
    public static class DPoPKeyManager {
        
        /**
         * Generate a new EC P-256 key pair for DPoP
         */
        public static ECKey generateECKey() throws JOSEException {
            return new ECKeyGenerator(Curve.P_256)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        }
        
        /**
         * Generate a new RSA 2048-bit key pair for DPoP
         */
        public static RSAKey generateRSAKey() throws JOSEException {
            return new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        }
        
        /**
         * Calculate JKT thumbprint for a given key
         */
        public static String calculateJktThumbprint(JWK key) throws JOSEException {
            return key.computeThumbprint().toString();
        }
    }
    
    /**
     * DPoP Proof Generator for creating DPoP proofs
     */
    public static class DPoPProofGenerator {
        
        /**
         * Create a DPoP proof for token endpoint requests
         * 
         * @param keyPair The DPoP key pair
         * @param httpMethod HTTP method (POST for token endpoint)
         * @param httpUri Full HTTP URI of the token endpoint
         * @return DPoP proof JWT string
         */
        public static String createTokenEndpointProof(JWK keyPair, String httpMethod, String httpUri) 
                throws JOSEException {
            
            return createDPoPProof(keyPair, httpMethod, httpUri, null, null);
        }
        
        /**
         * Create a DPoP proof for resource endpoint requests
         * 
         * @param keyPair The DPoP key pair
         * @param httpMethod HTTP method (GET, POST, PUT, DELETE)
         * @param httpUri Full HTTP URI of the resource endpoint
         * @param accessToken The access token for hash calculation
         * @return DPoP proof JWT string
         */
        public static String createResourceEndpointProof(JWK keyPair, String httpMethod, String httpUri, 
                                                        String accessToken) throws JOSEException {
            
            return createDPoPProof(keyPair, httpMethod, httpUri, accessToken, null);
        }
        
        /**
         * Create a DPoP proof with nonce for enhanced security
         * 
         * @param keyPair The DPoP key pair
         * @param httpMethod HTTP method
         * @param httpUri Full HTTP URI
         * @param accessToken The access token (null for token endpoint)
         * @param nonce Server-provided nonce
         * @return DPoP proof JWT string
         */
        public static String createProofWithNonce(JWK keyPair, String httpMethod, String httpUri, 
                                                String accessToken, String nonce) throws JOSEException {
            
            return createDPoPProof(keyPair, httpMethod, httpUri, accessToken, nonce);
        }
        
        /**
         * Core DPoP proof creation method
         */
        private static String createDPoPProof(JWK keyPair, String httpMethod, String httpUri, 
                                            String accessToken, String nonce) throws JOSEException {
            
            // Determine algorithm based on key type
            JWSAlgorithm algorithm;
            JWSSigner signer;
            
            if (keyPair instanceof ECKey) {
                ECKey ecKey = (ECKey) keyPair;
                algorithm = JWSAlgorithm.ES256;
                signer = new ECDSASigner(ecKey);
            } else if (keyPair instanceof RSAKey) {
                RSAKey rsaKey = (RSAKey) keyPair;
                algorithm = JWSAlgorithm.RS256;
                signer = new RSASSASigner(rsaKey);
            } else {
                throw new IllegalArgumentException("Unsupported key type for DPoP");
            }
            
            // Create JWS header with public key
            JWSHeader header = new JWSHeader.Builder(algorithm)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();
            
            // Create JWT claims set
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(new Date());
            
            // Add access token hash if provided
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                String accessTokenHash = calculateSHA256Hash(accessToken);
                claimsBuilder.claim("ath", accessTokenHash);
            }
            
            // Add nonce if provided
            if (nonce != null && !nonce.trim().isEmpty()) {
                claimsBuilder.claim("nonce", nonce);
            }
            
            JWTClaimsSet claimsSet = claimsBuilder.build();
            
            // Create and sign the JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
        }
        
        /**
         * Calculate SHA-256 hash for access token
         */
        private static String calculateSHA256Hash(String input) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                return Base64URL.encode(hash).toString();
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate SHA-256 hash", e);
            }
        }
    }
    
    /**
     * DPoP HTTP Client Helper for making requests with DPoP proofs
     */
    public static class DPoPHttpClient {
        
        private final JWK dpopKeyPair;
        
        public DPoPHttpClient(JWK dpopKeyPair) {
            this.dpopKeyPair = dpopKeyPair;
        }
        
        /**
         * Get DPoP headers for a request
         * 
         * @param httpMethod HTTP method
         * @param httpUri Full HTTP URI
         * @param accessToken Access token (null for token endpoint)
         * @return DPoP header value
         */
        public String getDPoPHeader(String httpMethod, String httpUri, String accessToken) {
            try {
                if (accessToken != null) {
                    return DPoPProofGenerator.createResourceEndpointProof(dpopKeyPair, httpMethod, httpUri, accessToken);
                } else {
                    return DPoPProofGenerator.createTokenEndpointProof(dpopKeyPair, httpMethod, httpUri);
                }
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to create DPoP proof", e);
            }
        }
        
        /**
         * Get DPoP headers with nonce for a request
         */
        public String getDPoPHeaderWithNonce(String httpMethod, String httpUri, String accessToken, String nonce) {
            try {
                return DPoPProofGenerator.createProofWithNonce(dpopKeyPair, httpMethod, httpUri, accessToken, nonce);
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to create DPoP proof with nonce", e);
            }
        }
        
        /**
         * Get the JKT thumbprint for this client's DPoP key
         */
        public String getJktThumbprint() {
            try {
                return DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to calculate JKT thumbprint", e);
            }
        }
    }
    
    /**
     * Example usage of the DPoP Client Library
     */
    public static class Examples {
        
        public static void demonstrateUsage() throws JOSEException {
            // 1. Generate DPoP key pair
            ECKey dpopKey = DPoPKeyManager.generateECKey();
            System.out.println("Generated DPoP key with thumbprint: " + 
                             DPoPKeyManager.calculateJktThumbprint(dpopKey));
            
            // 2. Create DPoP HTTP client
            DPoPHttpClient client = new DPoPHttpClient(dpopKey);
            
            // 3. Example: PAR request with DPoP JKT
            String jktThumbprint = client.getJktThumbprint();
            System.out.println("Include in PAR request: dpop_jkt=" + jktThumbprint);
            
            // 4. Example: Token request
            String tokenEndpointUrl = "https://auth.example.com/oauth2/token";
            String tokenDPoPProof = client.getDPoPHeader("POST", tokenEndpointUrl, null);
            System.out.println("Token request DPoP header: " + tokenDPoPProof);
            
            // 5. Example: Resource request
            String resourceUrl = "https://api.example.com/loans";
            String accessToken = "valid.access.token";
            String resourceDPoPProof = client.getDPoPHeader("GET", resourceUrl, accessToken);
            System.out.println("Resource request DPoP header: " + resourceDPoPProof);
            
            // 6. Example: Request with nonce
            String nonce = "server-provided-nonce";
            String nonceProof = client.getDPoPHeaderWithNonce("GET", resourceUrl, accessToken, nonce);
            System.out.println("Request with nonce DPoP header: " + nonceProof);
        }
    }
}