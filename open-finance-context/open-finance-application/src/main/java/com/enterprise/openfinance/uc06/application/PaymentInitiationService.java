package com.enterprise.openfinance.uc06.application;

import com.enterprise.openfinance.uc06.domain.command.SubmitPaymentCommand;
import com.enterprise.openfinance.uc06.domain.model.IdempotencyRecord;
import com.enterprise.openfinance.uc06.domain.model.PaymentResult;
import com.enterprise.openfinance.uc06.domain.model.PaymentSettings;
import com.enterprise.openfinance.uc06.domain.model.PaymentStatus;
import com.enterprise.openfinance.uc06.domain.model.PaymentTransaction;
import com.enterprise.openfinance.uc06.domain.port.in.PaymentInitiationUseCase;
import com.enterprise.openfinance.uc06.domain.port.out.FundsReservationPort;
import com.enterprise.openfinance.uc06.domain.port.out.JwsSignatureValidationPort;
import com.enterprise.openfinance.uc06.domain.port.out.PaymentConsentPort;
import com.enterprise.openfinance.uc06.domain.port.out.PaymentEventPort;
import com.enterprise.openfinance.uc06.domain.port.out.PaymentIdempotencyPort;
import com.enterprise.openfinance.uc06.domain.port.out.PaymentTransactionPort;
import com.enterprise.openfinance.uc06.domain.port.out.PayloadHashPort;
import com.enterprise.openfinance.uc06.domain.port.out.RiskAssessmentPort;
import com.enterprise.openfinance.uc06.domain.service.PaymentStatusPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class PaymentInitiationService implements PaymentInitiationUseCase {

    private final PaymentConsentPort paymentConsentPort;
    private final PaymentIdempotencyPort paymentIdempotencyPort;
    private final PaymentTransactionPort paymentTransactionPort;
    private final FundsReservationPort fundsReservationPort;
    private final RiskAssessmentPort riskAssessmentPort;
    private final JwsSignatureValidationPort jwsSignatureValidationPort;
    private final PayloadHashPort payloadHashPort;
    private final PaymentEventPort paymentEventPort;
    private final PaymentStatusPolicy paymentStatusPolicy;
    private final PaymentSettings paymentSettings;
    private final Clock clock;

    public PaymentInitiationService(
            PaymentConsentPort paymentConsentPort,
            PaymentIdempotencyPort paymentIdempotencyPort,
            PaymentTransactionPort paymentTransactionPort,
            FundsReservationPort fundsReservationPort,
            RiskAssessmentPort riskAssessmentPort,
            JwsSignatureValidationPort jwsSignatureValidationPort,
            PayloadHashPort payloadHashPort,
            PaymentEventPort paymentEventPort,
            PaymentStatusPolicy paymentStatusPolicy,
            PaymentSettings paymentSettings,
            Clock clock
    ) {
        this.paymentConsentPort = paymentConsentPort;
        this.paymentIdempotencyPort = paymentIdempotencyPort;
        this.paymentTransactionPort = paymentTransactionPort;
        this.fundsReservationPort = fundsReservationPort;
        this.riskAssessmentPort = riskAssessmentPort;
        this.jwsSignatureValidationPort = jwsSignatureValidationPort;
        this.payloadHashPort = payloadHashPort;
        this.paymentEventPort = paymentEventPort;
        this.paymentStatusPolicy = paymentStatusPolicy;
        this.paymentSettings = paymentSettings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentResult submitPayment(SubmitPaymentCommand command) {
        if (!jwsSignatureValidationPort.isValid(command.jwsSignature(), command.rawPayload())) {
            throw new IllegalArgumentException("Signature Invalid");
        }

        Instant now = Instant.now(clock);
        String requestHash = payloadHashPort.hash(command.rawPayload());

        Optional<IdempotencyRecord> existingRecord = paymentIdempotencyPort.find(command.idempotencyKey(), command.tppId(), now);
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.orElseThrow();
            if (!record.requestHash().equals(requestHash)) {
                throw new IllegalStateException("Idempotency conflict");
            }
            return record.result().asReplay();
        }

        var consent = paymentConsentPort.findById(command.consentId())
                .orElseThrow(() -> new IllegalArgumentException("Consent not found"));
        if (!consent.canInitiate(command.initiation(), now)) {
            throw new IllegalStateException("Consent binding validation failed");
        }

        LocalDate processingDate = LocalDate.now(clock);
        var riskDecision = riskAssessmentPort.assess(command.initiation(), command.tppId());
        PaymentStatus status = paymentStatusPolicy.decide(processingDate, command.initiation().requestedExecutionDate(), riskDecision);

        if (status == PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS) {
            boolean reserved = fundsReservationPort.reserve(
                    command.initiation().debtorAccountId(),
                    command.initiation().instructedAmount(),
                    command.initiation().currency(),
                    command.idempotencyKey()
            );
            if (!reserved) {
                throw new IllegalStateException("Insufficient funds");
            }
        }

        PaymentTransaction transaction = PaymentTransaction.create(
                command.consentId(),
                command.tppId(),
                command.idempotencyKey(),
                status,
                command.initiation(),
                now
        );
        PaymentTransaction saved = paymentTransactionPort.save(transaction);
        paymentEventPort.publishSubmitted(saved);

        PaymentResult result = PaymentResult.from(saved, command.interactionId());
        paymentIdempotencyPort.save(new IdempotencyRecord(
                command.idempotencyKey(),
                command.tppId(),
                requestHash,
                result,
                now,
                now.plus(paymentSettings.idempotencyTtl())
        ));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> getPayment(String paymentId) {
        return paymentTransactionPort.findByPaymentId(paymentId);
    }
}
