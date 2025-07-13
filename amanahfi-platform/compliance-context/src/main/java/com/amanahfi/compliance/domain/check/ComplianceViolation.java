package com.amanahfi.compliance.domain.check;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compliance Violation Entity
 */
@Entity
@Table(name = "compliance_violations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComplianceViolation {

    @Id
    private String violationId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ViolationType violationType;

    @NotBlank
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SeverityLevel severityLevel;

    @NotNull
    private LocalDateTime detectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_check_id")
    private ComplianceCheck complianceCheck;

    public ComplianceViolation(ViolationType violationType, String description, SeverityLevel severityLevel) {
        this.violationId = generateViolationId();
        this.violationType = violationType;
        this.description = description;
        this.severityLevel = severityLevel;
        this.detectedAt = LocalDateTime.now();
    }

    void assignToCheck(ComplianceCheck check) {
        this.complianceCheck = check;
    }

    private String generateViolationId() {
        return "VIO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}