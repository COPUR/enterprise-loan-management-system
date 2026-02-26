package com.enterprise.openfinance.insurancequotes.domain.port.out;

import com.enterprise.openfinance.insurancequotes.domain.model.IssuedPolicy;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;

import java.time.Instant;

public interface PolicyIssuancePort {

    IssuedPolicy issuePolicy(MotorInsuranceQuote quote,
                             String paymentReference,
                             String interactionId,
                             Instant now);
}
