# Wire Diagrams - AI Enhanced Banking System
## System Integration and Data Flow Wire Diagrams

### Frontend to Backend Wire Diagram

```plantuml
@startuml Frontend_Backend_Wire_Diagram
!theme plain

title Frontend to Backend Integration Wire Diagram

rectangle "Browser Client" as BROWSER {
    component "Risk Dashboard\nHTML/CSS/JS" as DASHBOARD
    component "Chart.js\nVisualization" as CHARTS
    component "WebSocket Client\nReal-time Updates" as WS_CLIENT
}

rectangle "Load Balancer" as LB {
    component "Nginx\nPort 80/443" as NGINX
    component "SSL Termination\nCertificates" as SSL
}

rectangle "Application Server" as APP_SERVER {
    component "Spring Boot\nPort 5000" as SPRING_APP
    component "GraphQL Endpoint\n/graphql" as GRAPHQL_EP
    component "REST API\n/api/*" as REST_API
    component "WebSocket Server\n/mcp" as WS_SERVER
}

rectangle "AI Services" as AI_SERVICES {
    component "OpenAI Assistant\nGPT-4o" as OPENAI_ASSISTANT
    component "Risk Analytics\nEngine" as RISK_ENGINE
    component "MCP Protocol\nHandler" as MCP_HANDLER
}

rectangle "Database Cluster" as DB_CLUSTER {
    database "PostgreSQL\nPrimary" as PG_PRIMARY
    database "PostgreSQL\nReplica" as PG_REPLICA
    component "Redis Cache\nElastiCache" as REDIS
}

' Frontend connections
DASHBOARD --> NGINX : "HTTPS Requests\nPort 443"
CHARTS --> NGINX : "API Calls\nChart Data"
WS_CLIENT --> NGINX : "WebSocket\nConnection"

' Load balancer routing
NGINX --> SSL : "TLS Handshake\nCertificate Validation"
SSL --> SPRING_APP : "Decrypted Traffic\nHTTP/1.1 & HTTP/2"

' Application routing
SPRING_APP --> GRAPHQL_EP : "GraphQL Queries\nPOST /graphql"
SPRING_APP --> REST_API : "REST Requests\nGET/POST/PUT"
SPRING_APP --> WS_SERVER : "WebSocket Upgrade\nProtocol Switch"

' Backend to AI services
GRAPHQL_EP --> OPENAI_ASSISTANT : "AI Analysis Requests\nFunction Calling"
GRAPHQL_EP --> RISK_ENGINE : "Risk Calculations\nPortfolio Analytics"
REST_API --> RISK_ENGINE : "Dashboard Data\nMetrics Queries"
WS_SERVER --> MCP_HANDLER : "MCP Messages\nJSON-RPC 2.0"

' AI to database connections
OPENAI_ASSISTANT --> PG_PRIMARY : "Customer Data\nLoan Information"
RISK_ENGINE --> PG_REPLICA : "Analytics Queries\nRead Operations"
RISK_ENGINE --> REDIS : "Cache Operations\nMetrics Storage"

' Data flow annotations
DASHBOARD .> CHARTS : "Data Binding\nReactive Updates"
CHARTS .> WS_CLIENT : "Real-time Events\nChart Refreshes"

note right of OPENAI_ASSISTANT
  AI Wire Configuration:
  - API Key: Environment Variable
  - Model: gpt-4o
  - Max Tokens: 4096
  - Functions: 5 banking tools
  - Timeout: 30 seconds
end note

note bottom of REDIS
  Cache Wire Setup:
  - Connection Pool: 20 connections
  - Timeout: 2 seconds
  - Failover: Automatic
  - Serialization: JSON
end note

@enduml
```

### Data Flow Wire Diagram

