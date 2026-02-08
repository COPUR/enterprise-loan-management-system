package com.amanahfi.platform.shared.cache;

import com.amanahfi.platform.islamicfinance.domain.IslamicFinanceProduct;
import com.amanahfi.platform.islamicfinance.domain.ShariaComplianceDetails;
import com.amanahfi.platform.shared.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IslamicFinanceCacheService {

    private static final Logger logger = LoggerFactory.getLogger(IslamicFinanceCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    public IslamicFinanceCacheService(RedisTemplate<String, Object> redisTemplate,
                                    RedisTemplate<String, String> stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // Islamic Finance Product Caching
    @Cacheable(value = "islamic-finance-products", key = "#productId", unless = "#result == null")
    public IslamicFinanceProduct getIslamicFinanceProduct(UUID productId) {
        logger.debug("Cache miss for Islamic finance product: {}", productId);
        return null; // This will be populated by the calling service
    }

    @CachePut(value = "islamic-finance-products", key = "#product.productId")
    public IslamicFinanceProduct putIslamicFinanceProduct(IslamicFinanceProduct product) {
        logger.debug("Caching Islamic finance product: {}", product.getProductId());
        return product;
    }

    @CacheEvict(value = "islamic-finance-products", key = "#productId")
    public void evictIslamicFinanceProduct(UUID productId) {
        logger.debug("Evicting Islamic finance product from cache: {}", productId);
    }

    // Murabaha Calculation Caching
    @Cacheable(value = "murabaha-calculations", key = "#assetCost.amount + '_' + #profitMargin + '_' + #termMonths")
    public MurabahaCalculationResult getMurabahaCalculation(Money assetCost, BigDecimal profitMargin, int termMonths) {
        logger.debug("Cache miss for Murabaha calculation: asset={}, margin={}, term={}", 
                    assetCost.getAmount(), profitMargin, termMonths);
        return null;
    }

    @CachePut(value = "murabaha-calculations", key = "#assetCost.amount + '_' + #profitMargin + '_' + #termMonths")
    public MurabahaCalculationResult putMurabahaCalculation(Money assetCost, BigDecimal profitMargin, 
                                                          int termMonths, MurabahaCalculationResult result) {
        logger.debug("Caching Murabaha calculation result");
        return result;
    }

    // Sharia Compliance Caching
    @Cacheable(value = "sharia-compliance-rules", key = "#productType + '_' + #jurisdiction")
    public ShariaComplianceRules getShariaComplianceRules(String productType, String jurisdiction) {
        logger.debug("Cache miss for Sharia compliance rules: type={}, jurisdiction={}", productType, jurisdiction);
        return null;
    }

    @CachePut(value = "sharia-compliance-rules", key = "#productType + '_' + #jurisdiction")
    public ShariaComplianceRules putShariaComplianceRules(String productType, String jurisdiction, 
                                                         ShariaComplianceRules rules) {
        logger.debug("Caching Sharia compliance rules for type={}, jurisdiction={}", productType, jurisdiction);
        return rules;
    }

    // Profit Margin Limits Caching
    @Cacheable(value = "profit-margin-limits", key = "#productType + '_' + #riskCategory")
    public ProfitMarginLimits getProfitMarginLimits(String productType, String riskCategory) {
        logger.debug("Cache miss for profit margin limits: type={}, risk={}", productType, riskCategory);
        return null;
    }

    @CachePut(value = "profit-margin-limits", key = "#productType + '_' + #riskCategory")
    public ProfitMarginLimits putProfitMarginLimits(String productType, String riskCategory, 
                                                   ProfitMarginLimits limits) {
        logger.debug("Caching profit margin limits for type={}, risk={}", productType, riskCategory);
        return limits;
    }

    // Customer Islamic Finance Eligibility Caching
    public void cacheCustomerEligibility(UUID customerId, IslamicFinanceEligibility eligibility) {
        String key = "customer:eligibility:" + customerId;
        redisTemplate.opsForValue().set(key, eligibility, Duration.ofMinutes(30));
        logger.debug("Cached customer eligibility for customer: {}", customerId);
    }

    public IslamicFinanceEligibility getCustomerEligibility(UUID customerId) {
        String key = "customer:eligibility:" + customerId;
        IslamicFinanceEligibility eligibility = (IslamicFinanceEligibility) redisTemplate.opsForValue().get(key);
        if (eligibility != null) {
            logger.debug("Cache hit for customer eligibility: {}", customerId);
        } else {
            logger.debug("Cache miss for customer eligibility: {}", customerId);
        }
        return eligibility;
    }

    // Asset Halal Status Caching
    public void cacheAssetHalalStatus(String assetCategory, String assetSpecification, boolean isHalal) {
        String key = "asset:halal:" + assetCategory + ":" + assetSpecification.hashCode();
        stringRedisTemplate.opsForValue().set(key, String.valueOf(isHalal), Duration.ofHours(24));
        logger.debug("Cached asset Halal status: category={}, spec={}, halal={}", 
                    assetCategory, assetSpecification, isHalal);
    }

    public Boolean getAssetHalalStatus(String assetCategory, String assetSpecification) {
        String key = "asset:halal:" + assetCategory + ":" + assetSpecification.hashCode();
        String status = stringRedisTemplate.opsForValue().get(key);
        if (status != null) {
            logger.debug("Cache hit for asset Halal status: category={}, spec={}", assetCategory, assetSpecification);
            return Boolean.valueOf(status);
        }
        logger.debug("Cache miss for asset Halal status: category={}, spec={}", assetCategory, assetSpecification);
        return null;
    }

    // Payment Schedule Caching
    public void cachePaymentSchedule(UUID financingId, PaymentSchedule schedule) {
        String key = "payment:schedule:" + financingId;
        redisTemplate.opsForValue().set(key, schedule, Duration.ofHours(2));
        logger.debug("Cached payment schedule for financing: {}", financingId);
    }

    public PaymentSchedule getPaymentSchedule(UUID financingId) {
        String key = "payment:schedule:" + financingId;
        PaymentSchedule schedule = (PaymentSchedule) redisTemplate.opsForValue().get(key);
        if (schedule != null) {
            logger.debug("Cache hit for payment schedule: {}", financingId);
        } else {
            logger.debug("Cache miss for payment schedule: {}", financingId);
        }
        return schedule;
    }

    // Supplier Verification Status Caching
    public void cacheSupplierVerification(String supplierRegistrationNumber, SupplierVerificationStatus status) {
        String key = "supplier:verification:" + supplierRegistrationNumber;
        redisTemplate.opsForValue().set(key, status, Duration.ofHours(6));
        logger.debug("Cached supplier verification status: {}", supplierRegistrationNumber);
    }

    public SupplierVerificationStatus getSupplierVerification(String supplierRegistrationNumber) {
        String key = "supplier:verification:" + supplierRegistrationNumber;
        SupplierVerificationStatus status = (SupplierVerificationStatus) redisTemplate.opsForValue().get(key);
        if (status != null) {
            logger.debug("Cache hit for supplier verification: {}", supplierRegistrationNumber);
        } else {
            logger.debug("Cache miss for supplier verification: {}", supplierRegistrationNumber);
        }
        return status;
    }

    // Sharia Board Approval Cache
    public void cacheShariaApproval(UUID productId, ShariaApprovalStatus approval) {
        String key = "sharia:approval:" + productId;
        redisTemplate.opsForValue().set(key, approval, Duration.ofHours(48));
        logger.debug("Cached Sharia approval for product: {}", productId);
    }

    public ShariaApprovalStatus getShariaApproval(UUID productId) {
        String key = "sharia:approval:" + productId;
        ShariaApprovalStatus approval = (ShariaApprovalStatus) redisTemplate.opsForValue().get(key);
        if (approval != null) {
            logger.debug("Cache hit for Sharia approval: {}", productId);
        } else {
            logger.debug("Cache miss for Sharia approval: {}", productId);
        }
        return approval;
    }

    // Risk Assessment Caching
    public void cacheRiskAssessment(UUID customerId, UUID productId, RiskAssessmentResult result) {
        String key = "risk:assessment:" + customerId + ":" + productId;
        redisTemplate.opsForValue().set(key, result, Duration.ofHours(4));
        logger.debug("Cached risk assessment for customer={}, product={}", customerId, productId);
    }

    public RiskAssessmentResult getRiskAssessment(UUID customerId, UUID productId) {
        String key = "risk:assessment:" + customerId + ":" + productId;
        RiskAssessmentResult result = (RiskAssessmentResult) redisTemplate.opsForValue().get(key);
        if (result != null) {
            logger.debug("Cache hit for risk assessment: customer={}, product={}", customerId, productId);
        } else {
            logger.debug("Cache miss for risk assessment: customer={}, product={}", customerId, productId);
        }
        return result;
    }

    // Islamic Finance Statistics Caching
    public void cacheIslamicFinanceStatistics(String period, IslamicFinanceStatistics stats) {
        String key = "islamic:finance:stats:" + period;
        redisTemplate.opsForValue().set(key, stats, Duration.ofMinutes(15));
        logger.debug("Cached Islamic finance statistics for period: {}", period);
    }

    public IslamicFinanceStatistics getIslamicFinanceStatistics(String period) {
        String key = "islamic:finance:stats:" + period;
        IslamicFinanceStatistics stats = (IslamicFinanceStatistics) redisTemplate.opsForValue().get(key);
        if (stats != null) {
            logger.debug("Cache hit for Islamic finance statistics: {}", period);
        } else {
            logger.debug("Cache miss for Islamic finance statistics: {}", period);
        }
        return stats;
    }

    // Cache warming methods
    public void warmupCache() {
        logger.info("Starting Islamic finance cache warmup");
        
        // Warm up commonly accessed data
        warmupShariaComplianceRules();
        warmupProfitMarginLimits();
        warmupAssetCategories();
        
        logger.info("Completed Islamic finance cache warmup");
    }

    private void warmupShariaComplianceRules() {
        // Preload common Sharia compliance rules
        String[] productTypes = {"MURABAHA", "MUSHARAKAH", "IJARAH", "SALAM", "ISTISNA", "QARD_HASSAN"};
        String[] jurisdictions = {"UAE", "SAU", "QAT", "KWT", "BHR", "OMN"};
        
        for (String productType : productTypes) {
            for (String jurisdiction : jurisdictions) {
                // This would typically load from database
                ShariaComplianceRules rules = createDefaultComplianceRules(productType, jurisdiction);
                putShariaComplianceRules(productType, jurisdiction, rules);
            }
        }
    }

    private void warmupProfitMarginLimits() {
        // Preload profit margin limits
        String[] productTypes = {"MURABAHA", "MUSHARAKAH", "IJARAH"};
        String[] riskCategories = {"LOW", "MEDIUM", "HIGH"};
        
        for (String productType : productTypes) {
            for (String riskCategory : riskCategories) {
                ProfitMarginLimits limits = createDefaultMarginLimits(productType, riskCategory);
                putProfitMarginLimits(productType, riskCategory, limits);
            }
        }
    }

    private void warmupAssetCategories() {
        // Preload common asset Halal status
        Map<String, Boolean> commonAssets = Map.of(
            "VEHICLE:PASSENGER_CAR", true,
            "VEHICLE:MOTORCYCLE", true,
            "REAL_ESTATE:RESIDENTIAL", true,
            "REAL_ESTATE:COMMERCIAL", true,
            "MACHINERY:MANUFACTURING", true,
            "COMMODITY:HALAL_FOOD", true,
            "COMMODITY:GOLD", true,
            "COMMODITY:AGRICULTURAL", true
        );
        
        for (Map.Entry<String, Boolean> entry : commonAssets.entrySet()) {
            String[] parts = entry.getKey().split(":");
            cacheAssetHalalStatus(parts[0], parts[1], entry.getValue());
        }
    }

    // Cache statistics and monitoring
    public CacheStatistics getCacheStatistics() {
        // Implementation would collect cache hit/miss ratios, memory usage, etc.
        return new CacheStatistics();
    }

    // Bulk cache operations
    @CacheEvict(value = {"islamic-finance-products", "murabaha-calculations", "sharia-compliance-rules"}, allEntries = true)
    public void evictAllIslamicFinanceCaches() {
        logger.info("Evicted all Islamic finance caches");
    }

    // Helper methods to create default objects (would typically load from database)
    private ShariaComplianceRules createDefaultComplianceRules(String productType, String jurisdiction) {
        return new ShariaComplianceRules(productType, jurisdiction, List.of(), LocalDateTime.now());
    }

    private ProfitMarginLimits createDefaultMarginLimits(String productType, String riskCategory) {
        BigDecimal maxMargin = switch (riskCategory) {
            case "LOW" -> new BigDecimal("0.15");
            case "MEDIUM" -> new BigDecimal("0.25");
            case "HIGH" -> new BigDecimal("0.30");
            default -> new BigDecimal("0.30");
        };
        return new ProfitMarginLimits(productType, riskCategory, BigDecimal.ZERO, maxMargin);
    }

    // Data classes for cached objects
    public record MurabahaCalculationResult(
        Money totalAmount,
        Money profitAmount,
        Money monthlyInstallment,
        int numberOfInstallments,
        LocalDateTime calculatedAt
    ) {}

    public record ShariaComplianceRules(
        String productType,
        String jurisdiction,
        List<String> rules,
        LocalDateTime lastUpdated
    ) {}

    public record ProfitMarginLimits(
        String productType,
        String riskCategory,
        BigDecimal minMargin,
        BigDecimal maxMargin
    ) {}

    public record IslamicFinanceEligibility(
        UUID customerId,
        boolean eligible,
        List<String> eligibleProducts,
        Money maxFinancingAmount,
        String riskCategory,
        LocalDateTime assessedAt
    ) {}

    public record PaymentSchedule(
        UUID financingId,
        List<PaymentInstallment> installments,
        LocalDateTime generatedAt
    ) {}

    public record PaymentInstallment(
        int installmentNumber,
        LocalDateTime dueDate,
        Money amount,
        Money principalAmount,
        Money profitAmount,
        String status
    ) {}

    public record SupplierVerificationStatus(
        String supplierRegistrationNumber,
        boolean verified,
        String verificationLevel,
        LocalDateTime verifiedAt,
        LocalDateTime expiresAt
    ) {}

    public record ShariaApprovalStatus(
        UUID productId,
        boolean approved,
        String shariaBoard,
        String certificateNumber,
        LocalDateTime approvedAt,
        LocalDateTime expiresAt
    ) {}

    public record RiskAssessmentResult(
        UUID customerId,
        UUID productId,
        String riskLevel,
        BigDecimal riskScore,
        List<String> riskFactors,
        LocalDateTime assessedAt
    ) {}

    public record IslamicFinanceStatistics(
        String period,
        long totalProducts,
        Money totalFinancingAmount,
        Map<String, Long> productBreakdown,
        double averageMargin,
        double complianceRate,
        LocalDateTime generatedAt
    ) {}

    public record CacheStatistics(
        long hitCount,
        long missCount,
        double hitRatio,
        long evictionCount,
        long memoryUsage
    ) {
        public CacheStatistics() {
            this(0L, 0L, 0.0, 0L, 0L);
        }
    }
}