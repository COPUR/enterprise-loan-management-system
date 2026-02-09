package com.enterprise.openfinance.uc10.infrastructure.persistence;

import com.enterprise.openfinance.uc10.domain.model.IssuedPolicy;
import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.port.out.PolicyIssuancePort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class GeneratedPolicyIssuanceAdapter implements PolicyIssuancePort {

    @Override
    public IssuedPolicy issuePolicy(MotorInsuranceQuote quote,
                                    String paymentReference,
                                    String interactionId,
                                    Instant now) {
        String policyId = "POL-MTR-" + UUID.randomUUID();
        String policyNumber = "MTR-" + now.atZone(ZoneOffset.UTC).toLocalDate() + "-" + quote.quoteId().substring(0, 6);
        String certificateId = "CERT-" + Math.abs((quote.quoteId() + paymentReference + interactionId).hashCode());
        return new IssuedPolicy(policyId, policyNumber, certificateId);
    }
}
