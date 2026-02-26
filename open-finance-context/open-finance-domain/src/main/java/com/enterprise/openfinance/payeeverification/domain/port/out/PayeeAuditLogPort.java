package com.enterprise.openfinance.payeeverification.domain.port.out;

import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationAuditRecord;

public interface PayeeAuditLogPort {

    void log(ConfirmationAuditRecord record);
}
