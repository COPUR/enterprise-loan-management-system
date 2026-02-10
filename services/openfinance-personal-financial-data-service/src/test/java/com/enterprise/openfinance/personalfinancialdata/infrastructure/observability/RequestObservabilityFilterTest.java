package com.enterprise.openfinance.personalfinancialdata.infrastructure.observability;

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
    void shouldAddTraceHeaderAndPublishMetrics() throws ServletException, IOException {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestObservabilityFilter filter = new RequestObservabilityFilter(
                meterRegistry,
                Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC),
                new ObjectMapper(),
                "openfinance-personal-financial-data-service"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open-finance/v1/accounts");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/open-finance/v1/accounts");
        request.addHeader("X-FAPI-Interaction-ID", "ix-personalfinancialdata-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(200));

        assertThat(response.getHeader("X-Trace-ID")).isEqualTo("ix-personalfinancialdata-1");
        assertThat(meterRegistry.find("openfinance_http_requests_total")
                .tags("service", "openfinance-personal-financial-data-service", "method", "GET", "route",
                        "/open-finance/v1/accounts", "status", "200")
                .counter()).isNotNull();
    }

    @Test
    void shouldUseTraceparentWhenAvailable() throws ServletException, IOException {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestObservabilityFilter filter = new RequestObservabilityFilter(
                meterRegistry,
                Clock.systemUTC(),
                new ObjectMapper(),
                "openfinance-personal-financial-data-service"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open-finance/v1/accounts");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        request.addHeader("X-FAPI-Interaction-ID", "ix-personalfinancialdata-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(200));

        assertThat(response.getHeader("X-Trace-ID")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }
}
