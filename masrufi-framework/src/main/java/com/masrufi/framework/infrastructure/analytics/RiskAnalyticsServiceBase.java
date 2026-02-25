package com.masrufi.framework.infrastructure.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Local base class replacing the external
 * {@code com.bank.infrastructure.analytics.RiskAnalyticsService}.
 * Provides the minimal contract used by {@link IslamicRiskAnalyticsService}.
 *
 * <p>
 * The analytics files in this package are excluded from Gradle compilation via
 * {@code sourceSets.main.java.exclude} in {@code build.gradle}. This stub
 * exists solely
 * to keep the IDE error-free during the in-progress modularisation.
 * </p>
 */
public abstract class RiskAnalyticsServiceBase {

    protected final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected RiskAnalyticsServiceBase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Return the base dashboard overview node. Subclasses can call
     * {@code super.getDashboardOverview()} and augment the result.
     */
    public ObjectNode getDashboardOverview() {
        return objectMapper.createObjectNode();
    }

    /**
     * Return real-time alert information.
     */
    public ObjectNode getRealTimeAlerts() {
        return objectMapper.createObjectNode();
    }

    /**
     * Return portfolio performance metrics.
     */
    public ObjectNode getPortfolioPerformance() {
        return objectMapper.createObjectNode();
    }
}
