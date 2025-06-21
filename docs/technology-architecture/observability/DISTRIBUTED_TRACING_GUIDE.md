# Distributed Tracing Implementation Guide

## Overview

This guide provides comprehensive documentation for implementing distributed tracing in the Enterprise Loan Management System using OpenTelemetry and Jaeger, with banking-specific compliance and security considerations.

## Distributed Tracing Fundamentals

### Core Concepts

#### Trace
A complete journey of a request through the system, spanning multiple services and operations.

```
Trace ID: 1a2b3c4d5e6f7g8h
├── Span: loan-application-request
│   ├── Span: customer-validation
│   ├── Span: credit-check
│   └── Span: risk-assessment
└── Span: loan-approval-decision
```

#### Span
A single unit of work within a trace, representing an operation with start time, duration, and metadata.

#### Context Propagation
Mechanism for passing trace context across service boundaries and thread boundaries.

## Architecture Implementation

### OpenTelemetry SDK Integration

#### Maven Dependencies
```xml
<dependencies>
    <!-- OpenTelemetry Core -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk</artifactId>
        <version>1.32.0</version>
    </dependency>
    
    <!-- OpenTelemetry Spring Boot Starter -->
    <dependency>
        <groupId>io.opentelemetry.instrumentation</groupId>
        <artifactId>opentelemetry-spring-boot-starter</artifactId>
        <version>1.32.0-alpha</version>
    </dependency>
    
    <!-- OTLP Exporter -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
        <version>1.32.0</version>
    </dependency>
    
    <!-- Jaeger Exporter -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-jaeger</artifactId>
        <version>1.32.0</version>
    </dependency>
</dependencies>
```

#### Configuration
```java
@Configuration
@EnableAutoConfiguration
public class TracingConfiguration {

    @Value("${tracing.jaeger.endpoint:http://jaeger:14250}")
    private String jaegerEndpoint;
    
    @Value("${tracing.service.name:loan-management-system}")
    private String serviceName;
    
    @Value("${tracing.service.version:1.0.0}")
    private String serviceVersion;

    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(BatchSpanProcessor.builder(
                        OtlpGrpcSpanExporter.builder()
                            .setEndpoint("http://otel-collector:4317")
                            .setHeaders(Map.of(
                                "X-Service-Name", serviceName,
                                "X-Compliance-Level", "PCI-DSS-v4"
                            ))
                            .build())
                        .setMaxExportBatchSize(512)
                        .setExportTimeout(Duration.ofSeconds(2))
                        .setScheduleDelay(Duration.ofSeconds(5))
                        .build())
                    .setResource(Resource.getDefault()
                        .merge(Resource.builder()
                            .put(ResourceAttributes.SERVICE_NAME, serviceName)
                            .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                            .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, getEnvironment())
                            .put("banking.compliance.framework", "PCI-DSS-v4")
                            .put("banking.data.residency", getDataResidency())
                            .build()))
                    .setSampler(createBankingSampler())
                    .build())
            .buildAndRegisterGlobal();
    }

    private Sampler createBankingSampler() {
        return Sampler.create(
            // 100% sampling for PCI transactions
            TraceIdRatioBasedSampler.create(1.0)
        );
    }
}
```

### Banking-Specific Trace Instrumentation

