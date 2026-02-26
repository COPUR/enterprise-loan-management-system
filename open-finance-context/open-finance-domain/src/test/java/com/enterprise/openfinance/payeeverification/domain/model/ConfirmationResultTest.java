package com.enterprise.openfinance.payeeverification.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmationResultTest {

    @Test
    void shouldKeepMatchedNameForCloseMatchOnly() {
        ConfirmationResult closeMatch = new ConfirmationResult(
                AccountStatus.ACTIVE,
                NameMatchDecision.CLOSE_MATCH,
                "Al Tareq Trading LLC",
                90,
                true
        );
        ConfirmationResult match = new ConfirmationResult(
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                "Should be ignored",
                100,
                false
        );

        assertThat(closeMatch.matchedName()).isEqualTo("Al Tareq Trading LLC");
        assertThat(match.matchedName()).isNull();
    }

    @Test
    void shouldRejectInvalidResultState() {
        assertThatThrownBy(() -> new ConfirmationResult(null, NameMatchDecision.MATCH, null, 100, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accountStatus is required");

        assertThatThrownBy(() -> new ConfirmationResult(AccountStatus.ACTIVE, null, null, 100, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nameMatched is required");

        assertThatThrownBy(() -> new ConfirmationResult(AccountStatus.ACTIVE, NameMatchDecision.MATCH, null, -1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("matchScore must be between 0 and 100");

        assertThatThrownBy(() -> new ConfirmationResult(AccountStatus.ACTIVE, NameMatchDecision.MATCH, null, 101, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("matchScore must be between 0 and 100");
    }
}
