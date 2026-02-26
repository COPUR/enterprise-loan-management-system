package com.bank.customer.infrastructure.persistence;

import com.bank.customer.domain.Customer;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerJpaEntityTest {

    @Test
    void fromDomainShouldMapCoreFields() {
        Customer customer = sampleCustomer("CUST-INFRA-001", "a@example.com", "10000.00");
        customer.reserveCredit(Money.aed(new BigDecimal("1200.00")));

        CustomerJpaEntity entity = CustomerJpaEntity.fromDomain(customer);

        assertThat(entity.getCustomerId()).isEqualTo("CUST-INFRA-001");
        assertThat(entity.getFirstName()).isEqualTo("Ali");
        assertThat(entity.getLastName()).isEqualTo("Copur");
        assertThat(entity.getEmail()).isEqualTo("a@example.com");
        assertThat(entity.getCreditLimit()).isEqualByComparingTo("10000.00");
        assertThat(entity.getUsedCredit()).isEqualByComparingTo("1200.00");
        assertThat(entity.getCurrency()).isEqualTo("AED");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void toDomainShouldPreserveCreditUsageAndIdentity() {
        Customer customer = sampleCustomer("CUST-INFRA-002", "b@example.com", "9000.00");
        customer.reserveCredit(Money.aed(new BigDecimal("750.00")));

        CustomerJpaEntity entity = CustomerJpaEntity.fromDomain(customer);

        Customer mapped = entity.toDomain();

        assertThat(mapped.getId().getValue()).isEqualTo("CUST-INFRA-002");
        assertThat(mapped.getEmail()).isEqualTo("b@example.com");
        assertThat(mapped.getCreditProfile().getCreditLimit().getAmount()).isEqualByComparingTo("9000.00");
        assertThat(mapped.getCreditProfile().getUsedCredit().getAmount()).isEqualByComparingTo("750.00");
    }

    @Test
    void updateFromDomainShouldRefreshMutableFields() {
        Customer initial = sampleCustomer("CUST-INFRA-003", "initial@example.com", "7000.00");
        CustomerJpaEntity entity = CustomerJpaEntity.fromDomain(initial);

        Customer updated = sampleCustomer("CUST-INFRA-003", "updated@example.com", "11000.00");
        updated.reserveCredit(Money.aed(new BigDecimal("500.00")));

        entity.updateFromDomain(updated);

        assertThat(entity.getEmail()).isEqualTo("updated@example.com");
        assertThat(entity.getCreditLimit()).isEqualByComparingTo("11000.00");
        assertThat(entity.getUsedCredit()).isEqualByComparingTo("500.00");
        assertThat(entity.getCurrency()).isEqualTo("AED");
    }

    private static Customer sampleCustomer(String id, String email, String creditLimit) {
        return Customer.create(
            CustomerId.of(id),
            "Ali",
            "Copur",
            email,
            "+971500000001",
            Money.aed(new BigDecimal(creditLimit))
        );
    }
}
