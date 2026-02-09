package com.enterprise.openfinance.uc03.infrastructure.audit;

import com.enterprise.openfinance.uc03.domain.model.AccountStatus;
import com.enterprise.openfinance.uc03.domain.model.ConfirmationAuditRecord;
import com.enterprise.openfinance.uc03.domain.model.NameMatchDecision;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPayeeAuditLogAdapterTest {

    @Test
    void shouldStoreAuditRecords() {
        InMemoryPayeeAuditLogAdapter adapter = new InMemoryPayeeAuditLogAdapter();

        adapter.log(new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "GB82WEST12345698765432",
                "Al Tareq Trading LLC",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                Instant.now()
        ));

        assertThat(adapter.size()).isEqualTo(1);
    }
}
