package com.bank.loan.infrastructure.repository;

import com.bank.loan.domain.*;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.infrastructure.repository.OptimizedRepositoryBase;
import com.bank.infrastructure.caching.MultiLevelCacheService;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized Loan Repository Implementation
 * 
 * High-performance repository for loan operations with:
 * - Native SQL queries for complex loan analytics
 * - Batch processing for portfolio operations
 * - Specialized caching for loan data
 * - Banking-specific query optimizations
 * - Risk assessment queries
 */
@Repository
public class OptimizedLoanRepository extends OptimizedRepositoryBase<Loan, LoanId> implements LoanRepository {
    
    public OptimizedLoanRepository(MultiLevelCacheService cacheService) {
        super(Loan.class, "loans", cacheService);
    }
    
    @Override
    public List<Loan> findByCustomerId(CustomerId customerId) {
        // Use native query for better performance
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.customer_id = ?1 
            ORDER BY l.created_at DESC
            """;
        
        Map<String, Object> parameters = Map.of("1", customerId.getId());
        return executeNativeQuery(sql, parameters);
    }
    
    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        // Cache frequently accessed active loans
        if (status == LoanStatus.ACTIVE) {
            return cacheService.get(
                "loans", 
                "active-loans", 
                List.class,
                key -> findByStatusUncached(status)
            );
        }
        
        return findByStatusUncached(status);
    }
    
    @Override
    public List<Loan> findOverdueLoans() {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.status = 'ACTIVE' 
            AND l.maturity_date < CURRENT_DATE 
            ORDER BY l.maturity_date ASC
            """;
        
        return executeNativeQuery(sql, Map.of());
    }
    
    /**
     * Find loans by amount range with risk categorization
     */
    public List<Loan> findByAmountRange(Money minAmount, Money maxAmount) {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.principal_amount BETWEEN ?1 AND ?2 
            AND l.currency = ?3 
            ORDER BY l.principal_amount DESC
            """;
        
        Map<String, Object> parameters = Map.of(
            "1", minAmount.getAmount(),
            "2", maxAmount.getAmount(),
            "3", minAmount.getCurrency()
        );
        
        return executeNativeQuery(sql, parameters);
    }
    
    /**
     * Find loans by interest rate range
     */
    public List<Loan> findByInterestRateRange(double minRate, double maxRate) {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.interest_rate BETWEEN ?1 AND ?2 
            ORDER BY l.interest_rate DESC
            """;
        
        Map<String, Object> parameters = Map.of(
            "1", minRate,
            "2", maxRate
        );
        
        return executeNativeQuery(sql, parameters);
    }
    
    /**
     * Find loans by term range
     */
    public List<Loan> findByTermRange(int minMonths, int maxMonths) {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.term_months BETWEEN ?1 AND ?2 
            ORDER BY l.term_months ASC
            """;
        
        Map<String, Object> parameters = Map.of(
            "1", minMonths,
            "2", maxMonths
        );
        
        return executeNativeQuery(sql, parameters);
    }
    
    /**
     * Find loans created within date range
     */
    public List<Loan> findByCreationDateRange(LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE DATE(l.created_at) BETWEEN ?1 AND ?2 
            ORDER BY l.created_at DESC
            """;
        
        Map<String, Object> parameters = Map.of(
            "1", fromDate,
            "2", toDate
        );
        
        return executeNativeQuery(sql, parameters);
    }
    
