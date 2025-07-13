package com.bank.shared.kernel.web;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for enabling OpenTelemetry tracing headers in API endpoints
 * 
 * Automatically adds distributed tracing capabilities to banking API endpoints:
 * - Extracts tracing context from incoming requests
 * - Creates spans for financial operations
 * - Propagates trace context to downstream services
 * - Adds banking-specific trace attributes
 * - Supports FAPI compliance tracing requirements
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TracingHeaders {
    
    /**
     * Custom operation name for the span (defaults to method name)
     */
    String operationName() default "";
    
    /**
     * Banking service category for span classification
     */
    BankingServiceType serviceType() default BankingServiceType.GENERAL;
    
    /**
     * Whether to capture request/response payloads (use carefully for PII)
     */
    boolean capturePayload() default false;
    
    /**
     * Additional custom attributes to add to spans
     */
    String[] customAttributes() default {};
    
    /**
     * Banking service types for span classification
     */
    enum BankingServiceType {
        CUSTOMER_MANAGEMENT("customer"),
        LOAN_PROCESSING("loan"),
        PAYMENT_PROCESSING("payment"),
        COMPLIANCE("compliance"),
        AUTHENTICATION("auth"),
        GENERAL("general");
        
        private final String spanPrefix;
        
        BankingServiceType(String spanPrefix) {
            this.spanPrefix = spanPrefix;
        }
        
        public String getSpanPrefix() {
            return spanPrefix;
        }
    }
}

/**
 * Utility class for OpenTelemetry tracing operations
 */
class TracingUtils {
    
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("banking-platform");
    
    // Standard banking trace attributes
    public static final String BANKING_OPERATION = "banking.operation";
    public static final String BANKING_CUSTOMER_ID = "banking.customer_id";
    public static final String BANKING_LOAN_ID = "banking.loan_id";
    public static final String BANKING_PAYMENT_ID = "banking.payment_id";
    public static final String BANKING_ACCOUNT_ID = "banking.account_id";
    public static final String BANKING_TRANSACTION_AMOUNT = "banking.transaction_amount";
    public static final String BANKING_CURRENCY = "banking.currency";
    public static final String BANKING_RISK_SCORE = "banking.risk_score";
    public static final String BANKING_COMPLIANCE_STATUS = "banking.compliance_status";
    public static final String BANKING_FRAUD_SCORE = "banking.fraud_score";
    
    // FAPI specific attributes
    public static final String FAPI_INTERACTION_ID = "fapi.interaction_id";
    public static final String FAPI_FINANCIAL_ID = "fapi.financial_id";
    public static final String FAPI_CLIENT_ID = "fapi.client_id";
    
    // Security attributes
    public static final String SECURITY_USER_ID = "security.user_id";
    public static final String SECURITY_CLIENT_IP = "security.client_ip";
    public static final String SECURITY_USER_AGENT = "security.user_agent";
    public static final String SECURITY_AUTH_METHOD = "security.auth_method";
    
