# Keycloak OAuth 2.1 Integration with FAPI 2.0 and DPoP

This document describes the comprehensive Keycloak integration for the Enterprise Loan Management System, implementing OAuth 2.1, FAPI 2.0 compliance, and DPoP (Demonstration of Proof-of-Possession) for financial-grade security.

## Overview

The integration provides enterprise-grade authentication and authorization using Keycloak as the identity provider, with full compliance to:

- **OAuth 2.1**: Latest OAuth specification with enhanced security
- **FAPI 2.0**: Financial-grade API security profile
- **DPoP (RFC 9449)**: Demonstration of Proof-of-Possession for enhanced token security
- **PKCE (RFC 7636)**: Proof Key for Code Exchange
- **PAR (RFC 9126)**: Pushed Authorization Requests

## Architecture

### Core Components

1. **KeycloakOAuth21Configuration**: Main configuration class with FAPI and DPoP settings
2. **DPoPValidator**: Validates DPoP proofs according to RFC 9449
3. **FapiComplianceValidator**: Ensures FAPI 2.0 compliance for all requests
4. **KeycloakSecurityFilter**: Security filter integrating all validations
5. **OAuth2 Resource Server**: Spring Security integration for JWT validation

### Component Diagram

```
┌─────────────────────────────────────┐
│           Client Application        │
└──────────────┬──────────────────────┘
               │ OAuth 2.1 + FAPI + DPoP
               ▼
┌─────────────────────────────────────┐
│      Keycloak Security Filter       │
│  - FAPI compliance validation       │
│  - DPoP proof verification          │
│  - Token binding validation         │
└──────────────┬──────────────────────┘
               │
         ┌─────┴─────┐
         ▼           ▼
┌─────────────┐ ┌─────────────┐
│FAPI Validator│ │DPoP Validator│
│- PKCE       │ │- Proof JWT  │
│- PAR        │ │- Binding    │
│- Signed Req │ │- Replay     │
└─────────────┘ └─────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│         Keycloak Server             │
│  - Banking Realm                    │
│  - Financial-grade policies         │
│  - Advanced security features       │
└─────────────────────────────────────┘
```

## Configuration

### Application Configuration

Configure the integration in `application-keycloak.yml`:

```yaml
keycloak:
  server:
    base-url: "https://auth.loanmanagement.com"
    realm: "banking"
    ssl-required: true
    
  fapi:
    enabled: true
    enforce-pkce: true
    require-signed-request-object: true
    require-pushed-authorization-requests: true
    allowed-signature-algorithms: ["RS256", "ES256", "PS256"]
    token-lifetime: 3600
    
  dpop:
    enabled: true
    enforced: false  # Start with non-enforced mode
    max-age: 300
    allowed-algorithms: ["RS256", "ES256", "PS256"]
    require-jti: true
    
  client:
    client-id: "loan-management-system"
    client-secret: "${KEYCLOAK_CLIENT_SECRET}"
    client-type: "CONFIDENTIAL"
    
  security:
    enforce-fapi-compliance: true
    enforce-dpop-validation: false
```

### Keycloak Realm Configuration

#### 1. Create Banking Realm

```bash
# Create realm
kcadm.sh create realms -s realm=banking -s enabled=true

# Configure realm settings
kcadm.sh update realms/banking -s loginTheme=banking \
  -s accessTokenLifespan=3600 \
  -s ssoSessionMaxLifespan=28800 \
  -s bruteForceProtected=true \
  -s failureFactor=5 \
  -s maxFailureWaitSeconds=900
```

#### 2. Configure Client

```bash
# Create FAPI-compliant client
kcadm.sh create clients -r banking \
  -s clientId=loan-management-system \
  -s enabled=true \
  -s clientAuthenticatorType=client-jwt \
  -s publicClient=false \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=false \
  -s serviceAccountsEnabled=true \
  -s attributes.'pkce.code.challenge.method'=S256 \
  -s attributes.'require.pushed.authorization.requests'=true \
  -s attributes.'dpop.bound.access.tokens'=true
```

#### 3. Configure Client Scopes

```bash
# Create banking-specific scopes
kcadm.sh create client-scopes -r banking \
  -s name=banking \
  -s description="Banking operations scope" \
  -s protocol=openid-connect

kcadm.sh create client-scopes -r banking \
  -s name=loans \
  -s description="Loan management operations" \
  -s protocol=openid-connect

kcadm.sh create client-scopes -r banking \
  -s name=payments \
  -s description="Payment processing operations" \
  -s protocol=openid-connect
```

