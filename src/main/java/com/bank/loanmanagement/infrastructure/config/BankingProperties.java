package com.bank.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "banking")
public class BankingProperties {

    private Business business = new Business();

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public static class Business {
        private Loan loan = new Loan();
        private Payment payment = new Payment();
        private CreditScoring creditScoring = new CreditScoring();

        public Loan getLoan() {
            return loan;
        }

        public void setLoan(Loan loan) {
            this.loan = loan;
        }

        public Payment getPayment() {
            return payment;
        }

        public void setPayment(Payment payment) {
            this.payment = payment;
        }

        public CreditScoring getCreditScoring() {
            return creditScoring;
        }

        public void setCreditScoring(CreditScoring creditScoring) {
            this.creditScoring = creditScoring;
        }

        public static class Loan {
            private InterestRate interestRate = new InterestRate();
            private List<Integer> installments;
            private Amount amount = new Amount();

            public InterestRate getInterestRate() {
                return interestRate;
            }

            public void setInterestRate(InterestRate interestRate) {
                this.interestRate = interestRate;
            }

            public List<Integer> getInstallments() {
                return installments;
            }

            public void setInstallments(List<Integer> installments) {
                this.installments = installments;
            }

            public Amount getAmount() {
                return amount;
            }

            public void setAmount(Amount amount) {
                this.amount = amount;
            }

            public static class InterestRate {
                private BigDecimal min;
                private BigDecimal max;

                public BigDecimal getMin() {
                    return min;
                }

                public void setMin(BigDecimal min) {
                    this.min = min;
                }

                public BigDecimal getMax() {
                    return max;
                }

                public void setMax(BigDecimal max) {
                    this.max = max;
                }
            }

            public static class Amount {
                private BigDecimal min;
                private BigDecimal max;

                public BigDecimal getMin() {
                    return min;
                }

                public void setMin(BigDecimal min) {
                    this.min = min;
                }

                public BigDecimal getMax() {
                    return max;
                }

                public void setMax(BigDecimal max) {
                    this.max = max;
                }
            }
        }

        public static class Payment {
            private Integer advanceLimitMonths;
            private BigDecimal discountRate;
            private BigDecimal penaltyRate;

            public Integer getAdvanceLimitMonths() {
                return advanceLimitMonths;
            }

            public void setAdvanceLimitMonths(Integer advanceLimitMonths) {
                this.advanceLimitMonths = advanceLimitMonths;
            }

            public BigDecimal getDiscountRate() {
                return discountRate;
            }

            public void setDiscountRate(BigDecimal discountRate) {
                this.discountRate = discountRate;
            }

            public BigDecimal getPenaltyRate() {
                return penaltyRate;
            }

            public void setPenaltyRate(BigDecimal penaltyRate) {
                this.penaltyRate = penaltyRate;
            }
        }

        public static class CreditScoring {
            private Integer minScore;
            private Integer maxScore;
            private Integer highRiskThreshold;
            private Integer excellentThreshold;

            public Integer getMinScore() {
                return minScore;
            }

            public void setMinScore(Integer minScore) {
                this.minScore = minScore;
            }

            public Integer getMaxScore() {
                return maxScore;
            }

            public void setMaxScore(Integer maxScore) {
                this.maxScore = maxScore;
            }

            public Integer getHighRiskThreshold() {
                return highRiskThreshold;
            }

            public void setHighRiskThreshold(Integer highRiskThreshold) {
                this.highRiskThreshold = highRiskThreshold;
            }

            public Integer getExcellentThreshold() {
                return excellentThreshold;
            }

            public void setExcellentThreshold(Integer excellentThreshold) {
                this.excellentThreshold = excellentThreshold;
            }
        }
    }
}