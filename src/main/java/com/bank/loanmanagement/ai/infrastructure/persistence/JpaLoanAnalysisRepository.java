package com.bank.loanmanagement.ai.infrastructure.persistence;

import com.bank.loanmanagement.ai.application.port.out.LoanAnalysisRepository;
import com.bank.loanmanagement.ai.domain.model.*;
import com.bank.loanmanagement.ai.infrastructure.persistence.entity.LoanAnalysisRequestEntity;
import com.bank.loanmanagement.ai.infrastructure.persistence.entity.LoanAnalysisResultEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of LoanAnalysisRepository port
 * Following Hexagonal Architecture - Infrastructure adapter
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaLoanAnalysisRepository implements LoanAnalysisRepository {

    private final SpringDataLoanAnalysisRequestRepository requestRepository;
    private final SpringDataLoanAnalysisResultRepository resultRepository;

    @Override
    public LoanAnalysisRequest save(LoanAnalysisRequest request) {
        log.debug("Saving loan analysis request: {}", request.getId());
        LoanAnalysisRequestEntity entity = LoanAnalysisRequestEntity.fromDomain(request);
        LoanAnalysisRequestEntity savedEntity = requestRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public LoanAnalysisResult save(LoanAnalysisResult result) {
        log.debug("Saving loan analysis result: {}", result.getId());
        LoanAnalysisResultEntity entity = LoanAnalysisResultEntity.fromDomain(result);
        LoanAnalysisResultEntity savedEntity = resultRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<LoanAnalysisRequest> findRequestById(LoanAnalysisRequestId id) {
        log.debug("Finding loan analysis request by ID: {}", id);
        return requestRepository.findById(id)
                .map(LoanAnalysisRequestEntity::toDomain);
    }

    @Override
    public Optional<LoanAnalysisResult> findResultById(LoanAnalysisResultId id) {
        log.debug("Finding loan analysis result by ID: {}", id);
        return resultRepository.findById(id)
                .map(LoanAnalysisResultEntity::toDomain);
    }

    @Override
    public Optional<LoanAnalysisResult> findResultByRequestId(String requestId) {
        log.debug("Finding loan analysis result by request ID: {}", requestId);
        return resultRepository.findByRequestId(requestId)
                .map(LoanAnalysisResultEntity::toDomain);
    }

    @Override
    public List<LoanAnalysisRequest> findRequestsByApplicantId(String applicantId) {
        log.debug("Finding loan analysis requests by applicant ID: {}", applicantId);
        return requestRepository.findByApplicantIdOrderByRequestedAtDesc(applicantId)
                .stream()
                .map(LoanAnalysisRequestEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanAnalysisRequest> findPendingRequests() {
        log.debug("Finding pending loan analysis requests");
        return requestRepository.findByAnalysisStatusOrderByRequestedAt(AnalysisStatus.PENDING)
                .stream()
                .map(LoanAnalysisRequestEntity::toDomain)
                .collect(Collectors.toList());
    }
}