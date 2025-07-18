package com.bank.infrastructure.cqrs.projections;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.loan.domain.*;
import com.bank.infrastructure.eventsourcing.EventStreamPublisher;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Loan Portfolio Projection for CQRS Read Models
 * 
 * Optimized read model for loan portfolio management:
 * - Loan performance tracking
 * - Portfolio risk analysis
 * - Payment schedule optimization
 * - Collections and recovery data
 * - Performance analytics for lending decisions
 */
@Component
@Transactional
public class LoanPortfolioProjection {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public LoanPortfolioProjection(EventStreamPublisher eventStreamPublisher) {
        // Subscribe to loan-related events
        eventStreamPublisher.subscribe("LoanCreatedEvent", this::handleLoanCreated);
        eventStreamPublisher.subscribe("LoanApprovedEvent", this::handleLoanApproved);
        eventStreamPublisher.subscribe("LoanDisbursedEvent", this::handleLoanDisbursed);
        eventStreamPublisher.subscribe("LoanPaymentMadeEvent", this::handlePaymentMade);
        eventStreamPublisher.subscribe("LoanFullyPaidEvent", this::handleLoanFullyPaid);
        eventStreamPublisher.subscribe("LoanDefaultedEvent", this::handleLoanDefaulted);
        eventStreamPublisher.subscribe("LoanRejectedEvent", this::handleLoanRejected);
    }
    
    /**
     * Find loan portfolio view by loan ID
     */
    public Optional<LoanPortfolioView> findByLoanId(LoanId loanId) {
        try {
            LoanPortfolioView view = entityManager.find(LoanPortfolioView.class, loanId.getId());
            return Optional.ofNullable(view);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Find all loans for a customer
     */
    public java.util.List<LoanPortfolioView> findByCustomerId(CustomerId customerId) {
        return entityManager.createQuery("""
            SELECT l FROM LoanPortfolioView l 
            WHERE l.customerId = :customerId 
            ORDER BY l.createdAt DESC
            """, LoanPortfolioView.class)
            .setParameter("customerId", customerId.getId())
            .getResultList();
    }
    
    /**
     * Find active loans by status
     */
    public java.util.List<LoanPortfolioView> findByStatus(String status) {
        return entityManager.createQuery("""
            SELECT l FROM LoanPortfolioView l 
            WHERE l.status = :status 
            ORDER BY l.nextPaymentDate ASC
            """, LoanPortfolioView.class)
            .setParameter("status", status)
            .getResultList();
    }
    
    /**
     * Find overdue loans
     */
    public java.util.List<LoanPortfolioView> findOverdueLoans() {
        return entityManager.createQuery("""
            SELECT l FROM LoanPortfolioView l 
            WHERE l.status = 'ACTIVE' 
            AND l.nextPaymentDate < :currentDate 
            ORDER BY l.daysPastDue DESC
            """, LoanPortfolioView.class)
            .setParameter("currentDate", LocalDate.now())
            .getResultList();
    }
    
    /**
     * Find loans by risk category
     */
    public java.util.List<LoanPortfolioView> findByRiskCategory(String riskCategory) {
        return entityManager.createQuery("""
            SELECT l FROM LoanPortfolioView l 
            WHERE l.riskCategory = :riskCategory 
            ORDER BY l.outstandingAmount DESC
            """, LoanPortfolioView.class)
            .setParameter("riskCategory", riskCategory)
            .getResultList();
    }
    
    /**
     * Get portfolio statistics
     */
    public PortfolioStatistics getPortfolioStatistics() {
        Object[] result = (Object[]) entityManager.createQuery("""
            SELECT 
                COUNT(l),
                SUM(CASE WHEN l.status = 'ACTIVE' THEN l.outstandingAmount ELSE 0 END),
                SUM(CASE WHEN l.status = 'ACTIVE' THEN 1 ELSE 0 END),
                SUM(CASE WHEN l.status = 'DEFAULTED' THEN l.outstandingAmount ELSE 0 END),
                SUM(CASE WHEN l.status = 'DEFAULTED' THEN 1 ELSE 0 END),
                AVG(l.interestRate),
                SUM(l.principalAmount)
            FROM LoanPortfolioView l
            """).getSingleResult();
        
        Long totalLoans = (Long) result[0];
        BigDecimal totalOutstanding = (BigDecimal) result[1];
        Long activeLoans = (Long) result[2];
        BigDecimal defaultedAmount = (BigDecimal) result[3];
        Long defaultedLoans = (Long) result[4];
        Double avgInterestRate = (Double) result[5];
        BigDecimal totalPrincipal = (BigDecimal) result[6];
        
        return new PortfolioStatistics(
            totalLoans,
            totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO,
            activeLoans,
            defaultedAmount != null ? defaultedAmount : BigDecimal.ZERO,
            defaultedLoans,
            avgInterestRate != null ? avgInterestRate : 0.0,
            totalPrincipal != null ? totalPrincipal : BigDecimal.ZERO
        );
    }
    
    // Event handlers
    
    private void handleLoanCreated(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanCreatedEvent createdEvent) {
            LoanPortfolioView view = new LoanPortfolioView();
            view.setLoanId(createdEvent.getLoanId().getId());
            view.setCustomerId(createdEvent.getCustomerId().getId());
            view.setPrincipalAmount(createdEvent.getPrincipalAmount().getAmount());
            view.setCurrency(createdEvent.getPrincipalAmount().getCurrency());
            view.setOutstandingAmount(createdEvent.getPrincipalAmount().getAmount());
            view.setInterestRate(createdEvent.getInterestRate().getAnnualRate().doubleValue());
            view.setTermMonths(createdEvent.getLoanTerm().getMonths());
            view.setStatus("CREATED");
            view.setCreatedAt(LocalDateTime.now());
            view.setLastUpdated(LocalDateTime.now());
            view.setRiskCategory(determineRiskCategory(createdEvent.getPrincipalAmount(), null));
            
            entityManager.persist(view);
        }
    }
    
    private void handleLoanApproved(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanApprovedEvent approvedEvent) {
            updateLoanStatus(approvedEvent.getLoanId(), "APPROVED");
        }
    }
    
