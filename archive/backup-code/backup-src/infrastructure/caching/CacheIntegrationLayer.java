package com.bank.loanmanagement.infrastructure.caching;

import com.bank.loanmanagement.config.RedisConfig;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;

public class CacheIntegrationLayer {
    
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
    private static volatile boolean cacheEnabled = true;
    
    // Initialize Redis connection and warm up cache
    public static void initialize() {
        System.out.println("Initializing Redis ElastiCache integration...");
        
        if (RedisConnectionManager.connect()) {
            System.out.println("Redis connection established successfully");
            
            // Warm up critical cache data
            warmUpCache();
            
            System.out.println("Cache warming completed");
        } else {
            System.err.println("Failed to establish Redis connection - operating without cache");
            cacheEnabled = false;
        }
    }
    
    // Generic cache-aside pattern implementation
    public static <T> T getOrCompute(String key, Supplier<T> dataSupplier, String cacheType) {
        if (!cacheEnabled) {
            return dataSupplier.get();
        }
        
        try {
            // Try to get from cache first
            String cachedValue = RedisConnectionManager.get(key);
            if (cachedValue != null) {
                return parseFromJson(cachedValue);
            }
            
            // Cache miss - compute value
            T computedValue = dataSupplier.get();
            if (computedValue != null) {
                // Cache the computed value asynchronously
                CompletableFuture.runAsync(() -> {
                    String jsonValue = toJson(computedValue);
                    long ttl = RedisConfig.getCacheTTL(cacheType).getSeconds();
                    RedisConnectionManager.set(key, jsonValue, ttl);
                }, asyncExecutor);
            }
            
            return computedValue;
            
        } catch (Exception e) {
            System.err.println("Cache operation failed: " + e.getMessage());
            // Fall back to direct computation
            return dataSupplier.get();
        }
    }
    
