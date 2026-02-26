package com.enterprise.openfinance.productcatalog.domain.port.out;

import com.enterprise.openfinance.productcatalog.domain.model.OpenProduct;

import java.util.List;
import java.util.Optional;

public interface ProductCatalogPort {

    List<OpenProduct> findProducts(Optional<String> type, Optional<String> segment);
}
