package com.bank.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "integrations")
public class IntegrationProperties {

    private OpenAi openai = new OpenAi();
    private Microservices microservices = new Microservices();

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public Microservices getMicroservices() {
        return microservices;
    }

    public void setMicroservices(Microservices microservices) {
        this.microservices = microservices;
    }

    public static class OpenAi {
        private String apiKey;
        private Integer timeoutSeconds;
        private String model;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Microservices {
        private ServiceConfig customerService = new ServiceConfig();
        private ServiceConfig loanService = new ServiceConfig();
        private ServiceConfig paymentService = new ServiceConfig();
        private ServiceConfig discoveryService = new ServiceConfig();

        public ServiceConfig getCustomerService() {
            return customerService;
        }

        public void setCustomerService(ServiceConfig customerService) {
            this.customerService = customerService;
        }

        public ServiceConfig getLoanService() {
            return loanService;
        }

        public void setLoanService(ServiceConfig loanService) {
            this.loanService = loanService;
        }

        public ServiceConfig getPaymentService() {
            return paymentService;
        }

        public void setPaymentService(ServiceConfig paymentService) {
            this.paymentService = paymentService;
        }

        public ServiceConfig getDiscoveryService() {
            return discoveryService;
        }

        public void setDiscoveryService(ServiceConfig discoveryService) {
            this.discoveryService = discoveryService;
        }

        public static class ServiceConfig {
            private String url;
            private Integer timeout;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public Integer getTimeout() {
                return timeout;
            }

            public void setTimeout(Integer timeout) {
                this.timeout = timeout;
            }
        }
    }
}