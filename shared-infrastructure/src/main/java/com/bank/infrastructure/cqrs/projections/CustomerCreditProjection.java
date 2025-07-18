package com.bank.infrastructure.cqrs.projections;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.customer.domain.*;
import com.bank.infrastructure.eventsourcing.EventStreamPublisher;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Customer Credit Projection for CQRS Read Models
 * 
 * Optimized read model for credit-related queries:
 * - Credit score and limit tracking
 * - Available credit calculations
 * - Credit utilization analytics
 * - Risk assessment data
 * - Performance-optimized for lending decisions
 */
@Component
@Transactional
public class CustomerCreditProjection {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public CustomerCreditProjection(EventStreamPublisher eventStreamPublisher) {
        // Subscribe to customer-related events
        eventStreamPublisher.subscribe("CustomerCreatedEvent", this::handleCustomerCreated);
        eventStreamPublisher.subscribe("CustomerCreditLimitUpdatedEvent", this::handleCreditLimitUpdated);
        eventStreamPublisher.subscribe("CustomerCreditReservedEvent", this::handleCreditReserved);
        eventStreamPublisher.subscribe("CustomerCreditReleasedEvent", this::handleCreditReleased);
        eventStreamPublisher.subscribe("CustomerCreditScoreUpdatedEvent", this::handleCreditScoreUpdated);
    }
    
