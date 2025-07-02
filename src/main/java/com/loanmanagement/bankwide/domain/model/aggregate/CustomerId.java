// bank-wide-service/domain/model/value/CustomerId.java
package com.loanmanagement.bankwide.domain.model.aggregate;

import com.loanmanagement.sharedkernel.domain.model.EntityId;
import java.util.UUID;

public final class CustomerId extends EntityId {
    
    private CustomerId(String value) {
        super(value);
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
}
