package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalCredentialPort;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredInternalCredentialAdapter implements InternalCredentialPort {

    private final InternalSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final String encodedPassword;

    public ConfiguredInternalCredentialAdapter(InternalSecurityProperties properties) {
        this.properties = properties;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.encodedPassword = passwordEncoder.encode(properties.getInternalPassword());
    }

    @Override
    public boolean matches(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        if (!properties.getInternalUsername().equals(username)) {
            return false;
        }

        return passwordEncoder.matches(password, encodedPassword);
    }
}

