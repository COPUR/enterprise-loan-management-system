package com.loanmanagement.security.zerotrust.domain.ports.inbound;

import com.loanmanagement.security.zerotrust.domain.model.*;
import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for Zero Trust Security operations
 * Defines the use cases for comprehensive zero trust security validation
 */
public interface ZeroTrustSecurityUseCase {
    
    /**
     * Perform comprehensive zero trust security validation
     * @param request The zero trust security request
     * @return CompletableFuture containing the security validation result
     */
    CompletableFuture<ZeroTrustSecurityResult> validateSecurity(ZeroTrustSecurityRequest request);
    
    /**
     * Perform continuous verification of security posture
     * @param request The continuous verification request
     * @return CompletableFuture containing the verification result
     */
    CompletableFuture<ContinuousVerificationResult> performContinuousVerification(
        ContinuousVerificationRequest request);
    
    /**
     * Assess threat landscape and security risks
     * @param request The threat assessment request
     * @return CompletableFuture containing the threat assessment result
     */
    CompletableFuture<ThreatAssessmentResult> assessThreat(ThreatAssessmentRequest request);
    
    /**
     * Enforce security policies dynamically
     * @param request The policy enforcement request
     * @return CompletableFuture containing the policy enforcement result
     */
    CompletableFuture<PolicyEnforcementResult> enforceSecurityPolicies(
        PolicyEnforcementRequest request);
    
    /**
     * Validate FAPI2 security compliance
     * @param request The FAPI2 security request
     * @return CompletableFuture containing the FAPI2 compliance result
     */
    CompletableFuture<FAPI2ComplianceResult> validateFAPI2Compliance(
        FAPI2SecurityRequest request);
}