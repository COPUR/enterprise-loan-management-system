package com.enterprise.openfinance.insurancedata.infrastructure.rest;

import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicy;
import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicyStatus;
import com.enterprise.openfinance.insurancedata.domain.port.in.InsuranceDataUseCase;
import com.enterprise.openfinance.insurancedata.infrastructure.rest.dto.InsurancePoliciesResponse;
import com.enterprise.openfinance.insurancedata.infrastructure.rest.dto.InsurancePolicyResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class InsuranceDataControllerUnitTest {

    @Test
    void shouldReturnPoliciesAndDetail() {
        InsuranceDataUseCase useCase = Mockito.mock(InsuranceDataUseCase.class);
        InsuranceDataController controller = new InsuranceDataController(useCase);

        Mockito.when(useCase.listMotorPolicies(Mockito.any())).thenReturn(new InsurancePolicyListResult(
                List.of(policy("POL-MTR-001")), 1, 50, 1, false
        ));
        Mockito.when(useCase.getMotorPolicy(Mockito.any())).thenReturn(new InsurancePolicyItemResult(policy("POL-MTR-001"), false));

        ResponseEntity<InsurancePoliciesResponse> list = controller.listMotorPolicies(
                "DPoP token", "proof", "ix-1", "CONS-INS-001", "TPP-001", null, null, null
        );
        ResponseEntity<InsurancePolicyResponse> item = controller.getMotorPolicy(
                "DPoP token", "proof", "ix-1", "CONS-INS-001", "TPP-001", "POL-MTR-001", null
        );

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getHeaders().getFirst("X-OF-Cache")).isEqualTo("MISS");
        assertThat(item.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        InsuranceDataUseCase useCase = Mockito.mock(InsuranceDataUseCase.class);
        InsuranceDataController controller = new InsuranceDataController(useCase);

        Mockito.when(useCase.listMotorPolicies(Mockito.any())).thenReturn(new InsurancePolicyListResult(
                List.of(policy("POL-MTR-001")), 1, 50, 1, false
        ));
        Mockito.when(useCase.getMotorPolicy(Mockito.any())).thenReturn(new InsurancePolicyItemResult(policy("POL-MTR-001"), false));

        ResponseEntity<InsurancePoliciesResponse> firstList = controller.listMotorPolicies(
                "DPoP token", "proof", "ix-1", "CONS-INS-001", "TPP-001", null, null, null
        );
        ResponseEntity<InsurancePolicyResponse> firstItem = controller.getMotorPolicy(
                "DPoP token", "proof", "ix-1", "CONS-INS-001", "TPP-001", "POL-MTR-001", null
        );

        ResponseEntity<InsurancePoliciesResponse> secondList = controller.listMotorPolicies(
                "DPoP token", "proof", "ix-1", "CONS-INS-001", "TPP-001", null, null, firstList.getHeaders().getETag()
        );
        ResponseEntity<InsurancePolicyResponse> secondItem = controller.getMotorPolicy(
                "DPoP token", "proof", "ix-1", "CONS-INS-001", "TPP-001", "POL-MTR-001", firstItem.getHeaders().getETag()
        );

        assertThat(secondList.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(secondItem.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        InsuranceDataUseCase useCase = Mockito.mock(InsuranceDataUseCase.class);
        InsuranceDataController controller = new InsuranceDataController(useCase);

        assertThatThrownBy(() -> controller.listMotorPolicies(
                "Basic token", "proof", "ix-1", "CONS-INS-001", "TPP-001", null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    private static MotorPolicy policy(String policyId) {
        return new MotorPolicy(
                policyId,
                "MTR-1",
                "Ali Copur",
                "A***",
                "Toyota",
                "Camry",
                2023,
                new BigDecimal("1000.00"),
                "AED",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                MotorPolicyStatus.ACTIVE,
                List.of("Collision", "Theft")
        );
    }
}
