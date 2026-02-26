package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CustomerTest {

    @Test
    void createShouldPopulateFieldsAndAddCreatedEvent() {
        Customer customer = Customer.create(
            CustomerId.of("CUST-UNIT-001"),
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(new BigDecimal("10000.00"))
        );

        assertThat(customer.getId().getValue()).isEqualTo("CUST-UNIT-001");
        assertThat(customer.getFullName()).isEqualTo("Ali Copur");
        assertThat(customer.getCreditProfile().getCreditLimit())
            .isEqualTo(Money.aed(new BigDecimal("10000.00")));
        assertThat(customer.getDomainEvents())
            .isNotEmpty()
            .first()
            .isInstanceOf(CustomerCreatedEvent.class);
    }

    @Test
    void createShouldRejectInvalidEmail() {
        assertThatThrownBy(() -> Customer.create(
            CustomerId.of("CUST-UNIT-002"),
            "Ali",
            "Copur",
            "invalid-email",
            "+971500000001",
            Money.aed(new BigDecimal("10000.00"))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email must be valid");
    }

    @Test
    void createWithCreditScoreShouldCalculateCreditLimitFromIncomeAndScore() {
        Customer customer = Customer.createWithCreditScore(
            CustomerId.of("CUST-UNIT-003"),
            "Lina",
            "Hassan",
            "lina@example.com",
            "+971500000002",
            Money.aed(new BigDecimal("2000.00")),
            760
        );

        assertThat(customer.getCreditScore()).isEqualTo(760);
        assertThat(customer.getMonthlyIncome()).isEqualTo(Money.aed(new BigDecimal("2000.00")));
        assertThat(customer.getCreditProfile().getCreditLimit())
            .isEqualTo(Money.aed(new BigDecimal("10000.00")));
    }

    @Test
    void createWithCreditScoreShouldRejectIncomeBelowMinimum() {
        assertThatThrownBy(() -> Customer.createWithCreditScore(
            CustomerId.of("CUST-UNIT-004"),
            "Lina",
            "Hassan",
            "lina@example.com",
            "+971500000002",
            Money.aed(new BigDecimal("999.00")),
            700
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Monthly income must be at least");
    }

    @Test
    void reserveCreditShouldUpdateUsedCreditAndEmitEvent() {
        Customer customer = createCustomer();

        customer.reserveCredit(Money.aed(new BigDecimal("1200.00")));

        assertThat(customer.getCreditProfile().getUsedCredit()).isEqualTo(Money.aed(new BigDecimal("1200.00")));
        assertThat(lastEvent(customer)).isInstanceOf(CustomerCreditReservedEvent.class);
    }

    @Test
    void reserveCreditShouldThrowWhenInsufficient() {
        Customer customer = createCustomer();

        assertThatThrownBy(() -> customer.reserveCredit(Money.aed(new BigDecimal("99999.00"))))
            .isInstanceOf(InsufficientCreditException.class)
            .hasMessageContaining("insufficient credit");
    }

    @Test
    void releaseCreditShouldDecreaseUsedCreditAndEmitEvent() {
        Customer customer = createCustomer();
        customer.reserveCredit(Money.aed(new BigDecimal("5000.00")));

        customer.releaseCredit(Money.aed(new BigDecimal("2000.00")));

        assertThat(customer.getCreditProfile().getUsedCredit()).isEqualTo(Money.aed(new BigDecimal("3000.00")));
        assertThat(lastEvent(customer)).isInstanceOf(CustomerCreditReleasedEvent.class);
    }

    @Test
    void updateCreditLimitShouldChangeLimitAndEmitEvent() {
        Customer customer = createCustomer();

        customer.updateCreditLimit(Money.aed(new BigDecimal("15000.00")));

        assertThat(customer.getCreditProfile().getCreditLimit()).isEqualTo(Money.aed(new BigDecimal("15000.00")));
        assertThat(lastEvent(customer)).isInstanceOf(CustomerCreditLimitUpdatedEvent.class);
    }

    @Test
    void updateContactInformationShouldIgnoreInvalidEmailButUpdatePhone() {
        Customer customer = createCustomer();

        customer.updateContactInformation("invalid", "+971500000099");

        assertThat(customer.getEmail()).isEqualTo("ali@example.com");
        assertThat(customer.getPhoneNumber()).isEqualTo("+971500000099");
        assertThat(lastEvent(customer)).isInstanceOf(CustomerContactUpdatedEvent.class);
    }

    @Test
    void updateCreditScoreShouldRecalculateCreditLimitAndEmitEvent() {
        Customer customer = Customer.createWithCreditScore(
            CustomerId.of("CUST-UNIT-005"),
            "Sara",
            "Noor",
            "sara@example.com",
            "+971500000003",
            Money.aed(new BigDecimal("3000.00")),
            650
        );
        customer.reserveCredit(Money.aed(new BigDecimal("1000.00")));

        customer.updateCreditScore(780);

        assertThat(customer.getCreditScore()).isEqualTo(780);
        assertThat(customer.getCreditProfile().getCreditLimit()).isEqualTo(Money.aed(new BigDecimal("15000.00")));
        assertThat(customer.getCreditProfile().getUsedCredit()).isEqualTo(Money.aed(new BigDecimal("1000.00")));
        assertThat(lastEvent(customer)).isInstanceOf(CustomerCreditScoreUpdatedEvent.class);
    }

    @Test
    void isEligibleForLoanShouldRequireScoreAndAvailability() {
        Customer eligible = Customer.createWithCreditScore(
            CustomerId.of("CUST-UNIT-006"),
            "Nora",
            "Khan",
            "nora@example.com",
            "+971500000004",
            Money.aed(new BigDecimal("2500.00")),
            700
        );

        assertThat(eligible.isEligibleForLoan(Money.aed(new BigDecimal("5000.00")))).isTrue();
        assertThat(eligible.isEligibleForLoan(Money.aed(new BigDecimal("12000.00")))).isFalse();
    }

    private static Customer createCustomer() {
        return Customer.create(
            CustomerId.of("CUST-UNIT-BASE"),
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(new BigDecimal("10000.00"))
        );
    }

    private static DomainEvent lastEvent(Customer customer) {
        List<DomainEvent> events = customer.getDomainEvents();
        return events.get(events.size() - 1);
    }
}
