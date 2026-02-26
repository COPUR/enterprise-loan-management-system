package com.enterprise.openfinance.payeeverification.application;

import com.enterprise.openfinance.payeeverification.domain.model.AccountStatus;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationAuditRecord;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationRequest;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationResult;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationSettings;
import com.enterprise.openfinance.payeeverification.domain.model.DirectoryEntry;
import com.enterprise.openfinance.payeeverification.domain.model.NameMatchDecision;
import com.enterprise.openfinance.payeeverification.domain.port.in.ConfirmationOfPayeeUseCase;
import com.enterprise.openfinance.payeeverification.domain.port.out.NameSimilarityPort;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeAuditLogPort;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeDirectoryCachePort;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeDirectoryPort;
import com.enterprise.openfinance.payeeverification.domain.service.ConfirmationDecisionPolicy;
import com.enterprise.openfinance.payeeverification.domain.service.IbanValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class ConfirmationOfPayeeService implements ConfirmationOfPayeeUseCase {

    private final PayeeDirectoryPort payeeDirectoryPort;
    private final PayeeDirectoryCachePort payeeDirectoryCachePort;
    private final PayeeAuditLogPort payeeAuditLogPort;
    private final NameSimilarityPort nameSimilarityPort;
    private final ConfirmationDecisionPolicy confirmationDecisionPolicy;
    private final ConfirmationSettings confirmationSettings;
    private final Clock clock;

    public ConfirmationOfPayeeService(
            PayeeDirectoryPort payeeDirectoryPort,
            PayeeDirectoryCachePort payeeDirectoryCachePort,
            PayeeAuditLogPort payeeAuditLogPort,
            NameSimilarityPort nameSimilarityPort,
            ConfirmationDecisionPolicy confirmationDecisionPolicy,
            ConfirmationSettings confirmationSettings,
            Clock clock
    ) {
        this.payeeDirectoryPort = payeeDirectoryPort;
        this.payeeDirectoryCachePort = payeeDirectoryCachePort;
        this.payeeAuditLogPort = payeeAuditLogPort;
        this.nameSimilarityPort = nameSimilarityPort;
        this.confirmationDecisionPolicy = confirmationDecisionPolicy;
        this.confirmationSettings = confirmationSettings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ConfirmationResult confirm(ConfirmationRequest request) {
        if ("IBAN".equals(request.schemeName()) && !IbanValidator.isValid(request.identification())) {
            throw new IllegalArgumentException("invalid IBAN");
        }

        Instant now = Instant.now(clock);
        String cacheKey = cacheKey(request.schemeName(), request.identification());

        Optional<DirectoryEntry> cachedEntry = payeeDirectoryCachePort.get(cacheKey, now);
        boolean fromCache = cachedEntry.isPresent();
        Optional<DirectoryEntry> directoryEntry = cachedEntry;
        if (directoryEntry.isEmpty()) {
            directoryEntry = payeeDirectoryPort.findBySchemeAndIdentification(request.schemeName(), request.identification());
            directoryEntry.ifPresent(entry -> payeeDirectoryCachePort.put(cacheKey, entry, now.plus(confirmationSettings.cacheTtl())));
        }

        ConfirmationResult result = buildResult(request, directoryEntry, fromCache);
        payeeAuditLogPort.log(new ConfirmationAuditRecord(
                request.tppId(),
                request.interactionId(),
                request.schemeName(),
                request.identification(),
                request.name(),
                result.accountStatus(),
                result.nameMatched(),
                result.matchScore(),
                result.fromCache(),
                now
        ));
        return result;
    }

    private ConfirmationResult buildResult(ConfirmationRequest request, Optional<DirectoryEntry> directoryEntry, boolean fromCache) {
        if (directoryEntry.isEmpty()) {
            return new ConfirmationResult(AccountStatus.UNKNOWN, NameMatchDecision.UNABLE_TO_CHECK, null, 0, fromCache);
        }

        DirectoryEntry entry = directoryEntry.orElseThrow();
        if (!entry.accountStatus().canReceivePayments()) {
            return new ConfirmationResult(entry.accountStatus(), NameMatchDecision.UNABLE_TO_CHECK, null, 0, fromCache);
        }

        int score = nameSimilarityPort.similarityScore(request.name(), entry.legalName());
        NameMatchDecision decision = confirmationDecisionPolicy.decide(score);
        String matchedName = decision == NameMatchDecision.CLOSE_MATCH ? entry.legalName() : null;
        return new ConfirmationResult(entry.accountStatus(), decision, matchedName, score, fromCache);
    }

    private static String cacheKey(String schemeName, String identification) {
        return schemeName.toUpperCase(Locale.ROOT) + ":" + identification.toUpperCase(Locale.ROOT);
    }
}
