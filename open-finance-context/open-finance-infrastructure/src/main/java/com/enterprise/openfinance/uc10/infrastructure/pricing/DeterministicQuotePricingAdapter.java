package com.enterprise.openfinance.uc10.infrastructure.pricing;

import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.port.out.QuotePricingPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;

public class DeterministicQuotePricingAdapter implements QuotePricingPort {

    private static final BigDecimal VEHICLE_YEAR_FACTOR = new BigDecimal("7.50");
    private static final BigDecimal YOUNG_DRIVER_FACTOR = new BigDecimal("22.50");
    private static final BigDecimal LICENSE_EXPERIENCE_FACTOR = new BigDecimal("15.00");

    private final BigDecimal basePremium;

    public DeterministicQuotePricingAdapter(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }

    @Override
    public BigDecimal calculatePremium(CreateMotorQuoteCommand command) {
        int currentYear = Year.now().getValue();
        int vehicleAge = Math.max(0, currentYear - command.vehicleYear());
        int youngDriverPenalty = Math.max(0, 30 - command.driverAge());
        int lowExperiencePenalty = Math.max(0, 5 - command.licenseDurationYears());

        return basePremium
                .add(VEHICLE_YEAR_FACTOR.multiply(BigDecimal.valueOf(vehicleAge)))
                .add(YOUNG_DRIVER_FACTOR.multiply(BigDecimal.valueOf(youngDriverPenalty)))
                .add(LICENSE_EXPERIENCE_FACTOR.multiply(BigDecimal.valueOf(lowExperiencePenalty)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
