package com.bank.customer.application;

import com.bank.customer.application.dto.CreateCustomerRequest;
import com.bank.customer.application.dto.CreateCustomerRequestWithCreditScore;
import com.bank.customer.application.dto.CustomerResponse;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerRepository;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerManagementServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerManagementService service;

    @Test
    void createCustomerShouldPersistAndReturnResponse() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("5000.00"),
            "AED"
        );
        when(customerRepository.existsByEmail("ali@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = service.createCustomer(request);

        assertThat(response.email()).isEqualTo("ali@example.com");
        assertThat(response.firstName()).isEqualTo("Ali");
        assertThat(response.creditLimit()).isEqualByComparingTo("5000.00");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomerShouldRejectDuplicateEmail() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("5000.00"),
            "AED"
        );
        when(customerRepository.existsByEmail("ali@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomer(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void findCustomerByIdShouldThrowWhenNotFound() {
        when(customerRepository.findById(any(CustomerId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCustomerById("CUST-MISSING"))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining("CUST-MISSING");
    }

    @Test
    void reserveAndReleaseCreditShouldPersistChanges() {
        Customer customer = customer();
        when(customerRepository.findById(any(CustomerId.class))).thenReturn(Optional.of(customer));
        doReturn(customer).when(customerRepository).save(customer);

        service.reserveCredit(customer.getId().getValue(), Money.aed(new BigDecimal("1000.00")));
        service.releaseCredit(customer.getId().getValue(), Money.aed(new BigDecimal("500.00")));

        assertThat(customer.getCreditProfile().getUsedCredit()).isEqualTo(Money.aed(new BigDecimal("500.00")));
        verify(customerRepository, times(2)).save(customer);
    }

    @Test
    void createCustomerWithCreditScoreShouldPersistAndReturnResponse() {
        CreateCustomerRequestWithCreditScore request = new CreateCustomerRequestWithCreditScore(
            "Lina",
            "Hassan",
            "lina@example.com",
            "+971500000002",
            Money.aed(new BigDecimal("2000.00")),
            750
        );
        when(customerRepository.existsByEmail("lina@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = service.createCustomerWithCreditScore(request);

        assertThat(response.creditScore()).isEqualTo(750);
        assertThat(response.monthlyIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.creditLimit()).isEqualByComparingTo("10000.00");
    }

    @Test
    void updateCreditScoreShouldPersistUpdatedCustomer() {
        Customer customer = Customer.createWithCreditScore(
            CustomerId.of("CUST-APP-001"),
            "Sara",
            "Noor",
            "sara@example.com",
            "+971500000003",
            Money.aed(new BigDecimal("2000.00")),
            650
        );
        when(customerRepository.findById(any(CustomerId.class))).thenReturn(Optional.of(customer));
        doReturn(customer).when(customerRepository).save(customer);

        CustomerResponse response = service.updateCreditScore(customer.getId().getValue(), 780);

        assertThat(response.creditScore()).isEqualTo(780);
        assertThat(response.creditLimit()).isEqualByComparingTo("10000.00");
        verify(customerRepository).save(customer);
    }

    @Test
    void findCustomerByEmailShouldThrowWhenNotFound() {
        when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCustomerByEmail("missing@example.com"))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining("missing@example.com");
    }

    @Test
    void isEligibleForLoanShouldDelegateToAggregateRules() {
        Customer customer = Customer.createWithCreditScore(
            CustomerId.of("CUST-APP-002"),
            "Nora",
            "Khan",
            "nora@example.com",
            "+971500000004",
            Money.aed(new BigDecimal("2500.00")),
            700
        );
        when(customerRepository.findById(any(CustomerId.class))).thenReturn(Optional.of(customer));

        boolean eligible = service.isEligibleForLoan(customer.getId().getValue(), Money.aed(new BigDecimal("4000.00")));

        assertThat(eligible).isTrue();
    }

    @Test
    void updateContactInformationShouldPersistAndReturnUpdatedResponse() {
        Customer customer = customer();
        when(customerRepository.findById(any(CustomerId.class))).thenReturn(Optional.of(customer));
        doReturn(customer).when(customerRepository).save(customer);

        CustomerResponse response = service.updateContactInformation(
            customer.getId().getValue(),
            "new-email@example.com",
            "+971500009999"
        );

        assertThat(response.email()).isEqualTo("new-email@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+971500009999");
        verify(customerRepository).save(customer);
    }

    private static Customer customer() {
        return Customer.create(
            CustomerId.of("CUST-APP-BASE"),
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(new BigDecimal("10000.00"))
        );
    }
}
