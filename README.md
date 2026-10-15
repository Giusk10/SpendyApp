---
title: Spendy
emoji: 🏆
colorFrom: green
colorTo: pink
sdk: docker
pinned: false
license: agpl-3.0
app_port: 7860
---

# Spendy Backend 🏠💸

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud_Gateway-Reactive-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-gateway)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas_%26_Local-47A248?style=flat&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Apache OpenNLP](https://img.shields.io/badge/Apache_OpenNLP-2.3.0-D22128?style=flat&logo=apache&logoColor=white)](https://opennlp.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-AGPLv3-blue.svg)](LICENSE)

**Spendy** is a high-performance, microservices-based backend platform designed for **personal finance management** as well as shared households and roommates to track daily expenses, import bank statements, and analyze cash flows with automated debt settlement. 

Engineered on **Java 21** and **Spring Boot 3.5**, Spendy utilizes a reactive **Spring Cloud Gateway**, **virtual threads**, independent **MongoDB** databases, and an integrated **Apache OpenNLP Machine Learning** engine for automated expense categorization.

---

## 📑 Table of Contents

- [System Architecture](#-system-architecture)
- [Microservices Breakdown](#-microservices-breakdown)
  - [API Gateway](#1-api-gateway)
  - [Auth Microservice](#2-auth-microservice)
  - [Expense Microservice](#3-expense-microservice)
- [Machine Learning & Categorization Engine](#-machine-learning--categorization-engine)
- [Tech Stack & Libraries](#-tech-stack--libraries)
- [Environment Configuration](#-environment-configuration)
- [REST API Reference](#-rest-api-reference)
  - [Authentication Endpoints](#authentication-endpoints)
  - [Expense Endpoints](#expense-endpoints)
- [Getting Started & Local Setup](#-getting-started--local-setup)
- [Docker & Containerized Deployment](#-docker--containerized-deployment)
- [Testing & Quality Assurance](#-testing--quality-assurance)
  - [Unit & Integration Tests](#unit--integration-tests)
  - [Performance & Load Testing (k6)](#performance--load-testing-k6)
- [License](#-license)

---

## 🏗 System Architecture

Spendy follows an asynchronous, cloud-native microservices architecture orchestrated behind a reactive reverse proxy (API Gateway). The Gateway manages SSL termination, unified CORS headers, JWT authentication filtering, and internal service route rewrites.

```mermaid
flowchart TD
    Client["Client Applications<br/>(iOS App / Web Client)"]
    
    subgraph Edge ["Edge Layer"]
        Gateway["Spring Cloud Gateway<br/>(Port: 7860 / 8080)<br/>- Reactive Netty WebFlux<br/>- JWT Filter & Token Verification<br/>- CORS & Security Policy"]
    end

    subgraph Microservices ["Microservices Layer (Internal)"]
        AuthService["AuthMicroService<br/>(Port: 8081)<br/>- Jersey / Spring Boot<br/>- BCrypt Password Hashing<br/>- JWT Token Generation & Rotation"]
        ExpenseService["ExpenseMicroService<br/>(Port: 8084)<br/>- Jersey / Spring Boot<br/>- Smart CSV Parsing<br/>- OpenNLP ML Categorizer<br/>- Expense Analytics & CRUD"]
    end

    subgraph DataStore ["Data Layer"]
        AuthDB[("MongoDB<br/>(Users & Auth Credentials)")]
        ExpenseDB[("MongoDB<br/>(Expenses & Categories)")]
    end

    Client -->|HTTP / HTTPS| Gateway
    Gateway -->|Rewrite: /Auth/** &rarr; /rest/auth/**| AuthService
    Gateway -->|Rewrite: /Expense/** &rarr; /rest/expense/**| ExpenseService
    AuthService --> AuthDB
    ExpenseService --> ExpenseDB
```

---

## 🧩 Microservices Breakdown

### 1. API Gateway
- **Directory**: `Gateway/`
- **Port**: `7860` (Production / Hugging Face Spaces) or `8080` (Local fallback)
- **Role**: Single entry point for all incoming traffic.
- **Key Features**:
  - Non-blocking reactive routing powered by **Spring WebFlux** and **Reactor Netty**.
  - Java 21 **Virtual Threads** enabled (`spring.threads.virtual.enabled=true`).
  - Pre-routing `JWTAuthenticationFilter` validating Bearer tokens against the HMAC SHA secret.
  - Path rewriting rules:
    - `/Auth/**` &rarr; mapped to `AuthMicroService`
    - `/Expense/**` &rarr; mapped to `ExpenseMicroService`
  - Fine-grained CORS configuration for web clients and local development environments.

### 2. Auth Microservice
- **Directory**: `AuthMicroService/`
- **Port**: `8081`
- **Role**: Identity provider and account lifecycle management.
- **Key Features**:
  - User registration with regex email validation and duplicate checking.
  - Secure credential authentication utilizing **jBCrypt** with salted hashes.
  - JWT Access Token generation (15-minute validity) alongside Refresh Token rotation.
  - User profile retrieval and updating (`/profile`, `/updateProfile`).
  - Persistence backed by MongoDB with standardized UUID representation.

### 3. Expense Microservice
- **Directory**: `ExpenseMicroService/`
- **Port**: `8084`
- **Role**: Financial management, transaction aggregation, and statement parsing.
- **Key Features**:
  - **Smart CSV Statement Importer**: Automatically detects column formats and delimiters (semicolon, comma, tab) from bank statements (e.g., Revolut, Intesa Sanpaolo, N26).
  - **Expense Analytics**: Aggregate expense figures by exact dates, monthly windows, and yearly breakdowns.
  - **Full Transaction CRUD**: Create, read, update, single delete, and bulk wipe transactions linked to the authenticated user ID.
  - **Dual Categorization**: Real-time category classification on imported and manually added expenses.

---

## 🤖 Machine Learning & Categorization Engine

Expenses are classified using a **dual-layer intelligence strategy** located in `ExpenseMicroService`:

```mermaid
flowchart LR
    Input["Transaction Description<br/>(e.g., 'PAGAMENTO POS CONAD')"] --> Preprocess["Tokenizer & Lowercase Normalizer"]
    Preprocess --> OpenNLP{"OpenNLP DoccatModel<br/>Available?"}
    OpenNLP -- Yes --> MaxEnt["Maximum Entropy ML Categorizer<br/>(expense-model.bin)"]
    OpenNLP -- No / Low Confidence --> Fallback["Rule-Based Fallback Engine<br/>(ExpenseClassifier)"]
    MaxEnt --> Category["Resolved Category<br/>(e.g., 'Supermercati e Alimentari')"]
    Fallback --> Category
```

1. **Machine Learning Model (`SmartCategorizerService`)**:
   - Built on **Apache OpenNLP Maximum Entropy (MaxEnt)** document categorization (`DocumentCategorizerME`).
   - Trained on financial transaction datasets via `ExpenseModelTrainer.java` (`100 iterations`, `cutoff = 0`).
   - Automatically loads pre-trained model binary (`expense-model.bin`) from application resources or local filesystem.
2. **Deterministic Fallback (`ExpenseClassifier`)**:
   - Keyword dictionary mapping common merchants and services when ML model confidence requires reinforcement:
     - **Supermercati e Alimentari**: *Carrefour, Lidl, Conad, Sole 365, Green Garden, Market*
     - **Ristorazione e Bar**: *McDonald's, Burger King, KFC, Cafes, Pizzerias*
     - **Abbonamenti e Servizi Digitali**: *Netflix, Spotify, Amazon Prime, Disney+, Google One*
     - **Trasporti**: *Uber, FREE NOW, Trenitalia, Taxi, Flights*
     - **Carburante e Auto**: *Gas, Fuel, Petrol stations*
     - **Alloggi e Viaggi**: *Airbnb, Booking.com, Hotels*
     - **Shopping e Abbigliamento**: *Zalando, Vinted, Douglas*

---

## 🛠 Tech Stack & Libraries

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 21 LTS | Utilizing modern language features and Virtual Threads (Project Loom) |
| **Framework** | Spring Boot 3.5.0 | Microservice runtime and dependency injection container |
| **Gateway** | Spring Cloud Gateway | Reactive reverse proxy built on Spring WebFlux & Reactor Netty |
| **REST APIs** | Jakarta RESTful Web Services / Jersey | JAX-RS implementation for service endpoint mapping |
| **Database** | MongoDB | Document database running on MongoDB Atlas / local replica |
| **Security** | Auth0 `java-jwt` & `jjwt` | HMAC SHA token creation, parsing, and claims verification |
| **Cryptography** | jBCrypt 0.4 | Salted one-way password hashing |
| **Machine Learning**| Apache OpenNLP 2.3.0 | Maximum Entropy natural language document categorization |
| **CSV Parsing** | OpenCSV 5.9 | Robust delimited file parser for financial statement imports |
| **Build System** | Maven 3.9+ | Multi-module build management with `license-maven-plugin` |
| **Testing** | JUnit 5, Mockito, JaCoCo | Comprehensive unit testing and test coverage analysis |
| **Load Testing** | k6 | Asynchronous stress and load benchmark suites |
| **Deployment** | Docker & Alpine Linux | Multi-stage lightweight OCI container image |

---

## ⚙️ Environment Configuration

Spendy utilizes a root `.env` file (loaded via `dotenv-java` across all modules) or native OS environment variables in production.

Create a `.env` file in the root directory:

```env
# Database Connection String
MONGODB_URI=mongodb+srv://<user>:<password>@cluster.mongodb.net/Spendy?retryWrites=true&w=majority

# JWT Authentication Config
JWT_SECRET=your_super_secret_base64_encoded_key_minimum_256_bits
JWT_EXPIRATION=900000

# Microservice Network Routing
AUTH_SERVICE_URI=http://localhost:8081
EXPENSE_SERVICE_URI=http://localhost:8084

# JVM Options (Optional)
JAVA_TOOL_OPTIONS=-Xms256m -Xmx1024m
```

| Variable | Description | Example |
| :--- | :--- | :--- |
| `MONGODB_URI` | MongoDB Atlas or local connection string | `mongodb://localhost:27017/Spendy` |
| `JWT_SECRET` | Secret key used to sign and verify HMAC tokens | `CW0I5xfJss...` |
| `JWT_EXPIRATION` | Access token lifespan in milliseconds | `900000` (15 minutes) |
| `AUTH_SERVICE_URI` | Destination URL for Auth Microservice | `http://localhost:8081` |
| `EXPENSE_SERVICE_URI`| Destination URL for Expense Microservice | `http://localhost:8084` |

---

## 🔌 REST API Reference

All requests originating from client applications must be sent through the **API Gateway**:
- **Production Base URL**: `https://khondor03-Spendy.hf.space`
- **Local Base URL**: `http://localhost:7860` (or `http://localhost:8080`)

### Authentication Endpoints

Base path via Gateway: `/Auth/rest/auth`

#### 1. User Registration
- **Endpoint**: `POST /Auth/rest/auth/register`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "username": "john_doe",
    "password": "SecurePassword123!",
    "name": "John",
    "surname": "Doe",
    "email": "john.doe@example.com"
  }
  ```
- **Responses**:
  - `200 OK`: `"Registration successful"`
  - `409 Conflict`: `"User already exists"`
  - `401 Unauthorized`: `"Email non valida"`

#### 2. User Login
- **Endpoint**: `POST /Auth/rest/auth/login`
- **Auth Required**: No
- **Request Body**: Accepts `username` (or `email`) and `password`:
  ```json
  {
    "username": "john_doe",
    "password": "SecurePassword123!"
  }
  ```
- **Response**:
  ```json
  {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "d8b3c9a1-..."
  }
  ```

#### 3. Refresh Access Token
- **Endpoint**: `POST /Auth/rest/auth/refresh`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "refreshToken": "d8b3c9a1-..."
  }
  ```
- **Response**:
  ```json
  {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "new_rotated_refresh_token_..."
  }
  ```

#### 4. Get User Profile
- **Endpoint**: `GET /Auth/rest/auth/profile`
- **Auth Required**: Yes (`Bearer <accessToken>`)
- **Response**:
  ```json
  {
    "username": "john_doe",
    "name": "John",
    "surname": "Doe",
    "email": "john.doe@example.com"
  }
  ```

#### 5. Update Profile
- **Endpoint**: `PUT /Auth/rest/auth/updateProfile`
- **Auth Required**: Yes (`Bearer <accessToken>`)
- **Request Body**: Updated `User` fields.

---

### Expense Endpoints

Base path via Gateway: `/Expense/rest/expense`  
*All expense endpoints require the `Authorization: Bearer <accessToken>` header.*

#### 1. Import Statement via CSV
- **Endpoint**: `POST /Expense/rest/expense/import`
- **Content-Type**: `multipart/form-data`
- **Form Data**: `file` (CSV file attachment)
- **Description**: Parses transactions, auto-detects column order and delimiter, infers category using OpenNLP, and persists expenses.

#### 2. List All Expenses
- **Endpoint**: `GET /Expense/rest/expense/getExpenses`
- **Response**:
  ```json
  [
    {
      "id": "664fa1...",
      "userId": "john_doe",
      "description": "Supermercato Conad",
      "amount": -45.50,
      "currency": "EUR",
      "category": "Supermercati e Alimentari",
      "startedDate": "2026-05-15T12:30:00Z",
      "completedDate": "2026-05-15T12:30:00Z"
    }
  ]
  ```

#### 3. Filter Expenses by Date Range
- **Endpoint**: `POST /Expense/rest/expense/getExpenseByDate`
- **Request Body**:
  ```json
  {
    "startedDate": "2026-05-01T00:00:00.000Z",
    "completedDate": "2026-05-31T23:59:59.000Z"
  }
  ```

#### 4. Filter Expenses by Month and Year
- **Endpoint**: `POST /Expense/rest/expense/getExpenseByMonth`
- **Request Body**:
  ```json
  {
    "month": "05",
    "year": "2026"
  }
  ```

#### 5. Monthly Aggregation of Year
- **Endpoint**: `POST /Expense/rest/expense/getMonthlyAmountOfYear`
- **Request Body**:
  ```json
  {
    "year": "2026"
  }
  ```
- **Description**: Computes total expenditure summed month-by-month for financial charts.

#### 6. Add Single Expense
- **Endpoint**: `POST /Expense/rest/expense/addExpense`
- **Request Body**:
  ```json
  {
    "description": "Pranzo Trattoria",
    "amount": "-24.00",
    "currency": "EUR",
    "type": "CARD_PAYMENT",
    "product": "Current",
    "startedDate": "2026-05-20T13:00:00.000Z",
    "completedDate": "2026-05-20T13:00:00.000Z"
  }
  ```
- **Response**: `201 Created`

#### 7. Update Expense
- **Endpoint**: `POST /Expense/rest/expense/updateExpense`
- **Request Body**: Expense payload containing target `expenseId` and modified fields.

#### 8. Delete Single Expense
- **Endpoint**: `DELETE /Expense/rest/expense/deleteExpense`
- **Request Body**:
  ```json
  {
    "expenseId": "664fa1..."
  }
  ```

#### 9. Delete All Expenses
- **Endpoint**: `DELETE /Expense/rest/expense/deleteAllExpenses`
- **Description**: Safely cleans all transaction history belonging to the authenticated user.

---

## 🚀 Getting Started & Local Setup

### Prerequisites
- **JDK 21** installed and configured (`java -version`).
- **Maven 3.9+** installed (`mvn -version`).
- A running **MongoDB** instance or connection string to MongoDB Atlas.

### 1. Clone the Repository
```bash
git clone https://github.com/Giusk10/SpendyApp.git
cd SpendyApp
```

### 2. Configure Environment
Copy or create `.env` in the project root:
```bash
cp .env.example .env # or edit .env directly
```

### 3. Build Entire Multi-Module Reactor
Run Maven from the root project folder:
```bash
mvn clean install -DskipTests
```
This builds `spendy` (parent POM), `AuthMicroService`, `Gateway`, and `ExpenseMicroService`, and aggregates third-party licenses into `THIRD-PARTY-ALL.txt`.

### 4. Running the Services Locally

You can launch each service in separate terminal windows:

#### Start AuthMicroService (Port 8081)
```bash
cd AuthMicroService
mvn spring-boot:run
```

#### Start ExpenseMicroService (Port 8084)
```bash
cd ExpenseMicroService
mvn spring-boot:run
```

#### Start Gateway (Port 7860 / 8080)
```bash
cd Gateway
mvn spring-boot:run
```

Verify the gateway health by opening:
```bash
curl http://localhost:7860/
# Returns: 🟢 Spendy Gateway è attivo e funzionante! (V. Finale)
```

---

## 🐳 Docker & Containerized Deployment

Spendy includes a multi-stage `Dockerfile` and an orchestration `entrypoint.sh` designed for deployment on container runtimes (such as **Hugging Face Spaces** or **Docker Compose**).

### Build Docker Image
```bash
docker build -t spendy-backend:latest .
```

### Run Container
```bash
docker run -d \
  --name spendy-app \
  -p 7860:7860 \
  --env-file .env \
  spendy-backend:latest
```

### Container Architecture Highlights
- **Multi-Stage Build**: Compiles all three services using `maven:3.9.6-eclipse-temurin-21-alpine` and exports lightweight runtime artifacts into an `eclipse-temurin:21-jdk-alpine` runner.
- **Rootless Execution**: Enforces non-root security (`UID 1000: user`) compliant with strict container sandbox environments.
- **JVM Optimization**: Uses `-XX:+AlwaysPreTouch` and `-XX:+UseG1GC` to prevent memory thrashing under bursty load.
- **Startup Orchestration**: `entrypoint.sh` starts `auth-service` and `expense-service` in the background, waits for backend port readiness, and then launches `gateway-service` in the foreground with HTTP access logging enabled.

---

## 🧪 Testing & Quality Assurance

### Unit & Integration Tests
Run unit tests across all microservices with coverage reporting via **JaCoCo**:
```bash
mvn test
```
JaCoCo reports are generated under:
`*/target/site/jacoco/index.html`

### Performance & Load Testing (k6)
Stress test scripts are available in the `performance-tests/` directory:

1. **Authentication Stress Test** (`load-multiple-login-test.js`):
   Simulates 50 concurrent users performing logins through the gateway with a p95 response time threshold of < 2600ms.
   ```bash
   k6 run performance-tests/load-multiple-login-test.js
   ```

2. **Full Pipeline Add Expense Test** (`load-multiple-add-test.js`):
   Simulates concurrent user logins, Bearer token extraction, and synchronous transaction creation via `/Expense/rest/expense/addExpense` with a threshold of < 2000ms.
   ```bash
   k6 run performance-tests/load-multiple-add-test.js
   ```

---

## 📄 License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPLv3)**.  
See the [LICENSE](LICENSE) file for full license terms.

Third-party dependencies and notices are aggregated automatically during the build into `THIRD-PARTY-ALL.txt`.
