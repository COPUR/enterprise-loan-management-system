package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command;

public record InternalAuthenticateCommand(
        String username,
        String password
) {
    public InternalAuthenticateCommand {
        if (isBlank(username)) {
            throw new IllegalArgumentException("username is required");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("password is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

