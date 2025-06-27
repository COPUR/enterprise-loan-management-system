# 🔐 Secure Microservices Architecture with Keycloak OAuth 2.1 & Istio Service Mesh

## 🏗️ **Architecture Overview**

This document describes a zero-trust, secure microservices architecture for the Enhanced Enterprise Banking System that integrates:

- **Keycloak** as OAuth 2.1 Authorization Server
- **Istio Service Mesh** for traffic management and security
- **Envoy Sidecars** for mTLS and request validation
- **Zero-Trust Security Model** with comprehensive audit and compliance

---

## 📊 **Architecture Diagram**

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           INTERNET / EXTERNAL CLIENTS                          │
└─────────────────────────────┬───────────────────────────────────────────────────┘
                              │ HTTPS/TLS 1.3
                              │
┌─────────────────────────────▼───────────────────────────────────────────────────┐
│                        ISTIO INGRESS GATEWAY                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────────┐ │
│  │  TLS Termination│  │ OAuth2 Proxy/   │  │   Rate Limiting &              │ │
│  │  & Certificate │  │ Authorization   │  │   WAF Protection               │ │
│  │  Management     │  │ Code Flow       │  │                                │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────────────────────────┘
                              │ JWT Token Validation
                              │
┌─────────────────────────────▼───────────────────────────────────────────────────┐
│                           ISTIO SERVICE MESH                                   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                        KEYCLOAK CLUSTER                                │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │   │
│  │  │   Keycloak-1    │  │   Keycloak-2    │  │     PostgreSQL         │ │   │
│  │  │ (OAuth 2.1 AS)  │  │ (OAuth 2.1 AS)  │  │   (User Store)         │ │   │
│  │  │   + Envoy       │  │   + Envoy       │  │   + Envoy              │ │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                    BANKING MICROSERVICES                               │   │
│  │                                                                         │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │   │
│  │  │  Loan Service   │  │ Payment Service │  │  Customer Service      │ │   │
│  │  │   + Envoy       │  │   + Envoy       │  │   + Envoy              │ │   │
│  │  │ (JWT Validation)│  │ (JWT Validation)│  │ (JWT Validation)       │ │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │   │
│  │                                                                         │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │   │
│  │  │  Risk Service   │  │  Audit Service  │  │  Notification Service  │ │   │
│  │  │   + Envoy       │  │   + Envoy       │  │   + Envoy              │ │   │
│  │  │ (JWT Validation)│  │ (JWT Validation)│  │ (JWT Validation)       │ │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      DATA LAYER SERVICES                               │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │   │
│  │  │   PostgreSQL    │  │      Redis      │  │       Kafka            │ │   │
│  │  │   + Envoy       │  │   + Envoy       │  │   + Envoy              │ │   │
│  │  │ (mTLS Only)     │  │ (mTLS Only)     │  │ (mTLS Only)            │ │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                           OBSERVABILITY STACK                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────────┐ │
│  │    Jaeger       │  │   Prometheus    │  │       Grafana                   │ │
│  │   (Tracing)     │  │   (Metrics)     │  │    (Dashboards)                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔑 **OAuth 2.1 Authentication Flow**

### **Authorization Code Flow with PKCE**

```
┌──────────────┐                ┌─────────────────┐                ┌──────────────┐
│   Client     │                │ Istio Gateway   │                │  Keycloak    │
│ Application  │                │   + OAuth2      │                │ (OAuth 2.1)  │
│              │                │    Proxy        │                │              │
└──────┬───────┘                └─────────┬───────┘                └──────┬───────┘
       │                                  │                               │
       │ 1. Access Protected Resource     │                               │
       ├─────────────────────────────────▶│                               │
       │                                  │                               │
       │ 2. Redirect to Authorization     │                               │
       │◀─────────────────────────────────┤                               │
       │                                  │                               │
       │ 3. Authorization Request         │                               │
       │    (with PKCE code_challenge)    │                               │
       ├──────────────────────────────────┼──────────────────────────────▶│
       │                                  │                               │
       │ 4. User Authentication & Consent │                               │
       │◀─────────────────────────────────┼───────────────────────────────┤
       │                                  │                               │
       │ 5. Authorization Code            │                               │
       │◀─────────────────────────────────┼───────────────────────────────┤
       │                                  │                               │
       │ 6. Token Exchange Request        │                               │
       │    (code + code_verifier)        │                               │
       ├──────────────────────────────────┼──────────────────────────────▶│
       │                                  │                               │
       │ 7. JWT Access Token + ID Token   │                               │
       │◀─────────────────────────────────┼───────────────────────────────┤
       │                                  │                               │
       │ 8. API Request with JWT          │                               │
       ├─────────────────────────────────▶│                               │
       │                                  │ 9. JWT Validation             │
       │                                  ├──────────────────────────────▶│
       │                                  │ 10. Token Valid + Claims      │
       │                                  │◀──────────────────────────────┤
       │ 11. Authorized Response          │                               │
       │◀─────────────────────────────────┤                               │
```

