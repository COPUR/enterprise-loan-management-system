package com.masrufi.framework.domain.port.out;

import com.masrufi.framework.domain.model.IslamicFinancing;
import com.masrufi.framework.domain.model.IslamicFinancingId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Islamic Financing persistence
 * 
 * This interface defines the contract for persisting and retrieving
 * Islamic financing data following the Repository pattern and
 * hexagonal architecture principles.
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public interface IslamicFinancingRepository {

    /**
     * Save an Islamic financing arrangement
     * 
     * @param islamicFinancing The Islamic financing to save
     * @return The saved Islamic financing with updated metadata
     */
    IslamicFinancing save(IslamicFinancing islamicFinancing);

    /**
     * Find Islamic financing by ID
     * 
     * @param id The Islamic financing ID
     * @return Optional containing the Islamic financing if found
     */
    Optional<IslamicFinancing> findById(IslamicFinancingId id);

    /**
     * Find all Islamic financings for a customer
     * 
     * @param customerId The customer ID
     * @return List of Islamic financings for the customer
     */
    List<IslamicFinancing> findByCustomerId(String customerId);

    /**
     * Find Islamic financings by type
     * 
     * @param type The Islamic financing type
     * @return List of Islamic financings of the specified type
     */
    List<IslamicFinancing> findByType(IslamicFinancing.IslamicFinancingType type);

    /**
     * Find active Islamic financings
     * 
     * @return List of active Islamic financings
     */
    List<IslamicFinancing> findActive();

    /**
     * Check if an Islamic financing exists
     * 
     * @param id The Islamic financing ID
     * @return true if exists, false otherwise
     */
    boolean existsById(IslamicFinancingId id);

    /**
     * Delete an Islamic financing
     * 
     * @param id The Islamic financing ID
     */
    void deleteById(IslamicFinancingId id);

    /**
     * Count total Islamic financings
     * 
     * @return Total count of Islamic financings
     */
    long count();

    /**
     * Count Islamic financings by type
     * 
     * @param type The Islamic financing type
     * @return Count of Islamic financings of the specified type
     */
    long countByType(IslamicFinancing.IslamicFinancingType type);
}