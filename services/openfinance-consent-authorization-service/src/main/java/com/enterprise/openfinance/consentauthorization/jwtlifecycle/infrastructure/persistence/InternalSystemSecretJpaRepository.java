package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InternalSystemSecretJpaRepository extends JpaRepository<InternalSystemSecretJpaEntity, UUID> {

    Optional<InternalSystemSecretJpaEntity> findBySecretKey(String secretKey);
}
