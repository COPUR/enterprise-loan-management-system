# Development Guide - Enterprise Loan Management System

Welcome to the development environment for the Enterprise Loan Management System.

## Quick Start

### Prerequisites
- Java 25.0.2+
- Gradle 9.3.1+
- Docker & Docker Compose
- Git

### Setup Development Environment
```bash
# Clone and setup
git clone <repository-url>
cd enterprise-loan-management-system

# Run setup script
./scripts/setup-local-dev.sh

# Start development server
./dev-start.sh
```

## Development Commands

### Application
```bash
# Start with hot reload
./dev-start.sh

# Start in debug mode
./gradlew runDebug

# Run tests
./dev-test.sh

# Reset database
./dev-reset-db.sh
```

### Gradle Tasks
```bash
# Development server with hot reload
./gradlew runDev

# Debug mode (port 5005)
./gradlew runDebug

# Development tests
./gradlew testDev

# Load test data
./gradlew loadTestData

# Clean development files
./gradlew cleanDev
```

## Development URLs

| Service | URL | Description |
|---------|-----|-------------|
| Main App | http://localhost:8080 | Banking application |
| H2 Console | http://localhost:8080/h2-console | Database console |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| GraphQL | http://localhost:8080/graphql | GraphQL playground |
| Actuator | http://localhost:8080/actuator | Application metrics |

## Database Configuration

### H2 Database (Default for Development)
- **URL**: `jdbc:h2:mem:banking_dev`
- **Username**: `sa`
- **Password**: *(empty)*
- **Console**: http://localhost:8080/h2-console

### Test Data
The development environment automatically loads test data including:
- Sample customers (individual and corporate)
- Sample loans (personal, auto, business)
- Sample payments and installments
- Credit scores and financial profiles

## Debugging

### IDE Configuration
- **IntelliJ IDEA**: Use "Banking App (Debug)" run configuration
- **VS Code**: Use "Banking App (Debug)" launch configuration
- **Remote Debug Port**: 5005

### Debug Features
- Spring DevTools hot reload
- LiveReload for web assets
- Debug logging enabled
- H2 console for database inspection

## Monitoring

### Actuator Endpoints
- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application information
- `/actuator/prometheus` - Prometheus metrics

### Local Prometheus (Optional)
```bash
# Start Prometheus for development
docker run -p 9090:9090 -v ./monitoring/prometheus/prometheus-dev.yml:/etc/prometheus/prometheus.yml prom/prometheus
```

## Testing

### Test Categories
- **Unit Tests**: `./gradlew test`
- **Integration Tests**: `./gradlew integrationTest`
- **Development Tests**: `./gradlew testDev`

### Test Data
Development test data is automatically created and includes:
- Customers: DEV-CUST-001, DEV-CUST-002, DEV-CORP-001
- Loans: Various loan types and statuses
- Payments: Sample payment transactions

## Configuration

### Environment Variables
Development configuration is in `.env.local`:
- Spring profiles: `local,development,h2`
- Database: H2 in-memory
- Security: Relaxed for development
- Logging: Debug level enabled

### Profiles
- `local` - Local development settings
- `development` - Development-specific features
- `h2` - H2 database configuration

## Development Tools

### Hot Reload
- Automatic restart on code changes
- LiveReload for web assets
- Class reloading without full restart

### Code Quality
- Checkstyle configuration
- SpotBugs integration
- JaCoCo test coverage

## Development Structure
```
.
├── src/main/java/              # Application code
├── src/test/java/              # Test code
├── src/main/resources/         # Resources
├── logs/dev/                   # Development logs
├── data/dev/                   # Development data
├── monitoring/                 # Monitoring configs
├── .env.local                  # Local environment
├── dev-start.sh               # Quick start script
├── dev-test.sh                # Test script
└── dev-reset-db.sh            # Database reset
```

## Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

#### Database Issues
```bash
# Reset development database
./dev-reset-db.sh
```

#### Hot Reload Not Working
```bash
# Restart with clean build
./gradlew clean runDev
```

### Getting Help
1. Check logs in `logs/dev/`
2. Verify H2 console at http://localhost:8080/h2-console
3. Check actuator health at http://localhost:8080/actuator/health
4. Review configuration in `.env.local`

## Next Steps

1. **API Development**: Add new endpoints in REST or GraphQL
2. **Database Changes**: Update domain models and migrations
3. **AI Integration**: Enable AI services with API keys
4. **Testing**: Add comprehensive test coverage
5. **Documentation**: Update API documentation
