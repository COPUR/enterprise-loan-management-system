package com.enterprise.openfinance.uc14.domain.port.in;

import com.enterprise.openfinance.uc14.domain.model.ProductListResult;
import com.enterprise.openfinance.uc14.domain.query.GetProductsQuery;

public interface ProductDataUseCase {

    ProductListResult listProducts(GetProductsQuery query);
}
