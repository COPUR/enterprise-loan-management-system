package com.bank.loanmanagement.loan.infrastructure.persistence;

import com.bank.loanmanagement.loan.domain.party.GroupRole;
import com.bank.loanmanagement.loan.domain.party.GroupType;
import com.bank.loanmanagement.loan.domain.party.RoleSource;
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
 * PartyGroup JPA Entity - Infrastructure Layer
 * Separate persistence model for PartyGroup domain object
 */
@Entity
@Table(name = "party_groups", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "group_name"}),
       indexes = {
           @Index(name = "idx_party_group_name", columnList = "groupName"),
           @Index(name = "idx_party_group_active", columnList = "active"),
           @Index(name = "idx_party_group_type", columnList = "groupType")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"partyId", "groupName"})
public class PartyGroupJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(nullable = false)
    private String groupName;

    @Column(nullable = false)
    private String groupDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType groupType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleSource groupSource;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime effectiveFrom = LocalDateTime.now();

    private LocalDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GroupRole groupRole = GroupRole.MEMBER;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    private String businessUnit;
    private String geographicScope;

    @Column(nullable = false)
    private String assignedBy;

    private String assignmentReason;

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