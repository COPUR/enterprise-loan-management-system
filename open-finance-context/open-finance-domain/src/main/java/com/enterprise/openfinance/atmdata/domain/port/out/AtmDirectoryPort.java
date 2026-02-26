package com.enterprise.openfinance.atmdata.domain.port.out;

import com.enterprise.openfinance.atmdata.domain.model.AtmLocation;

import java.util.List;

public interface AtmDirectoryPort {

    List<AtmLocation> listAtms();
}