#### Customer Management Service Tracing
```java
@Service
public class CustomerManagementService {
    
    private final Tracer tracer;
    private final LoggingPort loggingPort;
    
    public CustomerManagementService(OpenTelemetry openTelemetry, LoggingPort loggingPort) {
        this.tracer = openTelemetry.getTracer("customer-management-service");
        this.loggingPort = loggingPort;
    }
    
    @Transactional
    public Customer createCustomer(CreateCustomerCommand command) {
        Span span = tracer.spanBuilder("customer.create")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("banking.operation", "customer_creation")
            .setAttribute("banking.data_classification", "restricted")
            .setAttribute("banking.compliance_required", true)
            .startSpan();
            
        try (Scope scope = span.makeCurrent()) {
            // Add business context to span
            span.setAttribute("customer.type", command.getCustomerType().name());
            span.setAttribute("customer.region", command.getAddress().getCountry());
            span.setAttribute("banking.kyc_required", true);
            
            // Create nested spans for business operations
            Customer customer = performKycValidation(command);
            customer = performCreditAssessment(customer);
            customer = createCustomerRecord(customer);
            
            // Add business outcome to span
            span.setAttribute("customer.id", customer.getId().value());
            span.setAttribute("banking.operation_outcome", "success");
            span.setStatus(StatusCode.OK);
            
            // Structured logging with trace correlation
            loggingPort.logBusinessEvent(
                "CUSTOMER_CREATED",
                span.getSpanContext().getTraceId(),
                Map.of(
                    "customerId", customer.getId().value(),
                    "customerType", command.getCustomerType().name(),
                    "traceId", span.getSpanContext().getTraceId(),
                    "spanId", span.getSpanContext().getSpanId()
                ),
                DataClassification.RESTRICTED
            );
            
            return customer;
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            
            // Error logging with trace context
            loggingPort.logError(
                "Customer creation failed",
                e,
                Map.of(
                    "traceId", span.getSpanContext().getTraceId(),
                    "spanId", span.getSpanContext().getSpanId(),
                    "operation", "customer_creation"
                )
            );
            
            throw e;
        } finally {
            span.end();
        }
    }
    
    private Customer performKycValidation(CreateCustomerCommand command) {
        Span kycSpan = tracer.spanBuilder("customer.kyc_validation")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("banking.process", "kyc")
            .setAttribute("banking.regulatory_requirement", "BSA/AML")
            .startSpan();
            
        try (Scope scope = kycSpan.makeCurrent()) {
            // KYC validation logic
            kycSpan.addEvent("kyc.identity_verification.started");
            
            // External service call tracing
            Span externalSpan = tracer.spanBuilder("external.identity_service")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("http.method", "POST")
                .setAttribute("http.url", "https://identity-service.bank.com/verify")
                .setAttribute("banking.external_service", "identity_verification")
                .startSpan();
                
            try (Scope externalScope = externalSpan.makeCurrent()) {
                // External service call
                boolean verified = identityService.verify(command.getPersonalDetails());
                
                externalSpan.setAttribute("http.status_code", 200);
                externalSpan.setAttribute("banking.verification_result", verified);
                externalSpan.setStatus(StatusCode.OK);
                
                kycSpan.addEvent("kyc.identity_verification.completed", 
                    Attributes.of(AttributeKey.booleanKey("verified"), verified));
                
            } finally {
                externalSpan.end();
            }
            
            kycSpan.setAttribute("kyc.status", "completed");
            kycSpan.setStatus(StatusCode.OK);
            
            return Customer.builder()
                .personalDetails(command.getPersonalDetails())
                .kycStatus(KycStatus.VERIFIED)
                .build();
                
        } finally {
            kycSpan.end();
        }
    }
}
```

#### Loan Processing Service Tracing
```java
@Service
public class LoanProcessingService {
    
    private final Tracer tracer;
    
    @TraceAsync // Custom annotation for async operations
    public CompletableFuture<LoanDecision> processLoanApplication(LoanApplicationCommand command) {
        Span span = tracer.spanBuilder("loan.process_application")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("banking.product_type", "loan")
            .setAttribute("banking.loan_type", command.getLoanType().name())
            .setAttribute("banking.loan_amount", command.getAmount().getValue())
            .setAttribute("banking.currency", command.getAmount().getCurrency())
            .startSpan();
            
        return CompletableFuture.supplyAsync(() -> {
            try (Scope scope = span.makeCurrent()) {
                
                // Business process tracing
                span.addEvent("loan.application.received");
                
                // Credit scoring with nested span
                CreditScore creditScore = performCreditScoring(command.getCustomerId());
                span.setAttribute("banking.credit_score", creditScore.getValue());
                
                // Risk assessment
                RiskAssessment risk = performRiskAssessment(command, creditScore);
                span.setAttribute("banking.risk_level", risk.getLevel().name());
                
                // Decision engine
                LoanDecision decision = makeLoanDecision(command, creditScore, risk);
                span.setAttribute("banking.decision", decision.getDecision().name());
                span.setAttribute("banking.decision_reason", decision.getReason());
                
                // Business events
                span.addEvent("loan.decision.made", Attributes.of(
                    AttributeKey.stringKey("decision"), decision.getDecision().name(),
                    AttributeKey.stringKey("loan_id"), decision.getLoanId().value()
                ));
                
                span.setStatus(StatusCode.OK);
                return decision;
                
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                throw new RuntimeException(e);
            } finally {
                span.end();
            }
        });
    }
}
```