## Security Features

### 1. FAPI 2.0 Compliance

#### Authorization Request Validation
- **HTTPS Required**: All requests must use TLS
- **PKCE Mandatory**: Code challenge with S256 method
- **Signed Request Objects**: JWT-based request parameters
- **PAR Support**: Pushed Authorization Requests
- **State Parameter**: CSRF protection

#### Token Request Validation
- **Private Key JWT**: Client authentication using signed assertions
- **PKCE Verification**: Code verifier validation
- **Limited Grant Types**: Only authorization_code and client_credentials

#### Access Token Validation
- **Limited Lifetime**: Maximum 1 hour token lifetime
- **Required Claims**: sub, aud, iss, exp, iat
- **Strong Algorithms**: RS256, ES256, PS256 only
- **Token Binding**: cnf claim for DPoP binding

### 2. DPoP (Demonstration of Proof-of-Possession)

#### DPoP Proof Structure
```json
{
  "typ": "dpop+jwt",
  "alg": "RS256",
  "jwk": {
    "kty": "RSA",
    "n": "...",
    "e": "AQAB"
  }
}
{
  "jti": "unique-identifier",
  "htm": "POST",
  "htu": "https://api.loanmanagement.com/api/v1/loans",
  "iat": 1643723400,
  "ath": "fUHyO2r2Z3DZ53EsNrWBb0xWXoaNy59IiKCAqksmQEo"
}
```

#### Validation Rules
- **Signature Verification**: Using embedded JWK
- **HTTP Method Binding**: htm claim matches request method
- **URL Binding**: htu claim matches request URL
- **Freshness**: iat within max-age window
- **Replay Protection**: jti uniqueness validation
- **Token Binding**: ath claim matches access token hash

### 3. Enhanced Security Policies

#### Brute Force Protection
```yaml
security:
  enable-brute-force-protection: true
  failure-threshold: 5
  lockout-duration: 900  # 15 minutes
```

#### Password Policy
```yaml
password-policy:
  min-length: 12
  max-length: 128
  require-uppercase: true
  require-lowercase: true
  require-digits: true
  require-special-chars: true
  password-history: 5
  max-age: 7776000  # 90 days
```

## Usage Examples

### 1. Client Registration and Authentication

#### JavaScript Client Example
```javascript
// Configure OAuth2 client with FAPI compliance
const oauth2Config = {
  authorizationUrl: 'https://auth.loanmanagement.com/auth/realms/banking/protocol/openid-connect/auth',
  tokenUrl: 'https://auth.loanmanagement.com/auth/realms/banking/protocol/openid-connect/token',
  clientId: 'loan-management-system',
  redirectUri: 'https://app.loanmanagement.com/callback',
  scopes: ['openid', 'profile', 'banking', 'loans'],
  
  // FAPI requirements
  codeChallengeMethod: 'S256',
  requirePushedAuthorizationRequests: true,
  requireSignedRequestObject: true,
  
  // DPoP configuration
  useDPoP: true,
  dpopKeyId: 'client-dpop-key'
};

// Generate PKCE parameters
const codeVerifier = generateCodeVerifier();
const codeChallenge = await generateCodeChallenge(codeVerifier);

// Create signed request object (required for FAPI)
const requestObject = await createSignedRequestObject({
  client_id: oauth2Config.clientId,
  response_type: 'code',
  redirect_uri: oauth2Config.redirectUri,
  scope: oauth2Config.scopes.join(' '),
  state: generateSecureState(),
  nonce: generateSecureNonce(),
  code_challenge: codeChallenge,
  code_challenge_method: 'S256'
});

// Push authorization request (PAR)
const parResponse = await fetch(oauth2Config.tokenUrl.replace('/token', '/par'), {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: new URLSearchParams({
    request: requestObject,
    client_id: oauth2Config.clientId
  })
});

const { request_uri } = await parResponse.json();

// Redirect to authorization endpoint
const authUrl = new URL(oauth2Config.authorizationUrl);
authUrl.searchParams.set('client_id', oauth2Config.clientId);
authUrl.searchParams.set('request_uri', request_uri);

window.location.href = authUrl.toString();
```

