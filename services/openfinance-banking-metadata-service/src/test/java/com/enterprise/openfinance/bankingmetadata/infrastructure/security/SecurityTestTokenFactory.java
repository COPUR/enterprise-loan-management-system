package com.enterprise.openfinance.bankingmetadata.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class SecurityTestTokenFactory {

    public static final String TEST_JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final ECKey DPOP_KEY = generateDpopKey();

    private SecurityTestTokenFactory() {
    }

    public static String accessToken(String scopes) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("psu-test")
                .issuer("https://issuer.test")
                .audience("open-finance")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", scopes)
                .claim("client_id", "TPP-001")
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build(), claims);

        try {
            jwt.sign(new MACSigner(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign test access token", exception);
        }
        return jwt.serialize();
    }

    public static String dpopProof(String method, String htu, String accessToken) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .claim("htm", method.toUpperCase())
                .claim("htu", htu)
                .claim("ath", ath(accessToken))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("dpop+jwt"))
                .jwk(DPOP_KEY.toPublicJWK())
                .build(), claims);

        try {
            jwt.sign(new ECDSASigner(DPOP_KEY.toECPrivateKey()));
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign test DPoP proof", exception);
        }
        return jwt.serialize();
    }

    private static String ath(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64URL.encode(digest.digest(accessToken.getBytes(StandardCharsets.UTF_8))).toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash access token", exception);
        }
    }

    private static ECKey generateDpopKey() {
        try {
            return new ECKeyGenerator(Curve.P_256)
                    .keyID("test-dpop-key")
                    .generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to generate DPoP key", exception);
        }
    }
}
