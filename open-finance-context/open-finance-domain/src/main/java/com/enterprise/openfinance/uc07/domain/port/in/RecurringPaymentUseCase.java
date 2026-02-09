package com.enterprise.openfinance.uc07.domain.port.in;

import com.enterprise.openfinance.uc07.domain.command.CreateVrpConsentCommand;
import com.enterprise.openfinance.uc07.domain.command.RevokeVrpConsentCommand;
import com.enterprise.openfinance.uc07.domain.command.SubmitVrpPaymentCommand;
import com.enterprise.openfinance.uc07.domain.model.VrpCollectionResult;
import com.enterprise.openfinance.uc07.domain.model.VrpConsent;
import com.enterprise.openfinance.uc07.domain.model.VrpPayment;
import com.enterprise.openfinance.uc07.domain.query.GetVrpConsentQuery;
import com.enterprise.openfinance.uc07.domain.query.GetVrpPaymentQuery;

import java.util.Optional;

public interface RecurringPaymentUseCase {

    VrpConsent createConsent(CreateVrpConsentCommand command);

    Optional<VrpConsent> getConsent(GetVrpConsentQuery query);

    void revokeConsent(RevokeVrpConsentCommand command);

    VrpCollectionResult submitCollection(SubmitVrpPaymentCommand command);

    Optional<VrpPayment> getPayment(GetVrpPaymentQuery query);
}
