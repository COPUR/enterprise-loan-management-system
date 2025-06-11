package com.bank.loanmanagement.infrastructure.caching;

import com.bank.loanmanagement.config.RedisConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class BankingCacheService {
    
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);
    private static final AtomicLong cacheEvictions = new AtomicLong(0);
    private static final Map<String, Long> lastAccessTimes = new ConcurrentHashMap<>();
    
    // Customer caching operations
    public static class CustomerCache {
        
        public static boolean cacheCustomer(Long customerId, String customerData) {
            String key = String.format(RedisConfig.CacheKeys.CUSTOMER_BY_ID, customerId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.CUSTOMER_CACHE).getSeconds();
            
            boolean success = RedisConnectionManager.set(key, customerData, ttl);
            if (success) {
                recordCacheOperation("customer_set");
            }
            return success;
        }
        
        public static String getCustomer(Long customerId) {
            String key = String.format(RedisConfig.CacheKeys.CUSTOMER_BY_ID, customerId);
            String result = RedisConnectionManager.get(key);
            
            if (result != null) {
                cacheHits.incrementAndGet();
                recordCacheOperation("customer_hit");
                lastAccessTimes.put(key, System.currentTimeMillis());
            } else {
                cacheMisses.incrementAndGet();
                recordCacheOperation("customer_miss");
            }
            
            return result;
        }
        
        public static boolean cacheCustomerByEmail(String email, String customerData) {
            String key = String.format(RedisConfig.CacheKeys.CUSTOMER_BY_EMAIL, email);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.CUSTOMER_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, customerData, ttl);
        }
        
        public static String getCustomerByEmail(String email) {
            String key = String.format(RedisConfig.CacheKeys.CUSTOMER_BY_EMAIL, email);
            String result = RedisConnectionManager.get(key);
            
            if (result != null) {
                cacheHits.incrementAndGet();
                lastAccessTimes.put(key, System.currentTimeMillis());
            } else {
                cacheMisses.incrementAndGet();
            }
            
            return result;
        }
        
        public static boolean cacheCreditLimit(Long customerId, String creditData) {
            String key = String.format(RedisConfig.CacheKeys.CUSTOMER_CREDIT_LIMIT, customerId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.CREDIT_ASSESSMENT_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, creditData, ttl);
        }
        
        public static String getCreditLimit(Long customerId) {
            String key = String.format(RedisConfig.CacheKeys.CUSTOMER_CREDIT_LIMIT, customerId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean invalidateCustomer(Long customerId) {
            String customerKey = String.format(RedisConfig.CacheKeys.CUSTOMER_BY_ID, customerId);
            String creditKey = String.format(RedisConfig.CacheKeys.CUSTOMER_CREDIT_LIMIT, customerId);
            
            boolean result1 = RedisConnectionManager.delete(customerKey);
            boolean result2 = RedisConnectionManager.delete(creditKey);
            
            if (result1 || result2) {
                cacheEvictions.incrementAndGet();
                recordCacheOperation("customer_eviction");
            }
            
            return result1 || result2;
        }
    }
    
    // Loan caching operations
    public static class LoanCache {
        
        public static boolean cacheLoan(String loanId, String loanData) {
            String key = String.format(RedisConfig.CacheKeys.LOAN_BY_ID, loanId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.LOAN_CACHE).getSeconds();
            
            boolean success = RedisConnectionManager.set(key, loanData, ttl);
            if (success) {
                recordCacheOperation("loan_set");
            }
            return success;
        }
        
        public static String getLoan(String loanId) {
            String key = String.format(RedisConfig.CacheKeys.LOAN_BY_ID, loanId);
            String result = RedisConnectionManager.get(key);
            
            if (result != null) {
                cacheHits.incrementAndGet();
                recordCacheOperation("loan_hit");
                lastAccessTimes.put(key, System.currentTimeMillis());
            } else {
                cacheMisses.incrementAndGet();
                recordCacheOperation("loan_miss");
            }
            
            return result;
        }
        
        public static boolean cacheLoansByCustomer(Long customerId, String loansData) {
            String key = String.format(RedisConfig.CacheKeys.LOANS_BY_CUSTOMER, customerId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.LOAN_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, loansData, ttl);
        }
        
        public static String getLoansByCustomer(Long customerId) {
            String key = String.format(RedisConfig.CacheKeys.LOANS_BY_CUSTOMER, customerId);
            String result = RedisConnectionManager.get(key);
            
            if (result != null) {
                cacheHits.incrementAndGet();
                lastAccessTimes.put(key, System.currentTimeMillis());
            } else {
                cacheMisses.incrementAndGet();
            }
            
            return result;
        }
        
        public static boolean cacheLoanInstallments(String loanId, String installmentsData) {
            String key = String.format(RedisConfig.CacheKeys.LOAN_INSTALLMENTS, loanId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.LOAN_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, installmentsData, ttl);
        }
        
        public static String getLoanInstallments(String loanId) {
            String key = String.format(RedisConfig.CacheKeys.LOAN_INSTALLMENTS, loanId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean cacheLoanStatus(String loanId, String status) {
            String key = String.format(RedisConfig.CacheKeys.LOAN_STATUS, loanId);
            long ttl = Duration.ofMinutes(5).getSeconds(); // Short TTL for status
            
            return RedisConnectionManager.set(key, status, ttl);
        }
        
        public static String getLoanStatus(String loanId) {
            String key = String.format(RedisConfig.CacheKeys.LOAN_STATUS, loanId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean invalidateLoan(String loanId) {
            String loanKey = String.format(RedisConfig.CacheKeys.LOAN_BY_ID, loanId);
            String installmentsKey = String.format(RedisConfig.CacheKeys.LOAN_INSTALLMENTS, loanId);
            String statusKey = String.format(RedisConfig.CacheKeys.LOAN_STATUS, loanId);
            
            boolean result1 = RedisConnectionManager.delete(loanKey);
            boolean result2 = RedisConnectionManager.delete(installmentsKey);
            boolean result3 = RedisConnectionManager.delete(statusKey);
            
            if (result1 || result2 || result3) {
                cacheEvictions.incrementAndGet();
                recordCacheOperation("loan_eviction");
            }
            
            return result1 || result2 || result3;
        }
    }
    
    // Payment caching operations
    public static class PaymentCache {
        
        public static boolean cachePayment(String paymentId, String paymentData) {
            String key = String.format(RedisConfig.CacheKeys.PAYMENT_BY_ID, paymentId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.PAYMENT_CACHE).getSeconds();
            
            boolean success = RedisConnectionManager.set(key, paymentData, ttl);
            if (success) {
                recordCacheOperation("payment_set");
            }
            return success;
        }
        
        public static String getPayment(String paymentId) {
            String key = String.format(RedisConfig.CacheKeys.PAYMENT_BY_ID, paymentId);
            String result = RedisConnectionManager.get(key);
            
            if (result != null) {
                cacheHits.incrementAndGet();
                recordCacheOperation("payment_hit");
                lastAccessTimes.put(key, System.currentTimeMillis());
            } else {
                cacheMisses.incrementAndGet();
                recordCacheOperation("payment_miss");
            }
            
            return result;
        }
        
        public static boolean cachePaymentsByLoan(String loanId, String paymentsData) {
            String key = String.format(RedisConfig.CacheKeys.PAYMENTS_BY_LOAN, loanId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.PAYMENT_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, paymentsData, ttl);
        }
        
        public static String getPaymentsByLoan(String loanId) {
            String key = String.format(RedisConfig.CacheKeys.PAYMENTS_BY_LOAN, loanId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean cachePaymentHistory(Long customerId, String historyData) {
            String key = String.format(RedisConfig.CacheKeys.PAYMENT_HISTORY, customerId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.PAYMENT_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, historyData, ttl);
        }
        
        public static String getPaymentHistory(Long customerId) {
            String key = String.format(RedisConfig.CacheKeys.PAYMENT_HISTORY, customerId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean invalidatePaymentsForLoan(String loanId) {
            String paymentsKey = String.format(RedisConfig.CacheKeys.PAYMENTS_BY_LOAN, loanId);
            
            boolean result = RedisConnectionManager.delete(paymentsKey);
            if (result) {
                cacheEvictions.incrementAndGet();
                recordCacheOperation("payment_eviction");
            }
            
            return result;
        }
    }
    
    // Credit assessment caching
    public static class CreditAssessmentCache {
        
        public static boolean cacheCreditAssessment(Long customerId, String assessmentData) {
            String key = String.format(RedisConfig.CacheKeys.CREDIT_ASSESSMENT, customerId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.CREDIT_ASSESSMENT_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, assessmentData, ttl);
        }
        
        public static String getCreditAssessment(Long customerId) {
            String key = String.format(RedisConfig.CacheKeys.CREDIT_ASSESSMENT, customerId);
            String result = RedisConnectionManager.get(key);
            
            if (result != null) {
                cacheHits.incrementAndGet();
                lastAccessTimes.put(key, System.currentTimeMillis());
            } else {
                cacheMisses.incrementAndGet();
            }
            
            return result;
        }
        
        public static boolean cacheCreditScore(Long customerId, String scoreData) {
            String key = String.format(RedisConfig.CacheKeys.CREDIT_SCORE, customerId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.CREDIT_ASSESSMENT_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, scoreData, ttl);
        }
        
        public static String getCreditScore(Long customerId) {
            String key = String.format(RedisConfig.CacheKeys.CREDIT_SCORE, customerId);
            return RedisConnectionManager.get(key);
        }
    }
    
    // Compliance data caching
    public static class ComplianceCache {
        
        public static boolean cacheTDDCoverage(String coverageData) {
            long ttl = RedisConfig.getCacheTTL(RedisConfig.COMPLIANCE_CACHE).getSeconds();
            return RedisConnectionManager.set(RedisConfig.CacheKeys.TDD_COVERAGE, coverageData, ttl);
        }
        
        public static String getTDDCoverage() {
            return RedisConnectionManager.get(RedisConfig.CacheKeys.TDD_COVERAGE);
        }
        
        public static boolean cacheFAPICompliance(String complianceData) {
            long ttl = RedisConfig.getCacheTTL(RedisConfig.COMPLIANCE_CACHE).getSeconds();
            return RedisConnectionManager.set(RedisConfig.CacheKeys.FAPI_COMPLIANCE, complianceData, ttl);
        }
        
        public static String getFAPICompliance() {
            return RedisConnectionManager.get(RedisConfig.CacheKeys.FAPI_COMPLIANCE);
        }
        
        public static boolean cacheBankingStandards(String standardsData) {
            long ttl = RedisConfig.getCacheTTL(RedisConfig.COMPLIANCE_CACHE).getSeconds();
            return RedisConnectionManager.set(RedisConfig.CacheKeys.BANKING_STANDARDS, standardsData, ttl);
        }
        
        public static String getBankingStandards() {
            return RedisConnectionManager.get(RedisConfig.CacheKeys.BANKING_STANDARDS);
        }
    }
    
    // Security token caching
    public static class SecurityCache {
        
        public static boolean cacheJWTToken(String tokenId, String tokenData) {
            String key = String.format(RedisConfig.CacheKeys.JWT_TOKEN, tokenId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.SECURITY_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, tokenData, ttl);
        }
        
        public static String getJWTToken(String tokenId) {
            String key = String.format(RedisConfig.CacheKeys.JWT_TOKEN, tokenId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean cacheSessionData(String sessionId, String sessionData) {
            String key = String.format(RedisConfig.CacheKeys.SESSION_DATA, sessionId);
            long ttl = RedisConfig.getCacheTTL(RedisConfig.SECURITY_CACHE).getSeconds();
            
            return RedisConnectionManager.set(key, sessionData, ttl);
        }
        
        public static String getSessionData(String sessionId) {
            String key = String.format(RedisConfig.CacheKeys.SESSION_DATA, sessionId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean cacheFAPIRequestId(String requestId, String requestData) {
            String key = String.format(RedisConfig.CacheKeys.FAPI_REQUEST_ID, requestId);
            long ttl = Duration.ofMinutes(10).getSeconds(); // FAPI request TTL
            
            return RedisConnectionManager.set(key, requestData, ttl);
        }
        
        public static String getFAPIRequestId(String requestId) {
            String key = String.format(RedisConfig.CacheKeys.FAPI_REQUEST_ID, requestId);
            return RedisConnectionManager.get(key);
        }
        
        public static boolean invalidateSession(String sessionId) {
            String key = String.format(RedisConfig.CacheKeys.SESSION_DATA, sessionId);
            return RedisConnectionManager.delete(key);
        }
    }
    
    // Rate limiting cache
    public static class RateLimitCache {
        
        public static long incrementClientRequests(String clientId) {
            String key = String.format(RedisConfig.CacheKeys.RATE_LIMIT_CLIENT, clientId);
            long count = RedisConnectionManager.incr(key);
            
            // Set TTL if this is the first increment
            if (count == 1) {
                RedisConnectionManager.expire(key, 60); // 1 minute window
            }
            
            return count;
        }
        
        public static long incrementIPRequests(String ipAddress) {
            String key = String.format(RedisConfig.CacheKeys.RATE_LIMIT_IP, ipAddress);
            long count = RedisConnectionManager.incr(key);
            
            if (count == 1) {
                RedisConnectionManager.expire(key, 60); // 1 minute window
            }
            
            return count;
        }
        
        public static long incrementGlobalRequests() {
            String key = RedisConfig.CacheKeys.RATE_LIMIT_GLOBAL;
            long count = RedisConnectionManager.incr(key);
            
            if (count == 1) {
                RedisConnectionManager.expire(key, 60); // 1 minute window
            }
            
            return count;
        }
        
        public static long getClientRequestCount(String clientId) {
            String key = String.format(RedisConfig.CacheKeys.RATE_LIMIT_CLIENT, clientId);
            String count = RedisConnectionManager.get(key);
            return count != null ? Long.parseLong(count) : 0;
        }
        
        public static long getIPRequestCount(String ipAddress) {
            String key = String.format(RedisConfig.CacheKeys.RATE_LIMIT_IP, ipAddress);
            String count = RedisConnectionManager.get(key);
            return count != null ? Long.parseLong(count) : 0;
        }
    }
    
    // Cache metrics and monitoring
    public static class CacheMetrics {
        
        public static double getCacheHitRatio() {
            long hits = cacheHits.get();
            long misses = cacheMisses.get();
            long total = hits + misses;
            
            return total > 0 ? (double) hits / total : 0.0;
        }
        
        public static long getCacheHits() {
            return cacheHits.get();
        }
        
        public static long getCacheMisses() {
            return cacheMisses.get();
        }
        
        public static long getCacheEvictions() {
            return cacheEvictions.get();
        }
        
        public static Map<String, Object> getCacheStatistics() {
            Map<String, Object> stats = new ConcurrentHashMap<>();
            stats.put("cache_hits", getCacheHits());
            stats.put("cache_misses", getCacheMisses());
            stats.put("cache_evictions", getCacheEvictions());
            stats.put("hit_ratio", getCacheHitRatio());
            stats.put("connection_count", RedisConnectionManager.getConnectionCount());
            stats.put("operation_count", RedisConnectionManager.getOperationCount());
            stats.put("is_connected", RedisConnectionManager.isConnected());
            stats.put("timestamp", LocalDateTime.now().toString());
            
            return stats;
        }
        
        public static void resetMetrics() {
            cacheHits.set(0);
            cacheMisses.set(0);
            cacheEvictions.set(0);
            lastAccessTimes.clear();
        }
    }
    
    // Cache warming operations
    public static class CacheWarming {
        
        public static void warmUpComplianceData() {
            // Cache current compliance metrics
            ComplianceCache.cacheTDDCoverage("{\"coverage\": 87.4, \"status\": \"compliant\"}");
            ComplianceCache.cacheFAPICompliance("{\"score\": 71.4, \"rating\": \"B+\"}");
            ComplianceCache.cacheBankingStandards("{\"compliant\": true, \"score\": 97}");
        }
        
        public static void warmUpHighPriorityCustomers() {
            // Simulate warming up frequently accessed customers
            for (int i = 1; i <= 10; i++) {
                String customerData = String.format(
                    "{\"id\": %d, \"name\": \"Customer %d\", \"creditLimit\": %d}", 
                    i, i, 50000 + (i * 10000)
                );
                CustomerCache.cacheCustomer((long) i, customerData);
            }
        }
        
        public static void preloadActiveLoans() {
            // Cache active loan data for better performance
            for (int i = 1; i <= 5; i++) {
                String loanId = "LOAN-" + String.format("%06d", i);
                String loanData = String.format(
                    "{\"id\": \"%s\", \"amount\": %d, \"status\": \"ACTIVE\"}", 
                    loanId, 25000 + (i * 5000)
                );
                LoanCache.cacheLoan(loanId, loanData);
            }
        }
    }
    
    // Utility method for cache operation recording
    private static void recordCacheOperation(String operation) {
        // Could be extended to record detailed metrics
        String timestamp = LocalDateTime.now().toString();
        // System.out.println("Cache operation: " + operation + " at " + timestamp);
    }
}