#### Token Exchange with DPoP
```javascript
// Generate DPoP proof for token request
const dpopProof = await generateDPoPProof({
  htm: 'POST',
  htu: oauth2Config.tokenUrl,
  iat: Math.floor(Date.now() / 1000)
});

// Exchange authorization code for tokens
const tokenResponse = await fetch(oauth2Config.tokenUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
    'DPoP': dpopProof
  },
  body: new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: oauth2Config.clientId,
    code: authorizationCode,
    redirect_uri: oauth2Config.redirectUri,
    code_verifier: codeVerifier,
    client_assertion_type: 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
    client_assertion: await createClientAssertion()
  })
});

const tokens = await tokenResponse.json();
// tokens.access_token is now DPoP-bound
```

### 2. API Request with DPoP-bound Token

```javascript
// Generate DPoP proof for API request
const apiDpopProof = await generateDPoPProof({
  htm: 'GET',
  htu: 'https://api.loanmanagement.com/api/v1/loans',
  iat: Math.floor(Date.now() / 1000),
  ath: await calculateTokenHash(accessToken)
});

// Make API request
const response = await fetch('https://api.loanmanagement.com/api/v1/loans', {
  headers: {
    'Authorization': `DPoP ${accessToken}`,
    'DPoP': apiDpopProof
  }
});
```

### 3. Java Client Implementation

```java
@Service
public class KeycloakClientService {
    
    @Autowired
    private WebClient webClient;
    
    @Value("${keycloak.server.base-url}")
    private String keycloakBaseUrl;
    
    public Mono<String> getAccessToken(String clientId, String clientSecret) {
        // Create client assertion for private_key_jwt
        String clientAssertion = createClientAssertion(clientId);
        
        // Generate DPoP proof
        String dpopProof = createDPoPProof("POST", 
            keycloakBaseUrl + "/auth/realms/banking/protocol/openid-connect/token");
        
        return webClient.post()
            .uri(keycloakBaseUrl + "/auth/realms/banking/protocol/openid-connect/token")
            .header("DPoP", dpopProof)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(BodyInserters.fromFormData(
                MultiValueMap.of(
                    "grant_type", List.of("client_credentials"),
                    "scope", List.of("banking loans"),
                    "client_assertion_type", List.of("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                    "client_assertion", List.of(clientAssertion)
                )))
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .map(TokenResponse::getAccessToken);
    }
}
```

## Monitoring and Observability

### Security Events Logging

```java
@EventListener
public class SecurityEventLogger {
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("Authentication successful - User: {}, Client: {}, IP: {}", 
                event.getAuthentication().getName(),
                getClientId(event),
                getClientIp(event));
    }
    
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Authentication failed - Reason: {}, IP: {}", 
                event.getException().getMessage(),
                getClientIp(event));
    }
}
```

### Metrics Collection

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - metrics
          - oauth2-clients
          - security-events
          
  metrics:
    tags:
      application: "loan-management-system"
      environment: "${spring.profiles.active}"
