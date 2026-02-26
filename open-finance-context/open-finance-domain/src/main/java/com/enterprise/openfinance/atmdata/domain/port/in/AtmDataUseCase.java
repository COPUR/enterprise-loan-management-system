package com.enterprise.openfinance.atmdata.domain.port.in;

import com.enterprise.openfinance.atmdata.domain.model.AtmListResult;
import com.enterprise.openfinance.atmdata.domain.query.GetAtmsQuery;

public interface AtmDataUseCase {

    AtmListResult listAtms(GetAtmsQuery query);
}
