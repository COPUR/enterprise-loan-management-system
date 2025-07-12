package com.amanahfi.platform.regulatory.infrastructure.adapter;

import com.amanahfi.platform.regulatory.domain.*;
import com.amanahfi.platform.regulatory.infrastructure.dto.cbuae.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter for CBUAE (Central Bank of UAE) Open Finance API integration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CbuaeApiAdapter {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${amanahfi.platform.regulatory.cbuae.api-base-url}")
    private String apiBaseUrl;
    
    @Value("${amanahfi.platform.regulatory.cbuae.api-key}")
    private String apiKey;
    
    @Value("${amanahfi.platform.regulatory.cbuae.timeout-seconds:30}")
    private int timeoutSeconds;
    
    private WebClient webClient;
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("X-Client-Id", "amanahfi-platform")
                .build();
        }
        return webClient;
    }
    
    public void submitOpenFinanceCompliance(
            ComplianceSummary compliance,
            ComplianceAssessment assessment) {
        
        log.info("Submitting Open Finance compliance assessment to CBUAE");
        
        CbuaeComplianceSubmission submission = CbuaeComplianceSubmission.builder()
            .submissionId(UUID.randomUUID().toString())
            .entityId(compliance.getEntityId())
            .complianceType("OPEN_FINANCE_API")
            .assessmentScore(assessment.getScore())
            .assessmentResult(mapAssessmentResult(assessment.getResult()))
            .findings(assessment.getFindings())
            .recommendations(assessment.getRecommendations())
            .assessorId(assessment.getAssessorId())
            .build();
        
        getWebClient()
            .post()
            .uri("/api/v1/compliance/assessments")
            .bodyValue(submission)
            .retrieve()
            .bodyToMono(CbuaeApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(response -> log.info("CBUAE submission successful: {}", 
                response.getReferenceNumber()))
            .doOnError(error -> log.error("CBUAE submission failed", error))
            .subscribe();
    }
    
    public String submitAmlReport(
            ComplianceReport report,
            Map<String, Object> reportData) {
        
        log.info("Submitting AML report to CBUAE");
        
        CbuaeAmlReport amlReport = CbuaeAmlReport.builder()
            .reportId(report.getReportId())
            .reportingPeriodStart(report.getReportingPeriodStart())
            .reportingPeriodEnd(report.getReportingPeriodEnd())
            .reportData(reportData)
            .submittedBy(report.getSubmittedBy())
            .build();
        
        CbuaeApiResponse response = getWebClient()
            .post()
            .uri("/api/v1/aml/reports")
            .bodyValue(amlReport)
            .retrieve()
            .bodyToMono(CbuaeApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getReferenceNumber() : "CBUAE-" + UUID.randomUUID();
    }
    
    public String submitSuspiciousActivityReport(
            ComplianceViolation violation,
            Map<String, Object> transactionDetails) {
        
        log.info("Submitting SAR to CBUAE for violation: {}", violation.getViolationId());
        
        CbuaeSarSubmission sar = CbuaeSarSubmission.builder()
            .sarId(UUID.randomUUID().toString())
            .violationCode(violation.getViolationCode())
            .description(violation.getDescription())
            .severity(violation.getSeverity().name())
            .transactionDetails(transactionDetails)
            .detectedBy(violation.getDetectedBy())
            .detectedAt(violation.getDetectedAt())
            .build();
        
        CbuaeApiResponse response = getWebClient()
            .post()
            .uri("/api/v1/sar/submit")
            .bodyValue(sar)
            .retrieve()
            .bodyToMono(CbuaeApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getReferenceNumber() : "SAR-" + UUID.randomUUID();
    }
    
    public boolean checkApiHealth() {
        try {
            return Boolean.TRUE.equals(getWebClient()
                .get()
                .uri("/api/v1/health")
                .retrieve()
                .bodyToMono(CbuaeHealthResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(CbuaeHealthResponse::isHealthy)
                .block());
        } catch (Exception e) {
            log.error("CBUAE API health check failed", e);
            return false;
        }
    }
    
    private String mapAssessmentResult(AssessmentResult result) {
        return switch (result) {
            case PASS -> "COMPLIANT";
            case PASS_WITH_CONDITIONS -> "CONDITIONALLY_COMPLIANT";
            case FAIL -> "NON_COMPLIANT";
            case REVIEW_REQUIRED -> "UNDER_REVIEW";
        };
    }
}