```

### Health Checks

```java
@Component
public class KeycloakHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check Keycloak connectivity
            String wellKnownUrl = keycloakConfig.getServer().getBaseUrl() + 
                "/auth/realms/" + keycloakConfig.getServer().getRealm() + 
                "/.well-known/openid_connect_configuration";
                
            ResponseEntity<String> response = restTemplate.getForEntity(wellKnownUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("keycloak.realm", keycloakConfig.getServer().getRealm())
                    .withDetail("fapi.enabled", keycloakConfig.getFapi().isEnabled())
                    .withDetail("dpop.enabled", keycloakConfig.getDpop().isEnabled())
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Keycloak not responding")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Testing

### Unit Tests

```java
@Test
void shouldValidateFapiCompliantAuthorizationRequest() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    request.setParameter("response_type", "code");
    request.setParameter("client_id", "test-client");
    request.setParameter("code_challenge", "valid-challenge");
    request.setParameter("code_challenge_method", "S256");
    request.setParameter("state", "secure-state");
    
    // When
    FapiValidationResult result = fapiValidator.validateAuthorizationRequest(request);
    
    // Then
    assertTrue(result.isValid());
}
```

### Integration Tests

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class KeycloakIntegrationTest {
    
    @Test
    @Order(1)
    void shouldCompleteFullOAuth2Flow() {
        // Test complete OAuth2.1 + FAPI + DPoP flow
    }
    
    @Test
    @Order(2)
    void shouldValidateTokenBinding() {
        // Test DPoP token binding validation
    }
}
```

## Deployment

### Environment Variables

```bash
# Required environment variables
export KEYCLOAK_CLIENT_SECRET="your-secure-client-secret"
export KEYCLOAK_BASE_URL="https://auth.loanmanagement.com"
export SPRING_PROFILES_ACTIVE="prod"

# Optional security overrides
export KEYCLOAK_FAPI_ENABLED="true"
export KEYCLOAK_DPOP_ENFORCED="true"
```

### Docker Configuration

```dockerfile
# Security-hardened container
FROM openjdk:21-jre-slim

# Add security certificates
COPY certs/ /app/certs/
RUN update-ca-certificates

# Application configuration
COPY application-keycloak.yml /app/config/
COPY loan-management-system.jar /app/

# Run with security manager
ENTRYPOINT ["java", "-Djava.security.manager", "-jar", "/app/loan-management-system.jar"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: loan-management-system
spec:
  template:
    spec:
      containers:
      - name: app
        image: loan-management-system:latest
        env:
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: client-secret
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        securityContext:
          runAsNonRoot: true
          readOnlyRootFilesystem: true
          allowPrivilegeEscalation: false
```

## Security Considerations

### Production Deployment Checklist

- ✅ **TLS/HTTPS**: All communication encrypted
- ✅ **Client Authentication**: Private key JWT only
- ✅ **Token Binding**: DPoP enforcement enabled
- ✅ **PKCE**: Mandatory for all flows
- ✅ **PAR**: Pushed Authorization Requests required
- ✅ **Signed Requests**: Request objects signed
- ✅ **Token Lifetime**: Limited to 1 hour maximum
- ✅ **Scope Validation**: Principle of least privilege
- ✅ **Audit Logging**: All security events logged
- ✅ **Monitoring**: Real-time security alerts

### Compliance Validation

The implementation meets the following compliance requirements:

- **FAPI 2.0**: Financial-grade API security profile
- **OAuth 2.1**: Latest OAuth specification
- **OpenID Connect 1.0**: Identity layer compliance
- **RFC 9449 (DPoP)**: Proof-of-possession tokens
- **RFC 7636 (PKCE)**: Code exchange protection
- **RFC 9126 (PAR)**: Pushed authorization requests

## Troubleshooting

### Common Issues

1. **FAPI Validation Failures**
   - Ensure HTTPS is used for all requests
   - Verify PKCE parameters are present
   - Check request object signature

2. **DPoP Validation Failures**
   - Verify JWK is included in header
   - Check htm/htu claim accuracy
   - Ensure timestamp freshness

3. **Token Binding Errors**
   - Validate access token cnf claim
   - Verify DPoP proof JWK thumbprint
   - Check token-proof consistency

### Debug Logging

```yaml
logging:
  level:
    com.loanmanagement.security.keycloak: DEBUG
    org.springframework.security.oauth2: DEBUG
    org.springframework.web.client: DEBUG
```

## Migration Guide

### From Basic OAuth2 to FAPI

1. **Phase 1**: Deploy with FAPI validation disabled
2. **Phase 2**: Enable FAPI validation in non-enforced mode
3. **Phase 3**: Update clients to support FAPI requirements
4. **Phase 4**: Enable FAPI enforcement
5. **Phase 5**: Add DPoP support progressively

### Client Migration Steps

```javascript
// Step 1: Add PKCE support
const codeVerifier = generateCodeVerifier();
const codeChallenge = await generateCodeChallenge(codeVerifier);

// Step 2: Implement signed request objects
const requestObject = await createSignedRequestObject(params);

// Step 3: Add DPoP proof generation
const dpopProof = await generateDPoPProof(requestParams);

// Step 4: Update token requests
const tokens = await exchangeCodeForTokens(code, codeVerifier, dpopProof);
```

## References

- [OAuth 2.1 Authorization Framework](https://tools.ietf.org/html/draft-ietf-oauth-v2-1)
- [FAPI 2.0 Security Profile](https://openid.net/specs/fapi-2_0-security-profile.html)
- [RFC 9449: OAuth 2.0 Demonstration of Proof-of-Possession](https://tools.ietf.org/html/rfc9449)
- [RFC 7636: Proof Key for Code Exchange](https://tools.ietf.org/html/rfc7636)
- [RFC 9126: OAuth 2.0 Pushed Authorization Requests](https://tools.ietf.org/html/rfc9126)
- [Keycloak Documentation](https://www.keycloak.org/documentation)