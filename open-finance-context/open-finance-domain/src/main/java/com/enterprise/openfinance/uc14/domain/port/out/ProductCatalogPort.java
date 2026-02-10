package com.enterprise.openfinance.uc14.domain.port.out;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;

import java.util.List;
import java.util.Optional;

public interface ProductCatalogPort {

    List<OpenProduct> findProducts(Optional<String> type, Optional<String> segment);
}