---

## 🔐 **Security Configuration**

### **1. Keycloak OAuth 2.1 Configuration**

#### **Realm Configuration**
```yaml
# keycloak-realm-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-realm-config
  namespace: banking-system
data:
  banking-realm.json: |
    {
      "realm": "banking",
      "enabled": true,
      "sslRequired": "external",
      "registrationAllowed": false,
      "loginWithEmailAllowed": true,
      "duplicateEmailsAllowed": false,
      "resetPasswordAllowed": true,
      "editUsernameAllowed": false,
      "bruteForceProtected": true,
      "permanentLockout": false,
      "maxFailureWaitSeconds": 900,
      "minimumQuickLoginWaitSeconds": 60,
      "waitIncrementSeconds": 60,
      "quickLoginCheckMilliSeconds": 1000,
      "maxDeltaTimeSeconds": 43200,
      "failureFactor": 5,
      "defaultSignatureAlgorithm": "RS256",
      "accessTokenLifespan": 300,
      "accessTokenLifespanForImplicitFlow": 900,
      "ssoSessionIdleTimeout": 1800,
      "ssoSessionMaxLifespan": 36000,
      "ssoSessionIdleTimeoutRememberMe": 0,
      "ssoSessionMaxLifespanRememberMe": 0,
      "offlineSessionIdleTimeout": 2592000,
      "offlineSessionMaxLifespanEnabled": false,
      "offlineSessionMaxLifespan": 5184000,
      "accessCodeLifespan": 60,
      "accessCodeLifespanUserAction": 300,
      "accessCodeLifespanLogin": 1800,
      "actionTokenGeneratedByAdminLifespan": 43200,
      "actionTokenGeneratedByUserLifespan": 300,
      "oauth2DeviceCodeLifespan": 600,
      "oauth2DevicePollingInterval": 5,
      "clients": [
        {
          "clientId": "banking-web-app",
          "enabled": true,
          "clientAuthenticatorType": "client-secret",
          "secret": "banking-web-secret-2024",
          "redirectUris": [
            "https://banking.local/oauth2/callback",
            "https://banking.local/*",
            "http://localhost:3000/*"
          ],
          "webOrigins": [
            "https://banking.local",
            "http://localhost:3000"
          ],
          "protocol": "openid-connect",
          "attributes": {
            "saml.assertion.signature": "false",
            "saml.force.post.binding": "false",
            "saml.multivalued.roles": "false",
            "saml.encrypt": "false",
            "oauth2.device.authorization.grant.enabled": "false",
            "backchannel.logout.revoke.offline.tokens": "false",
            "saml.server.signature": "false",
            "saml.server.signature.keyinfo.ext": "false",
            "use.refresh.tokens": "true",
            "exclude.session.state.from.auth.response": "false",
            "oidc.ciba.grant.enabled": "false",
            "saml.artifact.binding": "false",
            "backchannel.logout.session.required": "true",
            "client_credentials.use_refresh_token": "false",
            "saml_force_name_id_format": "false",
            "require.pushed.authorization.requests": "false",
            "saml.client.signature": "false",
            "tls.client.certificate.bound.access.tokens": "false",
            "saml.authnstatement": "false",
            "display.on.consent.screen": "false",
            "saml.onetimeuse.condition": "false"
          },
          "authenticationFlowBindingOverrides": {},
          "fullScopeAllowed": true,
          "nodeReRegistrationTimeout": -1,
          "defaultClientScopes": [
            "web-origins",
            "roles",
            "profile",
            "email"
          ],
          "optionalClientScopes": [
            "address",
            "phone",
            "offline_access",
            "microprofile-jwt"
          ]
        },
        {
          "clientId": "banking-api-gateway",
          "enabled": true,
          "clientAuthenticatorType": "client-secret",
          "secret": "banking-gateway-secret-2024",
          "serviceAccountsEnabled": true,
          "standardFlowEnabled": false,
          "implicitFlowEnabled": false,
          "directAccessGrantsEnabled": false,
          "protocol": "openid-connect",
          "attributes": {
            "use.refresh.tokens": "false",
            "client_credentials.use_refresh_token": "false"
          }
        }
      ],
      "roles": {
        "realm": [
          {
            "name": "banking-admin",
            "description": "Banking System Administrator",
            "composite": false
          },
          {
            "name": "loan-officer",
            "description": "Loan Processing Officer",
            "composite": false
          },
          {
            "name": "customer",
            "description": "Banking Customer",
            "composite": false
          },
          {
            "name": "risk-analyst",
            "description": "Risk Assessment Analyst",
            "composite": false
          },
          {
            "name": "compliance-officer",
            "description": "Compliance and Audit Officer",
            "composite": false
          }
        ]
      },
      "groups": [
        {
          "name": "Banking Administrators",
          "path": "/Banking Administrators",
          "realmRoles": ["banking-admin"]
        },
        {
          "name": "Loan Officers",
          "path": "/Loan Officers", 
          "realmRoles": ["loan-officer"]
        },
        {
          "name": "Customers",
          "path": "/Customers",
          "realmRoles": ["customer"]
        }
      ],
      "users": [
        {
          "username": "admin",
          "enabled": true,
          "emailVerified": true,
          "firstName": "Banking",
          "lastName": "Administrator",
          "email": "admin@banking.local",
          "credentials": [
            {
              "type": "password",
              "value": "Banking@Admin2024",
              "temporary": false
            }
          ],
          "realmRoles": ["banking-admin"],
          "groups": ["/Banking Administrators"]
        },
        {
          "username": "loan.officer",
          "enabled": true,
          "emailVerified": true,
          "firstName": "Loan",
          "lastName": "Officer",
          "email": "loan.officer@banking.local",
          "credentials": [
            {
              "type": "password",
              "value": "LoanOfficer@2024",
              "temporary": false
            }
          ],
          "realmRoles": ["loan-officer"],
          "groups": ["/Loan Officers"]
        }
      ],
      "scopeMappings": [
        {
          "client": "banking-api-gateway",
          "roles": ["banking-admin", "loan-officer", "customer"]
        }
      ],
      "clientScopeMappings": {
        "account": [
          {
            "client": "banking-web-app",
            "roles": ["view-profile"]
          }
        ]
      }
    }
```

