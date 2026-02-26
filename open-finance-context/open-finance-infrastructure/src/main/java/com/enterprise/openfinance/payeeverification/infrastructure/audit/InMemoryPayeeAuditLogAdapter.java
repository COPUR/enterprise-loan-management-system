package com.enterprise.openfinance.payeeverification.infrastructure.audit;

import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationAuditRecord;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeAuditLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryPayeeAuditLogAdapter implements PayeeAuditLogPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryPayeeAuditLogAdapter.class);
    private final List<ConfirmationAuditRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void log(ConfirmationAuditRecord record) {
        records.add(record);
        LOGGER.info(
                "cop_audit tppId={} interactionId={} scheme={} decision={} score={} cache={}",
                record.tppId(),
                record.interactionId(),
                record.schemeName(),
                record.nameMatched(),
                record.matchScore(),
                record.fromCache()
        );
    }

    public int size() {
        return records.size();
    }
}
