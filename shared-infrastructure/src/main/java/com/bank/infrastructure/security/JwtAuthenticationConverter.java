package com.bank.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Authentication Converter for Enterprise Banking
 * 
 * Converts JWT tokens to Spring Security Authentication objects
 * with proper role and authority mapping for banking operations
 */
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    
    private static final String ROLES_CLAIM = "roles";
    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String ROLE_PREFIX = "ROLE_";
    
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }
    
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract roles from JWT claims
        Collection<String> roles = extractRoles(jwt);
        Collection<String> authorities = extractDirectAuthorities(jwt);
        
        // Convert roles to authorities with ROLE_ prefix
        List<GrantedAuthority> grantedAuthorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
            .collect(Collectors.toList());
        
        // Add direct authorities without prefix
        authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .forEach(grantedAuthorities::add);
        
        return grantedAuthorities;
    }
    
    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(Jwt jwt) {
        Object rolesObject = jwt.getClaim(ROLES_CLAIM);
        
        if (rolesObject instanceof Collection) {
            return (Collection<String>) rolesObject;
        } else if (rolesObject instanceof String) {
            return List.of((String) rolesObject);
        }
        
        // Fallback - check for realm_access.roles (Keycloak format)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Object realmRoles = realmAccess.get("roles");
            if (realmRoles instanceof Collection) {
                return (Collection<String>) realmRoles;
            }
        }
        
        return List.of();
    }
    
    @SuppressWarnings("unchecked")
    private Collection<String> extractDirectAuthorities(Jwt jwt) {
        Object authoritiesObject = jwt.getClaim(AUTHORITIES_CLAIM);
        
        if (authoritiesObject instanceof Collection) {
            return (Collection<String>) authoritiesObject;
        } else if (authoritiesObject instanceof String) {
            return List.of((String) authoritiesObject);
        }
        
        return List.of();
    }
}