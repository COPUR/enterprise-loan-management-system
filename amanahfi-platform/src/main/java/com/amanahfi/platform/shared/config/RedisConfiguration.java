package com.amanahfi.platform.shared.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfiguration {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:2000}")
    private int redisTimeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        
        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        factory.setValidateConnection(true);
        return factory;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        // Configure JSON serializer
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        
        // Key serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializers
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        return template;
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
            .disableCachingNullValues();

        // Configure specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Islamic Finance caches
        cacheConfigurations.put("islamic-finance-products", 
            defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("murabaha-calculations", 
            defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("sharia-compliance-rules", 
            defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("profit-margin-limits", 
            defaultConfig.entryTtl(Duration.ofHours(12)));
        
        // CBDC caches
        cacheConfigurations.put("cbdc-wallets", 
            defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("cbdc-balances", 
            defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("cbdc-exchange-rates", 
            defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("network-status", 
            defaultConfig.entryTtl(Duration.ofSeconds(30)));
        
        // Customer caches
        cacheConfigurations.put("customer-profiles", 
            defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("customer-risk-profiles", 
            defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("kyc-status", 
            defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Regulatory compliance caches
        cacheConfigurations.put("compliance-rules", 
            defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("regulatory-limits", 
            defaultConfig.entryTtl(Duration.ofHours(12)));
        cacheConfigurations.put("sanction-lists", 
            defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Security caches
        cacheConfigurations.put("jwt-blacklist", 
            defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("failed-auth-attempts", 
            defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("rate-limit-counters", 
            defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        // Session and token caches
        cacheConfigurations.put("user-sessions", 
            defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("oauth-tokens", 
            defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigurations.put("dpop-nonces", 
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}