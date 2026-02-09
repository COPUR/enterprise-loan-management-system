package com.enterprise.openfinance.uc06.domain.port.out;

import java.math.BigDecimal;

public interface FundsReservationPort {
    boolean reserve(String debtorAccountId, BigDecimal amount, String currency, String reservationReference);
}