### Payment Processing Tracing

#### PCI-DSS Compliant Payment Tracing
```java
@Service
public class PaymentProcessingService {
    
    private final Tracer tracer;
    
    public PaymentResult processPayment(PaymentCommand command) {
        Span span = tracer.spanBuilder("payment.process")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("banking.operation", "payment_processing")
            .setAttribute("banking.data_classification", "pci_dss")
            .setAttribute("banking.compliance_scope", "pci_dss_v4")
            .setAttribute("payment.method", command.getPaymentMethod().name())
            .setAttribute("payment.currency", command.getAmount().getCurrency())
            // Note: Never log actual payment card data
            .setAttribute("payment.card_type", maskCardType(command.getCardDetails()))
            .startSpan();
            
        try (Scope scope = span.makeCurrent()) {
            
            // PCI-DSS validation
            span.addEvent("payment.pci_validation.started");
            validatePciCompliance(command);
            span.addEvent("payment.pci_validation.completed");
            
            // Tokenization (spans for PCI operations)
            String token = tokenizePaymentData(command.getCardDetails());
            span.setAttribute("payment.tokenized", true);
            
            // Fraud detection
            FraudResult fraudResult = performFraudDetection(command, token);
            span.setAttribute("fraud.score", fraudResult.getScore());
            span.setAttribute("fraud.decision", fraudResult.getDecision().name());
            
            if (fraudResult.getDecision() == FraudDecision.DECLINE) {
                span.addEvent("payment.fraud_detected", Attributes.of(
                    AttributeKey.stringKey("reason"), fraudResult.getReason()
                ));
                span.setStatus(StatusCode.ERROR, "Fraud detected");
                throw new FraudDetectedException(fraudResult.getReason());
            }
            
            // Payment gateway integration
            PaymentResult result = processWithGateway(token, command.getAmount());
            span.setAttribute("payment.gateway_response", result.getGatewayResponse());
            span.setAttribute("payment.transaction_id", result.getTransactionId());
            
            span.addEvent("payment.completed", Attributes.of(
                AttributeKey.stringKey("status"), result.getStatus().name(),
                AttributeKey.stringKey("transaction_id"), result.getTransactionId()
            ));
            
            span.setStatus(StatusCode.OK);
            return result;
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            
            // Security event logging for payment failures
            if (e instanceof FraudDetectedException) {
                span.setAttribute("security.event_type", "fraud_detected");
                span.setAttribute("security.severity", "high");
            }
            
            throw e;
        } finally {
            span.end();
        }
    }
    
    private String tokenizePaymentData(CardDetails cardDetails) {
        Span tokenSpan = tracer.spanBuilder("payment.tokenization")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("banking.operation", "tokenization")
            .setAttribute("banking.pci_scope", true)
            .startSpan();
            
        try (Scope scope = tokenSpan.makeCurrent()) {
            // Never log actual card data in traces
            tokenSpan.setAttribute("card.type", cardDetails.getType().name());
            tokenSpan.setAttribute("card.last_four", cardDetails.getLastFourDigits());
            
            String token = tokenizationService.tokenize(cardDetails);
            tokenSpan.setAttribute("tokenization.success", true);
            tokenSpan.setStatus(StatusCode.OK);
            
            return token;
        } finally {
            tokenSpan.end();
        }
    }
}
```

## Context Propagation

