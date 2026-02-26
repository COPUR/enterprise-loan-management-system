package com.enterprise.openfinance.productcatalog.domain.port.in;

import com.enterprise.openfinance.productcatalog.domain.model.ProductListResult;
import com.enterprise.openfinance.productcatalog.domain.query.GetProductsQuery;

public interface ProductDataUseCase {

    ProductListResult listProducts(GetProductsQuery query);
}
