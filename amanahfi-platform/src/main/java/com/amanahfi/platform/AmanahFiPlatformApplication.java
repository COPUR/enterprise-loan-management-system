package com.amanahfi.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AmanahFi Platform Application - UAE & MENAT Islamic Finance Platform
 * 
 * This is the main application class for the AmanahFi Platform, a comprehensive
 * Islamic finance platform designed for the UAE and MENAT region with full
 * compliance to CBUAE, VARA, HSA regulations and Sharia principles.
 * 
 * Key Features:
 * - 6 Islamic Finance Models (Murabaha, Musharakah, Ijarah, Salam, Istisna, Qard Hassan)
 * - CBDC Digital Dirham integration with R3 Corda
 * - Multi-tenant architecture for MENAT expansion
 * - Zero Trust security with Keycloak and mTLS
 * - Custom event-driven architecture (no Axon Framework)
 * - Comprehensive exception handling and multilingual support
 * - Hexagonal DDD architecture with defensive programming
 * 
 * Regulatory Compliance:
 * - Central Bank of UAE (CBUAE) Open Finance
 * - Virtual Asset Regulatory Authority (VARA)
 * - Higher Sharia Authority (HSA)
 * - Basel III framework
 * - Anti-Money Laundering (AML)
 * 
 * Geographic Coverage:
 * - UAE (primary market with CBDC integration)
 * - Saudi Arabia, Turkey, Pakistan
 * - Azerbaijan, Iran, Israel
 * - Data and computational sovereignty compliance
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@SpringBootApplication
@EnableKafka
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConfigurationPropertiesScan
public class AmanahFiPlatformApplication {

    /**
     * Main application entry point
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        log.info("🌙 Starting AmanahFi Platform - UAE & MENAT Islamic Finance Platform");
        log.info("📋 Compliance: CBUAE ✓ | VARA ✓ | HSA ✓ | Basel III ✓");
        log.info("🕌 Islamic Finance Models: Murabaha | Musharakah | Ijarah | Salam | Istisna | Qard Hassan");
        log.info("💎 CBDC Ready: Digital Dirham (AED-CBDC) | R3 Corda Integration");
        log.info("🌍 Multi-Tenant: UAE | Saudi Arabia | Turkey | Pakistan | Azerbaijan | Iran | Israel");
        log.info("🔒 Security: Zero Trust | OAuth 2.1 | mTLS | HSM Integration");
        log.info("🏗️ Architecture: Hexagonal DDD | Event-Driven | Custom Event Store");
        
        try {
            var context = SpringApplication.run(AmanahFiPlatformApplication.class, args);
            
            log.info("✅ AmanahFi Platform started successfully");
            log.info("🎯 Platform ready for Islamic finance operations");
            log.info("📊 Health check: http://localhost:8080/actuator/health");
            log.info("📖 API Documentation: http://localhost:8080/swagger-ui.html");
            log.info("🔐 Security endpoint: http://localhost:8080/actuator/security");
            
            // Log active profiles
            String[] activeProfiles = context.getEnvironment().getActiveProfiles();
            if (activeProfiles.length > 0) {
                log.info("🎭 Active profiles: {}", String.join(", ", activeProfiles));
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to start AmanahFi Platform", e);
            System.exit(1);
        }
    }
}