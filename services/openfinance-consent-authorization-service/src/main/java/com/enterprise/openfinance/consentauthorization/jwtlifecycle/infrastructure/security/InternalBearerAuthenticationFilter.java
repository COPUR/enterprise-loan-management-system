package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalJwtLifecycleUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalBearerAuthenticationFilter extends OncePerRequestFilter {

    private final InternalJwtLifecycleUseCase useCase;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public InternalBearerAuthenticationFilter(
            InternalJwtLifecycleUseCase useCase,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.useCase = useCase;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"/internal/v1/logout".equals(path)
                && !"/internal/v1/business".equals(path)
                && !path.startsWith("/internal/v1/system/secrets");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            InternalTokenPrincipal principal = useCase.validateBearerToken(authorization);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Unauthorized", exception)
            );
        }
    }
}
