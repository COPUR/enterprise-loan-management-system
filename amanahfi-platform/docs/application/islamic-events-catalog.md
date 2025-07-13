# Islamic Banking Event Catalog - AmanahFi Platform

## Overview

This document provides a comprehensive catalog of domain events used in the AmanahFi Islamic Banking Platform. All events are designed to be Sharia-compliant and maintain complete audit trails for regulatory compliance.

## Event Categories

### 1. Murabaha Events (Cost-Plus Financing)

#### MurabahaContractCreated
**Purpose**: Initiated when a new Murabaha contract is created
**Aggregate**: Murabaha
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.murabaha.MurabahaContractCreated",
  "aggregateId": "murabaha-contract-id",
  "version": 1,
  "timestamp": "2024-12-27T10:00:00Z",
  "data": {
    "contractId": "uuid",
    "customerId": "uuid",
    "assetDetails": {
      "assetType": "REAL_ESTATE | VEHICLE | COMMODITY",
      "assetValue": {
        "amount": "decimal",
        "currency": "AED | USD | EUR"
      },
      "assetDescription": "string"
    },
    "purchasePrice": {
      "amount": "decimal",
      "currency": "AED"
    },
    "profitMargin": {
      "percentage": "decimal",
      "amount": "decimal"
    },
    "paymentTerms": {
      "installments": "integer",
      "paymentFrequency": "MONTHLY | QUARTERLY | ANNUALLY"
    },
    "shariaApprovalRequired": true,
    "contractStatus": "PENDING_SHARIA_APPROVAL"
  }
}
```

#### ProfitMarginCalculated
**Purpose**: Triggered when profit margin is calculated for Murabaha contract
**Aggregate**: Murabaha
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.murabaha.ProfitMarginCalculated",
  "aggregateId": "murabaha-contract-id",
  "version": 2,
  "timestamp": "2024-12-27T10:05:00Z",
  "data": {
    "contractId": "uuid",
    "costPrice": {
      "amount": "decimal",
      "currency": "AED"
    },
    "profitMargin": {
      "percentage": "decimal",
      "amount": "decimal"
    },
    "sellingPrice": {
      "amount": "decimal",
      "currency": "AED"
    },
    "calculationMethod": "FIXED_PERCENTAGE | DIMINISHING_BALANCE",
    "calculatedBy": "system-id",
    "calculationTimestamp": "2024-12-27T10:05:00Z"
  }
}
```

#### MurabahaContractApproved
**Purpose**: Raised when Sharia Board approves Murabaha contract
**Aggregate**: Murabaha
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.murabaha.MurabahaContractApproved",
  "aggregateId": "murabaha-contract-id",
  "version": 3,
  "timestamp": "2024-12-27T10:30:00Z",
  "data": {
    "contractId": "uuid",
    "shariaApproval": {
      "approvedBy": "sharia-board-member-id",
      "approvalDate": "2024-12-27",
      "fatwaNuber": "FB-2024-001",
      "approvalNotes": "string"
    },
    "contractStatus": "SHARIA_APPROVED",
    "effectiveDate": "2024-12-27",
    "expiryDate": "2027-12-27"
  }
}
```

### 2. CBDC Events (Central Bank Digital Currency)

#### CBDCWalletCreated
**Purpose**: Created when a new CBDC wallet is established
**Aggregate**: CBDCWallet
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.cbdc.CBDCWalletCreated",
  "aggregateId": "cbdc-wallet-id",
  "version": 1,
  "timestamp": "2024-12-27T09:00:00Z",
  "data": {
    "walletId": "uuid",
    "customerId": "uuid",
    "walletType": "INDIVIDUAL | BUSINESS | INSTITUTIONAL",
    "cbdcType": "UAE_DIGITAL_DIRHAM",
    "initialBalance": {
      "amount": "0.00",
      "currency": "CBDC-AED"
    },
    "kycLevel": "BASIC | ENHANCED | PREMIUM",
    "walletStatus": "ACTIVE",
    "centralBankRegistration": {
      "registrationId": "string",
      "registrationDate": "2024-12-27"
    }
  }
}
```

