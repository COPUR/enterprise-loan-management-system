package com.enterprise.openfinance.bankingmetadata.infrastructure.security;

import com.enterprise.openfinance.bankingmetadata.infrastructure.config.MetadataSecurityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

@Component
public class DpopProofValidationFilter extends OncePerRequestFilter {

    private static final String DPOP_HEADER = "DPoP";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DPOP_PREFIX = "DPoP ";
    private static final String DPOP_TYP = "dpop+jwt";

    private final MetadataSecurityProperties securityProperties;
    private final Clock clock;

    public DpopProofValidationFilter(MetadataSecurityProperties securityProperties, Clock metadataClock) {
        this.securityProperties = securityProperties;
        this.clock = metadataClock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/open-finance/v1/metadata/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String dpopProof = request.getHeader(DPOP_HEADER);
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!securityProperties.isDpopRequired() && isBlank(dpopProof)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isBlank(dpopProof)) {
            reject(response, "DPoP proof is required");
            return;
        }

        String accessToken = resolveAccessToken(authorization);
        if (isBlank(accessToken)) {
            reject(response, "Authorization token is required");
            return;
        }

        try {
            validateProof(dpopProof, request, accessToken);
            filterChain.doFilter(request, response);
        } catch (DpopValidationException exception) {
            reject(response, exception.getMessage());
        }
    }

    private void validateProof(String dpopProof, HttpServletRequest request, String accessToken) {
        SignedJWT signedJWT = parseJwt(dpopProof);
        validateHeader(signedJWT);
        verifySignature(signedJWT);
        validateClaims(signedJWT, request, accessToken);
    }

    private static SignedJWT parseJwt(String dpopProof) {
        try {
            return SignedJWT.parse(dpopProof);
        } catch (ParseException exception) {
            throw new DpopValidationException("Invalid DPoP proof format");
        }
    }

    private static void validateHeader(SignedJWT jwt) {
        if (jwt.getHeader().getType() == null || !DPOP_TYP.equalsIgnoreCase(jwt.getHeader().getType().toString())) {
            throw new DpopValidationException("Invalid DPoP typ header");
        }
        if (jwt.getHeader().getJWK() == null) {
            throw new DpopValidationException("DPoP public key is required in JWK header");
        }
    }

    private static void verifySignature(SignedJWT jwt) {
        JWSVerifier verifier = createVerifier(jwt.getHeader().getJWK());
        try {
            if (!jwt.verify(verifier)) {
                throw new DpopValidationException("Invalid DPoP signature");
            }
        } catch (JOSEException exception) {
            throw new DpopValidationException("Unable to verify DPoP signature");
        }
    }

    private void validateClaims(SignedJWT jwt, HttpServletRequest request, String accessToken) {
        JWTClaimsSet claims = readClaims(jwt);
        String methodClaim = readString(claims, "htm");
        String uriClaim = readString(claims, "htu");
        String accessHashClaim = readString(claims, "ath");
        String jti = readString(claims, "jti");
        Date iatDate = claims.getIssueTime();

        if (!request.getMethod().equalsIgnoreCase(methodClaim)) {
            throw new DpopValidationException("DPoP htm claim does not match request method");
        }
        if (!normalizeUri(request.getRequestURL().toString()).equals(normalizeUri(uriClaim))) {
            throw new DpopValidationException("DPoP htu claim does not match request URI");
        }
        if (iatDate == null) {
            throw new DpopValidationException("DPoP iat claim is required");
        }
        if (isBlank(jti)) {
            throw new DpopValidationException("DPoP jti claim is required");
        }
        validateIssuedAt(iatDate.toInstant());
        validateAccessTokenHash(accessToken, accessHashClaim);
    }

    private void validateIssuedAt(Instant iat) {
        Instant now = Instant.now(clock);
        if (iat.isBefore(now.minus(securityProperties.getDpopMaxProofAge()))) {
            throw new DpopValidationException("DPoP proof is too old");
        }
        if (iat.isAfter(now.plus(securityProperties.getDpopClockSkew()))) {
            throw new DpopValidationException("DPoP proof iat is in the future");
        }
    }

    private static void validateAccessTokenHash(String accessToken, String athClaim) {
        if (isBlank(athClaim)) {
            throw new DpopValidationException("DPoP ath claim is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String expected = Base64URL.encode(digest.digest(accessToken.getBytes(StandardCharsets.UTF_8))).toString();
            if (!expected.equals(athClaim)) {
                throw new DpopValidationException("DPoP ath claim does not match access token");
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new DpopValidationException("Unable to validate DPoP ath claim");
        }
    }

    private static JWTClaimsSet readClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException exception) {
            throw new DpopValidationException("Invalid DPoP claims");
        }
    }

    private static String readString(JWTClaimsSet claims, String key) {
        try {
            return claims.getStringClaim(key);
        } catch (ParseException exception) {
            throw new DpopValidationException("Invalid DPoP claim: " + key);
        }
    }

    private static JWSVerifier createVerifier(JWK jwk) {
        try {
            if (jwk instanceof ECKey ecKey) {
                return new ECDSAVerifier(ecKey.toECPublicKey());
            }
            if (jwk instanceof RSAKey rsaKey) {
                return new RSASSAVerifier(rsaKey.toRSAPublicKey());
            }
            throw new DpopValidationException("Unsupported DPoP JWK type");
        } catch (JOSEException exception) {
            throw new DpopValidationException("Invalid DPoP JWK");
        }
    }

    private static String normalizeUri(String raw) {
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }
            URI normalized = new URI(scheme, null, host, port, uri.getPath(), null, null);
            return normalized.toString();
        } catch (URISyntaxException exception) {
            throw new DpopValidationException("Invalid DPoP htu URI");
        }
    }

    private static String resolveAccessToken(String authorization) {
        if (isBlank(authorization)) {
            return null;
        }
        if (authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        if (authorization.startsWith(DPOP_PREFIX)) {
            return authorization.substring(DPOP_PREFIX.length()).trim();
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safeMessage = message.replace("\"", "'");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + safeMessage + "\"}");
    }

    private static final class DpopValidationException extends RuntimeException {
        private DpopValidationException(String message) {
            super(message);
        }
    }
}
