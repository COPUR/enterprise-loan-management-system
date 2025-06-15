
package com.bank.loanmanagement.infrastructure.mapper;

import com.bank.loanmanagement.application.dto.CustomerResponse;
import com.bank.loanmanagement.domain.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CustomerMapper {
    
    CustomerResponse toResponse(Customer customer);
}