#### CBDCTransferInitiated
**Purpose**: Initiated when CBDC transfer begins
**Aggregate**: CBDCTransaction
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.cbdc.CBDCTransferInitiated",
  "aggregateId": "cbdc-transaction-id",
  "version": 1,
  "timestamp": "2024-12-27T11:00:00Z",
  "data": {
    "transactionId": "uuid",
    "fromWalletId": "uuid",
    "toWalletId": "uuid",
    "amount": {
      "amount": "decimal",
      "currency": "CBDC-AED"
    },
    "transferType": "P2P | P2B | B2B | CROSS_BORDER",
    "purpose": "MURABAHA_PAYMENT | PROFIT_DISTRIBUTION | REGULAR_PAYMENT",
    "reference": "string",
    "transactionStatus": "INITIATED",
    "centralBankReference": "string"
  }
}
```

#### CBDCTransferCompleted
**Purpose**: Completed when CBDC transfer is finalized
**Aggregate**: CBDCTransaction
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.cbdc.CBDCTransferCompleted",
  "aggregateId": "cbdc-transaction-id",
  "version": 2,
  "timestamp": "2024-12-27T11:01:00Z",
  "data": {
    "transactionId": "uuid",
    "completionTimestamp": "2024-12-27T11:01:00Z",
    "finalAmount": {
      "amount": "decimal",
      "currency": "CBDC-AED"
    },
    "transactionFee": {
      "amount": "decimal",
      "currency": "CBDC-AED"
    },
    "transactionStatus": "COMPLETED",
    "centralBankConfirmation": "string",
    "settlementId": "string"
  }
}
```

### 3. Sharia Compliance Events

#### ShariaComplianceCheckInitiated
**Purpose**: Started when compliance check begins
**Aggregate**: ComplianceCheck
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.compliance.ShariaComplianceCheckInitiated",
  "aggregateId": "compliance-check-id",
  "version": 1,
  "timestamp": "2024-12-27T08:00:00Z",
  "data": {
    "checkId": "uuid",
    "entityType": "MURABAHA_CONTRACT | CUSTOMER | TRANSACTION",
    "entityId": "uuid",
    "checkType": "SHARIA_COMPLIANCE | AML_SCREENING | KYC_VERIFICATION",
    "priority": "HIGH | MEDIUM | LOW",
    "assignedTo": "compliance-officer-id",
    "checkStatus": "INITIATED",
    "dueDate": "2024-12-28"
  }
}
```

#### ShariaComplianceApproved
**Purpose**: Approved when entity passes Sharia compliance
**Aggregate**: ComplianceCheck
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.compliance.ShariaComplianceApproved",
  "aggregateId": "compliance-check-id",
  "version": 2,
  "timestamp": "2024-12-27T14:00:00Z",
  "data": {
    "checkId": "uuid",
    "approvalDetails": {
      "approvedBy": "sharia-scholar-id",
      "approvalDate": "2024-12-27",
      "complianceLevel": "FULLY_COMPLIANT",
      "fatwaBasis": "Quran 2:275, Hadith Reference",
      "approvalNotes": "string"
    },
    "checkStatus": "APPROVED",
    "validUntil": "2025-12-27"
  }
}
```

#### ShariaComplianceViolationDetected
**Purpose**: Triggered when Sharia violation is detected
**Aggregate**: ComplianceViolation
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.compliance.ShariaComplianceViolationDetected",
  "aggregateId": "violation-id",
  "version": 1,
  "timestamp": "2024-12-27T15:00:00Z",
  "data": {
    "violationId": "uuid",
    "entityType": "TRANSACTION | CONTRACT | PRODUCT",
    "entityId": "uuid",
    "violationType": "RIBA_DETECTED | GHARAR_PRESENT | HARAM_ACTIVITY",
    "severity": "CRITICAL | HIGH | MEDIUM | LOW",
    "description": "string",
    "detectedBy": "system | compliance-officer-id",
    "shariaReference": "Quran verse or Hadith reference",
    "immediateAction": "BLOCK_TRANSACTION | REQUIRE_APPROVAL | NOTIFY_BOARD",
    "violationStatus": "DETECTED"
  }
}
```

### 4. Islamic Account Events

#### IslamicAccountCreated
**Purpose**: Created when new Islamic banking account is opened
**Aggregate**: IslamicAccount
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.accounts.IslamicAccountCreated",
  "aggregateId": "account-id",
  "version": 1,
  "timestamp": "2024-12-27T09:30:00Z",
  "data": {
    "accountId": "uuid",
    "customerId": "uuid",
    "accountType": "CURRENT | SAVINGS | INVESTMENT | MURABAHA",
    "accountNumber": "string",
    "currency": "AED | USD | EUR",
    "shariaCompliant": true,
    "profitSharingRatio": {
      "customerShare": "decimal",
      "bankShare": "decimal"
    },
    "accountStatus": "ACTIVE",
    "openingBalance": {
      "amount": "decimal",
      "currency": "AED"
    },
    "shariaApprovalReference": "string"
  }
}
```

