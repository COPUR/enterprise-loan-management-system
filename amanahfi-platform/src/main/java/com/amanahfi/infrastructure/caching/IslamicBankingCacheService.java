package com.amanahfi.infrastructure.caching;

import com.bank.infrastructure.caching.BankingCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Islamic Banking Cache Service for AmanahFi Platform
 * 
 * Extends the enterprise BankingCacheService to provide Islamic finance-specific
 * caching capabilities including:
 * - Murabaha contract caching with Sharia compliance metadata
 * - Musharakah partnership data with profit/loss sharing information
 * - Ijarah lease agreements with asset ownership tracking
 * - Sharia compliance status caching for fast validation
 * - Islamic finance transaction patterns
 * - Halal asset verification cache
 * 
 * Islamic Finance Caching Strategy:
 * - Murabaha contracts: 2 hours TTL (contract data relatively stable)
 * - Musharakah partnerships: 1 hour TTL (profit sharing updates)
 * - Ijarah leases: 4 hours TTL (lease terms stable)
 * - Sharia compliance: 30 minutes TTL (compliance may change)
 * - Asset permissibility: 24 hours TTL (asset status rarely changes)
 * 
 * Cache Keys:
 * - islamic:murabaha:{contractId}
 * - islamic:musharakah:{partnershipId}
 * - islamic:ijarah:{leaseId}
 * - islamic:sharia:{transactionId}
 * - islamic:asset:{assetId}
 */
