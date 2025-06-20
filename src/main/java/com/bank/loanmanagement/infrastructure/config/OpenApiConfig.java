package com.bank.loanmanagement.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Enterprise Banking System
 * Comprehensive API documentation following best practices
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .servers(buildServers())
                .components(buildComponents())
                .security(buildSecurityRequirements());
    }

    private Info buildApiInfo() {
        return new Info()
                .title("Enterprise Banking Loan Management API")
                .description("""
                    ## Enterprise Banking System API Documentation
                    
                    A comprehensive loan management system with AI-powered features built using:
                    - **Hexagonal Architecture** - Clean separation of concerns
                    - **Domain-Driven Design** - Rich domain models and use cases
                    - **Spring AI** - OpenAI GPT-4 integration for intelligent recommendations
                    - **Event-Driven Architecture** - Asynchronous processing capabilities
                    - **FAPI Compliance** - Financial-grade API security standards
                    
                    ### Key Features
                    - 🤖 **AI-Powered Loan Recommendations** - Personalized loan offers using machine learning
                    - 👥 **Customer Management** - Complete customer lifecycle management
                    - 💳 **Credit Management** - Real-time credit evaluation and monitoring
                    - 🔒 **Risk Assessment** - Advanced risk scoring with traditional and AI models
                    - 📊 **Real-time Analytics** - Financial insights and dashboards
                    - 🛡️ **Fraud Detection** - AI-powered fraud prevention
                    
                    ### Architecture
                    - **Domain Layer**: Core business logic and domain entities
                    - **Application Layer**: Use cases and business orchestration
                    - **Infrastructure Layer**: External integrations and adapters
                    
                    ### Authentication
                    All endpoints require Bearer Token authentication following OAuth 2.0 standards.
                    
                    ### Rate Limiting
                    - Standard endpoints: 1000 requests/hour
                    - AI endpoints: 100 requests/hour
                    - Bulk operations: 50 requests/hour
                    
                    """)
                .version("2.1.0")
                .contact(new Contact()
                        .name("Banking API Team")
                        .email("api-support@enterprisebank.com")
                        .url("https://dev.enterprisebank.com/api-docs"))
                .license(new License()
                        .name("Enterprise License")
                        .url("https://enterprisebank.com/license"));
    }

    private List<Server> buildServers() {
        return List.of(
                new Server()
                        .url("https://api.enterprisebank.com")
                        .description("Production Server")
                        .variables(null),
                new Server()
                        .url("https://staging-api.enterprisebank.com")
                        .description("Staging Server")
                        .variables(null),
                new Server()
                        .url("http://localhost:8080")
                        .description("Development Server")
                        .variables(null)
        );
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer Token Authentication"))
                .addSecuritySchemes("oauth2", new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("OAuth 2.0 Authentication"));
    }

    private List<SecurityRequirement> buildSecurityRequirements() {
        return List.of(
                new SecurityRequirement().addList("bearerAuth"),
                new SecurityRequirement().addList("oauth2")
        );
    }
}