    /**
     * Portfolio analytics: Get loan statistics
     */
    public LoanPortfolioStatistics getPortfolioStatistics() {
        String sql = """
            SELECT 
                COUNT(*) as total_loans,
                COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_loans,
                COUNT(CASE WHEN status = 'DEFAULTED' THEN 1 END) as defaulted_loans,
                COUNT(CASE WHEN status = 'FULLY_PAID' THEN 1 END) as paid_loans,
                SUM(CASE WHEN status = 'ACTIVE' THEN principal_amount ELSE 0 END) as active_amount,
                SUM(CASE WHEN status = 'ACTIVE' THEN outstanding_balance ELSE 0 END) as outstanding_amount,
                AVG(interest_rate) as avg_interest_rate,
                MIN(interest_rate) as min_interest_rate,
                MAX(interest_rate) as max_interest_rate,
                AVG(term_months) as avg_term_months
            FROM loans
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        Object[] result = (Object[]) query.getSingleResult();
        
        return new LoanPortfolioStatistics(
            ((Number) result[0]).longValue(),  // total_loans
            ((Number) result[1]).longValue(),  // active_loans
            ((Number) result[2]).longValue(),  // defaulted_loans
            ((Number) result[3]).longValue(),  // paid_loans
            (java.math.BigDecimal) result[4],  // active_amount
            (java.math.BigDecimal) result[5],  // outstanding_amount
            ((Number) result[6]).doubleValue(), // avg_interest_rate
            ((Number) result[7]).doubleValue(), // min_interest_rate
            ((Number) result[8]).doubleValue(), // max_interest_rate
            ((Number) result[9]).doubleValue()  // avg_term_months
        );
    }
    
    /**
     * Risk analytics: Get high-risk loans
     */
    public List<Loan> findHighRiskLoans() {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.status = 'ACTIVE' 
            AND (
                l.principal_amount > 100000 
                OR l.interest_rate > 0.15 
                OR l.maturity_date < CURRENT_DATE + INTERVAL '30 days'
            )
            ORDER BY l.principal_amount DESC, l.interest_rate DESC
            """;
        
        return executeNativeQuery(sql, Map.of());
    }
    
    /**
     * Find loans requiring attention (payment due soon)
     */
    public List<Loan> findLoansRequiringAttention() {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.status = 'ACTIVE' 
            AND l.next_payment_date <= CURRENT_DATE + INTERVAL '7 days'
            ORDER BY l.next_payment_date ASC
            """;
        
        return executeNativeQuery(sql, Map.of());
    }
    
    /**
     * Customer analytics: Get customer loan summary
     */
    public CustomerLoanSummary getCustomerLoanSummary(CustomerId customerId) {
        String sql = """
            SELECT 
                COUNT(*) as total_loans,
                COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_loans,
                SUM(CASE WHEN status = 'ACTIVE' THEN outstanding_balance ELSE 0 END) as total_outstanding,
                SUM(principal_amount) as total_borrowed,
                AVG(interest_rate) as avg_interest_rate,
                MIN(created_at) as first_loan_date,
                MAX(created_at) as last_loan_date
            FROM loans 
            WHERE customer_id = ?1
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, customerId.getId());
        
        Object[] result = (Object[]) query.getSingleResult();
        
