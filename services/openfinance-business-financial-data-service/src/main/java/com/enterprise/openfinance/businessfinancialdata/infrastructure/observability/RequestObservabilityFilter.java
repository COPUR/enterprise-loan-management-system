package com.enterprise.openfinance.businessfinancialdata.infrastructure.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RequestObservabilityFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestObservabilityFilter.class);
    private static final String TRACE_HEADER = "X-Trace-ID";
    private static final String INTERACTION_HEADER = "X-FAPI-Interaction-ID";
    private static final String TRACECONTEXT_HEADER = "traceparent";

    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    public RequestObservabilityFilter(MeterRegistry meterRegistry,
                                      Clock corporateTreasuryClock,
                                      ObjectMapper objectMapper,
                                      @Value("${spring.application.name:openfinance-business-financial-data-service}") String serviceName) {
        this.meterRegistry = meterRegistry;
        this.clock = corporateTreasuryClock;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now(clock);
        long startNanos = System.nanoTime();
        String interactionId = normalizeHeader(request.getHeader(INTERACTION_HEADER));
        String traceId = resolveTraceId(request, interactionId);

        MDC.put("trace_id", traceId);
        MDC.put("interaction_id", interactionId == null ? "-" : interactionId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            Duration duration = Duration.ofNanos(System.nanoTime() - startNanos);
            String route = resolveRoute(request);
            String status = String.valueOf(response.getStatus());

            Tags tags = Tags.of(
                    "service", serviceName,
                    "method", request.getMethod(),
                    "route", route,
                    "status", status
            );
            meterRegistry.counter("openfinance_http_requests_total", tags).increment();
            meterRegistry.timer("openfinance_http_request_duration", tags).record(duration);

            logStructuredRequest(request, status, traceId, interactionId, route, start, duration);
            MDC.remove("trace_id");
            MDC.remove("interaction_id");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    private void logStructuredRequest(HttpServletRequest request,
                                      String status,
                                      String traceId,
                                      String interactionId,
                                      String route,
                                      Instant start,
                                      Duration duration) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "http_request_completed");
        payload.put("service", serviceName);
        payload.put("traceId", traceId);
        payload.put("interactionId", interactionId);
        payload.put("method", request.getMethod());
        payload.put("route", route);
        payload.put("status", status);
        payload.put("durationMs", duration.toMillis());
        payload.put("startTime", start);

        try {
            LOGGER.info("{}", objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            LOGGER.info("event=http_request_completed service={} traceId={} interactionId={} method={} route={} status={} durationMs={}",
                    serviceName, traceId, interactionId, request.getMethod(), route, status, duration.toMillis());
        }
    }

    private static String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }

    private static String resolveTraceId(HttpServletRequest request, String interactionId) {
        String traceparent = normalizeHeader(request.getHeader(TRACECONTEXT_HEADER));
        if (traceparent != null) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 4 && parts[1].length() == 32) {
                return parts[1];
            }
        }
        if (interactionId != null) {
            return interactionId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String normalizeHeader(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
