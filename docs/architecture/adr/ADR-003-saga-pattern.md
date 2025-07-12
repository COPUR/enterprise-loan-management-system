---
**Document Classification**: Technical Architecture Decision
**Author**: Lead Enterprise Architect - Banking Systems Engineering
**Version**: 1.2
**Last Updated**: 2024-07-12
**Review Cycle**: Quarterly
**Stakeholders**: Architecture Review Board, Engineering Teams, Risk Management
---

# ADR-003: SAGA Pattern for Distributed Transactions

## Status
Accepted

## Date
2024-01-15

## Executive Summary

This Architecture Decision Record documents the implementation of the SAGA orchestration pattern for managing distributed transactions in our enterprise banking microservices ecosystem. Drawing from extensive experience in mission-critical financial systems, this decision addresses the fundamental challenge of maintaining data consistency across distributed services while ensuring regulatory compliance and operational resilience. The SAGA pattern provides a proven approach for handling complex financial workflows that span multiple bounded contexts, enabling atomic-like behavior without the scalability limitations of traditional distributed transactions.

## Context

Our enterprise loan management system consists of multiple bounded contexts that need to coordinate complex business operations:

1. **Customer Management**: Credit limit validation and reservation
2. **Loan Origination**: Loan creation and installment scheduling
3. **Payment Processing**: Payment handling and loan completion

These operations span multiple aggregates and potentially multiple services, creating challenges:

- **Distributed Transactions**: Operations need to be coordinated across multiple bounded contexts
- **Data Consistency**: We need to maintain consistency without using distributed transactions (2PC)
- **Failure Handling**: Partial failures must be properly compensated
- **Business Processes**: Complex workflows like loan creation involve multiple steps

Traditional approaches have limitations:
- **ACID Transactions**: Don't scale across distributed systems
- **Two-Phase Commit (2PC)**: Creates tight coupling and availability issues
- **Eventual Consistency**: May not provide sufficient guarantees for critical business operations

## Decision

We will implement the SAGA pattern to manage distributed transactions across our bounded contexts.

### SAGA Pattern Overview

A SAGA is a sequence of local transactions where each transaction updates data within a single service. If a local transaction fails, the SAGA executes compensating transactions to undo the changes made by preceding transactions.

### Implementation Approach: Orchestration

We choose the **orchestration approach** over choreography for better visibility and control:

