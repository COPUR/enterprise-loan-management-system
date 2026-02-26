package com.enterprise.openfinance.payeeverification.domain.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class IbanValidatorTest {

    @Test
    void shouldValidateWellFormedIbanWithCorrectChecksum() {
        assertThat(IbanValidator.isValid("GB82WEST12345698765432")).isTrue();
        assertThat(IbanValidator.isValid("GB82 WEST 1234 5698 7654 32")).isTrue();
    }

    @Test
    void shouldRejectIbanWithInvalidChecksum() {
        assertThat(IbanValidator.isValid("GB83WEST12345698765432")).isFalse();
    }

    @Test
    void shouldRejectIbanWithInvalidCharacters() {
        assertThat(IbanValidator.isValid("GB82WEST1234@698765432")).isFalse();
    }

    @Test
    void shouldRejectIbanWithUnsupportedLength() {
        assertThat(IbanValidator.isValid("GB82")).isFalse();
    }
}
