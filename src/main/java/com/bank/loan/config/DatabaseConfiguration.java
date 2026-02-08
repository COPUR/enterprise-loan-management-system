package com.bank.loan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Database Configuration
 * 
 * Configures datasource and transaction management
 */
@Configuration
@EnableTransactionManagement
@EnableCaching
public class DatabaseConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource")
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("customers", "loans", "payments");
    }
}