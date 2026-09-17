# 🏔️ Chalet Core

A Spring Boot backend for a resort-management system, built with Java 21, MySQL, Flyway, Docker, Helm, and GitHub Actions.

## Features

- Customer management
- Room and room-type management
- Booking lifecycle management
- Flyway database migrations
- Spring Boot Actuator health checks
- Docker support
- Helm deployment manifests
- Automated CI with build, tests, and JaCoCo coverage

## Tech stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Build | Gradle |
| Database | MySQL 8.4 |
| Migration | Flyway |
| Mapping | MapStruct |
| Containerization | Docker |
| Kubernetes packaging | Helm |
| CI | GitHub Actions |

## Project structure

```text
chalet-core/
├── src/
│   ├── main/
│   └── test/
├── chalet-core/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
├── .github/
│   └── workflows/
│       └── gradle-ci.yml
├── Dockerfile
├── compose.yaml
├── build.gradle
└── README.md
```

## Build and test

```bash
./gradlew clean build
```

Generate code coverage locally:

```bash
./gradlew test jacocoTestReport
```

The HTML report is generated under:

```text
build/reports/jacoco/test/html/
```

## Run locally

```bash
./gradlew bootRun
```

Application:

```text
http://localhost:8080
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```

## Docker

Build:

```bash
docker build -t chalet-core .
```

Run the local stack:

```bash
docker compose up -d
```

## Helm

The chart is located at `./chalet-core`.

Lint and render it locally:

```bash
helm lint ./chalet-core --set mysql.auth.rootPassword=local-placeholder
helm template chalet-core ./chalet-core --set mysql.auth.rootPassword=local-placeholder
```

Install:

```bash
helm install chalet-core ./chalet-core --set mysql.auth.rootPassword='<password>'
```

The chart also supports supplying an existing Kubernetes Secret instead of an inline password.

## Continuous integration

The standard CI workflow runs on pull requests and pushes to `develop` and performs only basic repository quality checks:

- Gradle build
- Automated tests
- JaCoCo coverage report generation
- JaCoCo report artifact upload

**CI does not authenticate to AWS, log in to ECR, publish images, or deploy infrastructure.** Docker, Helm, AWS, and deployment checks are intentionally kept outside the current CI pipeline.

## Database

- MySQL 8.4
- Flyway-managed schema
- Hibernate schema validation with `ddl-auto=validate`

## Roadmap

Future work may include additional database invariants, ECR/EKS, ingress, autoscaling, observability, security scanning, and automated CD. These are not part of the current CI pipeline.

## Author

**Manjeet Kumar**

## License

This project is intended for learning, experimentation, and demonstrating modern Java backend and cloud-native development practices.