```plantuml
@startuml Data_Flow_Wire_Diagram
!theme plain

title Data Flow Wire Diagram - Real-time Banking Operations

rectangle "Data Sources" as SOURCES {
    component "Customer Data\nInput Forms" as CUSTOMER_INPUT
    component "Loan Applications\nProcessing" as LOAN_INPUT
    component "Payment Events\nTransactions" as PAYMENT_INPUT
    component "External APIs\nCredit Bureaus" as EXTERNAL_INPUT
}

rectangle "Data Ingestion Layer" as INGESTION {
    component "Input Validation\nSanitization" as VALIDATION
    component "Data Transformation\nNormalization" as TRANSFORM
    component "Event Streaming\nKafka Topics" as KAFKA
}

rectangle "Processing Layer" as PROCESSING {
    component "Business Logic\nValidation" as BUSINESS_LOGIC
    component "AI Processing\nOpenAI Analysis" as AI_PROCESSING
    component "Risk Calculation\nReal-time Scoring" as RISK_CALC
    component "Compliance Check\nRegulatory Validation" as COMPLIANCE
}

rectangle "Storage Layer" as STORAGE {
    database "Operational DB\nPostgreSQL" as OPERATIONAL_DB
    database "Analytics DB\nTime Series" as ANALYTICS_DB
    component "Cache Layer\nRedis Cluster" as CACHE_STORAGE
    component "Event Store\nAudit Trail" as EVENT_STORE
}

rectangle "Output Layer" as OUTPUT {
    component "GraphQL API\nReal-time Queries" as GRAPHQL_OUTPUT
    component "REST Endpoints\nDashboard Data" as REST_OUTPUT
    component "WebSocket Stream\nLive Updates" as WS_OUTPUT
    component "Alert System\nNotifications" as ALERT_OUTPUT
}

' Data ingestion flow
CUSTOMER_INPUT --> VALIDATION : "Form Data\nValidation Rules"
LOAN_INPUT --> VALIDATION : "Application Data\nBusiness Rules"
PAYMENT_INPUT --> VALIDATION : "Transaction Data\nFinancial Validation"
EXTERNAL_INPUT --> VALIDATION : "API Responses\nData Quality Checks"

VALIDATION --> TRANSFORM : "Validated Data\nStandardized Format"
TRANSFORM --> KAFKA : "Normalized Events\nEvent Sourcing"

' Processing flow
KAFKA --> BUSINESS_LOGIC : "Event Consumption\nBusiness Processing"
BUSINESS_LOGIC --> AI_PROCESSING : "Enriched Data\nAI Analysis"
BUSINESS_LOGIC --> RISK_CALC : "Customer Data\nRisk Assessment"
BUSINESS_LOGIC --> COMPLIANCE : "Transaction Data\nCompliance Validation"

' AI processing connections
AI_PROCESSING --> RISK_CALC : "AI Insights\nEnhanced Risk Data"
AI_PROCESSING --> COMPLIANCE : "AI Analysis\nCompliance Guidance"

' Storage operations
BUSINESS_LOGIC --> OPERATIONAL_DB : "CRUD Operations\nTransactional Data"
RISK_CALC --> ANALYTICS_DB : "Risk Metrics\nTime Series Data"
AI_PROCESSING --> CACHE_STORAGE : "AI Results\nFast Access"
KAFKA --> EVENT_STORE : "Event Persistence\nAudit Trail"

' Output generation
OPERATIONAL_DB --> GRAPHQL_OUTPUT : "Real-time Queries\nCustomer Data"
ANALYTICS_DB --> REST_OUTPUT : "Dashboard Metrics\nAnalytics Data"
CACHE_STORAGE --> WS_OUTPUT : "Live Updates\nStreaming Data"
RISK_CALC --> ALERT_OUTPUT : "Risk Alerts\nNotifications"

' Cross-layer data flow
RISK_CALC .> ALERT_OUTPUT : "Critical Alerts\nImmediate Notifications"
AI_PROCESSING .> WS_OUTPUT : "AI Insights\nReal-time Streaming"

note right of AI_PROCESSING
  AI Data Flow:
  1. Receive business data
  2. Call OpenAI Assistant
  3. Process AI response
  4. Update risk calculations
  5. Cache results
  6. Stream to dashboard
end note

note bottom of CACHE_STORAGE
  Cache Flow Strategy:
  - Write-through for critical data
  - Write-behind for analytics
  - TTL-based expiration
  - Invalidation on updates
end note

@enduml
```

### API Integration Wire Diagram

