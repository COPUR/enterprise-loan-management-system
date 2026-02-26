package com.bank.customer.domain;

import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CreditProfileTest {

    @Test
    void createShouldInitializeWithZeroUsedCredit() {
        CreditProfile profile = CreditProfile.create(Money.aed(new BigDecimal("10000.00")));

        assertThat(profile.getCreditLimit()).isEqualTo(Money.aed(new BigDecimal("10000.00")));
        assertThat(profile.getUsedCredit()).isEqualTo(Money.aed(BigDecimal.ZERO));
        assertThat(profile.getAvailableCredit()).isEqualTo(Money.aed(new BigDecimal("10000.00")));
    }

    @Test
    void reserveCreditShouldIncreaseUsedCredit() {
        CreditProfile profile = CreditProfile.create(Money.aed(new BigDecimal("10000.00")));

        CreditProfile updated = profile.reserveCredit(Money.aed(new BigDecimal("2500.00")));

        assertThat(updated.getUsedCredit()).isEqualTo(Money.aed(new BigDecimal("2500.00")));
        assertThat(updated.getAvailableCredit()).isEqualTo(Money.aed(new BigDecimal("7500.00")));
    }

    @Test
    void reserveCreditShouldFailWhenAmountExceedsAvailability() {
        CreditProfile profile = CreditProfile.create(Money.aed(new BigDecimal("500.00")));

        assertThatThrownBy(() -> profile.reserveCredit(Money.aed(new BigDecimal("1000.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient available credit");
    }

    @Test
    void releaseCreditShouldNotGoBelowZero() {
        CreditProfile profile = CreditProfile.create(
            Money.aed(new BigDecimal("1000.00")),
            Money.aed(new BigDecimal("200.00"))
        );

        CreditProfile updated = profile.releaseCredit(Money.aed(new BigDecimal("500.00")));

        assertThat(updated.getUsedCredit()).isEqualTo(Money.aed(BigDecimal.ZERO));
        assertThat(updated.getAvailableCredit()).isEqualTo(Money.aed(new BigDecimal("1000.00")));
    }

    @Test
    void updateCreditLimitShouldFailWhenBelowUsedCredit() {
        CreditProfile profile = CreditProfile.create(
            Money.aed(new BigDecimal("2000.00")),
            Money.aed(new BigDecimal("1500.00"))
        );

        assertThatThrownBy(() -> profile.updateCreditLimit(Money.aed(new BigDecimal("1000.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be less than used credit");
    }

    @Test
    void canBorrowShouldRejectNullZeroAndNegativeAmounts() {
        CreditProfile profile = CreditProfile.create(Money.aed(new BigDecimal("2000.00")));

        assertThat(profile.canBorrow(null)).isFalse();
        assertThat(profile.canBorrow(Money.aed(BigDecimal.ZERO))).isFalse();
        assertThat(profile.canBorrow(Money.aed(new BigDecimal("-1.00")))).isFalse();
        assertThat(profile.canBorrow(Money.aed(new BigDecimal("100.00")))).isTrue();
    }

    @Test
    void utilizationRatioShouldBeZeroWhenCreditLimitIsZero() {
        CreditProfile profile = CreditProfile.create(Money.aed(BigDecimal.ZERO));

        assertThat(profile.getCreditUtilizationRatio()).isEqualTo(BigDecimal.ZERO);
        assertThat(profile.isEmpty()).isTrue();
    }

    @Test
    void utilizationRatioShouldCalculateForNonZeroLimit() {
        CreditProfile profile = CreditProfile.create(
            Money.aed(new BigDecimal("1000.00")),
            Money.aed(new BigDecimal("333.33"))
        );

        assertThat(profile.getCreditUtilizationRatio()).isEqualByComparingTo("0.3333");
    }

    @Test
    void createShouldRejectNegativeCreditLimit() {
        assertThatThrownBy(() -> CreditProfile.create(Money.aed(new BigDecimal("-10.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Credit limit cannot be negative");
    }

    @Test
    void createShouldRejectNegativeUsedCreditAndUsedCreditAboveLimit() {
        assertThatThrownBy(() -> CreditProfile.create(
            Money.aed(new BigDecimal("100.00")),
            Money.aed(new BigDecimal("-1.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Used credit cannot be negative");

        assertThatThrownBy(() -> CreditProfile.create(
            Money.aed(new BigDecimal("100.00")),
            Money.aed(new BigDecimal("101.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Used credit cannot exceed credit limit");
    }

    @Test
    void equalsHashCodeAndToStringShouldBeValueBased() {
        CreditProfile left = CreditProfile.create(
            Money.aed(new BigDecimal("500.00")),
            Money.aed(new BigDecimal("125.00"))
        );
        CreditProfile right = CreditProfile.create(
            Money.aed(new BigDecimal("500.00")),
            Money.aed(new BigDecimal("125.00"))
        );
        CreditProfile different = CreditProfile.create(
            Money.aed(new BigDecimal("500.00")),
            Money.aed(new BigDecimal("100.00"))
        );

        assertThat(left).isEqualTo(left);
        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left).isNotEqualTo(different);
        assertThat(left).isNotEqualTo(null);
        assertThat(left).isNotEqualTo("other-type");
        assertThat(left.toString()).contains("limit=", "used=", "available=");
    }
}
