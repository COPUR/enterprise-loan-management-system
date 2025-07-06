package com.bank.loan.loan.security.config;

import com.bank.loan.loan.security.interceptor.FAPISecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security Interceptor Configuration
 * 
 * Configures FAPI security interceptors for automatic validation
 * across all FAPI-secured endpoints.
 */
@Configuration
public class SecurityInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private FAPISecurityInterceptor fapiSecurityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add FAPI security interceptor for all API endpoints
        registry.addInterceptor(fapiSecurityInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/v1/health",           // Health check endpoint
                    "/api/v1/info",             // Info endpoint
                    "/oauth2/par",              // PAR endpoint has its own validation
                    "/oauth2/token",            // Token endpoint has its own validation
                    "/oauth2/authorize"         // Authorization endpoint has its own validation
                );
    }
}