package com.enterprise.openfinance.uc15.domain.port.out;

import com.enterprise.openfinance.uc15.domain.model.AtmLocation;

import java.util.List;

public interface AtmDirectoryPort {

    List<AtmLocation> listAtms();
}