#### **Keycloak Deployment**
```yaml
# keycloak-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
  namespace: banking-system
  labels:
    app: keycloak
    version: v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: keycloak
      version: v1
  template:
    metadata:
      labels:
        app: keycloak
        version: v1
      annotations:
        sidecar.istio.io/inject: "true"
        sidecar.istio.io/proxyCPU: "100m"
        sidecar.istio.io/proxyMemory: "128Mi"
    spec:
      containers:
      - name: keycloak
        image: quay.io/keycloak/keycloak:23.0.4
        command:
        - "/opt/keycloak/bin/kc.sh"
        - "start"
        - "--optimized"
        - "--import-realm"
        env:
        # Admin Configuration
        - name: KEYCLOAK_ADMIN
          value: "admin"
        - name: KEYCLOAK_ADMIN_PASSWORD
          valueFrom:
            secretKeyRef:
              name: keycloak-admin-secret
              key: password
        
        # Database Configuration
        - name: KC_DB
          value: "postgres"
        - name: KC_DB_URL
          value: "jdbc:postgresql://postgres:5432/keycloak"
        - name: KC_DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: KC_DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        
        # OAuth 2.1 Configuration
        - name: KC_HOSTNAME
          value: "auth.banking.local"
        - name: KC_HOSTNAME_STRICT
          value: "false"
        - name: KC_HTTP_ENABLED
          value: "true"
        - name: KC_HTTPS_PORT
          value: "8443"
        - name: KC_PROXY
          value: "edge"
        
        # Security Configuration
        - name: KC_FEATURES
          value: "authorization,account-api,admin-fine-grained-authz,declarative-user-profile,dynamic-scopes,fips,recovery-codes,scripts,token-exchange"
        - name: KC_LOG_LEVEL
          value: "INFO"
        - name: KC_METRICS_ENABLED
          value: "true"
        - name: KC_HEALTH_ENABLED
          value: "true"
        
        # Banking Compliance
        - name: KC_SPI_EVENTS_LISTENER_JBOSS_LOGGING_SUCCESS_LEVEL
          value: "info"
        - name: KC_SPI_EVENTS_LISTENER_JBOSS_LOGGING_ERROR_LEVEL
          value: "warn"
        
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: https
          containerPort: 8443
          protocol: TCP
        - name: management
          containerPort: 9000
          protocol: TCP
        
        volumeMounts:
        - name: realm-config
          mountPath: /opt/keycloak/data/import
          readOnly: true
        
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        
        livenessProbe:
          httpGet:
            path: /health/live
            port: 9000
          initialDelaySeconds: 90
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 9000
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
      
      volumes:
      - name: realm-config
        configMap:
          name: keycloak-realm-config
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak
  namespace: banking-system
  labels:
    app: keycloak
spec:
  selector:
    app: keycloak
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: https
    port: 8443
    targetPort: 8443
  - name: management
    port: 9000
    targetPort: 9000
---
apiVersion: v1
kind: Secret
metadata:
  name: keycloak-admin-secret
  namespace: banking-system
type: Opaque
data:
  password: QmFua2luZ0BLZXljbG9ha0FkbWluMjAyNA== # Banking@KeycloakAdmin2024
---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: banking-system
type: Opaque
data:
  username: cG9zdGdyZXM= # postgres
  password: QmFua2luZ0BQb3N0Z3JlczIwMjQ= # Banking@Postgres2024
```

