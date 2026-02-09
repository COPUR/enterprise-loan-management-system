package com.enterprise.openfinance.uc03.domain.port.out;

import com.enterprise.openfinance.uc03.domain.model.ConfirmationAuditRecord;

public interface PayeeAuditLogPort {

    void log(ConfirmationAuditRecord record);
}
