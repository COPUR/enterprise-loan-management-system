package com.banking.loans.infrastructure.persistence;

import com.banking.loans.domain.party.RoleSource;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PartyRole JPA Entity - Infrastructure Layer
 * Separate persistence model for PartyRole domain object
 */
@Entity
@Table(name = "party_roles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "role_name"}),
       indexes = {
           @Index(name = "idx_party_role_name", columnList = "roleName"),
           @Index(name = "idx_party_role_active", columnList = "active"),
           @Index(name = "idx_party_role_effective", columnList = "effectiveFrom, effectiveTo")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"partyId", "roleName"})
public class PartyRoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(nullable = false)
    private String roleName;

    @Column(nullable = false)
    private String roleDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleSource roleSource;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime effectiveFrom = LocalDateTime.now();

    private LocalDateTime effectiveTo;

    @Column(nullable = false)
    @Builder.Default
    private Integer authorityLevel = 1;

    private String businessUnit;
    private String geographicScope;
    private Long monetaryLimit;

    @Column(nullable = false)
    private String assignedBy;

    private String assignmentReason;
    private String approvalReference;

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

    // Compliance tracking fields
    private LocalDateTime lastReviewedAt;
    private String reviewedBy;
    private LocalDateTime nextReviewDue;
    private Boolean requiresReview;
    private String complianceNotes;
}