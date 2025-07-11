package com.amanahfi.platform.islamicfinance.domain.events;

import com.amanahfi.platform.islamicfinance.domain.CustomerId;
import com.amanahfi.platform.islamicfinance.domain.IslamicFinanceProductId;
import com.amanahfi.platform.islamicfinance.domain.IslamicFinanceType;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.shared.domain.Money;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing the creation of an Islamic Finance Product
 * 
 * This event is published when a new Islamic finance product is created
 * in the AmanahFi Platform, enabling downstream processing and integration
 * with MasruFi Framework capabilities.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
public class ProductCreatedEvent implements DomainEvent {

    UUID eventId;
    IslamicFinanceProductId productId;
    CustomerId customerId;
    IslamicFinanceType financeType;
    Money principalAmount;
    String jurisdiction;
    Instant occurredOn;
    EventMetadata metadata;

    public ProductCreatedEvent(IslamicFinanceProductId productId, CustomerId customerId,
                              IslamicFinanceType financeType, Money principalAmount,
                              String jurisdiction) {
        this.eventId = UUID.randomUUID();
        this.productId = productId;
        this.customerId = customerId;
        this.financeType = financeType;
        this.principalAmount = principalAmount;
        this.jurisdiction = jurisdiction;
        this.occurredOn = Instant.now();
        this.metadata = EventMetadata.system("ISLAMIC_FINANCE_PRODUCT_SERVICE");
    }

    @Override
    public String getAggregateId() {
        return productId.toString();
    }

    @Override
    public String getAggregateType() {
        return "IslamicFinanceProduct";
    }

    @Override
    public Long getAggregateVersion() {
        return 1L;
    }

    @Override
    public UUID getCorrelationId() {
        return eventId;
    }

    @Override
    public UUID getCausationId() {
        return null; // Root event
    }

    @Override
    public boolean requiresShariaCompliance() {
        return true;
    }

    @Override
    public boolean requiresRegulatoryReporting() {
        return true;
    }
}