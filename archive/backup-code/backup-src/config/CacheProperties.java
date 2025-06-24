package com.bank.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    private Ttl ttl = new Ttl();

    public Ttl getTtl() {
        return ttl;
    }

    public void setTtl(Ttl ttl) {
        this.ttl = ttl;
    }

    public static class Ttl {
        private Integer customersMinutes;
        private Integer loansMinutes;
        private Integer paymentsMinutes;
        private Integer creditAssessmentMinutes;
        private Integer complianceMinutes;
        private Integer securityMinutes;
        private Integer rateLimitMinutes;

        public Integer getCustomersMinutes() {
            return customersMinutes;
        }

        public void setCustomersMinutes(Integer customersMinutes) {
            this.customersMinutes = customersMinutes;
        }

        public Integer getLoansMinutes() {
            return loansMinutes;
        }

        public void setLoansMinutes(Integer loansMinutes) {
            this.loansMinutes = loansMinutes;
        }

        public Integer getPaymentsMinutes() {
            return paymentsMinutes;
        }

        public void setPaymentsMinutes(Integer paymentsMinutes) {
            this.paymentsMinutes = paymentsMinutes;
        }

        public Integer getCreditAssessmentMinutes() {
            return creditAssessmentMinutes;
        }

        public void setCreditAssessmentMinutes(Integer creditAssessmentMinutes) {
            this.creditAssessmentMinutes = creditAssessmentMinutes;
        }

        public Integer getComplianceMinutes() {
            return complianceMinutes;
        }

        public void setComplianceMinutes(Integer complianceMinutes) {
            this.complianceMinutes = complianceMinutes;
        }

        public Integer getSecurityMinutes() {
            return securityMinutes;
        }

        public void setSecurityMinutes(Integer securityMinutes) {
            this.securityMinutes = securityMinutes;
        }

        public Integer getRateLimitMinutes() {
            return rateLimitMinutes;
        }

        public void setRateLimitMinutes(Integer rateLimitMinutes) {
            this.rateLimitMinutes = rateLimitMinutes;
        }
    }
}