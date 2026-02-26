package com.loanmanagement.security.keycloak.dpop;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * RFC 9449 compliant DPoP client library for generating and managing DPoP proofs
 * Provides complete client-side implementation for OAuth 2.1 with DPoP
 */
public class DPoPClientLibrary {

    private static final Logger logger = LoggerFactory.getLogger(DPoPClientLibrary.class);
    
    private static final String DPOP_JWT_TYPE = "dpop+jwt";
    
    /**
     * DPoP key manager for key generation and management
     */
    public static class DPoPKeyManager {
        
        /**
         * Generates EC P-256 key pair for DPoP
         */
        public static ECKey generateECKey() throws JOSEException {
            return new ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .generate();
        }
        
        /**
         * Generates RSA 2048-bit key pair for DPoP
         */
        public static RSAKey generateRSAKey() throws JOSEException {
            return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();
        }
        
        /**
         * Calculates JKT thumbprint for the given key
         */
        public static String calculateJKTThumbprint(JWK key) throws JOSEException {
            byte[] thumbprint = key.computeThumbprint("SHA-256").decode();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(thumbprint);
        }
    }
    
    /**
     * DPoP proof generator
     */
    public static class DPoPProofGenerator {
        
        private final JWK privateKey;
        private final JWSSigner signer;
        
        public DPoPProofGenerator(ECKey privateKey) throws JOSEException {
            this.privateKey = privateKey;
            this.signer = new ECDSASigner(privateKey);
        }
        
        public DPoPProofGenerator(RSAKey privateKey) throws JOSEException {
            this.privateKey = privateKey;
            this.signer = new RSASSASigner(privateKey);
        }
        
        /**
         * Generates DPoP proof for API access
         */
        public String generateProof(String httpMethod, String httpUri) throws JOSEException {
            return generateProof(httpMethod, httpUri, null);
        }
        
        /**
         * Generates DPoP proof with access token binding
         */
        public String generateProof(String httpMethod, String httpUri, String accessToken) throws JOSEException {
            Instant now = Instant.now();
            
            // Build claims
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", httpMethod.toUpperCase())
                .claim("htu", normalizeUri(httpUri))
                .issueTime(Date.from(now));
            
            // Add access token hash if provided
            if (accessToken != null) {
                String ath = calculateAccessTokenHash(accessToken);
                claimsBuilder.claim("ath", ath);
            }
            
            JWTClaimsSet claims = claimsBuilder.build();
            
            // Build header with public key
            JWSHeader header = new JWSHeader.Builder(getAlgorithm())
                .type(new JOSEObjectType(DPOP_JWT_TYPE))
                .jwk(privateKey.toPublicJWK())
                .build();
            
            // Create and sign JWT
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
        }
        
        private JWSAlgorithm getAlgorithm() {
            if (privateKey instanceof ECKey) {
                return JWSAlgorithm.ES256;
            } else if (privateKey instanceof RSAKey) {
                return JWSAlgorithm.RS256;
            }
            throw new IllegalStateException("Unsupported key type");
        }
        
        private String normalizeUri(String uri) {
            try {
                URI parsed = URI.create(uri);
                return new URI(parsed.getScheme(), null, parsed.getHost(), 
                              parsed.getPort(), parsed.getPath(), null, null).toString();
            } catch (Exception e) {
                return uri;
            }
        }
        
        private String calculateAccessTokenHash(String accessToken) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(accessToken.getBytes("UTF-8"));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate access token hash", e);
            }
        }
        
        public String getJKTThumbprint() throws JOSEException {
            return DPoPKeyManager.calculateJKTThumbprint(privateKey);
        }
    }
    
    /**
     * DPoP-enabled HTTP client
     */
    public static class DPoPHttpClient {
        
        private final HttpClient httpClient;
        private final DPoPProofGenerator proofGenerator;
        
        public DPoPHttpClient(DPoPProofGenerator proofGenerator) {
            this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
            this.proofGenerator = proofGenerator;
        }
        
        /**
         * Sends GET request with DPoP proof
         */
        public HttpResponse<String> get(String uri, String accessToken) 
                throws IOException, InterruptedException, JOSEException {
            return sendRequest("GET", uri, null, accessToken);
        }
        
        /**
         * Sends POST request with DPoP proof
         */
        public HttpResponse<String> post(String uri, String body, String accessToken) 
                throws IOException, InterruptedException, JOSEException {
            return sendRequest("POST", uri, body, accessToken);
        }
        
        /**
         * Sends PUT request with DPoP proof
         */
        public HttpResponse<String> put(String uri, String body, String accessToken) 
                throws IOException, InterruptedException, JOSEException {
            return sendRequest("PUT", uri, body, accessToken);
        }
        
        /**
         * Sends DELETE request with DPoP proof
         */
        public HttpResponse<String> delete(String uri, String accessToken) 
                throws IOException, InterruptedException, JOSEException {
            return sendRequest("DELETE", uri, null, accessToken);
        }
        
        private HttpResponse<String> sendRequest(String method, String uri, String body, String accessToken) 
                throws IOException, InterruptedException, JOSEException {
            
            // Generate DPoP proof
            String dpopProof = proofGenerator.generateProof(method, uri, accessToken);
            
            // Build request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("DPoP", dpopProof)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30));
            
            // Add authorization header if access token provided
            if (accessToken != null) {
                requestBuilder.header("Authorization", "DPoP " + accessToken);
            }
            
            // Set HTTP method and body
            switch (method.toUpperCase()) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "POST":
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                    break;
                case "PUT":
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
            
            HttpRequest request = requestBuilder.build();
            
            logger.debug("Sending {} request to {} with DPoP proof", method, uri);
            
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
    
    /**
     * Example usage and utility methods
     */
    public static class Examples {
        
        /**
         * Example: Create EC key and generate DPoP proof
         */
        public static void ecKeyExample() throws JOSEException {
            // Generate EC key
            ECKey ecKey = DPoPKeyManager.generateECKey();
            
            // Create proof generator
            DPoPProofGenerator generator = new DPoPProofGenerator(ecKey);
            
            // Generate proof for API call
            String proof = generator.generateProof("GET", "https://api.bank.com/loans");
            
            System.out.println("DPoP Proof: " + proof);
            System.out.println("JKT Thumbprint: " + generator.getJKTThumbprint());
        }
        
        /**
         * Example: Create RSA key and generate DPoP proof with access token
         */
        public static void rsaKeyExample() throws JOSEException {
            // Generate RSA key
            RSAKey rsaKey = DPoPKeyManager.generateRSAKey();
            
            // Create proof generator
            DPoPProofGenerator generator = new DPoPProofGenerator(rsaKey);
            
            // Generate proof with access token binding
            String accessToken = "example_access_token";
            String proof = generator.generateProof("POST", "https://api.bank.com/payments", accessToken);
            
            System.out.println("DPoP Proof with Token Binding: " + proof);
        }
        
        /**
         * Example: Complete HTTP client usage
         */
        public static void httpClientExample() throws JOSEException, IOException, InterruptedException {
            // Setup
            ECKey ecKey = DPoPKeyManager.generateECKey();
            DPoPProofGenerator generator = new DPoPProofGenerator(ecKey);
            DPoPHttpClient client = new DPoPHttpClient(generator);
            
            // Make authenticated API call
            String accessToken = "example_access_token";
            HttpResponse<String> response = client.get("https://api.bank.com/loans", accessToken);
            
            System.out.println("Response Status: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
        }
    }
}