package com.bank.customer.infrastructure.web.enhanced;

import com.bank.customer.application.CustomerManagementService;
import com.bank.customer.application.dto.CreateCustomerRequest;
import com.bank.customer.application.dto.CustomerResponse;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerApiControllerTest {

    @Mock
    private CustomerManagementService customerService;

    @InjectMocks
    private CustomerApiController controller;

    @BeforeEach
    void setUpRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("localhost");
        request.setScheme("http");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createCustomerShouldReturnCreatedWithIdempotencyHeaders() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Ali", "Copur", "ali@example.com", "+971500000001", new BigDecimal("10000.00"), "AED"
        );
        CustomerResponse response = sampleResponse("CUST-API-001");
        when(customerService.createCustomer(request)).thenReturn(response);

        ResponseEntity<EntityModel<CustomerResponse>> entity =
            controller.createCustomer("idem-001", "GB-FCA-123456", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getHeaders().getFirst("X-Resource-Id")).isEqualTo("CUST-API-001");
        assertThat(entity.getHeaders().getFirst("X-Idempotency-Key")).isEqualTo("idem-001");
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).isEqualTo(response);
        assertThat(entity.getBody().getLinks()).isNotEmpty();
    }

    @Test
    void getCustomerShouldReturnOkWithResourceVersionHeader() {
        CustomerResponse response = sampleResponse("CUST-API-002");
        when(customerService.findCustomerById("CUST-API-002")).thenReturn(response);

        ResponseEntity<EntityModel<CustomerResponse>> entity = controller.getCustomer("CUST-API-002");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().getFirst("X-Resource-Version"))
            .isEqualTo(response.lastModifiedAt().toString());
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).isEqualTo(response);
    }

    @Test
    void updateCreditLimitShouldReturnOkAndEchoIdempotencyHeader() {
        CustomerResponse response = sampleResponse("CUST-API-003");
        when(customerService.updateCreditLimit(eq("CUST-API-003"), eq(Money.aed(new BigDecimal("15000.00")))))
            .thenReturn(response);

        CustomerApiController.UpdateCreditLimitRequest request =
            new CustomerApiController.UpdateCreditLimitRequest(new BigDecimal("15000.00"), "AED", "Review");

        ResponseEntity<EntityModel<CustomerResponse>> entity =
            controller.updateCreditLimit("CUST-API-003", "idem-003", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().getFirst("X-Idempotency-Key")).isEqualTo("idem-003");
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).isEqualTo(response);
    }

    @Test
    void reserveCreditShouldReturnOkAndEchoIdempotencyHeader() {
        CustomerResponse response = sampleResponse("CUST-API-004");
        when(customerService.reserveCredit(eq("CUST-API-004"), eq(Money.aed(new BigDecimal("500.00")))))
            .thenReturn(response);

        CustomerApiController.ReserveCreditRequest request =
            new CustomerApiController.ReserveCreditRequest(new BigDecimal("500.00"), "AED", "Booking");

        ResponseEntity<EntityModel<CustomerResponse>> entity =
            controller.reserveCredit("CUST-API-004", "idem-004", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().getFirst("X-Idempotency-Key")).isEqualTo("idem-004");
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).isEqualTo(response);
    }

    @Test
    void releaseCreditShouldReturnOkAndEchoIdempotencyHeader() {
        CustomerResponse response = sampleResponse("CUST-API-005");
        when(customerService.releaseCredit(eq("CUST-API-005"), eq(Money.aed(new BigDecimal("250.00")))))
            .thenReturn(response);

        CustomerApiController.ReleaseCreditRequest request =
            new CustomerApiController.ReleaseCreditRequest(new BigDecimal("250.00"), "AED", "Settlement");

        ResponseEntity<EntityModel<CustomerResponse>> entity =
            controller.releaseCredit("CUST-API-005", "idem-005", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().getFirst("X-Idempotency-Key")).isEqualTo("idem-005");
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getContent()).isEqualTo(response);
    }

    @Test
    void getCustomerEventsShouldReturnEmitter() {
        SseEmitter emitter = controller.getCustomerEvents("CUST-API-006", 10);

        assertThat(emitter).isNotNull();
    }

    @Test
    void getCustomerMetricsShouldReturnDefaultMetrics() {
        ResponseEntity<CustomerApiController.CustomerMetricsResponse> entity =
            controller.getCustomerMetrics("CUST-API-007");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().customerId()).isEqualTo("CUST-API-007");
        assertThat(entity.getBody().totalTransactions()).isZero();
    }

    private static CustomerResponse sampleResponse(String id) {
        return new CustomerResponse(
            id,
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("10000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("9000.00"),
            "ACTIVE",
            720,
            new BigDecimal("6000.00"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z")
        );
    }
}
