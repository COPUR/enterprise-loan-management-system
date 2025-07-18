package com.bank.infrastructure.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

/**
 * Production-Ready API Configuration
 * 
 * Comprehensive configuration for enterprise banking API including:
 * - OpenAPI 3.1 specification with complete documentation
 * - OAuth 2.1 with DPoP and mTLS security schemes
 * - FAPI 2.0 compliance configuration
 * - Production-grade CORS and security headers
 * - API versioning and content negotiation
 * - Comprehensive error response schemas
 * - Rate limiting and monitoring integration
 */
@Configuration
@Profile("!test")
@ConfigurationProperties(prefix = "banking.api")
public class ProductionApiConfiguration implements WebMvcConfigurer {
    
    private String title = "Enterprise Banking Platform API";
    private String version = "v1.0.0";
    private String description = "Production-ready banking API with Islamic finance support";
    private String termsOfService = "https://banking.example.com/terms";
    private String contactName = "API Support";
    private String contactEmail = "api-support@banking.example.com";
    private String contactUrl = "https://banking.example.com/support";
    private String licenseName = "Proprietary";
    private String licenseUrl = "https://banking.example.com/license";
    private List<String> serverUrls = List.of("https://api.banking.example.com");
    private List<String> allowedOrigins = List.of("https://banking.example.com");
    private boolean enableSwaggerUI = true;
    private boolean enableActuatorEndpoints = true;
    
