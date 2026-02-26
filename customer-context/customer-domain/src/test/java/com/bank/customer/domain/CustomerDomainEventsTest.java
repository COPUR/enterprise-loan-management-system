package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CustomerDomainEventsTest {

    @Test
    void createdEventShouldExposeFieldsAndToString() {
        CustomerCreatedEvent event = new CustomerCreatedEvent(CustomerId.of("CUST-EVT-001"), "Ali Copur");

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getCustomerId().getValue()).isEqualTo("CUST-EVT-001");
        assertThat(event.getCustomerName()).isEqualTo("Ali Copur");
        assertThat(event.toString()).contains("CUST-EVT-001").contains("Ali Copur");
    }

    @Test
    void contactUpdatedEventShouldExposeFields() {
        CustomerContactUpdatedEvent event = new CustomerContactUpdatedEvent(
            CustomerId.of("CUST-EVT-002"),
            "new@example.com",
            "+971500001111"
        );

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getCustomerId().getValue()).isEqualTo("CUST-EVT-002");
        assertThat(event.getNewEmail()).isEqualTo("new@example.com");
        assertThat(event.getNewPhoneNumber()).isEqualTo("+971500001111");
    }

    @Test
    void creditLimitUpdatedEventShouldExposeFields() {
        CustomerCreditLimitUpdatedEvent event = new CustomerCreditLimitUpdatedEvent(
            CustomerId.of("CUST-EVT-003"),
            Money.aed(new BigDecimal("5000.00")),
            Money.aed(new BigDecimal("9000.00"))
        );

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getCustomerId().getValue()).isEqualTo("CUST-EVT-003");
        assertThat(event.getOldCreditLimit()).isEqualTo(Money.aed(new BigDecimal("5000.00")));
        assertThat(event.getNewCreditLimit()).isEqualTo(Money.aed(new BigDecimal("9000.00")));
    }

    @Test
    void creditReservedAndReleasedEventsShouldExposeAmount() {
        CustomerCreditReservedEvent reservedEvent = new CustomerCreditReservedEvent(
            CustomerId.of("CUST-EVT-004"),
            Money.aed(new BigDecimal("1500.00"))
        );
        CustomerCreditReleasedEvent releasedEvent = new CustomerCreditReleasedEvent(
            CustomerId.of("CUST-EVT-004"),
            Money.aed(new BigDecimal("700.00"))
        );

        assertThat(reservedEvent.getEventId()).isNotBlank();
        assertThat(reservedEvent.getOccurredOn()).isNotNull();
        assertThat(reservedEvent.getCustomerId().getValue()).isEqualTo("CUST-EVT-004");
        assertThat(reservedEvent.getReservedAmount()).isEqualTo(Money.aed(new BigDecimal("1500.00")));

        assertThat(releasedEvent.getEventId()).isNotBlank();
        assertThat(releasedEvent.getOccurredOn()).isNotNull();
        assertThat(releasedEvent.getCustomerId().getValue()).isEqualTo("CUST-EVT-004");
        assertThat(releasedEvent.getReleasedAmount()).isEqualTo(Money.aed(new BigDecimal("700.00")));
    }

    @Test
    void creditScoreUpdatedEventShouldExposeFieldsAndToString() {
        CustomerCreditScoreUpdatedEvent event = new CustomerCreditScoreUpdatedEvent(
            CustomerId.of("CUST-EVT-005"),
            780
        );

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getCustomerId().getValue()).isEqualTo("CUST-EVT-005");
        assertThat(event.getNewCreditScore()).isEqualTo(780);
        assertThat(event.toString()).contains("CUST-EVT-005").contains("780");
    }
}
