package com.bank.infrastructure.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Redis Configuration for Multi-Level Caching
 * 
 * Optimized Redis setup for banking platform with:
 * - Redis Cluster support for high availability
 * - Multi-level cache strategy (L1, L2, L3)
 * - Banking-specific cache policies
 * - Performance monitoring and metrics
 * - Connection pooling optimization
 * - Serialization optimization for banking data
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis.enterprise")
public class EnterpriseRedisConfig {

    private String nodes;
    private String password;
    private int database = 0;
    private Duration timeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(3);
    private Duration writeTimeout = Duration.ofSeconds(3);
    private int maxRedirects = 3;
    private boolean enableMetrics = true;
    
    // Connection pool settings
    private int maxActive = 50;
    private int maxIdle = 20;
    private int minIdle = 5;
    private Duration maxWait = Duration.ofSeconds(10);
    
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // Configure Redis Cluster
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        
        if (nodes != null && !nodes.isEmpty()) {
            String[] nodeArray = nodes.split(",");
            for (String node : nodeArray) {
                String[] hostPort = node.trim().split(":");
                clusterConfiguration.clusterNode(hostPort[0], Integer.parseInt(hostPort[1]));
            }
        } else {
            // Default single node for development
            clusterConfiguration.clusterNode("localhost", 6379);
        }
        
        clusterConfiguration.setMaxRedirects(maxRedirects);
        if (password != null && !password.isEmpty()) {
            clusterConfiguration.setPassword(password);
        }
        
        // Configure Lettuce client options
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
            .enablePeriodicRefresh(Duration.ofMinutes(5))
            .enableAllAdaptiveRefreshTriggers()
            .build();
        
        SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(timeout)
            .keepAlive(true)
            .tcpNoDelay(true)
            .build();
        
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
            .fixedTimeout(timeout)
            .build();
        
        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
            .topologyRefreshOptions(topologyRefreshOptions)
            .socketOptions(socketOptions)
            .timeoutOptions(timeoutOptions)
            .autoReconnect(true)
            .build();
        
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(timeout)
            .readFrom(ReadFrom.REPLICA_PREFERRED) // Read from replicas when possible
            .build();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(true);
        
        return factory;
    }
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Configure serializers for optimal performance
        ObjectMapper objectMapper = createOptimizedObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.setEnableDefaultSerializer(true);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(createOptimizedObjectMapper())))
            .entryTtl(Duration.ofHours(1)) // Default TTL
            .disableCachingNullValues()
            .prefixCacheNameWith("banking:");
        
        // Configure different cache policies for different data types
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // L1 Cache - Hot data (frequently accessed)
        cacheConfigurations.put("customers", defaultConfig
            .entryTtl(Duration.ofMinutes(15))
            .prefixCacheNameWith("banking:l1:customers:"));
        
        cacheConfigurations.put("loans", defaultConfig
            .entryTtl(Duration.ofMinutes(30))
            .prefixCacheNameWith("banking:l1:loans:"));
        
        cacheConfigurations.put("payments", defaultConfig
            .entryTtl(Duration.ofMinutes(5))
            .prefixCacheNameWith("banking:l1:payments:"));
        
        // L2 Cache - Warm data (moderately accessed)
        cacheConfigurations.put("creditProfiles", defaultConfig
            .entryTtl(Duration.ofHours(2))
            .prefixCacheNameWith("banking:l2:credit:"));
        
        cacheConfigurations.put("portfolios", defaultConfig
            .entryTtl(Duration.ofHours(4))
            .prefixCacheNameWith("banking:l2:portfolios:"));
        
        cacheConfigurations.put("aggregateEvents", defaultConfig
            .entryTtl(Duration.ofHours(6))
            .prefixCacheNameWith("banking:l2:events:"));
        
        // L3 Cache - Cold data (rarely accessed but expensive to compute)
        cacheConfigurations.put("analytics", defaultConfig
            .entryTtl(Duration.ofHours(24))
            .prefixCacheNameWith("banking:l3:analytics:"));
        
        cacheConfigurations.put("reports", defaultConfig
            .entryTtl(Duration.ofHours(12))
            .prefixCacheNameWith("banking:l3:reports:"));
        
        cacheConfigurations.put("aggregateSnapshots", defaultConfig
            .entryTtl(Duration.ofDays(7))
            .prefixCacheNameWith("banking:l3:snapshots:"));
        
        // Islamic Banking specific caches
        cacheConfigurations.put("islamicContracts", defaultConfig
            .entryTtl(Duration.ofHours(1))
            .prefixCacheNameWith("amanahfi:contracts:"));
        
        cacheConfigurations.put("shariahCompliance", defaultConfig
            .entryTtl(Duration.ofHours(8))
            .prefixCacheNameWith("amanahfi:compliance:"));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
    
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisTemplate<String, byte[]> byteRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.ByteArrayRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisCacheMetrics redisCacheMetrics(MeterRegistry meterRegistry, 
                                               RedisConnectionFactory connectionFactory) {
        return new RedisCacheMetrics(meterRegistry, connectionFactory);
    }
    
    private ObjectMapper createOptimizedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        
        // Configure for banking data optimization
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Enable type information for polymorphic deserialization
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        
        return mapper;
    }
    
    // Getters and setters for configuration properties
    public String getNodes() { return nodes; }
    public void setNodes(String nodes) { this.nodes = nodes; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public int getDatabase() { return database; }
    public void setDatabase(int database) { this.database = database; }
    
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    
    public Duration getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(Duration writeTimeout) { this.writeTimeout = writeTimeout; }
    
    public int getMaxRedirects() { return maxRedirects; }
    public void setMaxRedirects(int maxRedirects) { this.maxRedirects = maxRedirects; }
    
    public boolean isEnableMetrics() { return enableMetrics; }
    public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
    
    public int getMaxActive() { return maxActive; }
    public void setMaxActive(int maxActive) { this.maxActive = maxActive; }
    
    public int getMaxIdle() { return maxIdle; }
    public void setMaxIdle(int maxIdle) { this.maxIdle = maxIdle; }
    
    public int getMinIdle() { return minIdle; }
    public void setMinIdle(int minIdle) { this.minIdle = minIdle; }
    
    public Duration getMaxWait() { return maxWait; }
    public void setMaxWait(Duration maxWait) { this.maxWait = maxWait; }
}