    /**
     * Find customer credit view by ID
     */
    public Optional<CustomerCreditView> findByCustomerId(CustomerId customerId) {
        try {
            CustomerCreditView view = entityManager.find(CustomerCreditView.class, customerId.getId());
            return Optional.ofNullable(view);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Find customers by credit score range
     */
    public java.util.List<CustomerCreditView> findByScoreRange(int minScore, int maxScore) {
        return entityManager.createQuery("""
            SELECT c FROM CustomerCreditView c 
            WHERE c.creditScore BETWEEN :minScore AND :maxScore 
            ORDER BY c.creditScore DESC
            """, CustomerCreditView.class)
            .setParameter("minScore", minScore)
            .setParameter("maxScore", maxScore)
            .getResultList();
    }
    
    /**
     * Find customers with high credit utilization
     */
    public java.util.List<CustomerCreditView> findHighUtilizationCustomers(double utilizationThreshold) {
        return entityManager.createQuery("""
            SELECT c FROM CustomerCreditView c 
            WHERE c.creditUtilization > :threshold 
            ORDER BY c.creditUtilization DESC
            """, CustomerCreditView.class)
            .setParameter("threshold", utilizationThreshold)
            .getResultList();
    }
    
    /**
     * Get credit statistics
     */
    public CreditStatistics getCreditStatistics() {
        Object[] result = (Object[]) entityManager.createQuery("""
            SELECT 
                COUNT(c),
                AVG(c.creditScore),
                AVG(c.creditUtilization),
                SUM(c.creditLimitAmount),
                SUM(c.availableCreditAmount)
            FROM CustomerCreditView c
            """).getSingleResult();
        
        Long totalCustomers = (Long) result[0];
        Double avgCreditScore = (Double) result[1];
        Double avgUtilization = (Double) result[2];
        BigDecimal totalCreditLimit = (BigDecimal) result[3];
        BigDecimal totalAvailableCredit = (BigDecimal) result[4];
        
        return new CreditStatistics(
            totalCustomers,
            avgCreditScore != null ? avgCreditScore : 0.0,
            avgUtilization != null ? avgUtilization : 0.0,
            totalCreditLimit != null ? totalCreditLimit : BigDecimal.ZERO,
            totalAvailableCredit != null ? totalAvailableCredit : BigDecimal.ZERO
        );
    }
    
    // Event handlers
    
    private void handleCustomerCreated(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof CustomerCreatedEvent createdEvent) {
            CustomerCreditView view = new CustomerCreditView();
            view.setCustomerId(createdEvent.getCustomerId().getId());
            view.setCustomerName(createdEvent.getCustomerName());
            view.setCreditScore(600); // Default score
            view.setCreditLimitAmount(BigDecimal.ZERO);
            view.setCreditLimitCurrency("AED");
            view.setAvailableCreditAmount(BigDecimal.ZERO);
            view.setAvailableCreditCurrency("AED");
            view.setUsedCreditAmount(BigDecimal.ZERO);
            view.setCreditUtilization(0.0);
            view.setLastUpdated(LocalDateTime.now());
            view.setRiskCategory(determineRiskCategory(600));
            
            entityManager.persist(view);
        }
    }
    
    private void handleCreditLimitUpdated(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof CustomerCreditLimitUpdatedEvent limitEvent) {
            Optional<CustomerCreditView> viewOpt = findByCustomerId(limitEvent.getCustomerId());
            if (viewOpt.isPresent()) {
                CustomerCreditView view = viewOpt.get();
                view.setCreditLimitAmount(limitEvent.getNewCreditLimit().getAmount());
                view.setCreditLimitCurrency(limitEvent.getNewCreditLimit().getCurrency());
                
                // Recalculate available credit and utilization
                BigDecimal availableCredit = view.getCreditLimitAmount().subtract(view.getUsedCreditAmount());
                view.setAvailableCreditAmount(availableCredit);
                
                double utilization = calculateUtilization(view.getUsedCreditAmount(), view.getCreditLimitAmount());
                view.setCreditUtilization(utilization);
                view.setRiskCategory(determineRiskCategory(view.getCreditScore(), utilization));
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    private void handleCreditReserved(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof CustomerCreditReservedEvent reservedEvent) {
            Optional<CustomerCreditView> viewOpt = findByCustomerId(reservedEvent.getCustomerId());
            if (viewOpt.isPresent()) {
                CustomerCreditView view = viewOpt.get();
                
                // Update used credit
                BigDecimal newUsedCredit = view.getUsedCreditAmount().add(reservedEvent.getAmount().getAmount());
                view.setUsedCreditAmount(newUsedCredit);
                
                // Update available credit
                BigDecimal newAvailableCredit = view.getCreditLimitAmount().subtract(newUsedCredit);
                view.setAvailableCreditAmount(newAvailableCredit);
                
                // Update utilization
                double utilization = calculateUtilization(newUsedCredit, view.getCreditLimitAmount());
                view.setCreditUtilization(utilization);
                view.setRiskCategory(determineRiskCategory(view.getCreditScore(), utilization));
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    private void handleCreditReleased(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof CustomerCreditReleasedEvent releasedEvent) {
            Optional<CustomerCreditView> viewOpt = findByCustomerId(releasedEvent.getCustomerId());
            if (viewOpt.isPresent()) {
                CustomerCreditView view = viewOpt.get();
                
                // Update used credit
                BigDecimal newUsedCredit = view.getUsedCreditAmount().subtract(releasedEvent.getAmount().getAmount());
                if (newUsedCredit.compareTo(BigDecimal.ZERO) < 0) {
                    newUsedCredit = BigDecimal.ZERO;
                }
                view.setUsedCreditAmount(newUsedCredit);
                
                // Update available credit
                BigDecimal newAvailableCredit = view.getCreditLimitAmount().subtract(newUsedCredit);
                view.setAvailableCreditAmount(newAvailableCredit);
                
                // Update utilization
                double utilization = calculateUtilization(newUsedCredit, view.getCreditLimitAmount());
                view.setCreditUtilization(utilization);
                view.setRiskCategory(determineRiskCategory(view.getCreditScore(), utilization));
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    private void handleCreditScoreUpdated(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof CustomerCreditScoreUpdatedEvent scoreEvent) {
            Optional<CustomerCreditView> viewOpt = findByCustomerId(scoreEvent.getCustomerId());
            if (viewOpt.isPresent()) {
                CustomerCreditView view = viewOpt.get();
                view.setCreditScore(scoreEvent.getNewCreditScore());
                view.setRiskCategory(determineRiskCategory(scoreEvent.getNewCreditScore(), view.getCreditUtilization()));
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    // Helper methods
    
    private double calculateUtilization(BigDecimal usedCredit, BigDecimal creditLimit) {
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return usedCredit.divide(creditLimit, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
    }
    
    private String determineRiskCategory(Integer creditScore) {
        return determineRiskCategory(creditScore, 0.0);
    }
    
    private String determineRiskCategory(Integer creditScore, double utilization) {
        if (creditScore >= 750 && utilization < 30) {
            return "LOW";
        } else if (creditScore >= 700 && utilization < 50) {
            return "MEDIUM";
        } else if (creditScore >= 650 && utilization < 70) {
            return "MEDIUM_HIGH";
        } else {
            return "HIGH";
        }
    }
    
    /**
     * Customer Credit View Entity - Optimized for Read Operations
     */
    @Entity
    @Table(name = "customer_credit_view", indexes = {
        @Index(name = "idx_credit_score", columnList = "creditScore"),
        @Index(name = "idx_credit_utilization", columnList = "creditUtilization"),
        @Index(name = "idx_risk_category", columnList = "riskCategory"),
        @Index(name = "idx_available_credit", columnList = "availableCreditAmount"),
        @Index(name = "idx_last_updated", columnList = "lastUpdated")
    })
    public static class CustomerCreditView {
        
        @Id
        @Column(name = "customer_id")
        private String customerId;
        
        @Column(name = "customer_name", nullable = false)
        private String customerName;
        
        @Column(name = "credit_score")
        private Integer creditScore;
        
        @Column(name = "credit_limit_amount", precision = 19, scale = 2)
        private BigDecimal creditLimitAmount;
        
        @Column(name = "credit_limit_currency", length = 3)
        private String creditLimitCurrency;
        
        @Column(name = "available_credit_amount", precision = 19, scale = 2)
        private BigDecimal availableCreditAmount;
        
        @Column(name = "available_credit_currency", length = 3)
        private String availableCreditCurrency;
        
        @Column(name = "used_credit_amount", precision = 19, scale = 2)
        private BigDecimal usedCreditAmount;
        
        @Column(name = "credit_utilization")
        private Double creditUtilization;
        
        @Column(name = "risk_category", length = 20)
        private String riskCategory;
        
        @Column(name = "last_updated", nullable = false)
        private LocalDateTime lastUpdated;
        
        // Constructors
        public CustomerCreditView() {}
        
        // Getters and Setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        
        public BigDecimal getCreditLimitAmount() { return creditLimitAmount; }
        public void setCreditLimitAmount(BigDecimal creditLimitAmount) { this.creditLimitAmount = creditLimitAmount; }
        
        public String getCreditLimitCurrency() { return creditLimitCurrency; }
        public void setCreditLimitCurrency(String creditLimitCurrency) { this.creditLimitCurrency = creditLimitCurrency; }
        
        public BigDecimal getAvailableCreditAmount() { return availableCreditAmount; }
        public void setAvailableCreditAmount(BigDecimal availableCreditAmount) { this.availableCreditAmount = availableCreditAmount; }
        
        public String getAvailableCreditCurrency() { return availableCreditCurrency; }
        public void setAvailableCreditCurrency(String availableCreditCurrency) { this.availableCreditCurrency = availableCreditCurrency; }
        
        public BigDecimal getUsedCreditAmount() { return usedCreditAmount; }
        public void setUsedCreditAmount(BigDecimal usedCreditAmount) { this.usedCreditAmount = usedCreditAmount; }
        
        public Double getCreditUtilization() { return creditUtilization; }
        public void setCreditUtilization(Double creditUtilization) { this.creditUtilization = creditUtilization; }
        
        public String getRiskCategory() { return riskCategory; }
        public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        // Business methods
        public Money getCreditLimit() {
            return Money.of(creditLimitAmount, creditLimitCurrency);
        }
        
        public Money getAvailableCredit() {
            return Money.of(availableCreditAmount, availableCreditCurrency);
        }
        
        public Money getUsedCredit() {
            return Money.of(usedCreditAmount, creditLimitCurrency);
        }
        
        public boolean isHighRisk() {
            return "HIGH".equals(riskCategory);
        }
        
        public boolean isEligibleForLoan(BigDecimal loanAmount) {
            return availableCreditAmount.compareTo(loanAmount) >= 0 && 
                   creditScore >= 600 && 
                   !"HIGH".equals(riskCategory);
        }
    }
    
    /**
     * Credit statistics for reporting
     */
    public static class CreditStatistics {
        private final Long totalCustomers;
        private final Double averageCreditScore;
        private final Double averageUtilization;
        private final BigDecimal totalCreditLimit;
        private final BigDecimal totalAvailableCredit;
        
        public CreditStatistics(Long totalCustomers, Double averageCreditScore, Double averageUtilization,
                              BigDecimal totalCreditLimit, BigDecimal totalAvailableCredit) {
            this.totalCustomers = totalCustomers;
            this.averageCreditScore = averageCreditScore;
            this.averageUtilization = averageUtilization;
            this.totalCreditLimit = totalCreditLimit;
            this.totalAvailableCredit = totalAvailableCredit;
        }
        
        // Getters
        public Long getTotalCustomers() { return totalCustomers; }
        public Double getAverageCreditScore() { return averageCreditScore; }
        public Double getAverageUtilization() { return averageUtilization; }
        public BigDecimal getTotalCreditLimit() { return totalCreditLimit; }
        public BigDecimal getTotalAvailableCredit() { return totalAvailableCredit; }
        
        public BigDecimal getTotalUsedCredit() {
            return totalCreditLimit.subtract(totalAvailableCredit);
        }
        
        public double getOverallUtilization() {
            if (totalCreditLimit.compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            return getTotalUsedCredit().divide(totalCreditLimit, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        }
    }
}