# 🏔️ Chalet Core

A Spring Boot backend for a resort-management system, built with Java 21, MySQL, Flyway, Docker, Helm, and GitHub Actions.

## Features

- Customer management
- Room and room-type management
- Booking lifecycle management
- Member sign-up/sign-in with password or OTP
- 500 welcome points (₹500 booking value) on first member signup
- Google OAuth UI reserved but currently disabled
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

## Local development prerequisites

Before running Chalet Core locally, make sure you have:

- Java 21
- Docker with Docker Compose
- Port `2211` available for MySQL
- Port `8080` available for Spring Boot

Start the local MySQL database:

```bash
make mysql-up
```

Local runs default to the `dev` profile and use these fixed local values:

```text
Database: chalet_db
Host: localhost
Port: 2211
Username: chalet_app
Password: chalet_app_dev
JDBC URL: jdbc:mysql://localhost:2211/chalet_db
```

The `dev` profile intentionally does not read `SPRING_DATASOURCE_*` environment variables. This prevents stale IDE or shell variables from overriding the local JDBC configuration.

Docker Compose and Kubernetes use explicit non-dev profiles and continue to receive their datasource settings from container/Kubernetes environment configuration.

## Run locally

Run the application with the development profile:

```bash
make run-dev
```

`make run-dev` starts the local MySQL container first and then runs the dedicated Gradle `bootRunDev` task. That task supplies the dev profile and local datasource as Spring Boot command-line arguments, so stale `SPRING_DATASOURCE_*` environment variables cannot override the local JDBC configuration.

Equivalent Gradle command:

```bash
./gradlew bootRunDev
```

Use `bootRunDev` for local development instead of plain `bootRun`.

Application:

```text
http://localhost:8080
```

Aaru's Chalet booking UI:

```text
http://localhost:8080/
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```

For a full local startup from a clean terminal, one command is enough:

```bash
make run-dev
```

## Authentication

The booking UI includes member sign-up and sign-in.

Supported flows:

- Email or phone + password
- Email or phone + 6-digit OTP
- Google sign-in button is intentionally disabled for now
- Session-based sign-in state
- Normal sign-up creates/links a `customer` record so the signed-in member can book directly

For local development, OTPs are returned by the API and logged so the flow can be tested without an SMS/email provider. OTP requests only proceed for an existing Aaru’s Chalet member account; unknown email/phone details are directed to sign up first. Production OTP delivery still requires an external email/SMS provider.

New local/password signups receive 500 persisted membership points, shown as ₹500 of booking value. Redemption against checkout/payment is a separate booking/payment capability and is not yet applied automatically.

Google sign-in is intentionally disabled in the UI for now.

## Docker

Build:

```bash
docker build -t chalet-core .
```

Run the local stack:

```bash
docker compose up -d
```

The Compose stack uses a dedicated MySQL application account (`chalet_app`) instead of connecting the service as `root`. The checked-in password defaults are development-only and can be overridden before startup:

```bash
export CHALET_DB_ROOT_PASSWORD='<local-root-password>'
export CHALET_DB_APP_USER='chalet_app'
export CHALET_DB_APP_PASSWORD='<local-app-password>'
docker compose up -d
```

## Helm

The chart is located at `./chalet-core`.

Lint and render it locally:

```bash
helm lint ./chalet-core \
  --set mysql.auth.rootPassword=local-root-placeholder \
  --set mysql.auth.appPassword=local-app-placeholder

helm template chalet-core ./chalet-core \
  --set mysql.auth.rootPassword=local-root-placeholder \
  --set mysql.auth.appPassword=local-app-placeholder
```

Install:

```bash
helm install chalet-core ./chalet-core \
  --set mysql.auth.rootPassword='<root-password>' \
  --set mysql.auth.appPassword='<app-password>'
```

The chart provisions a dedicated MySQL application user and keeps its password separate from the MySQL root credential. Existing Kubernetes Secrets can be supplied independently with `mysql.auth.existingSecret` for the root credential and `mysql.auth.appExistingSecret` for the application credential.

### Centralized environment configuration

Environment-specific **non-secret** values are owned by the private `aaruchalet/aaru-platform-config` repository. This repository continues to own the Helm chart templates and safe standalone defaults; the platform-config repository supplies deployment-time overlays.

With both repositories checked out as siblings, a development deployment can layer the centralized values over this chart:

```bash
helm upgrade --install chalet-core ./chalet-core \
  -f ../aaru-platform-config/services/chalet-core/base/values.yaml \
  -f ../aaru-platform-config/services/chalet-core/environments/dev.yaml \
  --set mysql.auth.rootPassword='<root-password>' \
  --set mysql.auth.appPassword='<app-password>'
```

The same structure exists for `qa` and `prod`. Production values that have not been explicitly defined remain absent rather than being guessed. Passwords and other secret values do **not** belong in `aaru-platform-config`; they must continue to come from Kubernetes Secrets or a dedicated secret backend.

There is no automated deployment consumer yet. CI remains build/test/coverage only, so the external configuration is applied only when an operator or future CD workflow explicitly supplies these values.

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
- Dedicated application database account; root is reserved for database initialization/administration

## Roadmap

Future work may include additional database invariants, ECR/EKS, ingress, autoscaling, observability, security scanning, and automated CD. These are not part of the current CI pipeline.

## Author

**Manjeet Kumar**

## License

This project is intended for learning, experimentation, and demonstrating modern Java backend and cloud-native development practices.
