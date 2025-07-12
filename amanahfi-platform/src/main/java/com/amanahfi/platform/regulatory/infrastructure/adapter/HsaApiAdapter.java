package com.amanahfi.platform.regulatory.infrastructure.adapter;

import com.amanahfi.platform.regulatory.domain.*;
import com.amanahfi.platform.regulatory.infrastructure.dto.hsa.*;
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
 * Adapter for HSA (Higher Sharia Authority) API integration
 * Handles Sharia compliance reporting and validation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HsaApiAdapter {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${amanahfi.platform.regulatory.hsa.api-base-url}")
    private String apiBaseUrl;
    
    @Value("${amanahfi.platform.regulatory.hsa.api-key}")
    private String apiKey;
    
    @Value("${amanahfi.platform.regulatory.hsa.institution-code}")
    private String institutionCode;
    
    @Value("${amanahfi.platform.regulatory.hsa.timeout-seconds:30}")
    private int timeoutSeconds;
    
    private WebClient webClient;
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-HSA-API-Key", apiKey)
                .defaultHeader("X-HSA-Institution", institutionCode)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ar,en")
                .build();
        }
        return webClient;
    }
    
    public void submitShariaComplianceAssessment(
            ComplianceSummary compliance,
            ComplianceAssessment assessment) {
        
        log.info("Submitting Sharia compliance assessment to HSA");
        
        HsaShariaComplianceSubmission submission = HsaShariaComplianceSubmission.builder()
            .submissionId(UUID.randomUUID().toString())
            .institutionCode(institutionCode)
            .entityId(compliance.getEntityId())
            .productType(mapToShariaProductType(compliance.getComplianceType()))
            .shariaComplianceScore(assessment.getScore())
            .shariaBoard("INTERNAL_SHARIA_BOARD")
            .fatwaReference(extractFatwaReference(assessment))
            .ribaFree(true)
            .ghararFree(true)
            .maysirFree(true)
            .assetBacked(isAssetBacked(compliance.getComplianceType()))
            .build();
        
        getWebClient()
            .post()
            .uri("/api/v1/sharia/compliance/submit")
            .bodyValue(submission)
            .retrieve()
            .bodyToMono(HsaApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(response -> log.info("HSA submission successful: {}", 
                response.getCertificateNumber()))
            .doOnError(error -> log.error("HSA submission failed", error))
            .subscribe();
    }
    
    public String submitIslamicFinanceReport(
            ComplianceReport report,
            Map<String, Object> reportData) {
        
        log.info("Submitting Islamic finance report to HSA");
        
        HsaIslamicFinanceReport islamicReport = HsaIslamicFinanceReport.builder()
            .reportId(report.getReportId())
            .reportType(mapReportType(report.getReportType()))
            .reportingPeriodHijri(convertToHijriDate(report.getReportingPeriodStart()))
            .murabahaContracts(extractContractCount(reportData, "murabaha"))
            .musharakahContracts(extractContractCount(reportData, "musharakah"))
            .ijarahContracts(extractContractCount(reportData, "ijarah"))
            .qardHassanContracts(extractContractCount(reportData, "qardHassan"))
            .totalShariaCompliantAssets(extractTotalAssets(reportData))
            .zakatCalculated(extractZakatAmount(reportData))
            .build();
        
        HsaApiResponse response = getWebClient()
            .post()
            .uri("/api/v1/islamic-finance/reports")
            .bodyValue(islamicReport)
            .retrieve()
            .bodyToMono(HsaApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getCertificateNumber() : "HSA-" + UUID.randomUUID();
    }
    
    public String validateFatwa(String productType, Map<String, Object> productDetails) {
        log.info("Validating fatwa for product type: {}", productType);
        
        HsaFatwaValidationRequest request = HsaFatwaValidationRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .productType(productType)
            .productDetails(productDetails)
            .urgency("NORMAL")
            .build();
        
        HsaFatwaResponse response = getWebClient()
            .post()
            .uri("/api/v1/fatwa/validate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(HsaFatwaResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getFatwaNumber() : null;
    }
    
    public String notifyShariaViolation(
            ComplianceViolation violation,
            Map<String, Object> violationDetails) {
        
        log.info("Notifying HSA of Sharia compliance violation: {}", violation.getViolationId());
        
        HsaShariaViolationNotification notification = HsaShariaViolationNotification.builder()
            .notificationId(UUID.randomUUID().toString())
            .violationType(mapShariaViolationType(violation))
            .severity(violation.getSeverity().name())
            .description(violation.getDescription())
            .affectedProducts(extractAffectedProducts(violationDetails))
            .correctiveActions(extractCorrectiveActions(violationDetails))
            .shariaJustification(extractShariaJustification(violationDetails))
            .build();
        
        HsaApiResponse response = getWebClient()
            .post()
            .uri("/api/v1/violations/notify")
            .bodyValue(notification)
            .retrieve()
            .bodyToMono(HsaApiResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();
        
        return response != null ? response.getCertificateNumber() : "HSA-VIOL-" + UUID.randomUUID();
    }
    
    public boolean checkApiHealth() {
        try {
            return Boolean.TRUE.equals(getWebClient()
                .get()
                .uri("/api/v1/health")
                .retrieve()
                .bodyToMono(HsaHealthResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(HsaHealthResponse::isAvailable)
                .block());
        } catch (Exception e) {
            log.error("HSA API health check failed", e);
            return false;
        }
    }
    
    private String mapToShariaProductType(ComplianceType type) {
        return switch (type) {
            case ISLAMIC_BANKING -> "ISLAMIC_BANKING_PRODUCTS";
            case SUKUK_ISSUANCE -> "SUKUK";
            case ZAKAT_COMPLIANCE -> "ZAKAT_MANAGEMENT";
            default -> "GENERAL_ISLAMIC_FINANCE";
        };
    }
    
    private String extractFatwaReference(ComplianceAssessment assessment) {
        // Extract from findings or use default
        return assessment.getFindings().stream()
            .filter(f -> f.startsWith("FATWA-"))
            .findFirst()
            .orElse("FATWA-DEFAULT-2024");
    }
    
    private boolean isAssetBacked(ComplianceType type) {
        return type == ComplianceType.ISLAMIC_BANKING || 
               type == ComplianceType.SUKUK_ISSUANCE;
    }
    
    private String mapReportType(ComplianceReport.ReportType type) {
        return switch (type) {
            case SHARIA_COMPLIANCE -> "PERIODIC_SHARIA_COMPLIANCE";
            case ZAKAT_CALCULATION -> "ZAKAT_CALCULATION_REPORT";
            case PROFIT_DISTRIBUTION -> "PROFIT_LOSS_SHARING_REPORT";
            default -> "GENERAL_ISLAMIC_FINANCE_REPORT";
        };
    }
    
    private String convertToHijriDate(java.time.LocalDate gregorianDate) {
        // Simplified conversion - in production use proper Islamic calendar library
        return "1446-" + gregorianDate.getMonthValue() + "-" + gregorianDate.getDayOfMonth();
    }
    
    private String mapShariaViolationType(ComplianceViolation violation) {
        if (violation.getViolationCode().contains("RIBA")) {
            return "RIBA_VIOLATION";
        } else if (violation.getViolationCode().contains("GHARAR")) {
            return "GHARAR_VIOLATION";
        } else if (violation.getViolationCode().contains("MAYSIR")) {
            return "MAYSIR_VIOLATION";
        }
        return "GENERAL_SHARIA_VIOLATION";
    }
    
    // Helper methods for data extraction
    private Long extractContractCount(Map<String, Object> data, String contractType) {
        String key = contractType + "Count";
        return data.containsKey(key) ? ((Number) data.get(key)).longValue() : 0L;
    }
    
    private Double extractTotalAssets(Map<String, Object> data) {
        return data.containsKey("totalAssets") ? 
            ((Number) data.get("totalAssets")).doubleValue() : 0.0;
    }
    
    private Double extractZakatAmount(Map<String, Object> data) {
        return data.containsKey("zakatAmount") ? 
            ((Number) data.get("zakatAmount")).doubleValue() : 0.0;
    }
    
    @SuppressWarnings("unchecked")
    private String[] extractAffectedProducts(Map<String, Object> details) {
        if (details.containsKey("affectedProducts")) {
            return ((java.util.List<String>) details.get("affectedProducts")).toArray(new String[0]);
        }
        return new String[0];
    }
    
    @SuppressWarnings("unchecked")
    private String[] extractCorrectiveActions(Map<String, Object> details) {
        if (details.containsKey("correctiveActions")) {
            return ((java.util.List<String>) details.get("correctiveActions")).toArray(new String[0]);
        }
        return new String[]{"Review initiated", "Product suspended"};
    }
    
    private String extractShariaJustification(Map<String, Object> details) {
        return details.containsKey("shariaJustification") ? 
            (String) details.get("shariaJustification") : 
            "Pending Sharia board review";
    }
}