package com.bank.infrastructure.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Request Tracing Interceptor
 * 
 * Provides comprehensive request tracing for API calls including:
 * - Request ID generation and propagation
 * - Correlation ID handling
 * - MDC (Mapped Diagnostic Context) setup
 * - Performance timing
 * - Request/response logging
 */
@Component
public class RequestTracingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestTracingInterceptor.class);
    
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String SPAN_ID_HEADER = "X-Span-ID";
    
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    public static final String START_TIME_ATTRIBUTE = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        
        // Generate or extract request ID
        String requestId = getOrGenerateRequestId(request);
        String correlationId = getOrGenerateCorrelationId(request);
        
        // Set request attributes
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        
        // Set response headers
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        // Setup MDC for logging
        setupMDC(request, requestId, correlationId);
        
        // Log request start
        if (logger.isDebugEnabled()) {
            logger.debug("API Request started: {} {} from {}",
                request.getMethod(),
                request.getRequestURI(),
                getClientIpAddress(request));
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                
                // Add timing header
                response.setHeader("X-Response-Time", String.valueOf(duration));
                
                // Log request completion
                if (logger.isDebugEnabled()) {
                    logger.debug("API Request completed: {} {} - Status: {} - Duration: {}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        duration);
                }
                
                // Log performance warning for slow requests
                if (duration > 5000) { // 5 seconds
                    logger.warn("Slow API request detected: {} {} - Duration: {}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        duration);
                }
            }
            
            // Log any exceptions
            if (ex != null) {
                logger.error("API Request failed: {} {} - Error: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage(),
                    ex);
            }
        } finally {
            // Clean up MDC
            clearMDC();
        }
    }
    
    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = generateRequestId();
        }
        return requestId;
    }
    
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }
        return correlationId;
    }
    
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateCorrelationId() {
        return "corr_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private void setupMDC(HttpServletRequest request, String requestId, String correlationId) {
        MDC.put("requestId", requestId);
        MDC.put("correlationId", correlationId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        MDC.put("userAgent", request.getHeader("User-Agent"));
        MDC.put("clientIp", getClientIpAddress(request));
        
        // Add trace information if available
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        
        String spanId = request.getHeader(SPAN_ID_HEADER);
        if (spanId != null) {
            MDC.put("spanId", spanId);
        }
    }
    
    private void clearMDC() {
        MDC.remove("requestId");
        MDC.remove("correlationId");
        MDC.remove("method");
        MDC.remove("uri");
        MDC.remove("userAgent");
        MDC.remove("clientIp");
        MDC.remove("traceId");
        MDC.remove("spanId");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Originating-IP",
            "CF-Connecting-IP",
            "True-Client-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (X-Forwarded-For can contain multiple IPs)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}