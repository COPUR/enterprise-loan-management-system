package com.enterprise.openfinance.insurancequotes.domain.port.out;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;

public interface MotorQuoteEventPort {

    void publishQuoteCreated(MotorInsuranceQuote quote);

    void publishQuoteAccepted(MotorInsuranceQuote quote);

    void publishPolicyIssued(MotorInsuranceQuote quote);
}
