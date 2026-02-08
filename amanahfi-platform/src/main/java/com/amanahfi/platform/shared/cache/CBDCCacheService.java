package com.amanahfi.platform.shared.cache;

import com.amanahfi.platform.cbdc.domain.DigitalDirham;
import com.amanahfi.platform.cbdc.domain.Transaction;
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
import java.util.Set;
import java.util.UUID;

@Service
public class CBDCCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CBDCCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    public CBDCCacheService(RedisTemplate<String, Object> redisTemplate,
                           RedisTemplate<String, String> stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // CBDC Wallet Caching
    @Cacheable(value = "cbdc-wallets", key = "#walletId", unless = "#result == null")
    public DigitalDirham getCBDCWallet(UUID walletId) {
        logger.debug("Cache miss for CBDC wallet: {}", walletId);
        return null;
    }

    @CachePut(value = "cbdc-wallets", key = "#wallet.walletId")
    public DigitalDirham putCBDCWallet(DigitalDirham wallet) {
        logger.debug("Caching CBDC wallet: {}", wallet.getWalletId());
        return wallet;
    }

    @CacheEvict(value = "cbdc-wallets", key = "#walletId")
    public void evictCBDCWallet(UUID walletId) {
        logger.debug("Evicting CBDC wallet from cache: {}", walletId);
    }

    // CBDC Balance Caching
    @Cacheable(value = "cbdc-balances", key = "#walletId", unless = "#result == null")
    public Money getCBDCBalance(UUID walletId) {
        logger.debug("Cache miss for CBDC balance: {}", walletId);
        return null;
    }

    @CachePut(value = "cbdc-balances", key = "#walletId")
    public Money putCBDCBalance(UUID walletId, Money balance) {
        logger.debug("Caching CBDC balance for wallet: {} = {}", walletId, balance.getAmount());
        return balance;
    }

    @CacheEvict(value = "cbdc-balances", key = "#walletId")
    public void evictCBDCBalance(UUID walletId) {
        logger.debug("Evicting CBDC balance from cache: {}", walletId);
    }

    // Exchange Rate Caching
    @Cacheable(value = "cbdc-exchange-rates", key = "#fromCurrency + '_' + #toCurrency")
    public ExchangeRate getExchangeRate(String fromCurrency, String toCurrency) {
        logger.debug("Cache miss for exchange rate: {} -> {}", fromCurrency, toCurrency);
        return null;
    }

    @CachePut(value = "cbdc-exchange-rates", key = "#fromCurrency + '_' + #toCurrency")
    public ExchangeRate putExchangeRate(String fromCurrency, String toCurrency, ExchangeRate rate) {
        logger.debug("Caching exchange rate: {} -> {} = {}", fromCurrency, toCurrency, rate.rate());
        return rate;
    }

    // Network Status Caching
    @Cacheable(value = "network-status", key = "'corda-network'")
    public NetworkStatus getNetworkStatus() {
        logger.debug("Cache miss for network status");
        return null;
    }

    @CachePut(value = "network-status", key = "'corda-network'")
    public NetworkStatus putNetworkStatus(NetworkStatus status) {
        logger.debug("Caching network status: {}", status.status());
        return status;
    }

    // Transaction Status Caching
    public void cacheTransactionStatus(UUID transactionId, CBDCTransactionStatus status) {
        String key = "cbdc:transaction:status:" + transactionId;
        redisTemplate.opsForValue().set(key, status, Duration.ofMinutes(10));
        logger.debug("Cached transaction status: {} = {}", transactionId, status.status());
    }

    public CBDCTransactionStatus getTransactionStatus(UUID transactionId) {
        String key = "cbdc:transaction:status:" + transactionId;
        CBDCTransactionStatus status = (CBDCTransactionStatus) redisTemplate.opsForValue().get(key);
        if (status != null) {
            logger.debug("Cache hit for transaction status: {}", transactionId);
        } else {
            logger.debug("Cache miss for transaction status: {}", transactionId);
        }
        return status;
    }

    // Blockchain Hash Caching
    public void cacheBlockchainHash(UUID transactionId, String blockchainHash, Long blockNumber) {
        String key = "cbdc:blockchain:" + transactionId;
        BlockchainInfo info = new BlockchainInfo(blockchainHash, blockNumber, LocalDateTime.now());
        redisTemplate.opsForValue().set(key, info, Duration.ofHours(24));
        logger.debug("Cached blockchain info for transaction: {}", transactionId);
    }

    public BlockchainInfo getBlockchainInfo(UUID transactionId) {
        String key = "cbdc:blockchain:" + transactionId;
        BlockchainInfo info = (BlockchainInfo) redisTemplate.opsForValue().get(key);
        if (info != null) {
            logger.debug("Cache hit for blockchain info: {}", transactionId);
        } else {
            logger.debug("Cache miss for blockchain info: {}", transactionId);
        }
        return info;
    }

    // Wallet Address Mapping Cache
    public void cacheWalletAddress(UUID walletId, String cordaAddress) {
        String key = "cbdc:wallet:address:" + walletId;
        stringRedisTemplate.opsForValue().set(key, cordaAddress, Duration.ofHours(24));
        logger.debug("Cached wallet address mapping: {} -> {}", walletId, cordaAddress);
    }

    public String getWalletAddress(UUID walletId) {
        String key = "cbdc:wallet:address:" + walletId;
        String address = stringRedisTemplate.opsForValue().get(key);
        if (address != null) {
            logger.debug("Cache hit for wallet address: {}", walletId);
        } else {
            logger.debug("Cache miss for wallet address: {}", walletId);
        }
        return address;
    }

    // Transaction Fee Caching
    public void cacheTransactionFees(String transactionType, Money amount, TransactionFees fees) {
        String key = "cbdc:fees:" + transactionType + ":" + amount.getCurrency().getCurrencyCode();
        redisTemplate.opsForValue().set(key, fees, Duration.ofMinutes(30));
        logger.debug("Cached transaction fees for type: {}, currency: {}", transactionType, amount.getCurrency());
    }

    public TransactionFees getTransactionFees(String transactionType, Money amount) {
        String key = "cbdc:fees:" + transactionType + ":" + amount.getCurrency().getCurrencyCode();
        TransactionFees fees = (TransactionFees) redisTemplate.opsForValue().get(key);
        if (fees != null) {
            logger.debug("Cache hit for transaction fees: {}", transactionType);
        } else {
            logger.debug("Cache miss for transaction fees: {}", transactionType);
        }
        return fees;
    }

    // Cross-Border Transfer Limits Caching
    public void cacheCrossBorderLimits(String fromCountry, String toCountry, CrossBorderLimits limits) {
        String key = "cbdc:cross-border:" + fromCountry + ":" + toCountry;
        redisTemplate.opsForValue().set(key, limits, Duration.ofHours(12));
        logger.debug("Cached cross-border limits: {} -> {}", fromCountry, toCountry);
    }

    public CrossBorderLimits getCrossBorderLimits(String fromCountry, String toCountry) {
        String key = "cbdc:cross-border:" + fromCountry + ":" + toCountry;
        CrossBorderLimits limits = (CrossBorderLimits) redisTemplate.opsForValue().get(key);
        if (limits != null) {
            logger.debug("Cache hit for cross-border limits: {} -> {}", fromCountry, toCountry);
        } else {
            logger.debug("Cache miss for cross-border limits: {} -> {}", fromCountry, toCountry);
        }
        return limits;
    }

    // Wallet Transaction History Summary Caching
    public void cacheTransactionSummary(UUID walletId, String period, TransactionSummary summary) {
        String key = "cbdc:wallet:summary:" + walletId + ":" + period;
        redisTemplate.opsForValue().set(key, summary, Duration.ofMinutes(15));
        logger.debug("Cached transaction summary for wallet: {}, period: {}", walletId, period);
    }

    public TransactionSummary getTransactionSummary(UUID walletId, String period) {
        String key = "cbdc:wallet:summary:" + walletId + ":" + period;
        TransactionSummary summary = (TransactionSummary) redisTemplate.opsForValue().get(key);
        if (summary != null) {
            logger.debug("Cache hit for transaction summary: {}, period: {}", walletId, period);
        } else {
            logger.debug("Cache miss for transaction summary: {}, period: {}", walletId, period);
        }
        return summary;
    }

    // AML/KYC Verification Results Caching
    public void cacheAMLVerification(UUID customerId, UUID transactionId, AMLVerificationResult result) {
        String key = "cbdc:aml:" + customerId + ":" + transactionId;
        redisTemplate.opsForValue().set(key, result, Duration.ofHours(1));
        logger.debug("Cached AML verification result for customer: {}, transaction: {}", customerId, transactionId);
    }

    public AMLVerificationResult getAMLVerification(UUID customerId, UUID transactionId) {
        String key = "cbdc:aml:" + customerId + ":" + transactionId;
        AMLVerificationResult result = (AMLVerificationResult) redisTemplate.opsForValue().get(key);
        if (result != null) {
            logger.debug("Cache hit for AML verification: customer: {}, transaction: {}", customerId, transactionId);
        } else {
            logger.debug("Cache miss for AML verification: customer: {}, transaction: {}", customerId, transactionId);
        }
        return result;
    }

    // Sanctioned Addresses Cache
    public void cacheSanctionedAddresses(Set<String> addresses) {
        String key = "cbdc:sanctions:addresses";
        redisTemplate.opsForValue().set(key, addresses, Duration.ofHours(6));
        logger.debug("Cached {} sanctioned addresses", addresses.size());
    }

    public Set<String> getSanctionedAddresses() {
        String key = "cbdc:sanctions:addresses";
        @SuppressWarnings("unchecked")
        Set<String> addresses = (Set<String>) redisTemplate.opsForValue().get(key);
        if (addresses != null) {
            logger.debug("Cache hit for sanctioned addresses: {} addresses", addresses.size());
        } else {
            logger.debug("Cache miss for sanctioned addresses");
        }
        return addresses;
    }

    // CBDC Market Data Caching
    public void cacheCBDCMarketData(CBDCMarketData marketData) {
        String key = "cbdc:market:data";
        redisTemplate.opsForValue().set(key, marketData, Duration.ofMinutes(5));
        logger.debug("Cached CBDC market data");
    }

    public CBDCMarketData getCBDCMarketData() {
        String key = "cbdc:market:data";
        CBDCMarketData marketData = (CBDCMarketData) redisTemplate.opsForValue().get(key);
        if (marketData != null) {
            logger.debug("Cache hit for CBDC market data");
        } else {
            logger.debug("Cache miss for CBDC market data");
        }
        return marketData;
    }

    // Central Bank Operations Cache
    public void cacheCentralBankOperation(UUID operationId, CentralBankOperation operation) {
        String key = "cbdc:central-bank:" + operationId;
        redisTemplate.opsForValue().set(key, operation, Duration.ofHours(48));
        logger.debug("Cached central bank operation: {}", operationId);
    }

    public CentralBankOperation getCentralBankOperation(UUID operationId) {
        String key = "cbdc:central-bank:" + operationId;
        CentralBankOperation operation = (CentralBankOperation) redisTemplate.opsForValue().get(key);
        if (operation != null) {
            logger.debug("Cache hit for central bank operation: {}", operationId);
        } else {
            logger.debug("Cache miss for central bank operation: {}", operationId);
        }
        return operation;
    }

    // Rate Limiting for CBDC Operations
    public boolean checkRateLimit(String operation, String identifier, int maxRequests, Duration window) {
        String key = "cbdc:rate-limit:" + operation + ":" + identifier;
        String currentCountStr = stringRedisTemplate.opsForValue().get(key);
        
        if (currentCountStr == null) {
            // First request in the window
            stringRedisTemplate.opsForValue().set(key, "1", window);
            logger.debug("Rate limit - first request for {}: {}", operation, identifier);
            return true;
        }
        
        int currentCount = Integer.parseInt(currentCountStr);
        if (currentCount >= maxRequests) {
            logger.warn("Rate limit exceeded for {}: {} ({} requests)", operation, identifier, currentCount);
            return false;
        }
        
        stringRedisTemplate.opsForValue().increment(key);
        logger.debug("Rate limit - request {} of {} for {}: {}", currentCount + 1, maxRequests, operation, identifier);
        return true;
    }

    // Cache warming for CBDC operations
    public void warmupCBDCCache() {
        logger.info("Starting CBDC cache warmup");
        
        warmupExchangeRates();
        warmupTransactionFees();
        warmupCrossBorderLimits();
        warmupNetworkStatus();
        
        logger.info("Completed CBDC cache warmup");
    }

    private void warmupExchangeRates() {
        // Preload common exchange rates
        String[] currencies = {"AED-CBDC", "SAR-CBDC", "QAR-CBDC", "KWD-CBDC", "BHD-CBDC", "OMR-CBDC"};
        
        for (String from : currencies) {
            for (String to : currencies) {
                if (!from.equals(to)) {
                    // Mock exchange rate - in production, this would load from external service
                    ExchangeRate rate = new ExchangeRate(from, to, BigDecimal.ONE, LocalDateTime.now());
                    putExchangeRate(from, to, rate);
                }
            }
        }
    }

    private void warmupTransactionFees() {
        String[] transactionTypes = {"DOMESTIC", "CROSS_BORDER", "ISLAMIC_FINANCE", "COMMERCIAL"};
        String[] currencies = {"AED-CBDC", "SAR-CBDC", "QAR-CBDC"};
        
        for (String type : transactionTypes) {
            for (String currency : currencies) {
                Money baseAmount = new Money(BigDecimal.valueOf(1000), java.util.Currency.getInstance(currency.replace("-CBDC", "")));
                TransactionFees fees = createDefaultTransactionFees(type, currency);
                cacheTransactionFees(type, baseAmount, fees);
            }
        }
    }

    private void warmupCrossBorderLimits() {
        String[] countries = {"AE", "SA", "QA", "KW", "BH", "OM"};
        
        for (String from : countries) {
            for (String to : countries) {
                if (!from.equals(to)) {
                    CrossBorderLimits limits = createDefaultCrossBorderLimits(from, to);
                    cacheCrossBorderLimits(from, to, limits);
                }
            }
        }
    }

    private void warmupNetworkStatus() {
        NetworkStatus status = new NetworkStatus("OPERATIONAL", 99.9, LocalDateTime.now(), Map.of());
        putNetworkStatus(status);
    }

    // Bulk cache operations
    @CacheEvict(value = {"cbdc-wallets", "cbdc-balances", "cbdc-exchange-rates", "network-status"}, allEntries = true)
    public void evictAllCBDCCaches() {
        logger.info("Evicted all CBDC caches");
    }

    // Helper methods
    private TransactionFees createDefaultTransactionFees(String transactionType, String currency) {
        BigDecimal networkFee = new BigDecimal("0.01");
        BigDecimal processingFee = switch (transactionType) {
            case "DOMESTIC" -> new BigDecimal("0.50");
            case "CROSS_BORDER" -> new BigDecimal("2.00");
            case "ISLAMIC_FINANCE" -> new BigDecimal("1.00");
            default -> new BigDecimal("1.50");
        };
        return new TransactionFees(networkFee, processingFee, currency);
    }

    private CrossBorderLimits createDefaultCrossBorderLimits(String fromCountry, String toCountry) {
        return new CrossBorderLimits(
            fromCountry,
            toCountry,
            new BigDecimal("100000"),
            new BigDecimal("1000000"),
            "AED",
            true
        );
    }

    // Cache statistics
    public CBDCCacheStatistics getCBDCCacheStatistics() {
        return new CBDCCacheStatistics();
    }

    // Data classes for cached objects
    public record ExchangeRate(
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        LocalDateTime timestamp
    ) {}

    public record NetworkStatus(
        String status,
        double uptime,
        LocalDateTime lastChecked,
        Map<String, Object> metrics
    ) {}

    public record CBDCTransactionStatus(
        UUID transactionId,
        String status,
        String statusDetails,
        int confirmations,
        LocalDateTime lastUpdated
    ) {}

    public record BlockchainInfo(
        String hash,
        Long blockNumber,
        LocalDateTime timestamp
    ) {}

    public record TransactionFees(
        BigDecimal networkFee,
        BigDecimal processingFee,
        String currency
    ) {}

    public record CrossBorderLimits(
        String fromCountry,
        String toCountry,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        String currency,
        boolean enabled
    ) {}

    public record TransactionSummary(
        UUID walletId,
        String period,
        long transactionCount,
        Money totalVolume,
        Money averageAmount,
        LocalDateTime generatedAt
    ) {}

    public record AMLVerificationResult(
        UUID customerId,
        UUID transactionId,
        boolean approved,
        String riskLevel,
        List<String> flags,
        LocalDateTime verifiedAt
    ) {}

    public record CBDCMarketData(
        Map<String, BigDecimal> volumes,
        Map<String, BigDecimal> rates,
        long totalTransactions,
        LocalDateTime timestamp
    ) {}

    public record CentralBankOperation(
        UUID operationId,
        String operationType,
        Money amount,
        String status,
        LocalDateTime executedAt
    ) {}

    public record CBDCCacheStatistics(
        long walletCacheHits,
        long walletCacheMisses,
        long balanceCacheHits,
        long balanceCacheMisses,
        long exchangeRateCacheHits,
        long exchangeRateCacheMisses,
        double overallHitRatio
    ) {
        public CBDCCacheStatistics() {
            this(0L, 0L, 0L, 0L, 0L, 0L, 0.0);
        }
    }
}