### HTTP Header Propagation
```java
@Component
public class TracingInterceptor implements HandlerInterceptor {
    
    private final TextMapPropagator propagator;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Extract trace context from HTTP headers
        Context extractedContext = propagator.extract(
            Context.current(),
            request,
            HttpServletRequestGetter.INSTANCE
        );
        
        // Make extracted context current
        try (Scope scope = extractedContext.makeCurrent()) {
            return true;
        }
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Inject trace context into response headers for debugging
        Span currentSpan = Span.current();
        if (currentSpan.getSpanContext().isValid()) {
            response.addHeader("X-Trace-Id", currentSpan.getSpanContext().getTraceId());
            response.addHeader("X-Span-Id", currentSpan.getSpanContext().getSpanId());
        }
    }
}
```

### Async Processing Context Propagation
```java
@Service
public class AsyncLoanProcessingService {
    
    @Async("loanProcessingExecutor")
    @TraceAsync
    public CompletableFuture<Void> processLoanAsync(LoanApplication application) {
        // Context is automatically propagated with @TraceAsync
        Span span = tracer.spanBuilder("loan.async_processing")
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();
            
        try (Scope scope = span.makeCurrent()) {
            // Async business logic
            return CompletableFuture.completedFuture(null);
        } finally {
            span.end();
        }
    }
}

@Component
public class TraceAsyncAspect {
    
    @Around("@annotation(TraceAsync)")
    public Object traceAsync(ProceedingJoinPoint joinPoint) throws Throwable {
        Context currentContext = Context.current();
        
        return CompletableFuture.supplyAsync(() -> {
            try (Scope scope = currentContext.makeCurrent()) {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
}
```

## Custom Span Processors

### Banking Compliance Span Processor
```java
@Component
public class BankingComplianceSpanProcessor implements SpanProcessor {
    
    private final ComplianceValidator complianceValidator;
    private final LoggingPort loggingPort;
    
    @Override
    public void onStart(Context parentContext, SpanBuilder spanBuilder) {
        // Add compliance attributes at span creation
        spanBuilder.setAttribute("banking.compliance.framework", "PCI-DSS-v4");
        spanBuilder.setAttribute("banking.audit.required", true);
        spanBuilder.setAttribute("banking.data.residency", getDataResidency());
    }
    
    @Override
    public void onEnd(SpanData spanData) {
        // Validate compliance on span completion
        if (requiresComplianceValidation(spanData)) {
            ComplianceResult result = complianceValidator.validate(spanData);
            
            if (!result.isCompliant()) {
                // Log compliance violation
                loggingPort.logComplianceEvent(
                    "PCI-DSS-v4",
                    "COMPLIANCE_VIOLATION",
                    getUserId(spanData),
                    spanData.getSpanId(),
                    Map.of(
                        "violation_type", result.getViolationType(),
                        "span_name", spanData.getName(),
                        "trace_id", spanData.getTraceId()
                    )
                );
            }
        }
    }
    
    private boolean requiresComplianceValidation(SpanData spanData) {
        return spanData.getAttributes().get(AttributeKey.stringKey("banking.data_classification")) != null &&
               spanData.getAttributes().get(AttributeKey.stringKey("banking.data_classification")).equals("pci_dss");
    }
}
```

### Data Masking Span Processor
```java
@Component
public class DataMaskingSpanProcessor implements SpanProcessor {
    
    private final DataMaskingService dataMaskingService;
    
    @Override
    public void onEnd(SpanData spanData) {
        // Mask sensitive data in span attributes
        AttributesBuilder maskedAttributes = spanData.getAttributes().toBuilder();
        
        spanData.getAttributes().forEach((key, value) -> {
            if (isSensitiveAttribute(key)) {
                String maskedValue = dataMaskingService.maskSensitiveData(value.toString());
                maskedAttributes.put(key, maskedValue);
            }
        });
        
        // Create new span with masked data
        SpanData maskedSpan = spanData.toBuilder()
            .setAttributes(maskedAttributes.build())
            .build();
            
        // Export masked span
        exportMaskedSpan(maskedSpan);
    }
    
    private boolean isSensitiveAttribute(AttributeKey<?> key) {
        String keyName = key.getKey().toLowerCase();
        return keyName.contains("card") ||
               keyName.contains("ssn") ||
               keyName.contains("account") ||
               keyName.contains("personal");
    }
}
```