        return new CustomerLoanSummary(
            customerId,
            ((Number) result[0]).longValue(),
            ((Number) result[1]).longValue(),
            (java.math.BigDecimal) result[2],
            (java.math.BigDecimal) result[3],
            ((Number) result[4]).doubleValue(),
            (LocalDateTime) result[5],
            (LocalDateTime) result[6]
        );
    }
    
    /**
     * Batch update loan statuses
     */
    public void updateLoanStatuses(Map<LoanId, LoanStatus> statusUpdates) {
        if (statusUpdates.isEmpty()) {
            return;
        }
        
        // Use batch update for performance
        String sql = """
            UPDATE loans 
            SET status = ?1, updated_at = CURRENT_TIMESTAMP 
            WHERE loan_id = ?2
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        
        int batchSize = 0;
        for (Map.Entry<LoanId, LoanStatus> entry : statusUpdates.entrySet()) {
            query.setParameter(1, entry.getValue().name());
            query.setParameter(2, entry.getKey().getId());
            query.addBatch();
            
            if (++batchSize % 50 == 0) {
                query.executeBatch();
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        // Execute remaining batch
        if (batchSize % 50 != 0) {
            query.executeBatch();
        }
        
        // Clear cache for updated loans
        statusUpdates.keySet().forEach(loanId -> 
            cacheService.evict("loans", loanId.getId())
        );
    }
    
    /**
     * Advanced search with multiple criteria
     */
    public Page<Loan> searchLoans(LoanSearchCriteria criteria, Pageable pageable) {
        StringBuilder sql = new StringBuilder("SELECT l.* FROM loans l WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM loans l WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();
        
        // Build dynamic query based on criteria
        if (criteria.getCustomerId() != null) {
            sql.append(" AND l.customer_id = :customerId");
            countSql.append(" AND l.customer_id = :customerId");
            parameters.put("customerId", criteria.getCustomerId().getId());
        }
        
        if (criteria.getStatus() != null) {
            sql.append(" AND l.status = :status");
            countSql.append(" AND l.status = :status");
            parameters.put("status", criteria.getStatus().name());
        }
        
        if (criteria.getMinAmount() != null) {
            sql.append(" AND l.principal_amount >= :minAmount");
            countSql.append(" AND l.principal_amount >= :minAmount");
            parameters.put("minAmount", criteria.getMinAmount().getAmount());
        }
        
        if (criteria.getMaxAmount() != null) {
            sql.append(" AND l.principal_amount <= :maxAmount");
            countSql.append(" AND l.principal_amount <= :maxAmount");
            parameters.put("maxAmount", criteria.getMaxAmount().getAmount());
        }
        
        if (criteria.getFromDate() != null) {
            sql.append(" AND l.created_at >= :fromDate");
            countSql.append(" AND l.created_at >= :fromDate");
            parameters.put("fromDate", criteria.getFromDate());
        }
        
        if (criteria.getToDate() != null) {
            sql.append(" AND l.created_at <= :toDate");
            countSql.append(" AND l.created_at <= :toDate");
            parameters.put("toDate", criteria.getToDate());
        }
        
        // Add ordering
        sql.append(" ORDER BY l.created_at DESC");
        
        // Get total count
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        parameters.forEach(countQuery::setParameter);
        Long totalCount = ((Number) countQuery.getSingleResult()).longValue();
        
        if (totalCount == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Get paginated results
        Query dataQuery = entityManager.createNativeQuery(sql.toString(), Loan.class);
        parameters.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<Loan> results = dataQuery.getResultList();
        
        return new PageImpl<>(results, pageable, totalCount);
    }
    
    // Implementation of abstract methods
    
    @Override
    protected String buildFindByIdsQuery(int idCount) {
        String placeholders = String.join(",", Collections.nCopies(idCount, "?"));
        return String.format("SELECT * FROM loans WHERE loan_id IN (%s)", placeholders);
    }
    
    @Override
    protected String buildDeleteByIdsQuery(int idCount) {
        String placeholders = String.join(",", Collections.nCopies(idCount, "?"));
        return String.format("DELETE FROM loans WHERE loan_id IN (%s)", placeholders);
    }
    
    @Override
    protected String getTableName() {
        return "loans";
    }
    
    @Override
    protected String getIdColumnName() {
        return "loan_id";
    }
    
    @Override
    protected LoanId convertStringToId(String id) {
        return LoanId.of(id);
    }
    
    // Private helper methods
    
    private List<Loan> findByStatusUncached(LoanStatus status) {
        String sql = """
            SELECT l.* FROM loans l 
            WHERE l.status = ?1 
            ORDER BY l.created_at DESC
            """;
        
        Map<String, Object> parameters = Map.of("1", status.name());
        return executeNativeQuery(sql, parameters);
    }
    
    /**
     * Loan Portfolio Statistics
     */
    public static class LoanPortfolioStatistics {
        private final long totalLoans;
        private final long activeLoans;
        private final long defaultedLoans;
        private final long paidLoans;
        private final java.math.BigDecimal activeAmount;
        private final java.math.BigDecimal outstandingAmount;
        private final double averageInterestRate;
        private final double minInterestRate;
        private final double maxInterestRate;
        private final double averageTermMonths;
        
        public LoanPortfolioStatistics(long totalLoans, long activeLoans, long defaultedLoans, long paidLoans,
                                     java.math.BigDecimal activeAmount, java.math.BigDecimal outstandingAmount,
                                     double averageInterestRate, double minInterestRate, double maxInterestRate,
                                     double averageTermMonths) {
            this.totalLoans = totalLoans;
            this.activeLoans = activeLoans;
            this.defaultedLoans = defaultedLoans;
            this.paidLoans = paidLoans;
            this.activeAmount = activeAmount;
            this.outstandingAmount = outstandingAmount;
            this.averageInterestRate = averageInterestRate;
            this.minInterestRate = minInterestRate;
            this.maxInterestRate = maxInterestRate;
            this.averageTermMonths = averageTermMonths;
        }
        
        // Getters
        public long getTotalLoans() { return totalLoans; }
        public long getActiveLoans() { return activeLoans; }
        public long getDefaultedLoans() { return defaultedLoans; }
        public long getPaidLoans() { return paidLoans; }
        public java.math.BigDecimal getActiveAmount() { return activeAmount; }
        public java.math.BigDecimal getOutstandingAmount() { return outstandingAmount; }
        public double getAverageInterestRate() { return averageInterestRate; }
        public double getMinInterestRate() { return minInterestRate; }
        public double getMaxInterestRate() { return maxInterestRate; }
        public double getAverageTermMonths() { return averageTermMonths; }
        
        public double getDefaultRate() {
            return totalLoans > 0 ? (double) defaultedLoans / totalLoans * 100 : 0.0;
        }
        
        public double getCompletionRate() {
            return totalLoans > 0 ? (double) paidLoans / totalLoans * 100 : 0.0;
        }
    }
    
    /**
     * Customer Loan Summary
     */
    public static class CustomerLoanSummary {
        private final CustomerId customerId;
        private final long totalLoans;
        private final long activeLoans;
        private final java.math.BigDecimal totalOutstanding;
        private final java.math.BigDecimal totalBorrowed;
        private final double averageInterestRate;
        private final LocalDateTime firstLoanDate;
        private final LocalDateTime lastLoanDate;
        
        public CustomerLoanSummary(CustomerId customerId, long totalLoans, long activeLoans,
                                 java.math.BigDecimal totalOutstanding, java.math.BigDecimal totalBorrowed,
                                 double averageInterestRate, LocalDateTime firstLoanDate, LocalDateTime lastLoanDate) {
            this.customerId = customerId;
            this.totalLoans = totalLoans;
            this.activeLoans = activeLoans;
            this.totalOutstanding = totalOutstanding;
            this.totalBorrowed = totalBorrowed;
            this.averageInterestRate = averageInterestRate;
            this.firstLoanDate = firstLoanDate;
            this.lastLoanDate = lastLoanDate;
        }
        
        // Getters
        public CustomerId getCustomerId() { return customerId; }
        public long getTotalLoans() { return totalLoans; }
        public long getActiveLoans() { return activeLoans; }
        public java.math.BigDecimal getTotalOutstanding() { return totalOutstanding; }
        public java.math.BigDecimal getTotalBorrowed() { return totalBorrowed; }
        public double getAverageInterestRate() { return averageInterestRate; }
        public LocalDateTime getFirstLoanDate() { return firstLoanDate; }
        public LocalDateTime getLastLoanDate() { return lastLoanDate; }
    }
    
    /**
     * Loan Search Criteria
     */
    public static class LoanSearchCriteria {
        private CustomerId customerId;
        private LoanStatus status;
        private Money minAmount;
        private Money maxAmount;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;
        
        // Getters and setters
        public CustomerId getCustomerId() { return customerId; }
        public void setCustomerId(CustomerId customerId) { this.customerId = customerId; }
        
        public LoanStatus getStatus() { return status; }
        public void setStatus(LoanStatus status) { this.status = status; }
        
        public Money getMinAmount() { return minAmount; }
        public void setMinAmount(Money minAmount) { this.minAmount = minAmount; }
        
        public Money getMaxAmount() { return maxAmount; }
        public void setMaxAmount(Money maxAmount) { this.maxAmount = maxAmount; }
        
        public LocalDateTime getFromDate() { return fromDate; }
        public void setFromDate(LocalDateTime fromDate) { this.fromDate = fromDate; }
        
        public LocalDateTime getToDate() { return toDate; }
        public void setToDate(LocalDateTime toDate) { this.toDate = toDate; }
    }
}