@Service
public class IslamicBankingCacheService extends BankingCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(IslamicBankingCacheService.class);
    
    // Islamic finance cache prefixes
    private static final String MURABAHA_PREFIX = "islamic:murabaha:";
    private static final String MUSHARAKAH_PREFIX = "islamic:musharakah:";
    private static final String IJARAH_PREFIX = "islamic:ijarah:";
    private static final String SHARIA_COMPLIANCE_PREFIX = "islamic:sharia:";
    private static final String ASSET_PERMISSIBILITY_PREFIX = "islamic:asset:";
    
    // Islamic finance TTL configurations
    private static final Duration MURABAHA_TTL = Duration.ofHours(2);
    private static final Duration MUSHARAKAH_TTL = Duration.ofHours(1);
    private static final Duration IJARAH_TTL = Duration.ofHours(4);
    private static final Duration SHARIA_COMPLIANCE_TTL = Duration.ofMinutes(30);
    private static final Duration ASSET_PERMISSIBILITY_TTL = Duration.ofHours(24);
    
    public IslamicBankingCacheService(RedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
    }
    
    /**
     * Cache Murabaha contract data with Islamic finance metadata
     * 
     * @param contractId the Murabaha contract ID
     * @param contractData the contract data including profit margin and asset details
     */
    public void cacheMurabahaContract(String contractId, String contractData) {
        String key = MURABAHA_PREFIX + contractId;
        redisTemplate.opsForValue().set(key, contractData, MURABAHA_TTL);
        logger.debug("Cached Murabaha contract data for ID: {}", contractId);
    }
    
    /**
     * Retrieve Murabaha contract data from cache
     * 
     * @param contractId the Murabaha contract ID
     * @return cached contract data or null if not found
     */
    public String getMurabahaContract(String contractId) {
        String key = MURABAHA_PREFIX + contractId;
        String contractData = redisTemplate.opsForValue().get(key);
        
        if (contractData != null) {
            logger.debug("Cache hit for Murabaha contract: {}", contractId);
        } else {
            logger.debug("Cache miss for Murabaha contract: {}", contractId);
        }
        
        return contractData;
    }
    
    /**
     * Cache Musharakah partnership data with profit/loss sharing information
     * 
     * @param partnershipId the Musharakah partnership ID
     * @param partnershipData the partnership data including sharing ratios
     */
    public void cacheMusharakahPartnership(String partnershipId, String partnershipData) {
        String key = MUSHARAKAH_PREFIX + partnershipId;
        redisTemplate.opsForValue().set(key, partnershipData, MUSHARAKAH_TTL);
        logger.debug("Cached Musharakah partnership data for ID: {}", partnershipId);
    }
    
    /**
     * Retrieve Musharakah partnership data from cache
     * 
     * @param partnershipId the Musharakah partnership ID
     * @return cached partnership data or null if not found
     */
    public String getMusharakahPartnership(String partnershipId) {
        String key = MUSHARAKAH_PREFIX + partnershipId;
        String partnershipData = redisTemplate.opsForValue().get(key);
        
        if (partnershipData != null) {
            logger.debug("Cache hit for Musharakah partnership: {}", partnershipId);
        } else {
            logger.debug("Cache miss for Musharakah partnership: {}", partnershipId);
        }
        
        return partnershipData;
    }
    
    /**
     * Cache Ijarah lease agreement data with asset ownership tracking
     * 
     * @param leaseId the Ijarah lease ID
     * @param leaseData the lease data including asset and rental information
     */
    public void cacheIjarahLease(String leaseId, String leaseData) {
        String key = IJARAH_PREFIX + leaseId;
        redisTemplate.opsForValue().set(key, leaseData, IJARAH_TTL);
        logger.debug("Cached Ijarah lease data for ID: {}", leaseId);
    }
    
    /**
     * Retrieve Ijarah lease data from cache
     * 
     * @param leaseId the Ijarah lease ID
     * @return cached lease data or null if not found
     */
    public String getIjarahLease(String leaseId) {
        String key = IJARAH_PREFIX + leaseId;
        String leaseData = redisTemplate.opsForValue().get(key);
        
        if (leaseData != null) {
            logger.debug("Cache hit for Ijarah lease: {}", leaseId);
        } else {
            logger.debug("Cache miss for Ijarah lease: {}", leaseId);
        }
        
        return leaseData;
    }
    
    /**
     * Cache Sharia compliance status for fast validation
     * 
     * @param transactionId the transaction ID
     * @param complianceStatus the Sharia compliance status
     */
    public void cacheShariaCompliance(String transactionId, String complianceStatus) {
        String key = SHARIA_COMPLIANCE_PREFIX + transactionId;
        redisTemplate.opsForValue().set(key, complianceStatus, SHARIA_COMPLIANCE_TTL);
        logger.debug("Cached Sharia compliance status for transaction: {}", transactionId);
    }
    
    /**
     * Retrieve Sharia compliance status from cache
     * 
     * @param transactionId the transaction ID
     * @return cached compliance status or null if not found
     */
    public String getShariaCompliance(String transactionId) {
        String key = SHARIA_COMPLIANCE_PREFIX + transactionId;
        String complianceStatus = redisTemplate.opsForValue().get(key);
        
        if (complianceStatus != null) {
            logger.debug("Cache hit for Sharia compliance: {}", transactionId);
        } else {
            logger.debug("Cache miss for Sharia compliance: {}", transactionId);
        }
        
        return complianceStatus;
    }
    
    /**
     * Cache asset permissibility status for Halal verification
     * 
     * @param assetId the asset ID
     * @param permissibilityStatus the asset permissibility status
     */
    public void cacheAssetPermissibility(String assetId, String permissibilityStatus) {
        String key = ASSET_PERMISSIBILITY_PREFIX + assetId;
        redisTemplate.opsForValue().set(key, permissibilityStatus, ASSET_PERMISSIBILITY_TTL);
        logger.debug("Cached asset permissibility status for asset: {}", assetId);
    }
    
    /**
     * Retrieve asset permissibility status from cache
     * 
     * @param assetId the asset ID
     * @return cached permissibility status or null if not found
     */
    public String getAssetPermissibility(String assetId) {
        String key = ASSET_PERMISSIBILITY_PREFIX + assetId;
        String permissibilityStatus = redisTemplate.opsForValue().get(key);
        
        if (permissibilityStatus != null) {
            logger.debug("Cache hit for asset permissibility: {}", assetId);
        } else {
            logger.debug("Cache miss for asset permissibility: {}", assetId);
        }
        
        return permissibilityStatus;
    }
    
    /**
     * Clear all Islamic finance-related cache entries
     */
    public void clearIslamicFinanceCache() {
        logger.info("Clearing Islamic finance cache");
        
        // Clear each Islamic finance cache prefix
        clearCacheByPrefix(MURABAHA_PREFIX);
        clearCacheByPrefix(MUSHARAKAH_PREFIX);
        clearCacheByPrefix(IJARAH_PREFIX);
        clearCacheByPrefix(SHARIA_COMPLIANCE_PREFIX);
        clearCacheByPrefix(ASSET_PERMISSIBILITY_PREFIX);
        
        logger.info("Islamic finance cache cleared successfully");
    }
    
    /**
     * Get Islamic finance cache statistics
     * 
     * @return cache statistics for Islamic finance operations
     */
    public String getIslamicFinanceCacheStats() {
        logger.debug("Calculating Islamic finance cache statistics");
        
        int murabahaCount = getCacheKeyCount(MURABAHA_PREFIX);
        int musharakahCount = getCacheKeyCount(MUSHARAKAH_PREFIX);
        int ijarahCount = getCacheKeyCount(IJARAH_PREFIX);
        int shariaCount = getCacheKeyCount(SHARIA_COMPLIANCE_PREFIX);
        int assetCount = getCacheKeyCount(ASSET_PERMISSIBILITY_PREFIX);
        
        return String.format(
            "Islamic Finance Cache Stats: Murabaha=%d, Musharakah=%d, Ijarah=%d, Sharia=%d, Assets=%d",
            murabahaCount, musharakahCount, ijarahCount, shariaCount, assetCount
        );
    }
    
    /**
     * Helper method to clear cache entries by prefix
     * 
     * @param prefix the cache key prefix
     */
    private void clearCacheByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.debug("Cleared {} cache entries for prefix: {}", keys.size(), prefix);
        }
    }
    
    /**
     * Helper method to get cache key count by prefix
     * 
     * @param prefix the cache key prefix
     * @return number of cache entries with the prefix
     */
    private int getCacheKeyCount(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        return keys != null ? keys.size() : 0;
    }
}