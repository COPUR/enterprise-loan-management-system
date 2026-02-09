package com.enterprise.openfinance.uc10.domain.port.out;

import com.enterprise.openfinance.uc10.domain.model.IssuedPolicy;
import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;

import java.time.Instant;

public interface PolicyIssuancePort {

    IssuedPolicy issuePolicy(MotorInsuranceQuote quote,
                             String paymentReference,
                             String interactionId,
                             Instant now);
}