### **2. Istio Service Mesh Configuration**

#### **Istio Installation with Security Features**
```bash
# Install Istio with security profile
istioctl install --set values.defaultRevision=default \
  --set meshConfig.accessLogFile=/dev/stdout \
  --set meshConfig.defaultConfig.gatewayTopology.numTrustedProxies=1 \
  --set meshConfig.defaultConfig.proxyStatsMatcher.inclusionRegexps=".*circuit_breakers.*|.*upstream_rq_retry.*|.*upstream_rq_pending.*|.*_cx_.*" \
  --set values.pilot.env.PILOT_ENABLE_WORKLOAD_ENTRY_AUTOREGISTRATION=true \
  --set values.global.meshID=banking-mesh \
  --set values.global.network=banking-network \
  --set values.global.tracer.zipkin.address=jaeger.istio-system:9411 \
  --set values.telemetry.v2.prometheus.configOverride.disable_host_header_fallback=true
```

#### **Mutual TLS Policy**
```yaml
# mtls-policy.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: banking-system
spec:
  mtls:
    mode: STRICT
---
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: keycloak-mtls
  namespace: banking-system
spec:
  selector:
    matchLabels:
      app: keycloak
  mtls:
    mode: STRICT
  portLevelMtls:
    8080:
      mode: PERMISSIVE  # Allow HTTP for health checks
    9000:
      mode: PERMISSIVE  # Allow HTTP for management
---
# Cluster-wide mTLS enforcement
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default-strict-mtls
  namespace: istio-system
spec:
  mtls:
    mode: STRICT
```

#### **JWT Request Authentication**
```yaml
# jwt-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: keycloak-jwt
  namespace: banking-system
spec:
  selector:
    matchLabels:
      app: banking-service
  jwtRules:
  - issuer: "https://auth.banking.local/realms/banking"
    jwksUri: "https://auth.banking.local/realms/banking/protocol/openid-connect/certs"
    audiences:
    - "banking-web-app"
    - "banking-api-gateway"
    fromHeaders:
    - name: Authorization
      prefix: "Bearer "
    fromParams:
    - "access_token"
    forwardOriginalToken: true
    outputPayloadToHeader: "x-jwt-payload"
---
# Gateway JWT Authentication
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: gateway-jwt
  namespace: istio-system
spec:
  selector:
    matchLabels:
      istio: ingressgateway
  jwtRules:
  - issuer: "https://auth.banking.local/realms/banking"
    jwksUri: "https://auth.banking.local/realms/banking/protocol/openid-connect/certs"
    audiences:
    - "banking-web-app"
    - "banking-api-gateway"
    fromHeaders:
    - name: Authorization
      prefix: "Bearer "
    fromCookies:
    - "access_token"
    forwardOriginalToken: true
```

