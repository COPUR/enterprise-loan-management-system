package com.enterprise.openfinance.uc10.infrastructure.pricing;

import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DeterministicQuotePricingAdapterTest {

    @Test
    void shouldCalculatePremiumDeterministically() {
        DeterministicQuotePricingAdapter adapter = new DeterministicQuotePricingAdapter(new BigDecimal("750.00"));

        BigDecimal youngerDriver = adapter.calculatePremium(new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 21, 1
        ));
        BigDecimal matureDriver = adapter.calculatePremium(new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 40, 15
        ));

        assertThat(youngerDriver).isGreaterThan(matureDriver);
        assertThat(youngerDriver.scale()).isEqualTo(2);
    }
}
