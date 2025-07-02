# Loan Management System

A backend API for managing loans, built with Spring Boot following Hexagonal Architecture principles.

## Features

- Create loans with customer credit validation
- List loans with optional filters
- List loan installments
- Process loan payments with early payment discounts and late payment penalties
- Role-based access control (ADMIN and CUSTOMER roles)

## Architecture

The application follows Hexagonal Architecture (Ports & Adapters) with Domain-Driven Design:

- **Domain Layer**: Core business logic, entities, and value objects
- **Application Layer**: Use cases and application services
- **Infrastructure Layer**: Database adapters, REST controllers, and configuration

## Requirements

- Java 21
- Gradle 8.5+

## Running the Application

1. Clone the repository
2. Run the application:
   ```bash
   ./gradlew bootRun


Access the application:

API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
H2 Console: http://localhost:8080/h2-console



Default Users

Admin: admin / admin123
Customer: customer1 / customer123

API Endpoints
Create Loan
POST /api/loans
Authorization: Basic (admin only)
Body: {
"customerId": 1,
"amount": 10000,
"interestRate": 0.2,
"numberOfInstallments": 12
}
List Loans
GET /api/loans/customer/{customerId}?numberOfInstallments=12&isPaid=false
Authorization: Basic (admin or owner customer)
List Installments
GET /api/loans/{loanId}/installments
Authorization: Basic (admin or owner customer)
Pay Loan
POST /api/loans/{loanId}/payments
Authorization: Basic (admin or owner customer)
Body: {
"amount": 5000
}
Business Rules

Loan Creation:

Customer must have sufficient credit limit
Interest rate: 0.1 - 0.5
Installments: 6, 9, 12, or 24
Total amount = principal × (1 + interest rate)


Payment Processing:

Installments must be paid in full
Earliest unpaid installments are paid first
Maximum 3 months advance payment allowed
Early payment discount: amount × 0.001 × days before due date
Late payment penalty: amount × 0.001 × days after due date



Testing
Run unit tests:
bash./gradlew test
Run integration tests:
bash./gradlew integrationTest
Database Schema

Customer: id, name, surname, creditLimit, usedCreditLimit
Loan: id, customerId, loanAmount, numberOfInstallment, createDate, isPaid
LoanInstallment: id, loanId, amount, paidAmount, dueDate, paymentDate, isPaid


This implementation provides a complete loan management system following the requirements from the PDF, with:

1. **Hexagonal Architecture** with clear separation of concerns
2. **Domain-Driven Design** with rich domain models
3. **Clean Code** principles with meaningful names and single responsibility
4. **Comprehensive validation** at multiple layers
5. **Security** with role-based access control
6. **Testability** with unit and integration test examples
7. **Documentation** with Swagger/OpenAPI support

The system handles all the specified requirements including credit validation, installment calculations, payment processing with discounts/penalties, and proper authorization.