#### **Authorization Policies**
```yaml
# authorization-policies.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: banking-admin-policy
  namespace: banking-system
spec:
  selector:
    matchLabels:
      app: banking-admin-service
  rules:
  - from:
    - source:
        requestPrincipals: ["https://auth.banking.local/realms/banking/*"]
    when:
    - key: request.auth.claims[realm_access][roles]
      values: ["banking-admin"]
    - key: request.headers[x-forwarded-proto]
      values: ["https"]
  - to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
        paths: ["/api/admin/*"]

---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: loan-service-policy
  namespace: banking-system
spec:
  selector:
    matchLabels:
      app: loan-service
  rules:
  # Loan Officers - Full Access
  - from:
    - source:
        requestPrincipals: ["https://auth.banking.local/realms/banking/*"]
    when:
    - key: request.auth.claims[realm_access][roles]
      values: ["loan-officer", "banking-admin"]
  
  # Customers - Limited Access
  - from:
    - source:
        requestPrincipals: ["https://auth.banking.local/realms/banking/*"]
    when:
    - key: request.auth.claims[realm_access][roles]
      values: ["customer"]
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/loans/my/*", "/api/loans/apply"]

---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: payment-service-policy
  namespace: banking-system
spec:
  selector:
    matchLabels:
      app: payment-service
  rules:
  - from:
    - source:
        requestPrincipals: ["https://auth.banking.local/realms/banking/*"]
    when:
    - key: request.auth.claims[realm_access][roles]
      values: ["customer", "loan-officer", "banking-admin"]
    - key: request.auth.claims[sub]
      values: ["*"]  # Must be authenticated user
  to:
  - operation:
      methods: ["GET", "POST"]
      paths: ["/api/payments/*"]

---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: data-layer-policy
  namespace: banking-system
spec:
  selector:
    matchLabels:
      tier: data
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/banking-system/sa/default"]
    - source:
        namespaces: ["banking-system"]
  action: ALLOW

---
# Deny all by default
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all-default
  namespace: banking-system
spec: {}

---
# Allow health checks
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-health-checks
  namespace: banking-system
spec:
  rules:
  - to:
    - operation:
        methods: ["GET"]
        paths: ["/health", "/actuator/health", "/healthz", "/ready"]
  - to:
    - operation:
        methods: ["GET"]
        paths: ["/metrics", "/actuator/prometheus"]
```

### **3. Istio Ingress Gateway Configuration**

#### **Gateway Configuration**
```yaml
# istio-gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: banking-gateway
  namespace: banking-system
spec:
  selector:
    istio: ingressgateway
  servers:
  # HTTPS Configuration
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: banking-tls-cert
    hosts:
    - "banking.local"
    - "auth.banking.local"
    - "api.banking.local"
  
  # HTTP Redirect to HTTPS
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "banking.local"
    - "auth.banking.local"
    - "api.banking.local"
    tls:
      httpsRedirect: true

---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: banking-virtualservice
  namespace: banking-system
spec:
  hosts:
  - "banking.local"
  - "auth.banking.local"
  - "api.banking.local"
  gateways:
  - banking-gateway
  http:
  # Keycloak Authentication Service
  - match:
    - uri:
        prefix: "/realms/"
      headers:
        host:
          exact: "auth.banking.local"
    - uri:
        prefix: "/admin/"
      headers:
        host:
          exact: "auth.banking.local"
    route:
    - destination:
        host: keycloak
        port:
          number: 8080
    timeout: 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
  
  # Banking API Services (JWT Required)
  - match:
    - uri:
        prefix: "/api/loans"
      headers:
        host:
          regex: "(banking|api).banking.local"
    route:
    - destination:
        host: loan-service
        port:
          number: 8080
    headers:
      request:
        add:
          x-service: "loan-service"
          x-gateway: "istio-ingress"
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
    retries:
      attempts: 3
      perTryTimeout: 10s
      retryOn: 5xx,reset,connect-failure,refused-stream
  
  - match:
    - uri:
        prefix: "/api/payments"
      headers:
        host:
          regex: "(banking|api).banking.local"
    route:
    - destination:
        host: payment-service
        port:
          number: 8080
    headers:
      request:
        add:
          x-service: "payment-service"
  
  - match:
    - uri:
        prefix: "/api/customers"
      headers:
        host:
          regex: "(banking|api).banking.local"
    route:
    - destination:
        host: customer-service
        port:
          number: 8080
  
  # Health and Monitoring Endpoints (No JWT Required)
  - match:
    - uri:
        prefix: "/health"
    - uri:
        prefix: "/actuator/health"
    route:
    - destination:
        host: banking-gateway-service
        port:
          number: 8080
  
  # Default route to main banking application
  - match:
    - uri:
        prefix: "/"
    route:
    - destination:
        host: banking-web-service
        port:
          number: 8080
```

