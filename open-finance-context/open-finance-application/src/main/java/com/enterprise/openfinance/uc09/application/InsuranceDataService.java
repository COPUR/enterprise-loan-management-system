package com.enterprise.openfinance.uc09.application;

import com.enterprise.openfinance.uc09.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc09.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc09.domain.model.InsuranceConsentContext;
import com.enterprise.openfinance.uc09.domain.model.InsuranceDataSettings;
import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.uc09.domain.model.MotorPolicy;
import com.enterprise.openfinance.uc09.domain.port.in.InsuranceDataUseCase;
import com.enterprise.openfinance.uc09.domain.port.out.InsuranceConsentPort;
import com.enterprise.openfinance.uc09.domain.port.out.InsurancePolicyCachePort;
import com.enterprise.openfinance.uc09.domain.port.out.MotorPolicyReadPort;
import com.enterprise.openfinance.uc09.domain.query.GetMotorPoliciesQuery;
import com.enterprise.openfinance.uc09.domain.query.GetMotorPolicyQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InsuranceDataService implements InsuranceDataUseCase {

    private static final String REQUIRED_SCOPE = "ReadPolicies";

    private final InsuranceConsentPort consentPort;
    private final MotorPolicyReadPort readPort;
    private final InsurancePolicyCachePort cachePort;
    private final InsuranceDataSettings settings;
    private final Clock clock;

    public InsuranceDataService(InsuranceConsentPort consentPort,
                                MotorPolicyReadPort readPort,
                                InsurancePolicyCachePort cachePort,
                                InsuranceDataSettings settings,
                                Clock clock) {
        this.consentPort = consentPort;
        this.readPort = readPort;
        this.cachePort = cachePort;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public InsurancePolicyListResult listMotorPolicies(GetMotorPoliciesQuery query) {
        InsuranceConsentContext consent = validateConsent(query.consentId(), query.tppId());

        int page = query.resolvePage();
        int pageSize = query.resolvePageSize(settings.defaultPageSize(), settings.maxPageSize());
        Instant now = Instant.now(clock);
        String cacheKey = "motor-policies:" + query.consentId() + ':' + query.tppId() + ':' + page + ':' + pageSize;

        var cached = cachePort.getPolicyList(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<MotorPolicy> activePolicies = readPort.findByPolicyIds(consent.policyIds()).stream()
                .filter(MotorPolicy::isActive)
                .sorted(Comparator.comparing(MotorPolicy::policyId))
                .toList();

        InsurancePolicyListResult paged = paginate(activePolicies, page, pageSize).withCacheHit(false);
        cachePort.putPolicyList(cacheKey, paged, now.plus(settings.cacheTtl()));
        return paged;
    }

    @Override
    public InsurancePolicyItemResult getMotorPolicy(GetMotorPolicyQuery query) {
        InsuranceConsentContext consent = validateConsent(query.consentId(), query.tppId());
        ensurePolicyAccess(consent, query.policyId());

        Instant now = Instant.now(clock);
        String cacheKey = "motor-policy:" + query.consentId() + ':' + query.policyId();

        var cached = cachePort.getPolicy(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        MotorPolicy policy = readPort.findByPolicyId(query.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        if (!policy.isActive()) {
            throw new ResourceNotFoundException("Policy not active");
        }

        InsurancePolicyItemResult result = new InsurancePolicyItemResult(policy, false);
        cachePort.putPolicy(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }

    private InsuranceConsentContext validateConsent(String consentId, String tppId) {
        InsuranceConsentContext consent = consentPort.findById(consentId)
                .orElseThrow(() -> new ForbiddenException("Consent not found"));

        Instant now = Instant.now(clock);
        if (!consent.belongsToTpp(tppId)) {
            throw new ForbiddenException("Consent participant mismatch");
        }
        if (!consent.isActive(now)) {
            throw new ForbiddenException("Consent expired");
        }
        if (!consent.hasScope(REQUIRED_SCOPE)) {
            throw new ForbiddenException("Required scope missing: " + REQUIRED_SCOPE);
        }

        return consent;
    }

    private static void ensurePolicyAccess(InsuranceConsentContext consent, String policyId) {
        if (!consent.allowsPolicy(policyId)) {
            throw new ForbiddenException("Resource not linked to consent");
        }
    }

    private static InsurancePolicyListResult paginate(List<MotorPolicy> source, int page, int pageSize) {
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= source.size()) {
            return new InsurancePolicyListResult(List.of(), page, pageSize, source.size(), false);
        }

        int toIndex = Math.min(source.size(), fromIndex + pageSize);
        return new InsurancePolicyListResult(source.subList(fromIndex, toIndex), page, pageSize, source.size(), false);
    }
}
