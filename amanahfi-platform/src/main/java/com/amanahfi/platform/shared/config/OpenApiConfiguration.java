package com.amanahfi.platform.shared.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
    name = "oauth2",
    type = SecuritySchemeType.OAUTH2,
    flows = @io.swagger.v3.oas.annotations.security.OAuthFlows(
        authorizationCode = @io.swagger.v3.oas.annotations.security.OAuthFlow(
            authorizationUrl = "https://auth.example.com/oauth2/authorize",
            tokenUrl = "https://auth.example.com/oauth2/token",
            scopes = {
                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "islamic-finance", description = "Access to Islamic finance operations"),
                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "cbdc", description = "Access to CBDC operations"),
                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "customer-management", description = "Access to customer management"),
                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "regulatory-compliance", description = "Access to regulatory compliance features")
            }
        )
    )
)
@SecurityScheme(
    name = "dpop",
    type = SecuritySchemeType.APIKEY,
    in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER,
    paramName = "DPoP",
    description = "DPoP (Demonstration of Proof-of-Possession) JWT as per RFC 9449"
)
@SecurityScheme(
    name = "mtls",
    type = SecuritySchemeType.MUTUALTLS,
    description = "Mutual TLS authentication for high-security operations"
)
public class OpenApiConfiguration {

    @Value("${API_VERSION:1.0.0}")
    private String apiVersion;

    @Value("${API_SERVER_URL:https://api.example.com}")
    private String serverUrl;

    @Value("${API_SERVER_DESCRIPTION:Example Platform API}")
    private String serverDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url(serverUrl)
                    .description(serverDescription),
                new Server()
                    .url("https://staging.example.com")
                    .description("Staging API"),
                new Server()
                    .url("https://dev.example.com")
                    .description("Development API")
            ))
            .security(List.of(
                new SecurityRequirement().addList("oauth2"),
                new SecurityRequirement().addList("dpop"),
                new SecurityRequirement().addList("mtls")
            ))
            .tags(List.of(
                createTag("Islamic Finance", 
                    "Sharia-compliant financial products including Murabaha, Musharakah, Ijarah, and other Islamic finance instruments"),
                createTag("Murabaha", 
                    "Cost-plus financing arrangements compliant with Islamic principles"),
                createTag("Musharakah", 
                    "Partnership financing with profit and loss sharing"),
                createTag("Ijarah", 
                    "Islamic leasing arrangements with optional ownership transfer"),
                createTag("CBDC", 
                    "Central Bank Digital Currency operations and Digital Dirham transactions"),
                createTag("Digital Dirham", 
                    "UAE's Central Bank Digital Currency transactions and wallet management"),
                createTag("Regulatory Compliance", 
                    "CBUAE, VARA, and HSA regulatory compliance operations"),
                createTag("Customer Management", 
                    "Customer onboarding, profile management, and KYC operations"),
                createTag("Payments", 
                    "Payment processing, scheduling, and transaction management"),
                createTag("Security", 
                    "Authentication, authorization, and security operations"),
                createTag("Compliance", 
                    "Sharia compliance validation and regulatory reporting"),
                createTag("Network", 
                    "R3 Corda network status and blockchain operations"),
                createTag("Central Bank Operations", 
                    "Digital currency minting, burning, and monetary policy operations"),
                createTag("Wallet Management", 
                    "Digital wallet creation, management, and operations"),
                createTag("Transfers", 
                    "Domestic and cross-border digital currency transfers"),
                createTag("Balance", 
                    "Account balance inquiries and financial summaries"),
                createTag("Transactions", 
                    "Transaction history, status tracking, and details"),
                createTag("Monitoring", 
                    "System monitoring, health checks, and operational status")
            ));
    }

    private Info apiInfo() {
        return new Info()
            .title("Platform API")
            .description(buildApiDescription())
            .version(apiVersion)
            .contact(new Contact()
                .name("Platform Team")
                .email("support@example.com")
                .url("https://developers.example.com"))
            .license(new License()
                .name("License")
                .url("https://example.com/license"));
    }

    private String buildApiDescription() {
        return """
            # Platform API
            
            Welcome to the Platform API. This documentation describes core banking, payments,
            compliance, and optional Islamic finance capabilities.
            
            ## Capabilities
            
            - Customer onboarding and profile management
            - Loan origination and servicing
            - Payments and reconciliation
            - Compliance and audit trails
            - Optional Islamic finance products
            
            ## Getting Started
            
            1. Configure OAuth 2.1 credentials.
            2. Use this documentation to explore endpoints.
            3. Review the Developer Portal for integration guides.
            4. Contact support for onboarding assistance.
            
            ## Resources
            
            - Developer Portal: https://developers.example.com
            - API Guidelines: https://docs.example.com/api-guidelines
            
            ---
            """;
    }

    private Tag createTag(String name, String description) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        return tag;
    }
}
