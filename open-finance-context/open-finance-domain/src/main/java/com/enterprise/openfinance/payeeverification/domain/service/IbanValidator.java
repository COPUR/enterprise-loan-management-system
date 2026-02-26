package com.enterprise.openfinance.payeeverification.domain.service;

import java.util.Locale;

public final class IbanValidator {

    private IbanValidator() {
    }

    public static boolean isValid(String iban) {
        if (iban == null || iban.isBlank()) {
            return false;
        }

        String normalized = iban.replace(" ", "").toUpperCase(Locale.ROOT);
        if (normalized.length() < 15 || normalized.length() > 34) {
            return false;
        }
        if (!normalized.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$")) {
            return false;
        }

        String rearranged = normalized.substring(4) + normalized.substring(0, 4);
        int mod = 0;

        for (int i = 0; i < rearranged.length(); i++) {
            char c = rearranged.charAt(i);
            if (Character.isDigit(c)) {
                mod = (mod * 10 + (c - '0')) % 97;
                continue;
            }
            if (!Character.isLetter(c)) {
                return false;
            }
            int value = c - 'A' + 10;
            mod = (mod * 10 + (value / 10)) % 97;
            mod = (mod * 10 + (value % 10)) % 97;
        }

        return mod == 1;
    }
}
