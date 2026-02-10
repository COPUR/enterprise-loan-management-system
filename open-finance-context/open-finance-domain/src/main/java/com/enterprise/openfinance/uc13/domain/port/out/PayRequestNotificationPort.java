package com.enterprise.openfinance.uc13.domain.port.out;

import com.enterprise.openfinance.uc13.domain.model.PayRequest;

public interface PayRequestNotificationPort {

    void notifyPayRequestCreated(PayRequest request);

    void notifyPayRequestFinalized(PayRequest request);
}
