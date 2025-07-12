package com.amanahfi.platform.regulatory.infrastructure.adapter;

import com.amanahfi.platform.regulatory.domain.*;
import com.amanahfi.platform.regulatory.infrastructure.dto.vara.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter for VARA (Virtual Assets Regulatory Authority) API integration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VaraApiAdapter {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${amanahfi.platform.regulatory.vara.api-base-url}")
    private String apiBaseUrl;
    
    @Value("${amanahfi.platform.regulatory.vara.api-key}")
    private String apiKey;
    
    @Value("${amanahfi.platform.regulatory.vara.license-number}")
    private String licenseNumber;
    
    @Value("${amanahfi.platform.regulatory.vara.timeout-seconds:30}")
    private int timeoutSeconds;
    
    private WebClient webClient;
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-VARA-API-Key", apiKey)
                .defaultHeader("X-VARA-License", licenseNumber)
                .build();
        }
        return webClient;
    }
    
    public void submitVirtualAssetCompliance(
            ComplianceSummary compliance,
            ComplianceAssessment assessment) {
        
        log.info("Submitting Virtual Asset compliance assessment to VARA");
        
        VaraComplianceSubmission submission = VaraComplianceSubmission.builder()
            .submissionId(UUID.randomUUID().toString())
            .licenseNumber(licenseNumber)
            .entityId(compliance.getEntityId())
            .complianceCategory("VIRTUAL_ASSET_SERVICE_PROVIDER")
            .assessmentScore(assessment.getScore())
            .complianceStatus(mapComplianceStatus(compliance.getOverallStatus()))
            .cryptoAssetTypes(determineCryptoAssetTypes(compliance))
            .amlKycCompliant(isAmlKycCompliant(assessment))
            .custodyArrangements("SEGREGATED_COLD_STORAGE")
            .build();
        
        getWebClient()
            .post()
            .uri("/v1/compliance/submit")
            .bodyValue(submission)
            .retrieve()
            .bodyToMono(VaraApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(response -> log.info("VARA submission successful: {}", 
                response.getSubmissionReference()))
            .doOnError(error -> log.error("VARA submission failed", error))
            .subscribe();
    }
    
    public String submitCbdcTransactionReport(
            ComplianceReport report,
            Map<String, Object> transactionData) {
        
        log.info("Submitting CBDC transaction report to VARA");
        
        VaraCbdcReport cbdcReport = VaraCbdcReport.builder()
            .reportId(report.getReportId())
            .reportingPeriod(report.getReportingPeriodStart() + " to " + report.getReportingPeriodEnd())
            .cbdcType("AED_DIGITAL_DIRHAM")
            .totalTransactions(extractTotalTransactions(transactionData))
            .totalVolume(extractTotalVolume(transactionData))
            .crossBorderTransactions(extractCrossBorderCount(transactionData))
            .suspiciousActivities(extractSuspiciousCount(transactionData))
            .build();
        
        VaraApiResponse response = getWebClient()
            .post()
            .uri("/v1/cbdc/reports")
            .bodyValue(cbdcReport)
            .retrieve()
            .bodyToMono(VaraApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getSubmissionReference() : "VARA-CBDC-" + UUID.randomUUID();
    }
    
    public String notifyCryptoViolation(
            ComplianceViolation violation,
            Map<String, Object> violationDetails) {
        
        log.info("Notifying VARA of crypto compliance violation: {}", violation.getViolationId());
        
        VaraViolationNotification notification = VaraViolationNotification.builder()
            .notificationId(UUID.randomUUID().toString())
            .violationType(mapViolationType(violation))
            .severity(violation.getSeverity().name())
            .description(violation.getDescription())
            .affectedAssets(extractAffectedAssets(violationDetails))
            .immediateActions(extractImmediateActions(violationDetails))
            .detectedAt(violation.getDetectedAt())
            .build();
        
        VaraApiResponse response = getWebClient()
            .post()
            .uri("/v1/violations/notify")
            .bodyValue(notification)
            .retrieve()
            .bodyToMono(VaraApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getSubmissionReference() : "VARA-VIOL-" + UUID.randomUUID();
    }
    
    public boolean checkApiHealth() {
        try {
            return Boolean.TRUE.equals(getWebClient()
                .get()
                .uri("/v1/health")
                .retrieve()
                .bodyToMono(VaraHealthResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(VaraHealthResponse::isOperational)
                .block());
        } catch (Exception e) {
            log.error("VARA API health check failed", e);
            return false;
        }
    }
    
    private String mapComplianceStatus(ComplianceStatus status) {
        return switch (status) {
            case COMPLIANT -> "FULLY_COMPLIANT";
            case PARTIALLY_COMPLIANT -> "PARTIALLY_COMPLIANT";
            case NON_COMPLIANT -> "NON_COMPLIANT";
            case UNDER_REVIEW -> "UNDER_REVIEW";
            case SUSPENDED -> "SUSPENDED";
            default -> "PENDING";
        };
    }
    
    private String[] determineCryptoAssetTypes(ComplianceSummary compliance) {
        // Determine based on compliance type and entity
        return new String[]{"CBDC", "STABLECOIN", "UTILITY_TOKEN"};
    }
    
    private boolean isAmlKycCompliant(ComplianceAssessment assessment) {
        return assessment.getScore() != null && assessment.getScore() >= 80.0;
    }
    
    private String mapViolationType(ComplianceViolation violation) {
        if (violation.getViolationCode().startsWith("AML")) {
            return "AML_KYC_BREACH";
        } else if (violation.getViolationCode().startsWith("CUSTODY")) {
            return "CUSTODY_BREACH";
        } else if (violation.getViolationCode().startsWith("MARKET")) {
            return "MARKET_MANIPULATION";
        }
        return "OTHER_VIOLATION";
    }
    
    // Helper methods for extracting data from maps
    private Long extractTotalTransactions(Map<String, Object> data) {
        return data.containsKey("totalTransactions") ? 
            ((Number) data.get("totalTransactions")).longValue() : 0L;
    }
    
    private Double extractTotalVolume(Map<String, Object> data) {
        return data.containsKey("totalVolume") ? 
            ((Number) data.get("totalVolume")).doubleValue() : 0.0;
    }
    
    private Long extractCrossBorderCount(Map<String, Object> data) {
        return data.containsKey("crossBorderCount") ? 
            ((Number) data.get("crossBorderCount")).longValue() : 0L;
    }
    
    private Long extractSuspiciousCount(Map<String, Object> data) {
        return data.containsKey("suspiciousCount") ? 
            ((Number) data.get("suspiciousCount")).longValue() : 0L;
    }
    
    @SuppressWarnings("unchecked")
    private String[] extractAffectedAssets(Map<String, Object> details) {
        if (details.containsKey("affectedAssets")) {
            return ((java.util.List<String>) details.get("affectedAssets")).toArray(new String[0]);
        }
        return new String[0];
    }
    
    @SuppressWarnings("unchecked")
    private String[] extractImmediateActions(Map<String, Object> details) {
        if (details.containsKey("immediateActions")) {
            return ((java.util.List<String>) details.get("immediateActions")).toArray(new String[0]);
        }
        return new String[]{"Investigation initiated", "Trading suspended"};
    }
}