```plantuml
@startuml API_Integration_Wire_Diagram
!theme plain

title API Integration Wire Diagram - External Service Connections

rectangle "Internal APIs" as INTERNAL {
    component "GraphQL Gateway\n:5000/graphql" as GRAPHQL_GW
    component "REST API Gateway\n:5000/api" as REST_GW
    component "WebSocket Server\n:5000/mcp" as WS_GW
    component "Health Check\n:5000/actuator" as HEALTH
}

rectangle "AI Service APIs" as AI_APIS {
    cloud "OpenAI API\napi.openai.com" as OPENAI_API
    component "Assistant Endpoint\n/v1/assistants" as ASSISTANT_EP
    component "Chat Completions\n/v1/chat/completions" as CHAT_EP
    component "Function Calling\nTools Integration" as FUNCTION_EP
}

rectangle "Database APIs" as DB_APIS {
    component "PostgreSQL\nJDBC Connection" as PG_JDBC
    component "Redis Client\nJedis Connection" as REDIS_CLIENT
    component "Connection Pool\nHikariCP" as CONN_POOL
    component "Cache Manager\nSpring Cache" as CACHE_MGR
}

rectangle "Monitoring APIs" as MONITOR_APIS {
    component "Prometheus\nMetrics Endpoint" as PROMETHEUS
    component "Grafana\nVisualization" as GRAFANA
    component "Log Aggregation\nELK Stack" as ELK
    component "Alert Manager\nNotifications" as ALERT_MGR
}

rectangle "External Banking APIs" as BANKING_APIS {
    cloud "Credit Bureau\nEquifax/Experian" as CREDIT_API
    cloud "Payment Gateway\nStripe/PayPal" as PAYMENT_API
    cloud "Regulatory APIs\nFAPI Compliance" as REGULATORY_API
    cloud "Market Data\nFinancial APIs" as MARKET_API
}

' Internal API connections
GRAPHQL_GW --> OPENAI_API : "HTTPS/TLS 1.3\nAPI Key Authentication"
GRAPHQL_GW --> PG_JDBC : "TCP Connection\nSSL Encrypted"
REST_GW --> REDIS_CLIENT : "Redis Protocol\nPassword Authentication"
WS_GW --> CACHE_MGR : "Local Connection\nSpring Integration"

' AI API integration
OPENAI_API --> ASSISTANT_EP : "Assistant Management\nCRUD Operations"
OPENAI_API --> CHAT_EP : "Chat Interactions\nStreaming Responses"
OPENAI_API --> FUNCTION_EP : "Banking Functions\nReal-time Calling"

' Database connections
PG_JDBC --> CONN_POOL : "Connection Management\nPool Optimization"
REDIS_CLIENT --> CACHE_MGR : "Cache Operations\nEviction Policies"

' Monitoring integration
GRAPHQL_GW --> PROMETHEUS : "Metrics Export\nHTTP Scraping"
REST_GW --> PROMETHEUS : "Performance Metrics\nRequest Tracking"
PROMETHEUS --> GRAFANA : "Time Series Data\nVisualization Queries"
PROMETHEUS --> ALERT_MGR : "Alert Rules\nThreshold Monitoring"

' External banking APIs
GRAPHQL_GW --> CREDIT_API : "Credit Score Queries\nReal-time Verification"
REST_GW --> PAYMENT_API : "Payment Processing\nWebhook Integration"
GRAPHQL_GW --> REGULATORY_API : "Compliance Validation\nFAPI Requirements"
REST_GW --> MARKET_API : "Market Data\nRisk Assessment"

' Health and monitoring
HEALTH --> PROMETHEUS : "Health Metrics\nService Status"
HEALTH --> ELK : "Application Logs\nStructured Logging"

note right of OPENAI_API
  OpenAI Integration:
  - Authentication: Bearer Token
  - Rate Limit: 1000 RPM
  - Timeout: 30 seconds
  - Retry Policy: Exponential backoff
  - Error Handling: Circuit breaker
end note

note bottom of PG_JDBC
  Database Configuration:
  - Pool Size: 20 connections
  - Connection Timeout: 5 seconds
  - Validation Query: SELECT 1
  - SSL Mode: Required
  - Transaction Isolation: READ_COMMITTED
end note

@enduml
```

### Security Wire Diagram

