package com.loanmanagement.infrastructure.persistence.repository;

import com.loanmanagement.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository interface for CustomerJpaEntity.
 * This interface is internal to the infrastructure layer and should not be exposed outside.
 */
@Repository
public interface SpringDataCustomerRepository extends JpaRepository<CustomerJpaEntity, Long> {
}