#### ProfitDistributionCalculated
**Purpose**: Calculated when profit sharing is determined
**Aggregate**: IslamicAccount
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.accounts.ProfitDistributionCalculated",
  "aggregateId": "account-id",
  "version": 2,
  "timestamp": "2024-12-27T16:00:00Z",
  "data": {
    "accountId": "uuid",
    "calculationPeriod": {
      "startDate": "2024-12-01",
      "endDate": "2024-12-31"
    },
    "totalProfit": {
      "amount": "decimal",
      "currency": "AED"
    },
    "customerShare": {
      "amount": "decimal",
      "percentage": "decimal"
    },
    "bankShare": {
      "amount": "decimal",
      "percentage": "decimal"
    },
    "distributionMethod": "MONTHLY | QUARTERLY | ANNUALLY",
    "calculationBasis": "AVERAGE_BALANCE | DAILY_BALANCE",
    "shariaApproved": true
  }
}
```

### 5. Risk Assessment Events

#### IslamicRiskAssessmentInitiated
**Purpose**: Started when Islamic risk assessment begins
**Aggregate**: RiskAssessment
**Schema**:
```json
{
  "eventId": "uuid",
  "eventType": "com.amanahfi.risk.IslamicRiskAssessmentInitiated",
  "aggregateId": "risk-assessment-id",
  "version": 1,
  "timestamp": "2024-12-27T12:00:00Z",
  "data": {
    "assessmentId": "uuid",
    "customerId": "uuid",
    "assessmentType": "CREDIT_RISK | SHARIA_RISK | OPERATIONAL_RISK",
    "productType": "MURABAHA | MUSHARAKAH | IJARAH",
    "riskFactors": [
      {
        "factor": "PROFIT_VOLATILITY",
        "weight": "decimal",
        "score": "decimal"
      }
    ],
    "assessmentStatus": "INITIATED",
    "assignedAnalyst": "risk-analyst-id"
  }
}
```

## Event Sourcing Guidelines

### Event Versioning
- All events include a version number for backward compatibility
- Schema evolution follows additive-only pattern
- Breaking changes require new event types

### Event Metadata
```json
{
  "eventId": "uuid",
  "eventType": "fully.qualified.event.name",
  "aggregateId": "aggregate-root-id",
  "aggregateType": "aggregate-type-name",
  "version": "integer",
  "timestamp": "ISO-8601-timestamp",
  "correlationId": "uuid",
  "causationId": "uuid",
  "metadata": {
    "userId": "uuid",
    "tenantId": "uuid",
    "source": "application-name",
    "shariaCompliant": true
  }
}
```

### Islamic Banking Event Principles

1. **Sharia Compliance**: All events must comply with Islamic principles
2. **Audit Trail**: Complete immutable audit trail for regulatory compliance
3. **Temporal Queries**: Support for time-based queries and reporting
4. **Event Replay**: Ability to replay events for system recovery
5. **Real-time Processing**: Events processed in real-time for immediate compliance

### Event Storage Strategy

- **Primary Store**: PostgreSQL with JSONB for event data
- **Secondary Store**: Apache Kafka for event streaming
- **Cache Layer**: Redis for frequently accessed events
- **Archive**: Long-term storage in AWS S3 for compliance

### Sharia Compliance Validation

Each event undergoes automatic Sharia compliance validation:
- Real-time validation against Islamic finance rules
- Integration with Sharia Board approval workflows
- Automatic blocking of non-compliant transactions
- Audit trail for all compliance decisions

---

**Document Version**: 1.0.0  
**Last Updated**: December 2024  
**Maintained By**: AmanahFi Islamic Finance Architecture Team  
**Sharia Compliance**: Verified by Islamic Finance Board