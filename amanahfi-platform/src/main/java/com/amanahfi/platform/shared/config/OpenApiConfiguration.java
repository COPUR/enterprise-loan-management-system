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
            authorizationUrl = "https://iam.amanahfi.ae/auth/realms/amanahfi/protocol/openid-connect/auth",
            tokenUrl = "https://iam.amanahfi.ae/auth/realms/amanahfi/protocol/openid-connect/token",
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

    @Value("${amanahfi.api.version:1.0.0}")
    private String apiVersion;

    @Value("${amanahfi.api.server.url:https://api.amanahfi.ae}")
    private String serverUrl;

    @Value("${amanahfi.api.server.description:AmanahFi Platform Production API}")
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
                    .url("https://api-staging.amanahfi.ae")
                    .description("AmanahFi Platform Staging API"),
                new Server()
                    .url("https://api-dev.amanahfi.ae")
                    .description("AmanahFi Platform Development API")
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
            .title("AmanahFi Platform API")
            .description(buildApiDescription())
            .version(apiVersion)
            .contact(new Contact()
                .name("AmanahFi Platform Development Team")
                .email("api-support@amanahfi.ae")
                .url("https://developers.amanahfi.ae"))
            .license(new License()
                .name("Proprietary License")
                .url("https://amanahfi.ae/license"));
    }

    private String buildApiDescription() {
        return """
            # AmanahFi Platform API
            
            Welcome to the AmanahFi Platform API - the premier Islamic finance and CBDC-enabled banking platform for the UAE and MENAT region.
            
            ## 🕌 Islamic Finance Capabilities
            
            The AmanahFi Platform provides comprehensive Sharia-compliant financial services including:
            
            - **Murabaha**: Cost-plus financing with disclosed profit margins
            - **Musharakah**: Partnership financing with profit/loss sharing
            - **Ijarah**: Asset leasing with ownership transfer options
            - **Salam**: Forward sale financing for commodity transactions
            - **Istisna**: Manufacturing and construction project financing
            - **Qard Hassan**: Interest-free benevolent loans
            
            ## 💎 Digital Dirham Integration
            
            Native integration with UAE's Central Bank Digital Currency (CBDC):
            
            - Real-time Digital Dirham transactions
            - R3 Corda blockchain integration
            - Cross-border CBDC transfers
            - Wallet management and operations
            - Regulatory compliance automation
            
            ## 🔒 Security & Compliance
            
            Enterprise-grade security with Zero Trust architecture:
            
            - **OAuth 2.1 with DPoP**: RFC 9449 compliant authentication
            - **Mutual TLS**: Certificate-based authentication for high-security operations
            - **Regulatory Compliance**: Automated CBUAE, VARA, and HSA compliance
            - **Sharia Validation**: Real-time Islamic compliance checking
            
            ## 🌍 Regional Coverage
            
            Multi-tenant platform supporting:
            
            - UAE (Primary market)
            - Saudi Arabia
            - Qatar
            - Kuwait
            - Bahrain
            - Oman
            - Turkey
            
            ## 📚 Getting Started
            
            1. **Authentication**: Obtain OAuth 2.1 access token with DPoP proof
            2. **API Exploration**: Use this interactive documentation to explore endpoints
            3. **Integration**: Review our [Developer Portal](https://developers.amanahfi.ae) for guides
            4. **Support**: Contact our API support team for assistance
            
            ## 🔗 Related Resources
            
            - [Developer Portal](https://developers.amanahfi.ae)
            - [API Guidelines](https://docs.amanahfi.ae/api-guidelines)
            - [Islamic Finance Guide](https://docs.amanahfi.ae/islamic-finance)
            - [CBDC Integration Guide](https://docs.amanahfi.ae/cbdc-integration)
            - [Security Documentation](https://docs.amanahfi.ae/security)
            
            ## ⚠️ Important Notes
            
            - All Islamic finance operations are subject to Sharia compliance validation
            - CBDC operations require additional regulatory compliance checks
            - High-value transactions may require manual approval
            - Rate limiting applies to all endpoints
            - Sandbox environment available for testing
            
            ---
            
            *Built with 💚 for Sharia-compliant financial innovation*
            """;
    }

    private Tag createTag(String name, String description) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        return tag;
    }
}