    @Bean
    public OpenAPI productionOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(title)
                .version(version)
                .description(description + "\n\n" + getApiDescription())
                .termsOfService(termsOfService)
                .contact(new Contact()
                    .name(contactName)
                    .email(contactEmail)
                    .url(contactUrl))
                .license(new License()
                    .name(licenseName)
                    .url(licenseUrl)))
            .servers(serverUrls.stream()
                .map(url -> new Server().url(url).description("Production server"))
                .toList())
            .addSecurityItem(new SecurityRequirement().addList("OAuth2DPoP"))
            .addSecurityItem(new SecurityRequirement().addList("mTLS"))
            .components(new Components()
                .addSecuritySchemes("OAuth2DPoP", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("OAuth 2.1 with DPoP (Demonstrating Proof-of-Possession)")
                    .bearerFormat("JWT")
                    .scheme("bearer"))
                .addSecuritySchemes("mTLS", new SecurityScheme()
                    .type(SecurityScheme.Type.MUTUALTLS)
                    .description("Mutual TLS for high-security operations"))
                .addSecuritySchemes("ApiKey", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API Key for service-to-service authentication"))
                .addResponses("BadRequest", createErrorResponse("400", "Bad Request"))
                .addResponses("Unauthorized", createErrorResponse("401", "Unauthorized"))
                .addResponses("Forbidden", createErrorResponse("403", "Forbidden"))
                .addResponses("NotFound", createErrorResponse("404", "Not Found"))
                .addResponses("Conflict", createErrorResponse("409", "Conflict"))
                .addResponses("UnprocessableEntity", createErrorResponse("422", "Unprocessable Entity"))
                .addResponses("TooManyRequests", createErrorResponse("429", "Too Many Requests"))
                .addResponses("InternalServerError", createErrorResponse("500", "Internal Server Error"))
                .addResponses("ServiceUnavailable", createErrorResponse("503", "Service Unavailable"))
                .addSchemas("ErrorResponse", createErrorSchema())
                .addSchemas("PaginatedResponse", createPaginatedSchema())
                .addSchemas("ApiResponse", createApiResponseSchema()));
    }
    
    @Bean
    public ApiVersioningConfiguration apiVersioningConfiguration() {
        return new ApiVersioningConfiguration();
    }
    
    @Bean
    public RateLimitingConfiguration rateLimitingConfiguration() {
        return new RateLimitingConfiguration();
    }
    
    @Bean
    public SecurityHeadersConfiguration securityHeadersConfiguration() {
        return new SecurityHeadersConfiguration();
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
            .allowedHeaders("*")
            .exposedHeaders(
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.CONTENT_LENGTH,
                HttpHeaders.LOCATION,
                HttpHeaders.ETAG,
                HttpHeaders.LAST_MODIFIED,
                "X-Total-Count",
                "X-Request-ID",
                "X-Correlation-ID",
                "X-API-Version",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Reset"
            )
            .allowCredentials(true)
            .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestTracingInterceptor())
            .addPathPatterns("/api/**")
            .order(1);
        
        registry.addInterceptor(new SecurityHeadersInterceptor())
            .addPathPatterns("/api/**")
            .order(2);
        
        registry.addInterceptor(new RateLimitingInterceptor())
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/health/**", "/api/metrics/**")
            .order(3);
        
        registry.addInterceptor(new ApiVersioningInterceptor())
            .addPathPatterns("/api/**")
            .order(4);
    }
    
    private String getApiDescription() {
        return """
            ## Enterprise Banking Platform API
            
            This API provides comprehensive banking services including:
            
            ### Core Features
            - **Customer Management**: Complete customer lifecycle management
            - **Loan Processing**: Advanced loan origination and servicing
            - **Payment Processing**: Secure payment processing with multiple methods
            - **Islamic Banking**: Sharia-compliant financial products (Murabaha, Ijara)
            
            ### Security Features
            - **OAuth 2.1 with DPoP**: Proof-of-possession for enhanced security
            - **Mutual TLS**: Certificate-based authentication for high-security operations
            - **FAPI 2.0 Compliance**: Financial-grade API security implementation
            - **Request Signing**: JWS-based request integrity verification
            
            ### Enterprise Features
            - **Real-time Events**: Server-Sent Events for live updates
            - **Idempotency**: Safe retry mechanisms for all operations
            - **HATEOAS**: Hypermedia-driven API navigation
            - **Comprehensive Monitoring**: Full observability and metrics
            
            ### Compliance
            - **PCI DSS**: Payment card industry compliance
            - **GDPR**: Data protection and privacy compliance
            - **Banking Regulations**: UAE Central Bank compliance
            - **Islamic Finance**: Sharia board certified products
            
            ### API Conventions
            - **REST**: RESTful design with proper HTTP methods
            - **JSON**: Primary content type with HAL+JSON for hypermedia
            - **Versioning**: URL-based and header-based versioning
            - **Pagination**: Cursor-based pagination for large result sets
            - **Error Handling**: RFC 9457 Problem Details for errors
            """;
    }
    
    private ApiResponse createErrorResponse(String code, String description) {
        return new ApiResponse()
            .description(description)
            .content(new Content()
                .addMediaType("application/problem+json", new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
    }
    
    private Schema<?> createErrorSchema() {
        return new Schema<>()
            .type("object")
            .description("RFC 9457 Problem Details for HTTP APIs")
            .addProperty("type", new Schema<>()
                .type("string")
                .format("uri")
                .description("A URI reference that identifies the problem type"))
            .addProperty("title", new Schema<>()
                .type("string")
                .description("A short, human-readable summary of the problem type"))
            .addProperty("status", new Schema<>()
                .type("integer")
                .description("The HTTP status code"))
            .addProperty("detail", new Schema<>()
                .type("string")
                .description("A human-readable explanation specific to this occurrence"))
            .addProperty("instance", new Schema<>()
                .type("string")
                .format("uri")
                .description("A URI reference that identifies the specific occurrence"))
            .addProperty("timestamp", new Schema<>()
                .type("string")
                .format("date-time")
                .description("The time when the error occurred"))
            .addProperty("requestId", new Schema<>()
                .type("string")
                .description("Unique identifier for the request"))
            .addProperty("correlationId", new Schema<>()
                .type("string")
                .description("Correlation identifier for tracing"))
            .addProperty("errors", new Schema<>()
                .type("array")
                .items(new Schema<>()
                    .type("object")
                    .addProperty("field", new Schema<>().type("string"))
                    .addProperty("message", new Schema<>().type("string"))
                    .addProperty("code", new Schema<>().type("string")))
                .description("Detailed validation errors"))
            .required(List.of("type", "title", "status", "timestamp", "requestId"));
    }
    
    private Schema<?> createPaginatedSchema() {
        return new Schema<>()
            .type("object")
            .description("Paginated response wrapper")
            .addProperty("data", new Schema<>()
                .type("array")
                .items(new Schema<>())
                .description("Array of items"))
            .addProperty("pagination", new Schema<>()
                .type("object")
                .addProperty("page", new Schema<>().type("integer").description("Current page number"))
                .addProperty("size", new Schema<>().type("integer").description("Items per page"))
                .addProperty("totalElements", new Schema<>().type("integer").description("Total number of items"))
                .addProperty("totalPages", new Schema<>().type("integer").description("Total number of pages"))
                .addProperty("first", new Schema<>().type("boolean").description("First page indicator"))
                .addProperty("last", new Schema<>().type("boolean").description("Last page indicator"))
                .addProperty("hasNext", new Schema<>().type("boolean").description("Next page available"))
                .addProperty("hasPrevious", new Schema<>().type("boolean").description("Previous page available"))
                .required(List.of("page", "size", "totalElements", "totalPages")))
            .addProperty("_links", new Schema<>()
                .type("object")
                .description("HATEOAS navigation links")
                .addProperty("self", new Schema<>()
                    .type("object")
                    .addProperty("href", new Schema<>().type("string")))
                .addProperty("first", new Schema<>()
                    .type("object")
                    .addProperty("href", new Schema<>().type("string")))
                .addProperty("prev", new Schema<>()
                    .type("object")
                    .addProperty("href", new Schema<>().type("string")))
                .addProperty("next", new Schema<>()
                    .type("object")
                    .addProperty("href", new Schema<>().type("string")))
                .addProperty("last", new Schema<>()
                    .type("object")
                    .addProperty("href", new Schema<>().type("string"))))
            .required(List.of("data", "pagination"));
    }
    
    private Schema<?> createApiResponseSchema() {
        return new Schema<>()
            .type("object")
            .description("Standard API response wrapper")
            .addProperty("success", new Schema<>()
                .type("boolean")
                .description("Operation success indicator"))
            .addProperty("message", new Schema<>()
                .type("string")
                .description("Human-readable message"))
            .addProperty("data", new Schema<>()
                .description("Response data"))
            .addProperty("timestamp", new Schema<>()
                .type("string")
                .format("date-time")
                .description("Response timestamp"))
            .addProperty("requestId", new Schema<>()
                .type("string")
                .description("Unique request identifier"))
            .addProperty("correlationId", new Schema<>()
                .type("string")
                .description("Correlation identifier"))
            .addProperty("_links", new Schema<>()
                .type("object")
                .description("HATEOAS navigation links"))
            .required(List.of("success", "timestamp", "requestId"));
    }
    
    // Getters and setters for configuration properties
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTermsOfService() { return termsOfService; }
    public void setTermsOfService(String termsOfService) { this.termsOfService = termsOfService; }
    
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    
    public String getContactUrl() { return contactUrl; }
    public void setContactUrl(String contactUrl) { this.contactUrl = contactUrl; }
    
    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }
    
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    
    public List<String> getServerUrls() { return serverUrls; }
    public void setServerUrls(List<String> serverUrls) { this.serverUrls = serverUrls; }
    
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    
    public boolean isEnableSwaggerUI() { return enableSwaggerUI; }
    public void setEnableSwaggerUI(boolean enableSwaggerUI) { this.enableSwaggerUI = enableSwaggerUI; }
    
    public boolean isEnableActuatorEndpoints() { return enableActuatorEndpoints; }
    public void setEnableActuatorEndpoints(boolean enableActuatorEndpoints) { this.enableActuatorEndpoints = enableActuatorEndpoints; }
}