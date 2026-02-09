package com.enterprise.openfinance.uc10.infrastructure.event;

import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuoteEventPort;
import org.springframework.stereotype.Component;

@Component
public class NoOpMotorQuoteEventAdapter implements MotorQuoteEventPort {

    @Override
    public void publishQuoteCreated(MotorInsuranceQuote quote) {
    }

    @Override
    public void publishQuoteAccepted(MotorInsuranceQuote quote) {
    }

    @Override
    public void publishPolicyIssued(MotorInsuranceQuote quote) {
    }
}
