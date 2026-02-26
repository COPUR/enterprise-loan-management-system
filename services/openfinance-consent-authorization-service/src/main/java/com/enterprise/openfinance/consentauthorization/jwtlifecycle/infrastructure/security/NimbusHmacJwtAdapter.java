package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalTokenUnauthorizedException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalJwtPort;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class NimbusHmacJwtAdapter implements InternalJwtPort {

    private final InternalSecurityProperties properties;
    private final byte[] secret;
    private final MACSigner signer;
    private final MACVerifier verifier;

    public NimbusHmacJwtAdapter(InternalSecurityProperties properties) throws JOSEException {
        this.properties = properties;
        this.secret = properties.getJwtHmacSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT HMAC secret must be at least 32 bytes for HS256");
        }
        this.signer = new MACSigner(secret);
        this.verifier = new MACVerifier(secret);
    }

    @Override
    public InternalTokenIssueResult issueToken(String subject, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .audience(properties.getAudience())
                .subject(subject)
                .jwtID(jti)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claimsSet
        );

        try {
            signedJwt.sign(signer);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }

        return new InternalTokenIssueResult(
                signedJwt.serialize(),
                "Bearer",
                properties.getAccessTokenTtl().toSeconds(),
                jti,
                issuedAt,
                expiresAt
        );
    }

    @Override
    public InternalTokenPrincipal verify(String token, Instant now) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!signedJwt.verify(verifier)) {
                throw new InternalTokenUnauthorizedException("Invalid token signature");
            }

            JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();
            String issuer = claimsSet.getIssuer();
            if (!properties.getIssuer().equals(issuer)) {
                throw new InternalTokenUnauthorizedException("Invalid token issuer");
            }

            if (claimsSet.getAudience() == null || !claimsSet.getAudience().contains(properties.getAudience())) {
                throw new InternalTokenUnauthorizedException("Invalid token audience");
            }

            Date expiration = claimsSet.getExpirationTime();
            if (expiration == null) {
                throw new InternalTokenUnauthorizedException("Token is missing expiration");
            }

            Instant expirationInstant = expiration.toInstant();
            if (now.isAfter(expirationInstant.plus(properties.getAllowedClockSkew()))) {
                throw new InternalTokenUnauthorizedException("Token has expired");
            }

            Date issuedAtDate = claimsSet.getIssueTime();
            if (issuedAtDate == null) {
                throw new InternalTokenUnauthorizedException("Token is missing issued-at");
            }
            Instant issuedAt = issuedAtDate.toInstant();

            String subject = claimsSet.getSubject();
            String jti = claimsSet.getJWTID();
            if (subject == null || subject.isBlank() || jti == null || jti.isBlank()) {
                throw new InternalTokenUnauthorizedException("Token is missing required claims");
            }

            return new InternalTokenPrincipal(subject, jti, issuedAt, expirationInstant);
        } catch (ParseException | JOSEException exception) {
            throw new InternalTokenUnauthorizedException("Malformed token");
        }
    }
}

