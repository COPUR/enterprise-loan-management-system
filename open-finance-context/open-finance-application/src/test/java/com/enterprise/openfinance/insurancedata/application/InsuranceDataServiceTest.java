package com.enterprise.openfinance.insurancedata.application;

import com.enterprise.openfinance.insurancedata.domain.exception.ForbiddenException;
import com.enterprise.openfinance.insurancedata.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.insurancedata.domain.model.InsuranceConsentContext;
import com.enterprise.openfinance.insurancedata.domain.model.InsuranceDataSettings;
import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicy;
import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicyStatus;
import com.enterprise.openfinance.insurancedata.domain.port.out.InsuranceConsentPort;
import com.enterprise.openfinance.insurancedata.domain.port.out.InsurancePolicyCachePort;
import com.enterprise.openfinance.insurancedata.domain.port.out.MotorPolicyReadPort;
import com.enterprise.openfinance.insurancedata.domain.query.GetMotorPoliciesQuery;
import com.enterprise.openfinance.insurancedata.domain.query.GetMotorPolicyQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class InsuranceDataServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-09T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldListActivePoliciesAndPopulateCache() {
        TestConsentPort consentPort = new TestConsentPort();
        TestReadPort readPort = new TestReadPort();
        TestCachePort cachePort = new TestCachePort();
        InsuranceDataService service = service(consentPort, readPort, cachePort);

        InsurancePolicyListResult first = service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-INS-001", "TPP-001", "ix-1", 1, 10
        ));

        InsurancePolicyListResult second = service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-INS-001", "TPP-001", "ix-1", 1, 10
        ));

        assertThat(first.policies()).hasSize(2);
        assertThat(first.cacheHit()).isFalse();
        assertThat(second.cacheHit()).isTrue();
        assertThat(readPort.listLookups).isEqualTo(1);
    }

    @Test
    void shouldGetPolicyDetailAndUseCache() {
        TestConsentPort consentPort = new TestConsentPort();
        TestReadPort readPort = new TestReadPort();
        TestCachePort cachePort = new TestCachePort();
        InsuranceDataService service = service(consentPort, readPort, cachePort);

        InsurancePolicyItemResult first = service.getMotorPolicy(new GetMotorPolicyQuery(
                "CONS-INS-001", "TPP-001", "POL-MTR-001", "ix-1"
        ));
        InsurancePolicyItemResult second = service.getMotorPolicy(new GetMotorPolicyQuery(
                "CONS-INS-001", "TPP-001", "POL-MTR-001", "ix-1"
        ));

        assertThat(first.policy().policyId()).isEqualTo("POL-MTR-001");
        assertThat(first.cacheHit()).isFalse();
        assertThat(second.cacheHit()).isTrue();
        assertThat(readPort.itemLookups).isEqualTo(1);
    }

    @Test
    void shouldRejectConsentViolations() {
        TestConsentPort consentPort = new TestConsentPort();
        InsuranceDataService service = service(consentPort, new TestReadPort(), new TestCachePort());

        assertThatThrownBy(() -> service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-UNKNOWN", "TPP-001", "ix-1", 1, 10
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Consent not found");

        assertThatThrownBy(() -> service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-INS-EXPIRED", "TPP-001", "ix-1", 1, 10
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expired");

        assertThatThrownBy(() -> service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-INS-001", "TPP-OTHER", "ix-1", 1, 10
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("participant mismatch");

        assertThatThrownBy(() -> service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-INS-RO", "TPP-001", "ix-1", 1, 10
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Required scope missing");
    }

    @Test
    void shouldRejectBolaAndMissingOrInactivePolicy() {
        TestConsentPort consentPort = new TestConsentPort();
        InsuranceDataService service = service(consentPort, new TestReadPort(), new TestCachePort());

        assertThatThrownBy(() -> service.getMotorPolicy(new GetMotorPolicyQuery(
                "CONS-INS-001", "TPP-001", "POL-MTR-003", "ix-1"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("linked to consent");

        assertThatThrownBy(() -> service.getMotorPolicy(new GetMotorPolicyQuery(
                "CONS-INS-INACTIVE", "TPP-001", "POL-MTR-003", "ix-1"
        ))).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("active");

        assertThatThrownBy(() -> service.getMotorPolicy(new GetMotorPolicyQuery(
                "CONS-INS-001", "TPP-001", "POL-404", "ix-1"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("linked to consent");
    }

    @Test
    void shouldReturnEmptyPageWhenOutOfRange() {
        InsuranceDataService service = service(new TestConsentPort(), new TestReadPort(), new TestCachePort());

        InsurancePolicyListResult result = service.listMotorPolicies(new GetMotorPoliciesQuery(
                "CONS-INS-001", "TPP-001", "ix-1", 50, 10
        ));

        assertThat(result.policies()).isEmpty();
        assertThat(result.totalRecords()).isEqualTo(2);
    }

    private static InsuranceDataService service(InsuranceConsentPort consentPort,
                                                MotorPolicyReadPort readPort,
                                                InsurancePolicyCachePort cachePort) {
        return new InsuranceDataService(
                consentPort,
                readPort,
                cachePort,
                new InsuranceDataSettings(Duration.ofSeconds(30), 50, 200),
                CLOCK
        );
    }

    private static final class TestConsentPort implements InsuranceConsentPort {
        private final Map<String, InsuranceConsentContext> data = new ConcurrentHashMap<>();

        private TestConsentPort() {
            data.put("CONS-INS-001", new InsuranceConsentContext(
                    "CONS-INS-001",
                    "TPP-001",
                    Set.of("ReadPolicies"),
                    Set.of("POL-MTR-001", "POL-MTR-002"),
                    Instant.parse("2099-01-01T00:00:00Z")
            ));

            data.put("CONS-INS-INACTIVE", new InsuranceConsentContext(
                    "CONS-INS-INACTIVE",
                    "TPP-001",
                    Set.of("ReadPolicies"),
                    Set.of("POL-MTR-003"),
                    Instant.parse("2099-01-01T00:00:00Z")
            ));

            data.put("CONS-INS-RO", new InsuranceConsentContext(
                    "CONS-INS-RO",
                    "TPP-001",
                    Set.of("ReadBalances"),
                    Set.of("POL-MTR-001"),
                    Instant.parse("2099-01-01T00:00:00Z")
            ));

            data.put("CONS-INS-EXPIRED", new InsuranceConsentContext(
                    "CONS-INS-EXPIRED",
                    "TPP-001",
                    Set.of("ReadPolicies"),
                    Set.of("POL-MTR-001"),
                    Instant.parse("2026-01-01T00:00:00Z")
            ));
        }

        @Override
        public Optional<InsuranceConsentContext> findById(String consentId) {
            return Optional.ofNullable(data.get(consentId));
        }
    }

    private static final class TestReadPort implements MotorPolicyReadPort {
        private final Map<String, MotorPolicy> policies = new ConcurrentHashMap<>();
        private int listLookups;
        private int itemLookups;

        private TestReadPort() {
            policies.put("POL-MTR-001", policy("POL-MTR-001", MotorPolicyStatus.ACTIVE));
            policies.put("POL-MTR-002", policy("POL-MTR-002", MotorPolicyStatus.ACTIVE));
            policies.put("POL-MTR-003", policy("POL-MTR-003", MotorPolicyStatus.LAPSED));
        }

        @Override
        public List<MotorPolicy> findByPolicyIds(Set<String> policyIds) {
            listLookups++;
            return policyIds.stream().map(policies::get).filter(p -> p != null).toList();
        }

        @Override
        public Optional<MotorPolicy> findByPolicyId(String policyId) {
            itemLookups++;
            return Optional.ofNullable(policies.get(policyId));
        }
    }

    private static final class TestCachePort implements InsurancePolicyCachePort {
        private final Map<String, CacheEntry<InsurancePolicyListResult>> listCache = new ConcurrentHashMap<>();
        private final Map<String, CacheEntry<InsurancePolicyItemResult>> itemCache = new ConcurrentHashMap<>();

        @Override
        public Optional<InsurancePolicyListResult> getPolicyList(String key, Instant now) {
            CacheEntry<InsurancePolicyListResult> item = listCache.get(key);
            if (item == null || !item.expiresAt().isAfter(now)) {
                listCache.remove(key);
                return Optional.empty();
            }
            return Optional.of(item.value().withCacheHit(true));
        }

        @Override
        public void putPolicyList(String key, InsurancePolicyListResult result, Instant expiresAt) {
            listCache.put(key, new CacheEntry<>(result.withCacheHit(false), expiresAt));
        }

        @Override
        public Optional<InsurancePolicyItemResult> getPolicy(String key, Instant now) {
            CacheEntry<InsurancePolicyItemResult> item = itemCache.get(key);
            if (item == null || !item.expiresAt().isAfter(now)) {
                itemCache.remove(key);
                return Optional.empty();
            }
            return Optional.of(item.value().withCacheHit(true));
        }

        @Override
        public void putPolicy(String key, InsurancePolicyItemResult result, Instant expiresAt) {
            itemCache.put(key, new CacheEntry<>(result.withCacheHit(false), expiresAt));
        }
    }

    private static MotorPolicy policy(String policyId, MotorPolicyStatus status) {
        return new MotorPolicy(
                policyId,
                "MTR-2026-" + policyId.substring(policyId.length() - 3),
                "Ali Copur",
                null,
                "Toyota",
                "Camry",
                2023,
                new BigDecimal("1800.00"),
                "AED",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                status,
                List.of("Collision", "Theft")
        );
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
