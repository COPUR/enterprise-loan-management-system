package com.bank.customer.infrastructure.persistence;

import com.bank.customer.domain.Customer;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.shared.kernel.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCustomerRepositoryTest {

    @Mock
    private JpaCustomerRepository.SpringDataCustomerRepository springDataRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Test
    void saveShouldPublishAndClearDomainEventsWhenPresent() {
        Customer customer = sampleCustomer("CUST-REPO-001", "repo1@example.com");
        when(springDataRepository.save(customer)).thenReturn(customer);
        doNothing().when(eventPublisher).publishAll(anyIterable());

        JpaCustomerRepository repository = new JpaCustomerRepository(springDataRepository, eventPublisher);
        Customer saved = repository.save(customer);

        assertThat(saved).isSameAs(customer);
        verify(eventPublisher).publishAll(anyIterable());
        assertThat(customer.hasUnpublishedEvents()).isFalse();
    }

    @Test
    void saveShouldNotPublishWhenNoDomainEvents() {
        Customer customer = sampleCustomer("CUST-REPO-002", "repo2@example.com");
        customer.clearDomainEvents();
        when(springDataRepository.save(customer)).thenReturn(customer);

        JpaCustomerRepository repository = new JpaCustomerRepository(springDataRepository, eventPublisher);
        repository.save(customer);

        verify(eventPublisher, never()).publishAll(anyIterable());
    }

    @Test
    void queryOperationsShouldDelegateToSpringDataRepository() {
        Customer customer = sampleCustomer("CUST-REPO-003", "repo3@example.com");
        when(springDataRepository.findByCustomerId("CUST-REPO-003")).thenReturn(Optional.of(customer));
        when(springDataRepository.findByEmail("repo3@example.com")).thenReturn(Optional.of(customer));
        when(springDataRepository.existsByCustomerId("CUST-REPO-003")).thenReturn(true);
        when(springDataRepository.existsByEmail("repo3@example.com")).thenReturn(true);
        when(springDataRepository.findByStatus("ACTIVE")).thenReturn(List.of(customer));
        when(springDataRepository.findHighValueCustomers(new BigDecimal("5000.00"))).thenReturn(List.of(customer));

        JpaCustomerRepository repository = new JpaCustomerRepository(springDataRepository, eventPublisher);

        assertThat(repository.findById(CustomerId.of("CUST-REPO-003"))).contains(customer);
        assertThat(repository.findByEmail("repo3@example.com")).contains(customer);
        assertThat(repository.existsById(CustomerId.of("CUST-REPO-003"))).isTrue();
        assertThat(repository.existsByEmail("repo3@example.com")).isTrue();
        assertThat(repository.findByStatus("ACTIVE")).containsExactly(customer);
        assertThat(repository.findHighValueCustomers(new BigDecimal("5000.00"))).containsExactly(customer);
    }

    @Test
    void deleteByIdShouldDeleteWhenCustomerExists() {
        Customer customer = sampleCustomer("CUST-REPO-004", "repo4@example.com");
        when(springDataRepository.findByCustomerId("CUST-REPO-004")).thenReturn(Optional.of(customer));

        JpaCustomerRepository repository = new JpaCustomerRepository(springDataRepository, eventPublisher);
        repository.deleteById(CustomerId.of("CUST-REPO-004"));

        verify(springDataRepository).delete(customer);
    }

    @Test
    void deleteByIdShouldIgnoreWhenCustomerDoesNotExist() {
        when(springDataRepository.findByCustomerId("CUST-REPO-005")).thenReturn(Optional.empty());

        JpaCustomerRepository repository = new JpaCustomerRepository(springDataRepository, eventPublisher);
        repository.deleteById(CustomerId.of("CUST-REPO-005"));

        verify(springDataRepository, never()).delete(any(Customer.class));
    }

    private static Customer sampleCustomer(String id, String email) {
        return Customer.create(
            CustomerId.of(id),
            "Ali",
            "Copur",
            email,
            "+971500000001",
            Money.aed(new BigDecimal("10000.00"))
        );
    }
}
