package com.masrufi.framework.domain.service;

import com.masrufi.framework.domain.model.*;
import com.masrufi.framework.domain.port.in.CreateMurabahaUseCase;
import com.masrufi.framework.domain.port.out.*;
import com.masrufi.framework.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Domain Service for Murabaha (Cost-Plus Financing) Operations
 * 
 * Murabaha is a sale-based Islamic financing structure where the financier
 * purchases goods and sells them to the customer at cost plus a disclosed profit margin.
 * 
 * Key Sharia Principles:
 * - Asset-based transaction (financier must own the asset before selling)
 * - Disclosed profit margin (no hidden costs)
 * - Real economic activity (genuine sale transaction)
 * - Fixed profit margin (cannot be changed after agreement)
 * - Clear ownership transfer
 * 
 * This service follows hexagonal architecture principles and implements
 * the CreateMurabahaUseCase interface.
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class MurabahaService implements CreateMurabahaUseCase {

    private final IslamicFinancingRepository islamicFinancingRepository;
    private final ShariaComplianceValidationPort shariaComplianceValidation;
    private final AssetValidationPort assetValidation;
    private final CustomerValidationPort customerValidation;
    
    public MurabahaService(
            IslamicFinancingRepository islamicFinancingRepository,
            ShariaComplianceValidationPort shariaComplianceValidation,
            AssetValidationPort assetValidation,
            CustomerValidationPort customerValidation) {
        this.islamicFinancingRepository = islamicFinancingRepository;
        this.shariaComplianceValidation = shariaComplianceValidation;
        this.assetValidation = assetValidation;
        this.customerValidation = customerValidation;
    }

    /**
     * Create a new Murabaha financing arrangement
     * 
     * @param command The Murabaha creation command
     * @return The created Islamic financing arrangement
     * @throws ShariaViolationException if the request violates Sharia principles
     * @throws AssetValidationException if the asset is not valid or permissible
     * @throws CustomerValidationException if the customer is not eligible
     */
    @Override
    public IslamicFinancing createMurabaha(CreateMurabahaCommand command) {
        log.info("🛒 Creating Murabaha financing for customer: {}", 
            command != null ? command.getCustomerProfile().getCustomerId() : "null");

        // Validate input
        validateCommand(command);

        // Validate customer eligibility
        validateCustomer(command.getCustomerProfile());

        // Validate asset
        validateAsset(command);

        // Validate Sharia compliance
        validateShariaCompliance(command);

        // Generate unique ID
        IslamicFinancingId financingId = IslamicFinancingId.generateWithTimestamp(
            IslamicFinancing.IslamicFinancingType.MURABAHA
        );

        // Calculate selling price (cost + profit)
        Money sellingPrice = command.getSellingPrice();

        // Create Islamic financing record
        IslamicFinancing murabaha = IslamicFinancing.builder()
            .financingId(financingId)
            .islamicFinancingType(IslamicFinancing.IslamicFinancingType.MURABAHA)
            .customerProfile(command.getCustomerProfile())
            .principalAmount(command.getAssetCost())
            .totalAmount(sellingPrice)
            .profitMargin(command.getProfitMargin())
            .assetDescription(command.getAssetDescription())
            .maturityDate(command.getMaturityDate())
            .createdDate(LocalDateTime.now())
            .jurisdiction(command.getJurisdiction())
            .shariaCompliance(IslamicFinancing.ShariaComplianceValidation.builder()
                .isCompliant(true)
                .validationDate(LocalDateTime.now().toString())
                .validatingAuthority("UAE_HIGHER_SHARIA_AUTHORITY")
                .complianceNotes("Murabaha financing - fully compliant")
                .ribaFree(true)
                .ghararFree(true)
                .assetBacked(true)
                .permissibleAsset(true)
                .build())
            .build();

        // Save to repository
        IslamicFinancing savedMurabaha = islamicFinancingRepository.save(murabaha);

        log.info("✅ Murabaha financing created successfully: {}", savedMurabaha.getFinancingId());
        return savedMurabaha;
    }

    private void validateCommand(CreateMurabahaCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("CreateMurabahaCommand cannot be null");
        }
        
        // Let the command validate itself
        command.validate();
    }

    private void validateCustomer(CustomerProfile customerProfile) {
        if (!customerValidation.isEligibleForIslamicFinancing(customerProfile)) {
            throw new CustomerValidationException(
                "Customer not eligible for Islamic financing: " + customerProfile.getCustomerId()
            );
        }
    }

    private void validateAsset(CreateMurabahaCommand command) {
        if (!assetValidation.isAssetPermissible(command.getAssetDescription())) {
            throw new AssetValidationException(
                "Asset not permissible under Sharia law: " + command.getAssetDescription()
            );
        }

        if (!assetValidation.validateAssetValue(command.getAssetCost())) {
            throw new AssetValidationException(
                "Asset value validation failed: " + command.getAssetCost()
            );
        }
    }

    private void validateShariaCompliance(CreateMurabahaCommand command) {
        ShariaComplianceResult result = shariaComplianceValidation.validateMurabahaCompliance(command);
        
        if (!result.isCompliant()) {
            throw new ShariaViolationException(
                result.getMessage(),
                "MURABAHA_COMPLIANCE",
                "GENERAL_SHARIA_PRINCIPLES"
            );
        }
    }
}