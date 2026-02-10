package com.enterprise.openfinance.uc15.domain.port.in;

import com.enterprise.openfinance.uc15.domain.model.AtmListResult;
import com.enterprise.openfinance.uc15.domain.query.GetAtmsQuery;

public interface AtmDataUseCase {

    AtmListResult listAtms(GetAtmsQuery query);
}