    // Write-through cache pattern
    public static <T> boolean setWithWriteThrough(String key, T value, String cacheType, 
                                                  Supplier<Boolean> databaseWriter) {
        try {
            // Write to database first
            boolean dbSuccess = databaseWriter.get();
            if (!dbSuccess) {
                return false;
            }
            
            // Write to cache if database operation succeeded
            if (cacheEnabled && value != null) {
                String jsonValue = toJson(value);
                long ttl = RedisConfig.getCacheTTL(cacheType).getSeconds();
                RedisConnectionManager.set(key, jsonValue, ttl);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Write-through cache operation failed: " + e.getMessage());
            return false;
        }
    }
    
    // Write-behind cache pattern (eventual consistency)
    public static <T> boolean setWithWriteBehind(String key, T value, String cacheType, 
                                                 Supplier<Boolean> databaseWriter) {
        try {
            // Write to cache immediately
            if (cacheEnabled && value != null) {
                String jsonValue = toJson(value);
                long ttl = RedisConfig.getCacheTTL(cacheType).getSeconds();
                boolean cacheSuccess = RedisConnectionManager.set(key, jsonValue, ttl);
                
                if (cacheSuccess) {
                    // Schedule asynchronous database write
                    CompletableFuture.runAsync(() -> {
                        try {
                            databaseWriter.get();
                        } catch (Exception e) {
                            System.err.println("Asynchronous database write failed: " + e.getMessage());
                            // Could implement retry logic here
                        }
                    }, asyncExecutor);
                    
                    return true;
                }
            }
            
            // Fallback to synchronous database write
            return databaseWriter.get();
            
        } catch (Exception e) {
            System.err.println("Write-behind cache operation failed: " + e.getMessage());
            return databaseWriter.get();
        }
    }
    
    // Cache invalidation with pattern matching
    public static void invalidatePattern(String pattern) {
        if (!cacheEnabled) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                // Simple pattern matching for common cases
                if (pattern.contains("customer:")) {
                    // Invalidate customer-related caches
                    invalidateCustomerCaches(pattern);
                } else if (pattern.contains("loan:")) {
                    // Invalidate loan-related caches
                    invalidateLoanCaches(pattern);
                } else if (pattern.contains("payment:")) {
                    // Invalidate payment-related caches
                    invalidatePaymentCaches(pattern);
                }
            } catch (Exception e) {
                System.err.println("Cache invalidation failed: " + e.getMessage());
            }
        }, asyncExecutor);
    }
    
    // Multi-level cache strategy for hot data
    public static class MultiLevelCache {
        private static final int L1_CACHE_SIZE = 100;
        private static final java.util.Map<String, CacheEntry> l1Cache = 
            new java.util.concurrent.ConcurrentHashMap<>(L1_CACHE_SIZE);
        
        public static <T> T get(String key, Supplier<T> dataSupplier, String cacheType) {
            // Level 1: In-memory cache (fastest)
            CacheEntry l1Entry = l1Cache.get(key);
            if (l1Entry != null && !l1Entry.isExpired()) {
                return (T) l1Entry.getValue();
            }
            
            // Level 2: Redis cache
            if (cacheEnabled) {
                String cachedValue = RedisConnectionManager.get(key);
                if (cachedValue != null) {
                    T value = parseFromJson(cachedValue);
                    // Store in L1 cache for faster access
                    l1Cache.put(key, new CacheEntry(value, System.currentTimeMillis() + 60000)); // 1 minute L1 TTL
                    return value;
                }
            }
            
            // Level 3: Database/computation
            T computedValue = dataSupplier.get();
            if (computedValue != null) {
                // Cache in both levels
                l1Cache.put(key, new CacheEntry(computedValue, System.currentTimeMillis() + 60000));
                
                if (cacheEnabled) {
                    CompletableFuture.runAsync(() -> {
                        String jsonValue = toJson(computedValue);
                        long ttl = RedisConfig.getCacheTTL(cacheType).getSeconds();
                        RedisConnectionManager.set(key, jsonValue, ttl);
                    }, asyncExecutor);
                }
            }
            
            return computedValue;
        }
        
        public static void invalidate(String key) {
            l1Cache.remove(key);
            if (cacheEnabled) {
                RedisConnectionManager.delete(key);
            }
        }
        
        public static void clearL1Cache() {
            l1Cache.clear();
        }
        
        private static class CacheEntry {
            private final Object value;
            private final long expiryTime;
            
            public CacheEntry(Object value, long expiryTime) {
                this.value = value;
                this.expiryTime = expiryTime;
            }
            
            public Object getValue() {
                return value;
            }
            
            public boolean isExpired() {
                return System.currentTimeMillis() > expiryTime;
            }
        }
    }
    
    // Banking-specific caching strategies
    public static class BankingCacheStrategies {
        
        // Customer data with credit score caching
        public static String getCachedCustomerWithCredit(Long customerId) {
            return MultiLevelCache.get(
                String.format("customer:complete:%d", customerId),
                () -> {
                    // Simulate complex customer data aggregation
                    String customer = BankingCacheService.CustomerCache.getCustomer(customerId);
                    String creditData = BankingCacheService.CreditAssessmentCache.getCreditScore(customerId);
                    
                    if (customer != null && creditData != null) {
                        return combineCustomerAndCredit(customer, creditData);
                    } else if (customer != null) {
                        return customer;
                    }
                    
                    return loadCustomerFromDatabase(customerId);
                },
                RedisConfig.CUSTOMER_CACHE
            );
        }
        
        // Loan with payment history caching
        public static String getCachedLoanWithPayments(String loanId) {
            return MultiLevelCache.get(
                String.format("loan:complete:%s", loanId),
                () -> {
                    String loan = BankingCacheService.LoanCache.getLoan(loanId);
                    String payments = BankingCacheService.PaymentCache.getPaymentsByLoan(loanId);
                    
                    if (loan != null && payments != null) {
                        return combineLoanAndPayments(loan, payments);
                    } else if (loan != null) {
                        return loan;
                    }
                    
                    return loadLoanFromDatabase(loanId);
                },
                RedisConfig.LOAN_CACHE
            );
        }
        
        // Compliance data with aggregated metrics
        public static String getCachedComplianceReport() {
            return getOrCompute(
                "compliance:full_report",
                () -> {
                    String tddCoverage = BankingCacheService.ComplianceCache.getTDDCoverage();
                    String fapiCompliance = BankingCacheService.ComplianceCache.getFAPICompliance();
                    String bankingStandards = BankingCacheService.ComplianceCache.getBankingStandards();
                    
                    return aggregateComplianceData(tddCoverage, fapiCompliance, bankingStandards);
                },
                RedisConfig.COMPLIANCE_CACHE
            );
        }
        
        // Rate limiting with sliding window
        public static boolean checkRateLimit(String clientId, String ipAddress, int maxRequests) {
            try {
                long clientCount = BankingCacheService.RateLimitCache.incrementClientRequests(clientId);
                long ipCount = BankingCacheService.RateLimitCache.incrementIPRequests(ipAddress);
                
                return clientCount <= maxRequests && ipCount <= (maxRequests * 2); // Allow more requests per IP
                
            } catch (Exception e) {
                System.err.println("Rate limiting check failed: " + e.getMessage());
                return true; // Allow request if rate limiting fails
            }
        }
    }
    
    // Cache warming strategy
    private static void warmUpCache() {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Starting cache warm-up process...");
                
                // Warm up compliance data
                BankingCacheService.CacheWarming.warmUpComplianceData();
                
                // Warm up high-priority customer data
                BankingCacheService.CacheWarming.warmUpHighPriorityCustomers();
                
                // Warm up active loans
                BankingCacheService.CacheWarming.preloadActiveLoans();
                
                System.out.println("Cache warm-up completed successfully");
                
            } catch (Exception e) {
                System.err.println("Cache warm-up failed: " + e.getMessage());
            }
        }, asyncExecutor);
    }
    
    // Cache invalidation helpers
    private static void invalidateCustomerCaches(String pattern) {
        // Extract customer ID and invalidate related caches
        if (pattern.contains("customer:id:")) {
            String customerIdStr = pattern.substring(pattern.lastIndexOf(":") + 1);
            try {
                Long customerId = Long.valueOf(customerIdStr);
                BankingCacheService.CustomerCache.invalidateCustomer(customerId);
                MultiLevelCache.invalidate(String.format("customer:complete:%d", customerId));
            } catch (NumberFormatException e) {
                System.err.println("Invalid customer ID in pattern: " + pattern);
            }
        }
    }
    
    private static void invalidateLoanCaches(String pattern) {
        if (pattern.contains("loan:id:")) {
            String loanId = pattern.substring(pattern.lastIndexOf(":") + 1);
            BankingCacheService.LoanCache.invalidateLoan(loanId);
            BankingCacheService.PaymentCache.invalidatePaymentsForLoan(loanId);
            MultiLevelCache.invalidate(String.format("loan:complete:%s", loanId));
        }
    }
    
    private static void invalidatePaymentCaches(String pattern) {
        if (pattern.contains("payment:loan:")) {
            String loanId = pattern.substring(pattern.lastIndexOf(":") + 1);
            BankingCacheService.PaymentCache.invalidatePaymentsForLoan(loanId);
        }
    }
    
    // JSON serialization helpers (simplified)
    private static String toJson(Object obj) {
        if (obj == null) return null;
        return obj.toString(); // Simplified - in production use proper JSON library
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T parseFromJson(String json) {
        if (json == null) return null;
        return (T) json; // Simplified - in production use proper JSON parsing
    }
    
    // Mock database operations for demonstration
    private static String loadCustomerFromDatabase(Long customerId) {
        return String.format("{\"id\": %d, \"name\": \"Customer %d\", \"loaded_from\": \"database\"}", 
                           customerId, customerId);
    }
    
    private static String loadLoanFromDatabase(String loanId) {
        return String.format("{\"id\": \"%s\", \"status\": \"ACTIVE\", \"loaded_from\": \"database\"}", loanId);
    }
    
    // Data combination helpers
    private static String combineCustomerAndCredit(String customer, String creditData) {
        return String.format("{\"customer\": %s, \"credit\": %s}", customer, creditData);
    }
    
    private static String combineLoanAndPayments(String loan, String payments) {
        return String.format("{\"loan\": %s, \"payments\": %s}", loan, payments);
    }
    
    private static String aggregateComplianceData(String tdd, String fapi, String banking) {
        return String.format("{\"tdd\": %s, \"fapi\": %s, \"banking\": %s, \"aggregated_at\": \"%s\"}", 
                           tdd, fapi, banking, LocalDateTime.now());
    }
    
    // Health check and metrics
    public static boolean isHealthy() {
        if (!cacheEnabled) return false;
        return RedisConnectionManager.ping();
    }
    
    public static java.util.Map<String, Object> getCacheMetrics() {
        java.util.Map<String, Object> metrics = BankingCacheService.CacheMetrics.getCacheStatistics();
        metrics.put("cache_enabled", cacheEnabled);
        metrics.put("l1_cache_size", MultiLevelCache.l1Cache.size());
        return metrics;
    }
    
    // Graceful shutdown
    public static void shutdown() {
        System.out.println("Shutting down cache integration layer...");
        asyncExecutor.shutdown();
        RedisConnectionManager.disconnect();
        MultiLevelCache.clearL1Cache();
        System.out.println("Cache integration layer shutdown completed");
    }
}