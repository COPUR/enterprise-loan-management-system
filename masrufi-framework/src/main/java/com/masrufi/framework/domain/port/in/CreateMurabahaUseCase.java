package com.masrufi.framework.domain.port.in;

import com.masrufi.framework.domain.model.IslamicFinancing;
import com.masrufi.framework.domain.model.CreateMurabahaCommand;

/**
 * Use Case Interface for Murabaha Creation
 * 
 * This interface defines the contract for creating Murabaha financing
 * following the hexagonal architecture pattern. It represents the
 * business capability without revealing implementation details.
 * 
 * Domain Invariants:
 * - Asset must be permissible under Sharia law
 * - Profit margin must be disclosed and reasonable
 * - Financier must own the asset before selling
 * - Transaction must be backed by real economic activity
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public interface CreateMurabahaUseCase {

    /**
     * Create a new Murabaha financing arrangement
     * 
     * @param command The Murabaha creation command containing all required information
     * @return The created Islamic financing arrangement
     * @throws ShariaViolationException if the request violates Sharia principles
     * @throws AssetValidationException if the asset is not valid or permissible
     * @throws CustomerValidationException if the customer is not eligible
     */
    IslamicFinancing createMurabaha(CreateMurabahaCommand command);
}