## Trace Sampling Strategies

### Banking-Specific Sampling
```java
@Component
public class BankingSampler implements Sampler {
    
    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        
        // 100% sampling for PCI-DSS operations
        if (isPciOperation(attributes)) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
        
        // 100% sampling for financial transactions
        if (isFinancialTransaction(attributes)) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
        
        // 50% sampling for customer operations
        if (isCustomerOperation(attributes)) {
            return TraceIdRatioBasedSampler.create(0.5).shouldSample(
                parentContext, traceId, name, spanKind, attributes, parentLinks);
        }
        
        // 10% sampling for general operations
        return TraceIdRatioBasedSampler.create(0.1).shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    
    private boolean isPciOperation(Attributes attributes) {
        String dataClassification = attributes.get(AttributeKey.stringKey("banking.data_classification"));
        return "pci_dss".equals(dataClassification);
    }
}
```

## Trace Analysis & Monitoring

### Business Transaction Tracing
```java
@Component
public class BusinessTransactionTracer {
    
    public void traceLoanApplication(String customerId, String loanApplicationId) {
        Span businessSpan = tracer.spanBuilder("business.loan_application_journey")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("business.process", "loan_application")
            .setAttribute("business.customer_id", customerId)
            .setAttribute("business.application_id", loanApplicationId)
            .setAttribute("business.sla_target", "4_hours")
            .startSpan();
            
        // This span will encompass the entire business process
        try (Scope scope = businessSpan.makeCurrent()) {
            // Business logic will create child spans
        } finally {
            businessSpan.end();
        }
    }
}
```

### Performance Analysis Queries
```yaml
# Jaeger Query Examples
# Find slow loan processing requests
operation_name="loan.process_application" AND duration>5s

# Find failed payment transactions
operation_name="payment.process" AND error=true

# Find high-volume customers
tags.customer_id="12345" AND start_time>-24h

# Find PCI compliance violations
tags.banking.compliance_violation=true
```

## Integration with APM Tools

### Jaeger Integration
```yaml
# jaeger-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger-all-in-one
spec:
  template:
    spec:
      containers:
      - name: jaeger
        image: jaegertracing/all-in-one:1.52
        env:
        - name: COLLECTOR_OTLP_ENABLED
          value: "true"
        - name: SPAN_STORAGE_TYPE
          value: "elasticsearch"
        - name: ES_SERVER_URLS
          value: "https://elasticsearch:9200"
        - name: ES_USERNAME
          value: "elastic"
        - name: ES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: elasticsearch-credentials
              key: password
```

### Grafana Trace Visualization
```yaml
# grafana-datasource.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-datasources
data:
  datasources.yaml: |
    datasources:
    - name: Jaeger
      type: jaeger
      url: http://jaeger-query:16686
      access: proxy
      isDefault: false
      
    - name: Tempo
      type: tempo
      url: http://tempo:3200
      access: proxy
      isDefault: false
```

## Best Practices

### Trace Design Principles
1. **Meaningful Span Names**: Use business-oriented span names
2. **Appropriate Granularity**: Balance detail with performance
3. **Business Context**: Include business identifiers in spans
4. **Error Handling**: Properly record exceptions and errors
5. **Compliance Awareness**: Consider regulatory requirements

### Performance Considerations
1. **Sampling Strategy**: Implement business-aware sampling
2. **Batch Processing**: Use efficient span export batching
3. **Resource Limits**: Set appropriate memory and CPU limits
4. **Network Optimization**: Minimize network overhead

### Security Guidelines
1. **No Sensitive Data**: Never include PCI data in traces
2. **Data Masking**: Implement automatic data masking
3. **Access Controls**: Secure trace data access
4. **Audit Trails**: Log trace data access for compliance

---

This distributed tracing implementation provides comprehensive visibility into banking operations while maintaining strict security and compliance standards.