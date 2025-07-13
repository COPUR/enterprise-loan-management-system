package com.amanahfi.onboarding.domain.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * KYC Document Entity
 * Represents documents submitted for Know Your Customer verification
 */
@Entity
@Table(name = "kyc_documents")
@Getter
@NoArgsConstructor
public class KycDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String documentId;
    
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;
    
    private String fileName;
    private String fileContent; // Base64 encoded content
    private LocalDateTime uploadedAt;
    
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.UPLOADED;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    public KycDocument(DocumentType documentType, String fileName, String fileContent) {
        this.documentType = documentType;
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.uploadedAt = LocalDateTime.now();
    }
    
    public void assignToCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public void approve() {
        this.status = DocumentStatus.APPROVED;
    }
    
    public void reject(String reason) {
        this.status = DocumentStatus.REJECTED;
        // Store rejection reason if needed
    }
}