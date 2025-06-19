package com.banking.loans.infrastructure.persistence;

import com.banking.loans.domain.party.ComplianceLevel;
import com.banking.loans.domain.party.PartyStatus;
import com.banking.loans.domain.party.PartyType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * Party JPA Entity - Infrastructure Layer
 * Separate persistence model for Party domain object
 */
@Entity
@Table(name = "parties", indexes = {
    @Index(name = "idx_party_external_id", columnList = "externalId"),
    @Index(name = "idx_party_type_status", columnList = "partyType, status"),
    @Index(name = "idx_party_compliance_level", columnList = "complianceLevel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PartyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String externalId;

    @Column(unique = true, nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType partyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PartyStatus status = PartyStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ComplianceLevel complianceLevel = ComplianceLevel.STANDARD;

    @OneToMany(mappedBy = "partyId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PartyRoleJpaEntity> partyRoles = new HashSet<>();

    @OneToMany(mappedBy = "partyId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PartyGroupJpaEntity> partyGroups = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @Version
    private Long version;
}