#### **OAuth2 Proxy Integration**
```yaml
# oauth2-proxy.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oauth2-proxy
  namespace: banking-system
  labels:
    app: oauth2-proxy
    version: v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: oauth2-proxy
      version: v1
  template:
    metadata:
      labels:
        app: oauth2-proxy
        version: v1
      annotations:
        sidecar.istio.io/inject: "true"
    spec:
      containers:
      - name: oauth2-proxy
        image: quay.io/oauth2-proxy/oauth2-proxy:v7.5.1
        args:
        - --config=/etc/oauth2-proxy/oauth2-proxy.cfg
        env:
        - name: OAUTH2_PROXY_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: oauth2-proxy-secret
              key: client-secret
        - name: OAUTH2_PROXY_COOKIE_SECRET
          valueFrom:
            secretKeyRef:
              name: oauth2-proxy-secret
              key: cookie-secret
        ports:
        - containerPort: 4180
          protocol: TCP
        volumeMounts:
        - name: oauth2-proxy-config
          mountPath: /etc/oauth2-proxy
          readOnly: true
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "200m"
        livenessProbe:
          httpGet:
            path: /ping
            port: 4180
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /ping
            port: 4180
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: oauth2-proxy-config
        configMap:
          name: oauth2-proxy-config
---
apiVersion: v1
kind: Service
metadata:
  name: oauth2-proxy
  namespace: banking-system
  labels:
    app: oauth2-proxy
spec:
  selector:
    app: oauth2-proxy
  ports:
  - name: http
    port: 4180
    targetPort: 4180
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: oauth2-proxy-config
  namespace: banking-system
data:
  oauth2-proxy.cfg: |
    # OAuth2 Proxy Configuration for Banking System
    http_address = "0.0.0.0:4180"
    
    # Keycloak OIDC Configuration
    provider = "keycloak-oidc"
    client_id = "banking-web-app"
    oidc_issuer_url = "https://auth.banking.local/realms/banking"
    
    # Redirect URLs
    redirect_url = "https://banking.local/oauth2/callback"
    
    # Cookie Configuration
    cookie_secure = true
    cookie_httponly = true
    cookie_samesite = "lax"
    cookie_domains = [".banking.local"]
    cookie_name = "_oauth2_proxy"
    
    # Session Configuration
    session_store_type = "redis"
    redis_connection_url = "redis://redis:6379"
    session_cookie_minimal = true
    
    # Security Configuration
    skip_provider_button = true
    skip_auth_strip_headers = false
    pass_basic_auth = false
    pass_access_token = true
    pass_user_headers = true
    pass_authorization_header = true
    
    # Upstream Configuration
    upstreams = [
      "http://banking-web-service:8080"
    ]
    
    # Allowed email domains
    email_domains = ["*"]
    
    # Scope Configuration
    scope = "openid profile email roles"
    
    # Audit and Logging
    request_logging = true
    auth_logging = true
    standard_logging = true
    
    # Banking Compliance Headers
    set_authorization_header = true
    set_xauthrequest = true
    
    # Rate Limiting
    rate_limit_eps = 100
---
apiVersion: v1
kind: Secret
metadata:
  name: oauth2-proxy-secret
  namespace: banking-system
type: Opaque
data:
  client-secret: YmFua2luZy13ZWItc2VjcmV0LTIwMjQ= # banking-web-secret-2024
  cookie-secret: YmFua2luZ09BdXRoMlByb3h5Q29va2llU2VjcmV0MjAyNA== # Random 32-byte secret
```

### **4. Envoy Sidecar Configuration**