    private void handleLoanDisbursed(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanDisbursedEvent disbursedEvent) {
            Optional<LoanPortfolioView> viewOpt = findByLoanId(disbursedEvent.getLoanId());
            if (viewOpt.isPresent()) {
                LoanPortfolioView view = viewOpt.get();
                view.setStatus("ACTIVE");
                view.setDisbursementDate(disbursedEvent.getDisbursementDate());
                view.setMaturityDate(disbursedEvent.getMaturityDate());
                
                // Calculate first payment date (typically next month)
                LocalDate firstPaymentDate = disbursedEvent.getDisbursementDate().plusMonths(1);
                view.setNextPaymentDate(firstPaymentDate);
                
                // Calculate monthly payment amount
                Money monthlyPayment = calculateMonthlyPayment(
                    Money.of(view.getPrincipalAmount(), view.getCurrency()),
                    view.getInterestRate(),
                    view.getTermMonths()
                );
                view.setNextPaymentAmount(monthlyPayment.getAmount());
                view.setRemainingPayments(view.getTermMonths());
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    private void handlePaymentMade(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanPaymentMadeEvent paymentEvent) {
            Optional<LoanPortfolioView> viewOpt = findByLoanId(paymentEvent.getLoanId());
            if (viewOpt.isPresent()) {
                LoanPortfolioView view = viewOpt.get();
                
                // Update outstanding amount
                BigDecimal newOutstanding = view.getOutstandingAmount().subtract(paymentEvent.getPaymentAmount().getAmount());
                view.setOutstandingAmount(newOutstanding);
                
                // Update payment tracking
                view.setLastPaymentDate(LocalDate.now());
                view.setLastPaymentAmount(paymentEvent.getPaymentAmount().getAmount());
                view.setTotalPaid(view.getTotalPaid().add(paymentEvent.getPaymentAmount().getAmount()));
                
                // Calculate next payment date
                if (view.getNextPaymentDate() != null) {
                    view.setNextPaymentDate(view.getNextPaymentDate().plusMonths(1));
                }
                
                // Update remaining payments
                if (view.getRemainingPayments() > 0) {
                    view.setRemainingPayments(view.getRemainingPayments() - 1);
                }
                
                // Reset days past due if payment was made
                view.setDaysPastDue(0);
                
                // Update risk category
                view.setRiskCategory(determineRiskCategory(
                    Money.of(view.getOutstandingAmount(), view.getCurrency()),
                    view.getDaysPastDue()
                ));
                
                view.setLastUpdated(LocalDateTime.now());
                entityManager.merge(view);
            }
        }
    }
    
    private void handleLoanFullyPaid(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanFullyPaidEvent fullyPaidEvent) {
            Optional<LoanPortfolioView> viewOpt = findByLoanId(fullyPaidEvent.getLoanId());
            if (viewOpt.isPresent()) {
                LoanPortfolioView view = viewOpt.get();
                view.setStatus("FULLY_PAID");
                view.setOutstandingAmount(BigDecimal.ZERO);
                view.setRemainingPayments(0);
                view.setNextPaymentDate(null);
                view.setNextPaymentAmount(BigDecimal.ZERO);
                view.setCompletedDate(LocalDate.now());
                view.setRiskCategory("COMPLETED");
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    private void handleLoanDefaulted(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanDefaultedEvent defaultedEvent) {
            Optional<LoanPortfolioView> viewOpt = findByLoanId(defaultedEvent.getLoanId());
            if (viewOpt.isPresent()) {
                LoanPortfolioView view = viewOpt.get();
                view.setStatus("DEFAULTED");
                view.setDefaultDate(LocalDate.now());
                view.setDefaultReason(defaultedEvent.getReason());
                view.setRiskCategory("DEFAULT");
                view.setLastUpdated(LocalDateTime.now());
                
                entityManager.merge(view);
            }
        }
    }
    
    private void handleLoanRejected(com.bank.shared.kernel.domain.DomainEvent event) {
        if (event instanceof LoanRejectedEvent rejectedEvent) {
            updateLoanStatus(rejectedEvent.getLoanId(), "REJECTED");
        }
    }
    
    // Helper methods
    
    private void updateLoanStatus(LoanId loanId, String status) {
        Optional<LoanPortfolioView> viewOpt = findByLoanId(loanId);
        if (viewOpt.isPresent()) {
            LoanPortfolioView view = viewOpt.get();
            view.setStatus(status);
            view.setLastUpdated(LocalDateTime.now());
            entityManager.merge(view);
        }
    }
    
    private String determineRiskCategory(Money amount, Integer daysPastDue) {
        BigDecimal loanAmount = amount.getAmount();
        
        // High value loans are automatically higher risk
        if (loanAmount.compareTo(new BigDecimal("100000")) > 0) {
            if (daysPastDue != null && daysPastDue > 30) {
                return "CRITICAL";
            }
            return "HIGH";
        }
        
        // Risk based on days past due
        if (daysPastDue != null) {
            if (daysPastDue > 90) return "CRITICAL";
            if (daysPastDue > 30) return "HIGH";
            if (daysPastDue > 0) return "MEDIUM";
        }
        
        // Risk based on loan amount
        if (loanAmount.compareTo(new BigDecimal("50000")) > 0) {
            return "MEDIUM";
        }
        
        return "LOW";
    }
    
    private Money calculateMonthlyPayment(Money principal, double annualRate, int termMonths) {
        if (annualRate == 0) {
            return principal.divide(new BigDecimal(termMonths));
        }
        
        double monthlyRate = annualRate / 12;
        double factor = Math.pow(1 + monthlyRate, termMonths);
        double monthlyPayment = principal.getAmount().doubleValue() * monthlyRate * factor / (factor - 1);
        
        return Money.of(new BigDecimal(monthlyPayment).setScale(2, java.math.RoundingMode.HALF_UP), principal.getCurrency());
    }
    
    /**
     * Loan Portfolio View Entity - Optimized for Read Operations
     */
    @Entity
    @Table(name = "loan_portfolio_view", indexes = {
        @Index(name = "idx_portfolio_customer", columnList = "customerId"),
        @Index(name = "idx_portfolio_status", columnList = "status"),
        @Index(name = "idx_portfolio_next_payment", columnList = "nextPaymentDate"),
        @Index(name = "idx_portfolio_risk", columnList = "riskCategory"),
        @Index(name = "idx_portfolio_outstanding", columnList = "outstandingAmount"),
        @Index(name = "idx_portfolio_past_due", columnList = "daysPastDue")
    })
    public static class LoanPortfolioView {
        
        @Id
        @Column(name = "loan_id")
        private String loanId;
        
        @Column(name = "customer_id", nullable = false)
        private String customerId;
        
        @Column(name = "principal_amount", precision = 19, scale = 2, nullable = false)
        private BigDecimal principalAmount;
        
        @Column(name = "outstanding_amount", precision = 19, scale = 2, nullable = false)
        private BigDecimal outstandingAmount;
        
        @Column(name = "currency", length = 3, nullable = false)
        private String currency;
        
        @Column(name = "interest_rate", nullable = false)
        private Double interestRate;
        
        @Column(name = "term_months", nullable = false)
        private Integer termMonths;
        
        @Column(name = "remaining_payments")
        private Integer remainingPayments;
        
        @Column(name = "status", length = 20, nullable = false)
        private String status;
        
        @Column(name = "risk_category", length = 20)
        private String riskCategory;
        
        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;
        
        @Column(name = "disbursement_date")
        private LocalDate disbursementDate;
        
        @Column(name = "maturity_date")
        private LocalDate maturityDate;
        
        @Column(name = "next_payment_date")
        private LocalDate nextPaymentDate;
        
        @Column(name = "next_payment_amount", precision = 19, scale = 2)
        private BigDecimal nextPaymentAmount;
        
        @Column(name = "last_payment_date")
        private LocalDate lastPaymentDate;
        
        @Column(name = "last_payment_amount", precision = 19, scale = 2)
        private BigDecimal lastPaymentAmount;
        
        @Column(name = "total_paid", precision = 19, scale = 2)
        private BigDecimal totalPaid = BigDecimal.ZERO;
        
        @Column(name = "days_past_due")
        private Integer daysPastDue = 0;
        
        @Column(name = "default_date")
        private LocalDate defaultDate;
        
        @Column(name = "default_reason")
        private String defaultReason;
        
        @Column(name = "completed_date")
        private LocalDate completedDate;
        
        @Column(name = "last_updated", nullable = false)
        private LocalDateTime lastUpdated;
        
        // Constructors
        public LoanPortfolioView() {}
        
        // Getters and Setters
        public String getLoanId() { return loanId; }
        public void setLoanId(String loanId) { this.loanId = loanId; }
        
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public BigDecimal getPrincipalAmount() { return principalAmount; }
        public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
        
        public BigDecimal getOutstandingAmount() { return outstandingAmount; }
        public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public Double getInterestRate() { return interestRate; }
        public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }
        
        public Integer getTermMonths() { return termMonths; }
        public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
        
        public Integer getRemainingPayments() { return remainingPayments; }
        public void setRemainingPayments(Integer remainingPayments) { this.remainingPayments = remainingPayments; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getRiskCategory() { return riskCategory; }
        public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDate getDisbursementDate() { return disbursementDate; }
        public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
        
        public LocalDate getMaturityDate() { return maturityDate; }
        public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
        
        public LocalDate getNextPaymentDate() { return nextPaymentDate; }
        public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
        
        public BigDecimal getNextPaymentAmount() { return nextPaymentAmount; }
        public void setNextPaymentAmount(BigDecimal nextPaymentAmount) { this.nextPaymentAmount = nextPaymentAmount; }
        
        public LocalDate getLastPaymentDate() { return lastPaymentDate; }
        public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
        
        public BigDecimal getLastPaymentAmount() { return lastPaymentAmount; }
        public void setLastPaymentAmount(BigDecimal lastPaymentAmount) { this.lastPaymentAmount = lastPaymentAmount; }
        
        public BigDecimal getTotalPaid() { return totalPaid; }
        public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
        
        public Integer getDaysPastDue() { return daysPastDue; }
        public void setDaysPastDue(Integer daysPastDue) { this.daysPastDue = daysPastDue; }
        
        public LocalDate getDefaultDate() { return defaultDate; }
        public void setDefaultDate(LocalDate defaultDate) { this.defaultDate = defaultDate; }
        
        public String getDefaultReason() { return defaultReason; }
        public void setDefaultReason(String defaultReason) { this.defaultReason = defaultReason; }
        
        public LocalDate getCompletedDate() { return completedDate; }
        public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        // Business methods
        public Money getOutstanding() {
            return Money.of(outstandingAmount, currency);
        }
        
        public Money getPrincipal() {
            return Money.of(principalAmount, currency);
        }
        
        public boolean isOverdue() {
            return daysPastDue != null && daysPastDue > 0;
        }
        
        public boolean isHighRisk() {
            return "HIGH".equals(riskCategory) || "CRITICAL".equals(riskCategory);
        }
        
        public double getPaymentProgress() {
            if (termMonths == 0) return 0.0;
            int paidPayments = termMonths - (remainingPayments != null ? remainingPayments : termMonths);
            return (double) paidPayments / termMonths * 100;
        }
    }
    
    /**
     * Portfolio statistics for reporting
     */
    public static class PortfolioStatistics {
        private final Long totalLoans;
        private final BigDecimal totalOutstanding;
        private final Long activeLoans;
        private final BigDecimal defaultedAmount;
        private final Long defaultedLoans;
        private final Double averageInterestRate;
        private final BigDecimal totalPrincipal;
        
        public PortfolioStatistics(Long totalLoans, BigDecimal totalOutstanding, Long activeLoans,
                                 BigDecimal defaultedAmount, Long defaultedLoans, Double averageInterestRate,
                                 BigDecimal totalPrincipal) {
            this.totalLoans = totalLoans;
            this.totalOutstanding = totalOutstanding;
            this.activeLoans = activeLoans;
            this.defaultedAmount = defaultedAmount;
            this.defaultedLoans = defaultedLoans;
            this.averageInterestRate = averageInterestRate;
            this.totalPrincipal = totalPrincipal;
        }
        
        // Getters
        public Long getTotalLoans() { return totalLoans; }
        public BigDecimal getTotalOutstanding() { return totalOutstanding; }
        public Long getActiveLoans() { return activeLoans; }
        public BigDecimal getDefaultedAmount() { return defaultedAmount; }
        public Long getDefaultedLoans() { return defaultedLoans; }
        public Double getAverageInterestRate() { return averageInterestRate; }
        public BigDecimal getTotalPrincipal() { return totalPrincipal; }
        
        public double getDefaultRate() {
            return totalLoans > 0 ? (double) defaultedLoans / totalLoans * 100 : 0.0;
        }
        
        public double getPortfolioAtRisk() {
            return totalPrincipal.compareTo(BigDecimal.ZERO) > 0 ? 
                   defaultedAmount.divide(totalPrincipal, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 : 0.0;
        }
    }
}