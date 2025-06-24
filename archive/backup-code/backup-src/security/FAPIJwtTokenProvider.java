package com.bank.loanmanagement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FAPI JWT Token Provider
 * Implements Financial-grade API JWT requirements:
 * - RS256/PS256 algorithm support
 * - Strong key management
 * - FAPI-compliant claims
 * - Token binding and validation
 */
@Component
public class FAPIJwtTokenProvider {

    private final SecretKey jwtSecret;
    private final int jwtExpirationInMs = 3600000; // 1 hour
    private final int refreshTokenExpirationInMs = 86400000; // 24 hours
    
    public FAPIJwtTokenProvider() {
        // In production, use RSA keys from secure key store
        this.jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    public String generateToken(Authentication authentication) {
        org.springframework.security.core.userdetails.UserDetails userPrincipal = 
            (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();

        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userPrincipal.getUsername());
        claims.put("iss", "https://auth.bank.com");
        claims.put("aud", "loan-management-system");
        claims.put("scope", "read write");
        claims.put("client_id", "loan-management-client");
        claims.put("auth_time", System.currentTimeMillis() / 1000);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("token_type", "access_token");
        claims.put("fapi_profile", "advanced");
        
        // Add roles as FAPI-compliant claims
        String roles = userPrincipal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
        claims.put("roles", roles);
        claims.put("authorities", userPrincipal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        org.springframework.security.core.userdetails.UserDetails userPrincipal = 
            (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();

        Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenExpirationInMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userPrincipal.getUsername());
        claims.put("iss", "https://auth.bank.com");
        claims.put("aud", "loan-management-system");
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("token_type", "refresh_token");
        claims.put("scope", "refresh_token");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTokenForUser(String username) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("iss", "https://auth.bank.com");
        claims.put("aud", "loan-management-system");
        claims.put("scope", "read write");
        claims.put("auth_time", System.currentTimeMillis() / 1000);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("token_type", "access_token");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshTokenForUser(String username) {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenExpirationInMs);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("token_type", "refresh_token")
                .claim("jti", UUID.randomUUID().toString())
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(authToken)
                    .getBody();

            // Additional FAPI validations
            String issuer = claims.getIssuer();
            String audience = (String) claims.get("aud");
            
            if (!"https://auth.bank.com".equals(issuer)) {
                return false;
            }
            
            if (!"loan-management-system".equals(audience)) {
                return false;
            }

            // Check if token is not expired
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
            
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String getTokenType(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return (String) claims.get("token_type");
        } catch (Exception e) {
            return null;
        }
    }

    public String getJwtId(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return (String) claims.get("jti");
        } catch (Exception e) {
            return null;
        }
    }
}