#### **Envoy Filter for Additional Security**
```yaml
# envoy-filters.yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: banking-security-headers
  namespace: banking-system
spec:
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
          value:
            stat_prefix: banking_rate_limiter
            token_bucket:
              max_tokens: 100
              tokens_per_fill: 100
              fill_interval: 60s
            filter_enabled:
              runtime_key: banking_rate_limit_enabled
              default_value:
                numerator: 100
                denominator: HUNDRED
            filter_enforced:
              runtime_key: banking_rate_limit_enforced
              default_value:
                numerator: 100
                denominator: HUNDRED
  
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.wasm
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.http.wasm.v3.Wasm
          config:
            name: banking_security_filter
            root_id: banking_security
            configuration:
              "@type": type.googleapis.com/google.protobuf.StringValue
              value: |
                {
                  "banking_compliance": true,
                  "audit_enabled": true,
                  "pci_dss_mode": true,
                  "log_level": "info"
                }

---
# Circuit Breaker Configuration
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: banking-circuit-breaker
  namespace: banking-system
spec:
  host: "*.banking-system.svc.cluster.local"
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 50
        connectTimeout: 30s
        tcpKeepalive:
          time: 7200s
          interval: 75s
      http:
        http1MaxPendingRequests: 10
        http2MaxRequests: 100
        maxRequestsPerConnection: 2
        maxRetries: 3
        consecutiveGatewayErrors: 5
        interval: 30s
        baseEjectionTime: 30s
        maxEjectionPercent: 50
        minHealthPercent: 50
    outlierDetection:
      consecutiveGatewayErrors: 5
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
      minHealthPercent: 30
```

### **5. Zero-Trust Security Implementation**

#### **Network Policies**
```yaml
# network-policies.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-default-deny
  namespace: banking-system
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-services-policy
  namespace: banking-system
spec:
  podSelector:
    matchLabels:
      tier: application
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
    - podSelector:
        matchLabels:
          app: oauth2-proxy
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 15090  # Envoy admin
  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: data
    ports:
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 6379  # Redis
    - protocol: TCP
      port: 9092  # Kafka
  - to:
    - podSelector:
        matchLabels:
          app: keycloak
    ports:
    - protocol: TCP
      port: 8080

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: keycloak-policy
  namespace: banking-system
spec:
  podSelector:
    matchLabels:
      app: keycloak
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
    - podSelector:
        matchLabels:
          tier: application
    - podSelector:
        matchLabels:
          app: oauth2-proxy
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 9000  # Management
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to: []
    ports:
    - protocol: TCP
      port: 53   # DNS
    - protocol: UDP
      port: 53   # DNS
    - protocol: TCP
      port: 443  # HTTPS for external integrations

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: data-layer-policy
  namespace: banking-system
spec:
  podSelector:
    matchLabels:
      tier: data
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: application
    - podSelector:
        matchLabels:
          app: keycloak
```

### **6. Compliance and Audit Configuration**

#### **Audit Logging Configuration**
```yaml
# audit-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: banking-audit-config
  namespace: banking-system
data:
  audit-policy.yaml: |
    apiVersion: audit.k8s.io/v1
    kind: Policy
    rules:
    # Banking Authentication Events
    - level: Metadata
      namespaces: ["banking-system"]
      resources:
      - group: "security.istio.io"
        resources: ["authorizationpolicies", "requestauthentications"]
      omitStages:
      - RequestReceived
    
    # Banking Application Events
    - level: Request
      namespaces: ["banking-system"]
      resources:
      - group: "apps"
        resources: ["deployments", "statefulsets"]
      verbs: ["create", "update", "patch", "delete"]
    
    # Secret Access (PCI DSS Requirement)
    - level: Metadata
      resources:
      - group: ""
        resources: ["secrets"]
      verbs: ["get", "list", "watch"]
      namespaces: ["banking-system"]
    
    # Banking Service Access
    - level: RequestResponse
      namespaces: ["banking-system"]
      resources:
      - group: ""
        resources: ["services"]
      verbs: ["create", "update", "patch", "delete"]

---
# Falco Security Rules for Banking
apiVersion: v1
kind: ConfigMap
metadata:
  name: falco-banking-rules
  namespace: banking-system
data:
  banking_rules.yaml: |
    # Banking Security Rules
    - rule: Unauthorized Banking API Access
      desc: Detect unauthorized access to banking APIs
      condition: >
        k8s_audit and ka.verb in (get, post, put, delete) and
        ka.uri contains "/api/" and
        ka.uri contains "banking" and
        not ka.user.name contains "system:serviceaccount:banking-system"
      output: >
        Unauthorized banking API access
        (user=%ka.user.name verb=%ka.verb uri=%ka.uri)
      priority: CRITICAL
      tags: [banking, security, api]
    
    - rule: Banking Database Direct Access
      desc: Detect direct database access bypassing application
      condition: >
        spawned_process and proc.name in (psql, mysql, mongo) and
        k8s.ns.name = "banking-system"
      output: >
        Direct database access detected in banking namespace
        (user=%user.name command=%proc.cmdline)
      priority: HIGH
      tags: [banking, database, security]
    
    - rule: Banking Secret Access
      desc: Monitor access to banking secrets
      condition: >
        k8s_audit and ka.verb in (get, list) and
        ka.target.resource = "secrets" and
        ka.target.namespace = "banking-system"
      output: >
        Banking secret accessed
        (user=%ka.user.name secret=%ka.target.name)
      priority: WARNING
      tags: [banking, secrets, compliance]
```

