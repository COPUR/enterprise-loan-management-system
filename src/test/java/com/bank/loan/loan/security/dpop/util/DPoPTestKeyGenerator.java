package com.bank.loan.loan.security.dpop.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class DPoPTestKeyGenerator {

    public ECKey generateECKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    public ECKey generateWeakECKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    public RSAKey generateRSAKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    public String calculateJktThumbprint(ECKey key) {
        try {
            return key.computeThumbprint().toString();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to calculate JKT thumbprint", e);
        }
    }

    public String calculateJktThumbprint(RSAKey key) {
        try {
            return key.computeThumbprint().toString();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to calculate JKT thumbprint", e);
        }
    }

    public String createValidDPoPProof(ECKey keyPair, String httpMethod, String httpUri) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof", e);
        }
    }

    public String createDPoPProofWithAccessTokenHash(ECKey keyPair, String httpMethod, String httpUri, String accessToken) {
        try {
            String accessTokenHash = calculateAccessTokenHash(accessToken);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .claim("ath", accessTokenHash)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with access token hash", e);
        }
    }

    public String createDPoPProofWithNonce(ECKey keyPair, String httpMethod, String httpUri, String nonce) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .claim("nonce", nonce)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with nonce", e);
        }
    }

    public String createDPoPProofWithTimestamp(ECKey keyPair, String httpMethod, String httpUri, long timestamp) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .claim("iat", timestamp)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with timestamp", e);
        }
    }

    public String createDPoPProofWithInvalidTyp(ECKey keyPair, String httpMethod, String httpUri) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("invalid+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with invalid typ", e);
        }
    }

    public String createDPoPProofWithoutJWK(ECKey keyPair, String httpMethod, String httpUri) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof without JWK", e);
        }
    }

    public String createDPoPProofWithInvalidSignature(ECKey keyPair, String httpMethod, String httpUri) {
        try {
            String validProof = createValidDPoPProof(keyPair, httpMethod, httpUri);
            String[] parts = validProof.split("\\.");
            
            // Corrupt the signature
            String corruptedSignature = parts[2].substring(0, parts[2].length() - 5) + "XXXXX";
            
            return parts[0] + "." + parts[1] + "." + corruptedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DPoP proof with invalid signature", e);
        }
    }

    public String createDPoPProofWithMissingClaims(ECKey keyPair) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with missing claims", e);
        }
    }

    public String createDPoPProofWithUnsupportedAlgorithm(ECKey keyPair, String httpMethod, String httpUri) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with unsupported algorithm", e);
        }
    }

    public String createRSADPoPProof(RSAKey keyPair, String httpMethod, String httpUri) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create RSA DPoP proof", e);
        }
    }

    public String createDPoPProofWithCustomClaims(ECKey keyPair, String httpMethod, String httpUri, 
                                                  String customClaim, Object customValue) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(keyPair.toPublicJWK())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .claim(customClaim, customValue)
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(keyPair));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create DPoP proof with custom claims", e);
        }
    }

    private String calculateAccessTokenHash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));
            return Base64URL.encode(hash).toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate access token hash", e);
        }
    }

    public String generateRandomNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String generateRandomJti() {
        return UUID.randomUUID().toString();
    }

    public long getCurrentTimestamp() {
        return Instant.now().getEpochSecond();
    }

    public long getExpiredTimestamp() {
        return Instant.now().minusSeconds(300).getEpochSecond();
    }

    public long getFutureTimestamp() {
        return Instant.now().plusSeconds(300).getEpochSecond();
    }
}