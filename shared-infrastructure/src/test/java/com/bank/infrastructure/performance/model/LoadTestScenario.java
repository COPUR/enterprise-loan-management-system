package com.bank.infrastructure.performance.model;

import java.time.Duration;

/**
 * Load Test Scenario Configuration
 * 
 * Defines the parameters for a load test scenario including
 * endpoints, user simulation, and test duration.
 */
public class LoadTestScenario {
    
    private String name;
    private String endpoint;
    private String httpMethod;
    private String requestBody;
    private int maxConcurrentUsers;
    private Duration testDuration;
    private Duration rampUpTime;
    private Duration thinkTime;
    private boolean followRedirects;
    private int timeout;

    private LoadTestScenario(Builder builder) {
        this.name = builder.name;
        this.endpoint = builder.endpoint;
        this.httpMethod = builder.httpMethod;
        this.requestBody = builder.requestBody;
        this.maxConcurrentUsers = builder.maxConcurrentUsers;
        this.testDuration = builder.testDuration;
        this.rampUpTime = builder.rampUpTime;
        this.thinkTime = builder.thinkTime;
        this.followRedirects = builder.followRedirects;
        this.timeout = builder.timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getName() { return name; }
    public String getEndpoint() { return endpoint; }
    public String getHttpMethod() { return httpMethod; }
    public String getRequestBody() { return requestBody; }
    public int getMaxConcurrentUsers() { return maxConcurrentUsers; }
    public Duration getTestDuration() { return testDuration; }
    public Duration getRampUpTime() { return rampUpTime; }
    public Duration getThinkTime() { return thinkTime; }
    public boolean isFollowRedirects() { return followRedirects; }
    public int getTimeout() { return timeout; }

    public static class Builder {
        private String name;
        private String endpoint;
        private String httpMethod = "GET";
        private String requestBody;
        private int maxConcurrentUsers = 10;
        private Duration testDuration = Duration.ofMinutes(1);
        private Duration rampUpTime = Duration.ofSeconds(10);
        private Duration thinkTime = Duration.ofSeconds(1);
        private boolean followRedirects = true;
        private int timeout = 30;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder requestBody(String requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder maxConcurrentUsers(int maxConcurrentUsers) {
            this.maxConcurrentUsers = maxConcurrentUsers;
            return this;
        }

        public Builder testDuration(Duration testDuration) {
            this.testDuration = testDuration;
            return this;
        }

        public Builder rampUpTime(Duration rampUpTime) {
            this.rampUpTime = rampUpTime;
            return this;
        }

        public Builder thinkTime(Duration thinkTime) {
            this.thinkTime = thinkTime;
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public LoadTestScenario build() {
            if (name == null || endpoint == null) {
                throw new IllegalArgumentException("Name and endpoint are required");
            }
            return new LoadTestScenario(this);
        }
    }

    @Override
    public String toString() {
        return String.format("LoadTestScenario{name='%s', endpoint='%s', method='%s', users=%d, duration=%s}",
            name, endpoint, httpMethod, maxConcurrentUsers, testDuration);
    }
}