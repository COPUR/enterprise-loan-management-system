package com.enterprise.openfinance.uc06.infrastructure.funds;

import com.enterprise.openfinance.uc06.domain.port.out.FundsReservationPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFundsReservationAdapter implements FundsReservationPort {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public InMemoryFundsReservationAdapter() {
        balances.put("ACC-DEBTOR-001", new BigDecimal("10000.00"));
        balances.put("ACC-LOW-001", new BigDecimal("50.00"));
    }

    @Override
    public boolean reserve(String debtorAccountId, BigDecimal amount, String currency, String reservationReference) {
        if (debtorAccountId == null || amount == null) {
            return false;
        }
        synchronized (balances) {
            BigDecimal current = balances.getOrDefault(debtorAccountId, BigDecimal.ZERO);
            if (current.compareTo(amount) < 0) {
                return false;
            }
            balances.put(debtorAccountId, current.subtract(amount));
            return true;
        }
    }
}
