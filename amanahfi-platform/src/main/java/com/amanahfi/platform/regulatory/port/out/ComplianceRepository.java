package com.amanahfi.platform.regulatory.port.out;

import com.amanahfi.platform.regulatory.domain.ComplianceId;
import com.amanahfi.platform.regulatory.domain.ComplianceType;
import com.amanahfi.platform.regulatory.domain.RegulatoryCompliance;

import java.util.List;
import java.util.Optional;

/**
 * Output port for regulatory compliance persistence
 */
public interface ComplianceRepository {
    
    void save(RegulatoryCompliance compliance);
    
    Optional<RegulatoryCompliance> findById(ComplianceId complianceId);
    
    List<RegulatoryCompliance> findByEntityId(String entityId);
    
    boolean existsByEntityAndType(String entityId, ComplianceType complianceType);
    
    List<RegulatoryCompliance> findByJurisdiction(String jurisdiction);
    
    List<RegulatoryCompliance> findWithActiveViolations();
    
    List<RegulatoryCompliance> findRequiringAssessment();
}