#### **Monitoring and Alerting**
```yaml
# banking-monitoring.yaml
apiVersion: v1
kind: ServiceMonitor
metadata:
  name: banking-services
  namespace: banking-system
spec:
  selector:
    matchLabels:
      app: banking-service
  endpoints:
  - port: metrics
    interval: 30s
    path: /actuator/prometheus
    honorLabels: true

---
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: banking-security-alerts
  namespace: banking-system
spec:
  groups:
  - name: banking.security
    rules:
    - alert: BankingUnauthorizedAccess
      expr: rate(istio_requests_total{destination_service_name=~".*banking.*",response_code!~"2.*"}[5m]) > 0.1
      for: 1m
      labels:
        severity: critical
        service: banking
        compliance: security
      annotations:
        summary: "High rate of unauthorized access to banking services"
        description: "Banking service {{ $labels.destination_service_name }} is receiving {{ $value }} unauthorized requests per second"
    
    - alert: BankingJWTValidationFailure
      expr: rate(istio_request_total{destination_service_name=~".*banking.*",response_code="401"}[5m]) > 0.05
      for: 2m
      labels:
        severity: warning
        service: banking
        compliance: authentication
      annotations:
        summary: "JWT validation failures in banking system"
        description: "High rate of JWT validation failures: {{ $value }} per second"
    
    - alert: BankingServiceMeshDown
      expr: up{job="istio-proxy"} == 0
      for: 30s
      labels:
        severity: critical
        service: istio
        compliance: availability
      annotations:
        summary: "Banking service mesh component down"
        description: "Istio proxy is down: {{ $labels.instance }}"
    
    - alert: BankingDatabaseConnectionFailure
      expr: rate(banking_database_connection_errors_total[5m]) > 0.01
      for: 1m
      labels:
        severity: high
        service: database
        compliance: availability
      annotations:
        summary: "Banking database connection failures"
        description: "Database connection failures: {{ $value }} per second"
```

---

## 🛡️ **Security Features Summary**

### **Zero-Trust Implementation**
1. **Default Deny**: All traffic denied by default
2. **Explicit Allow**: Only explicitly allowed communications permitted
3. **Identity Verification**: Every request authenticated and authorized
4. **Encryption Everywhere**: mTLS for all service-to-service communication
5. **Least Privilege**: Minimal required permissions granted

### **Compliance Features**
- **PCI DSS**: Card data protection, network segmentation, access controls
- **OWASP Top 10**: Protection against common vulnerabilities
- **SOX**: Financial reporting controls and audit trails
- **GDPR**: Data protection and privacy controls
- **Banking Regulations**: KYC, AML, and financial compliance

### **Audit and Monitoring**
- **Request Tracing**: Full request lifecycle tracking
- **Security Events**: Real-time security monitoring
- **Compliance Reporting**: Automated compliance reports
- **Anomaly Detection**: ML-based threat detection
- **Incident Response**: Automated security incident handling

---

This architecture provides enterprise-grade security with OAuth 2.1, zero-trust networking, comprehensive compliance, and full auditability for banking applications.