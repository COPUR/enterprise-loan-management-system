# Microservice Template (Gradle / Java 23)

This template is the baseline for extracted microservice repos.

## Stack
- Gradle (Java 23 toolchain)
- Spring Boot (web + validation)
- JUnit 5 + Mockito
- Jacoco (85% coverage gate)

## Structure
```
.
├── build.gradle
├── settings.gradle
├── gradle.properties
├── src
│   ├── main
│   │   └── java
│   └── test
│       └── java
├── infrastructure
│   ├── Dockerfile
│   └── helm
│       └── chart
└── README.md
```

## Conventions
- Domain layer has zero framework dependencies.
- Application layer orchestrates use cases and depends only on domain ports.
- Infrastructure layer implements adapters (REST, persistence, external connectors).