```plantuml
@startuml Security_Wire_Diagram
!theme plain

title Security Wire Diagram - Authentication and Authorization Flow

rectangle "Client Security" as CLIENT_SEC {
    component "HTTPS Client\nTLS 1.3" as HTTPS_CLIENT
    component "JWT Token\nStorage" as JWT_STORAGE
    component "API Key\nManagement" as API_KEY_MGR
    component "Session\nManagement" as SESSION_MGR
}

rectangle "Edge Security" as EDGE_SEC {
    component "WAF\nCloudflare" as WAF
    component "DDoS Protection\nRate Limiting" as DDOS
    component "SSL/TLS\nTermination" as SSL_TERM
    component "Certificate\nManagement" as CERT_MGR
}

rectangle "Application Security" as APP_SEC {
    component "Authentication\nFilter Chain" as AUTH_FILTER
    component "Authorization\nRBAC Engine" as AUTHZ_ENGINE
    component "OWASP Filters\nSecurity Headers" as OWASP_FILTERS
    component "FAPI Compliance\nValidator" as FAPI_VALIDATOR
}

rectangle "API Security" as API_SEC {
    component "GraphQL Security\nQuery Validation" as GQL_SEC
    component "REST Security\nInput Validation" as REST_SEC
    component "WebSocket Security\nProtocol Validation" as WS_SEC
    component "OpenAI Security\nAPI Key Protection" as AI_SEC
}

rectangle "Data Security" as DATA_SEC {
    component "Database Encryption\nTDE + Column Level" as DB_ENCRYPT
    component "Cache Encryption\nRedis TLS" as CACHE_ENCRYPT
    component "Key Vault\nAWS Secrets Manager" as KEY_VAULT
    component "Audit Logging\nSecurity Events" as AUDIT_LOG
}

' Client to edge security flow
HTTPS_CLIENT --> WAF : "HTTPS Requests\nTLS Handshake"
JWT_STORAGE --> WAF : "Bearer Tokens\nAuthorization Header"
API_KEY_MGR --> WAF : "API Keys\nCustom Headers"

' Edge security processing
WAF --> DDOS : "Filtered Traffic\nThreat Detection"
DDOS --> SSL_TERM : "Rate Limited\nClean Traffic"
SSL_TERM --> CERT_MGR : "Certificate Validation\nChain Verification"

' Application security processing
CERT_MGR --> AUTH_FILTER : "Validated Requests\nDecrypted Payload"
AUTH_FILTER --> AUTHZ_ENGINE : "Authenticated Users\nRole Assignment"
AUTHZ_ENGINE --> OWASP_FILTERS : "Authorized Requests\nPermission Validation"
OWASP_FILTERS --> FAPI_VALIDATOR : "Secure Requests\nCompliance Check"

' API-level security
FAPI_VALIDATOR --> GQL_SEC : "GraphQL Requests\nQuery Complexity"
FAPI_VALIDATOR --> REST_SEC : "REST Requests\nPayload Validation"
FAPI_VALIDATOR --> WS_SEC : "WebSocket Upgrade\nProtocol Security"
FAPI_VALIDATOR --> AI_SEC : "AI Service Calls\nAPI Key Validation"

' Data security connections
GQL_SEC --> DB_ENCRYPT : "Database Queries\nEncrypted Connection"
REST_SEC --> CACHE_ENCRYPT : "Cache Operations\nTLS Encryption"
AI_SEC --> KEY_VAULT : "API Key Retrieval\nSecure Storage"
WS_SEC --> AUDIT_LOG : "WebSocket Events\nSecurity Logging"

' Cross-cutting security concerns
SESSION_MGR .> AUTH_FILTER : "Session Validation\nTimeout Management"
KEY_VAULT .> AI_SEC : "Dynamic Key Rotation\nSecret Management"
AUDIT_LOG .> OWASP_FILTERS : "Security Event Logging\nCompliance Tracking"

note right of AI_SEC
  AI Security Configuration:
  - API Key: Environment Variable
  - Request Signing: HMAC-SHA256
  - Rate Limiting: Per-user quotas
  - Content Filtering: Input/output validation
  - Audit Trail: All AI interactions logged
end note

note bottom of DB_ENCRYPT
  Database Security Setup:
  - Encryption at Rest: AES-256
  - Encryption in Transit: TLS 1.3
  - Column-level Encryption: PII data
  - Key Rotation: 90-day cycle
  - Access Logging: All queries audited
end note

@enduml
```

### Real-time Data Wire Diagram

