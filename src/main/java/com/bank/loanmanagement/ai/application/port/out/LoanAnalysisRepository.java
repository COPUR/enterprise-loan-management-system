package com.bank.loanmanagement.ai.application.port.out;

import com.bank.loanmanagement.ai.domain.model.LoanAnalysisRequest;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisRequestId;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisResult;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisResultId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for loan analysis persistence
 */
public interface LoanAnalysisRepository {

    /**
     * Save loan analysis request
     * 
     * @param request the request to save
     * @return the saved request
     */
    LoanAnalysisRequest save(LoanAnalysisRequest request);

    /**
     * Save loan analysis result
     * 
     * @param result the result to save
     * @return the saved result
     */
    LoanAnalysisResult save(LoanAnalysisResult result);

    /**
     * Find analysis request by ID
     * 
     * @param id the request ID
     * @return the request if found
     */
    Optional<LoanAnalysisRequest> findRequestById(LoanAnalysisRequestId id);

    /**
     * Find analysis result by ID
     * 
     * @param id the result ID
     * @return the result if found
     */
    Optional<LoanAnalysisResult> findResultById(LoanAnalysisResultId id);

    /**
     * Find analysis result by request ID
     * 
     * @param requestId the request ID
     * @return the result if found
     */
    Optional<LoanAnalysisResult> findResultByRequestId(String requestId);

    /**
     * Find all analysis requests for an applicant
     * 
     * @param applicantId the applicant ID
     * @return list of requests
     */
    List<LoanAnalysisRequest> findRequestsByApplicantId(String applicantId);

    /**
     * Find pending analysis requests
     * 
     * @return list of pending requests
     */
    List<LoanAnalysisRequest> findPendingRequests();
}