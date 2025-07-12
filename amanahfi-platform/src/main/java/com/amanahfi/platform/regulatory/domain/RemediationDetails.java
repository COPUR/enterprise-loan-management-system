package com.amanahfi.platform.regulatory.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Details of violation remediation
 */
@Value
@Builder
public class RemediationDetails {
    String remediationId;
    String remediatedBy;
    Instant remediatedAt;
    String remediationDescription;
    List<String> actionsTaken;
    List<String> preventiveMeasures;
    String evidenceReference;
    boolean regulatoryNotificationRequired;
    String regulatoryNotificationReference;
}