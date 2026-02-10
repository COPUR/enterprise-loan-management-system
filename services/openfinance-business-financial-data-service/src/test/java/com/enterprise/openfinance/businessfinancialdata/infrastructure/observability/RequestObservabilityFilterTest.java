package com.enterprise.openfinance.businessfinancialdata.infrastructure.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RequestObservabilityFilterTest {

    @Test
    void shouldPopulateTraceHeaderAndRecordMetrics() throws ServletException, IOException {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestObservabilityFilter filter = new RequestObservabilityFilter(
                meterRegistry,
                Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC),
                new ObjectMapper(),
                "openfinance-business-financial-data-service"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open-finance/v1/corporate/accounts");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/open-finance/v1/corporate/accounts");
        request.addHeader("X-FAPI-Interaction-ID", "ix-businessfinancialdata-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(200));

        assertThat(response.getHeader("X-Trace-ID")).isEqualTo("ix-businessfinancialdata-1");
        assertThat(meterRegistry.find("openfinance_http_requests_total")
                .tags("service", "openfinance-business-financial-data-service", "method", "GET", "route",
                        "/open-finance/v1/corporate/accounts", "status", "200")
                .counter())
                .isNotNull();
        assertThat(meterRegistry.find("openfinance_http_request_duration").timer()).isNotNull();
    }

    @Test
    void shouldPreferTraceparentTraceIdWhenProvided() throws ServletException, IOException {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestObservabilityFilter filter = new RequestObservabilityFilter(
                meterRegistry,
                Clock.systemUTC(),
                new ObjectMapper(),
                "openfinance-business-financial-data-service"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open-finance/v1/corporate/transactions");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        request.addHeader("X-FAPI-Interaction-ID", "ix-businessfinancialdata-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(200));

        assertThat(response.getHeader("X-Trace-ID")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }
}
