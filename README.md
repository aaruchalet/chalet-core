# 🏔️ Chalet Core

<p>
  <strong>A cloud-native Spring Boot backend for a Resort Management System demonstrating modern Java development, containerization, Kubernetes orchestration, and CI/CD using GitHub Actions and AWS.</strong>
</p>

<p>

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A?style=for-the-badge&logo=gradle)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-0F1689?style=for-the-badge&logo=helm)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions)
![AWS ECR](https://img.shields.io/badge/AWS-ECR-FF9900?style=for-the-badge&logo=amazonaws)

</p>

---

# 🏗️ Architecture

```text
                           Developer
                               │
                               ▼
                    GitHub Repository
                               │
                               ▼
                     GitHub Actions CI
                               │
                 ┌─────────────┴─────────────┐
                 ▼                           ▼
           Gradle / Tests              Docker Build
                 │                           │
                 └─────────────┬─────────────┘
                               ▼
                         Helm Validation

Manual AWS workflow
       │
       ▼
AWS Authentication
       │
       ▼
Amazon ECR Registry
       │
       ▼
Helm Chart
       │
       ▼
Amazon EKS Deployment (Next Phase)
```

---

# 🚀 Features

- Customer Management
- Room Management
- Booking Management
- REST APIs
- Spring Security
- Flyway Database Migrations
- Spring Boot Actuator
- Dockerized Application
- Kubernetes-ready Deployment using Helm
- Secret-backed MySQL credentials in Helm
- Persistent MySQL Storage for Local Development
- Automated CI Pipeline with GitHub Actions
- Docker Image Publishing to Amazon ECR

---

# 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Build Tool | Gradle |
| Database | MySQL 8.4 |
| Database Migration | Flyway |
| Containerization | Docker |
| Orchestration | Kubernetes |
| Package Manager | Helm |
| CI | GitHub Actions |
| Container Registry | Amazon Elastic Container Registry (ECR) |
| Cloud | AWS |

---

# 📁 Project Structure

```text
chalet-core/
├── src/
│   ├── main/
│   └── test/
│
├── chalet-core/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│
├── .github/
│   └── workflows/
│       ├── gradle-ci.yml
│       └── aws-auth.yml
│
├── Dockerfile
├── compose.yaml
├── build.gradle
├── Makefile
└── README.md
```

---

# ⚙️ Getting Started

## Clone Repository

```bash
git clone https://github.com/aaruchalet/chalet-core.git
cd chalet-core
```

## Build

```bash
./gradlew clean build
```

## Run Locally

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

---

# 🐳 Docker

Build:

```bash
docker build -t chalet-core .
```

Run:

```bash
docker compose up -d
```

---

# ☸️ Helm Deployment

The Helm chart lives in the repository's `chalet-core/` directory.

Install:

```bash
helm install chalet-core ./chalet-core
```

Upgrade:

```bash
helm upgrade chalet-core ./chalet-core
```

Verify:

```bash
kubectl get pods
kubectl get deployments
kubectl get services
```

---

# 🔄 Continuous Integration

The standard Gradle CI workflow runs on pull requests and pushes to `develop` and performs:

- Checkout
- Java 21 setup
- Gradle build and tests
- Docker image build
- Helm chart validation

AWS authentication and ECR publishing are handled by the separate `aws-auth.yml` workflow, which is currently triggered manually with `workflow_dispatch`.

---

# 📦 Docker Images

ECR images are tagged using the Git commit SHA.

Example:

```text
712532372065.dkr.ecr.ap-south-1.amazonaws.com/chalet-core:<commit-sha>
```

This keeps published images traceable to source commits.

---

# 🗄️ Database

- MySQL 8.4
- Flyway Database Migrations
- Hibernate schema validation (`ddl-auto=validate`)
- Persistent storage for local development using Docker volumes and Kubernetes persistent volumes
- Helm supports secret-backed MySQL credentials
- Production deployments can be configured to use an external database such as Amazon RDS

---

# 🩺 Health Checks

Spring Boot Actuator exposes:

```text
/actuator/health
```

The Helm deployment uses the endpoint for startup, readiness, and liveness probes.

---

# 🛣️ Roadmap

## ✅ Completed

- Spring Boot Backend
- REST APIs
- MySQL Integration
- Flyway Migrations
- Docker Support
- Docker Compose
- Kubernetes Setup
- Helm Charts
- Kubernetes Secrets for database credentials
- GitHub Actions CI
- Amazon ECR Integration

## 🚧 In Progress

- Amazon EKS Deployment
- Helm-based Continuous Deployment (CD)

## 📌 Planned

- Ingress Controller
- Horizontal Pod Autoscaler (HPA)
- Prometheus Monitoring
- Grafana Dashboards
- Centralized Logging
- SonarQube Analysis
- Trivy Image Scanning
- Blue-Green / Rolling Deployments

---

# 📈 Delivery Pipeline

```text
Developer
     │
     ▼
GitHub Repository
     │
     ▼
GitHub Actions CI
     │
     ├── Gradle Build + Tests
     ├── Docker Build
     └── Helm Validation

Manual AWS workflow
     │
     ├── AWS Authentication
     ├── ECR Login
     └── Push Image
     ▼
Amazon ECR
     │
     ▼
Helm
     │
     ▼
Amazon EKS (Next Phase)
```

---

# 👨‍💻 Author

**Manjeet Kumar**

GitHub: https://github.com/aaruchalet

---

# 📄 License

This project is intended for learning, experimentation, and demonstrating modern Java backend development and cloud-native DevOps practices.