    /**
     * Create a new span for banking operation
     */
    public static Span createBankingSpan(String operationName, TracingHeaders.BankingServiceType serviceType) {
        String spanName = serviceType.getSpanPrefix() + "." + operationName;
        return tracer.spanBuilder(spanName)
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER)
            .setAttribute("service.name", "banking-platform")
            .setAttribute("service.version", "1.0.0")
            .setAttribute(BANKING_OPERATION, operationName)
            .startSpan();
    }
    
    /**
     * Extract tracing context from HTTP request
     */
    public static Context extractContextFromRequest(HttpServletRequest request) {
        return GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .extract(Context.current(), request, new HttpServletRequestGetter());
    }
    
    /**
     * Inject tracing context into HTTP response
     */
    public static void injectContextIntoResponse(HttpServletResponse response, Context context) {
        GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .inject(context, response, new HttpServletResponseSetter());
    }
    
    /**
     * Add banking-specific attributes to current span
     */
    public static void addBankingAttributes(HttpServletRequest request) {
        Span currentSpan = Span.current();
        
        // Add FAPI headers
        String fapiInteractionId = request.getHeader("X-FAPI-Interaction-Id");
        if (fapiInteractionId != null) {
            currentSpan.setAttribute(FAPI_INTERACTION_ID, fapiInteractionId);
        }
        
        String fapiFinancialId = request.getHeader("X-FAPI-Financial-Id");
        if (fapiFinancialId != null) {
            currentSpan.setAttribute(FAPI_FINANCIAL_ID, fapiFinancialId);
        }
        
        String clientId = request.getHeader("X-Client-ID");
        if (clientId != null) {
            currentSpan.setAttribute(FAPI_CLIENT_ID, clientId);
        }
        
        // Add security context
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            currentSpan.setAttribute(SECURITY_USER_AGENT, userAgent);
        }
        
        String clientIP = getClientIP(request);
        if (clientIP != null) {
            currentSpan.setAttribute(SECURITY_CLIENT_IP, clientIP);
        }
        
        // Add request details
        currentSpan.setAttribute("http.method", request.getMethod());
        currentSpan.setAttribute("http.url", request.getRequestURL().toString());
        currentSpan.setAttribute("http.scheme", request.getScheme());
        currentSpan.setAttribute("http.host", request.getServerName());
        currentSpan.setAttribute("http.target", request.getRequestURI());
        
        // Add user context if available
        String userId = getCurrentUserId();
        if (userId != null) {
            currentSpan.setAttribute(SECURITY_USER_ID, userId);
        }
    }
    
    /**
     * Add custom banking attributes to span
     */
    public static void addCustomBankingAttribute(String key, String value) {
        if (key != null && value != null) {
            Span.current().setAttribute(key, value);
        }
    }
    
    /**
     * Add custom banking attributes to span (numeric)
     */
    public static void addCustomBankingAttribute(String key, long value) {
        if (key != null) {
            Span.current().setAttribute(key, value);
        }
    }
    
    /**
     * Add custom banking attributes to span (double)
     */
    public static void addCustomBankingAttribute(String key, double value) {
        if (key != null) {
            Span.current().setAttribute(key, value);
        }
    }
    
    /**
     * Record banking operation success
     */
    public static void recordSuccess(String operation) {
        Span currentSpan = Span.current();
        currentSpan.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
        currentSpan.setAttribute("banking.operation.status", "success");
        currentSpan.setAttribute("banking.operation.name", operation);
    }
    
    /**
     * Record banking operation error
     */
    public static void recordError(String operation, Throwable error) {
        Span currentSpan = Span.current();
        currentSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, error.getMessage());
        currentSpan.setAttribute("banking.operation.status", "error");
        currentSpan.setAttribute("banking.operation.name", operation);
        currentSpan.setAttribute("error.type", error.getClass().getSimpleName());
        currentSpan.setAttribute("error.message", error.getMessage());
        currentSpan.recordException(error);
    }
    
    /**
     * Record compliance check result
     */
    public static void recordComplianceCheck(String checkType, boolean passed, String details) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("banking.compliance.check_type", checkType);
        currentSpan.setAttribute("banking.compliance.passed", passed);
        if (details != null) {
            currentSpan.setAttribute("banking.compliance.details", details);
        }
    }
    
    /**
     * Record fraud detection result
     */
    public static void recordFraudCheck(double fraudScore, String riskLevel) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute(BANKING_FRAUD_SCORE, fraudScore);
        currentSpan.setAttribute("banking.fraud.risk_level", riskLevel);
    }
    
    /**
     * Get client IP from request
     */
    private static String getClientIP(HttpServletRequest request) {
        String clientIP = request.getHeader("X-Forwarded-For");
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getHeader("X-Real-IP");
        }
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getRemoteAddr();
        }
        
        // Handle comma-separated IPs
        if (clientIP != null && clientIP.contains(",")) {
            clientIP = clientIP.split(",")[0].trim();
        }
        
        return clientIP;
    }
    
    /**
     * Get current user ID from security context
     */
    private static String getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            return auth != null ? auth.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * TextMapGetter implementation for HttpServletRequest
     */
    private static class HttpServletRequestGetter implements TextMapGetter<HttpServletRequest> {
        @Override
        public Iterable<String> keys(HttpServletRequest request) {
            return java.util.Collections.list(request.getHeaderNames());
        }
        
        @Override
        public String get(HttpServletRequest request, String key) {
            return request.getHeader(key);
        }
    }
    
    /**
     * TextMapSetter implementation for HttpServletResponse
     */
    private static class HttpServletResponseSetter implements TextMapSetter<HttpServletResponse> {
        @Override
        public void set(HttpServletResponse response, String key, String value) {
            response.setHeader(key, value);
        }
    }
}

/**
 * Banking operation trace context holder
 */
class BankingTraceContext {
    private String customerId;
    private String loanId;
    private String paymentId;
    private String accountId;
    private java.math.BigDecimal transactionAmount;
    private String currency;
    private Double riskScore;
    private String operationType;
    
    // Getters and setters for trace context
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
        TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_CUSTOMER_ID, customerId);
    }
    
    public void setLoanId(String loanId) {
        this.loanId = loanId;
        TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_LOAN_ID, loanId);
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
        TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_PAYMENT_ID, paymentId);
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
        TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_ACCOUNT_ID, accountId);
    }
    
    public void setTransactionAmount(java.math.BigDecimal amount) {
        this.transactionAmount = amount;
        if (amount != null) {
            TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_TRANSACTION_AMOUNT, amount.doubleValue());
        }
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
        TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_CURRENCY, currency);
    }
    
    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
        if (riskScore != null) {
            TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_RISK_SCORE, riskScore);
        }
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
        TracingUtils.addCustomBankingAttribute(TracingUtils.BANKING_OPERATION, operationType);
    }
    
    // Static factory methods for common banking operations
    public static BankingTraceContext forCustomer(String customerId) {
        BankingTraceContext context = new BankingTraceContext();
        context.setCustomerId(customerId);
        return context;
    }
    
    public static BankingTraceContext forLoan(String loanId, String customerId) {
        BankingTraceContext context = new BankingTraceContext();
        context.setLoanId(loanId);
        context.setCustomerId(customerId);
        return context;
    }
    
    public static BankingTraceContext forPayment(String paymentId, String customerId, 
                                               java.math.BigDecimal amount, String currency) {
        BankingTraceContext context = new BankingTraceContext();
        context.setPaymentId(paymentId);
        context.setCustomerId(customerId);
        context.setTransactionAmount(amount);
        context.setCurrency(currency);
        return context;
    }
}