```plantuml
@startuml Realtime_Data_Wire_Diagram
!theme plain

title Real-time Data Wire Diagram - Live Dashboard Updates

rectangle "Data Sources" as SOURCES {
    component "Customer Updates\nProfile Changes" as CUSTOMER_UPDATES
    component "Loan Status\nPayment Events" as LOAN_UPDATES
    component "Risk Calculations\nScore Changes" as RISK_UPDATES
    component "AI Insights\nRecommendations" as AI_UPDATES
}

rectangle "Event Processing" as EVENT_PROC {
    component "Event Detector\nChange Monitoring" as EVENT_DETECTOR
    component "Event Enricher\nContext Addition" as EVENT_ENRICHER
    component "Event Router\nSubscription Manager" as EVENT_ROUTER
    component "Event Buffer\nBatch Processing" as EVENT_BUFFER
}

rectangle "Real-time Pipeline" as RT_PIPELINE {
    component "WebSocket Manager\nConnection Pool" as WS_MANAGER
    component "Subscription Handler\nClient Management" as SUB_HANDLER
    component "Message Serializer\nJSON Formatting" as MSG_SERIALIZER
    component "Broadcast Engine\nMulti-client Delivery" as BROADCAST
}

rectangle "Client Delivery" as CLIENT_DEL {
    component "Dashboard Client\nWebSocket Connection" as DASHBOARD_CLIENT
    component "Update Processor\nData Transformation" as UPDATE_PROCESSOR
    component "Chart Updater\nVisualization Refresh" as CHART_UPDATER
    component "Alert Renderer\nNotification Display" as ALERT_RENDERER
}

rectangle "Persistence Layer" as PERSISTENCE {
    component "Real-time Cache\nRedis Streams" as RT_CACHE
    component "Event Store\nAudit Trail" as EVENT_STORE
    component "Snapshot Storage\nState Persistence" as SNAPSHOT_STORE
}

' Data source to event processing
CUSTOMER_UPDATES --> EVENT_DETECTOR : "Profile Changes\nTrigger Detection"
LOAN_UPDATES --> EVENT_DETECTOR : "Payment Events\nStatus Updates"
RISK_UPDATES --> EVENT_DETECTOR : "Score Changes\nThreshold Alerts"
AI_UPDATES --> EVENT_DETECTOR : "AI Insights\nRecommendation Updates"

' Event processing flow
EVENT_DETECTOR --> EVENT_ENRICHER : "Raw Events\nContext Addition"
EVENT_ENRICHER --> EVENT_ROUTER : "Enriched Events\nRouting Rules"
EVENT_ROUTER --> EVENT_BUFFER : "Routed Events\nBatch Collection"

' Real-time pipeline processing
EVENT_BUFFER --> WS_MANAGER : "Batched Events\nConnection Routing"
WS_MANAGER --> SUB_HANDLER : "Connection Events\nSubscription Matching"
SUB_HANDLER --> MSG_SERIALIZER : "Targeted Events\nSerialization"
MSG_SERIALIZER --> BROADCAST : "JSON Messages\nMulti-cast Delivery"

' Client delivery and rendering
BROADCAST --> DASHBOARD_CLIENT : "WebSocket Messages\nReal-time Delivery"
DASHBOARD_CLIENT --> UPDATE_PROCESSOR : "Raw Updates\nClient Processing"
UPDATE_PROCESSOR --> CHART_UPDATER : "Processed Data\nChart Refresh"
UPDATE_PROCESSOR --> ALERT_RENDERER : "Alert Data\nNotification Display"

' Persistence for reliability
EVENT_ENRICHER --> RT_CACHE : "Event Caching\nFast Retrieval"
EVENT_ROUTER --> EVENT_STORE : "Event Persistence\nAudit Trail"
EVENT_BUFFER --> SNAPSHOT_STORE : "State Snapshots\nReplay Capability"

' Real-time data flow paths
RISK_UPDATES .> CHART_UPDATER : "Direct Risk Charts\nImmediate Update"
AI_UPDATES .> ALERT_RENDERER : "AI Alerts\nPriority Notifications"

note right of WS_MANAGER
  WebSocket Configuration:
  - Max Connections: 1000
  - Heartbeat Interval: 30 seconds
  - Message Buffer: 100 messages
  - Compression: Enabled
  - Reconnection: Automatic
end note

note bottom of RT_CACHE
  Real-time Cache Setup:
  - Stream Length: 10000 entries
  - TTL: 24 hours
  - Consumer Groups: Dashboard clients
  - Message Acknowledgment: Required
  - Delivery Guarantees: At-least-once
end note

@enduml
```