package com.enterprise.openfinance.uc10.domain.port.out;

import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;

public interface MotorQuoteEventPort {

    void publishQuoteCreated(MotorInsuranceQuote quote);

    void publishQuoteAccepted(MotorInsuranceQuote quote);

    void publishPolicyIssued(MotorInsuranceQuote quote);
}
