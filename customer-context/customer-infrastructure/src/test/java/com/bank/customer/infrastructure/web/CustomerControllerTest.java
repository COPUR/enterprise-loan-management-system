package com.bank.customer.infrastructure.web;

import com.bank.customer.application.CustomerManagementService;
import com.bank.customer.application.dto.CreateCustomerRequest;
import com.bank.customer.application.dto.CustomerResponse;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerManagementService customerService;

    @InjectMocks
    private CustomerController controller;

    @Test
    void createCustomerShouldReturnCreatedResponse() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Ali", "Copur", "ali@example.com", "+971500000001", new BigDecimal("10000.00"), "AED"
        );
        CustomerResponse response = sampleResponse("CUST-WEB-001");
        when(customerService.createCustomer(request)).thenReturn(response);

        ResponseEntity<CustomerResponse> entity = controller.createCustomer(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void getCustomerShouldReturnOkResponse() {
        CustomerResponse response = sampleResponse("CUST-WEB-002");
        when(customerService.findCustomerById("CUST-WEB-002")).thenReturn(response);

        ResponseEntity<CustomerResponse> entity = controller.getCustomer("CUST-WEB-002");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void updateCreditLimitShouldConvertMoneyAndDelegate() {
        CustomerResponse response = sampleResponse("CUST-WEB-003");
        when(customerService.updateCreditLimit(eq("CUST-WEB-003"), eq(Money.aed(new BigDecimal("12000.00")))))
            .thenReturn(response);

        CustomerController.UpdateCreditLimitRequest request =
            new CustomerController.UpdateCreditLimitRequest(new BigDecimal("12000.00"), "AED");

        ResponseEntity<CustomerResponse> entity = controller.updateCreditLimit("CUST-WEB-003", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void reserveCreditShouldConvertMoneyAndDelegate() {
        CustomerResponse response = sampleResponse("CUST-WEB-004");
        when(customerService.reserveCredit(eq("CUST-WEB-004"), eq(Money.aed(new BigDecimal("300.00")))))
            .thenReturn(response);

        CustomerController.ReserveCreditRequest request =
            new CustomerController.ReserveCreditRequest(new BigDecimal("300.00"), "AED");

        ResponseEntity<CustomerResponse> entity = controller.reserveCredit("CUST-WEB-004", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void releaseCreditShouldConvertMoneyAndDelegate() {
        CustomerResponse response = sampleResponse("CUST-WEB-005");
        when(customerService.releaseCredit(eq("CUST-WEB-005"), eq(Money.aed(new BigDecimal("150.00")))))
            .thenReturn(response);

        CustomerController.ReleaseCreditRequest request =
            new CustomerController.ReleaseCreditRequest(new BigDecimal("150.00"), "AED");

        ResponseEntity<CustomerResponse> entity = controller.releaseCredit("CUST-WEB-005", request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void reserveCreditShouldPassConvertedMoneyToService() {
        CustomerResponse response = sampleResponse("CUST-WEB-006");
        when(customerService.reserveCredit(eq("CUST-WEB-006"), eq(Money.aed(new BigDecimal("999.99")))))
            .thenReturn(response);

        CustomerController.ReserveCreditRequest request =
            new CustomerController.ReserveCreditRequest(new BigDecimal("999.99"), "AED");

        controller.reserveCredit("CUST-WEB-006", request);

        ArgumentCaptor<Money> captor = ArgumentCaptor.forClass(Money.class);
        verify(customerService).reserveCredit(eq("CUST-WEB-006"), captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("999.99");
        assertThat(captor.getValue().getCurrency().getCurrencyCode()).isEqualTo("AED");
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
