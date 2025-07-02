package com.bank.loanmanagement.loan.saga.context;

import java.util.HashMap;
import java.util.Map;

public class SagaContext {
    private final String sagaId;
    private final Map<String, Object> data;

    public SagaContext(String sagaId) {
        this.sagaId = sagaId;
        this.data = new HashMap<>();
    }

    public String getSagaId() {
        return sagaId;
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(data.get(key));
    }

    public void put(String key, Object value) {
        this.data.put(key, value);
    }
}
