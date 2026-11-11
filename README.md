# 🏋️ AI-Powered Fitness Microservices

A full-stack fitness tracking application built using **Spring Boot Microservices, React, Spring Cloud, RabbitMQ, MongoDB, PostgreSQL, Keycloak, and Google Gemini API**.

The application allows authenticated users to track fitness activities and receive **AI-generated analysis, improvements, workout suggestions, and safety recommendations** based on their activity data.

---

## 🚀 Features

* 🔐 **OAuth2 / Keycloak Authentication** for secure user access
* 👤 **User Management** with PostgreSQL
* 🏃 **Fitness Activity Tracking** with MongoDB
* 🤖 **AI-Powered Fitness Recommendations** using Google Gemini API
* 📨 **Asynchronous Processing** using RabbitMQ
* 🔎 **Service Discovery** using Netflix Eureka
* ⚙️ **Centralized Configuration** using Spring Cloud Config Server
* 🌐 **API Gateway** for routing and security
* ⚛️ **React Frontend** with Material UI
* 🔄 **REST APIs** for users, activities, and recommendations

---

## 🏗️ Architecture

```text
                    ┌─────────────────────┐
                    │   React Frontend    │
                    │     Port: 5173      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    API Gateway      │
                    │     Port: 8080      │
                    │  OAuth2 / Keycloak  │
                    └──────────┬──────────┘
                               │
                  ┌────────────┼─────────────┐
                  │            │             │
                  ▼            ▼             ▼
          ┌────────────┐ ┌────────────┐ ┌────────────┐
          │   User     │ │  Activity  │ │     AI     │
          │  Service   │ │  Service   │ │  Service   │
          │   :8081    │ │   :8082    │ │   :8083    │
          └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
                │               │              │
                ▼               ▼              ▼
           PostgreSQL       MongoDB        MongoDB
                                │
                                ▼
                          ┌────────────┐
                          │ RabbitMQ   │
                          │   :5672    │
                          └─────┬──────┘
                                │
                                ▼
                         Gemini AI API

       ┌──────────────────────────────────────────┐
       │          Eureka Server :8761             │
       │            Service Discovery             │
       └──────────────────────────────────────────┘

       ┌──────────────────────────────────────────┐
       │        Config Server :8888               │
       │       Centralized Configuration          │
       └──────────────────────────────────────────┘
```

---

## 🧩 Microservices

| Service              |   Port | Responsibility                           |
| -------------------- | -----: | ---------------------------------------- |
| **Config Server**    | `8888` | Centralized configuration                |
| **Eureka Server**    | `8761` | Service discovery                        |
| **API Gateway**      | `8080` | API routing, authentication and security |
| **User Service**     | `8081` | User registration and profiles           |
| **Activity Service** | `8082` | Fitness activity tracking                |
| **AI Service**       | `8083` | AI analysis and recommendations          |
| **React Frontend**   | `5173` | User interface                           |

---

## 🛠️ Tech Stack

### Backend

* Java 17
* Spring Boot 3.4.3
* Spring Cloud 2024.0.0
* Spring Cloud Config
* Spring Cloud Netflix Eureka
* Spring Cloud Gateway
* Spring Security
* OAuth2 / Keycloak
* Spring Data JPA
* Spring Data MongoDB
* Spring AMQP
* Lombok

### Databases & Messaging

* PostgreSQL
* MongoDB
* RabbitMQ

### AI

* Google Gemini API

### Frontend

* React 19
* Vite
* Material UI
* Redux Toolkit
* RTK Query
* OAuth2 PKCE

---

## 📁 Project Structure

```text
fitness-app-microservices/
│
├── pom.xml                     # aggregator: builds every module with one command
│
├── requirements.txt            # prerequisites, ports, env vars, Keycloak setup
│
├── start-all.ps1               # starts infrastructure and every service
├── stop-all.ps1                # stops everything again
│
├── fitness-common/             # shared Spring config + DTO/event contracts
│   ├── src/main/java/com/fitness/common/
│   └── pom.xml
│
├── activityservice/
│   ├── src/
│   └── pom.xml
│
├── aiservice/
│   ├── src/
│   └── pom.xml
│
├── configserver/
│   ├── src/
│   │   └── main/resources/config/
│   └── pom.xml
│
├── eureka/
│   ├── src/
│   └── pom.xml
│
├── gateway/
│   ├── src/
│   └── pom.xml
│
├── userservice/
│   ├── src/
│   └── pom.xml
│
└── fitness-app-frontend/
    ├── src/
    ├── package.json
    └── vite.config.js
```

### Shared module (`fitness-common`)

Code that used to be copy-pasted across services lives here instead of in a
third-party library:

| Shared item | Package | Used by |
| --- | --- | --- |
| Resource-server `SecurityFilterChain` | `com.fitness.common.config` | User, Activity, AI |
| RabbitMQ queue/exchange/binding + JSON converter | `com.fitness.common.config` | Activity, AI |
| `UserResponse` | `com.fitness.common.dto` | User Service, Gateway |
| `ActivityEvent` (RabbitMQ message) | `com.fitness.common.event` | Activity Service, AI Service |

The configuration classes are registered as Spring Boot auto-configurations
(`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`),
so a service gets them just by depending on the module. They apply only when
the matching starter and properties are present, and a service can still
override any bean.

Because the services resolve `fitness-common` from the local Maven repository,
install it before running a service on its own:

```bash
mvn install -DskipTests          # from the repository root: builds all modules
# or only the shared module:
mvn -f fitness-common/pom.xml install -DskipTests
```

`start-all.ps1` does this automatically.

---

# ⚙️ Setup & Installation

Everything the application needs to run — tool versions, ports,
infrastructure services, environment variables, the Gemini key and the
Keycloak realm — is listed in [`requirements.txt`](requirements.txt). Install
and configure those first.

## Quick checklist

* Java 17+, Maven 3.9+, Node.js 18+ and npm
* Docker Desktop (recommended) **or** local PostgreSQL, MongoDB, RabbitMQ and Keycloak
* `POSTGRES_PASSWORD` set for the User Service (no default)
* `GEMINI_URL` and `GEMINI_API_KEY` set for the AI Service
* Keycloak realm `fitness-oauth2` with the `oauth2-pkce-client` client

---

## 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/fitness_app.git

cd fitness_app
```

---

# ▶️ Running the Application

## ⚡ Quick Start (single command)

If Docker Desktop, Java 17+, Maven 3.9+ and Node.js 18+ are installed, start the
whole application from the repository root with one command:

```powershell
.\start-all.ps1
```

or double-click `start-all.cmd`.

The command:

1. starts **PostgreSQL, MongoDB, RabbitMQ and Keycloak** with Docker Compose
   (reusing any of them that are already listening on their ports),
2. creates the `fitness-oauth2` realm, `oauth2-pkce-client` and the
   `testuser` / `test1234` demo account in Keycloak, and turns on self sign-up
   (anyone can click **Create account** on the welcome screen; passwords need
   at least 8 characters),
3. starts the **Config Server → Eureka → User/Activity/AI Services → Gateway**
   in the required order,
4. starts the **React frontend**, and
5. opens <http://localhost:5173>.

Logs are written to `.logs/`. Stop everything with:

```powershell
.\stop-all.ps1
```

Use `.\stop-all.ps1 -KeepInfra` to stop the Spring Boot/frontend processes but
leave the Docker containers running.

Optional `.env` configuration:

```powershell
Copy-Item .env.example .env
```

Then edit `.env` to set your Gemini key or a different PostgreSQL password.
Values already exported in your shell take precedence. The launcher flags are:

| Flag | Meaning |
| ---- | ------- |
| `-SkipInfra` | Do not start Docker containers; use already-running infrastructure. |
| `-SkipFrontend` | Start only the backend and infrastructure. |
| `-NoBrowser` | Do not open the browser at the end. |
| `-StartupTimeoutSeconds 600` | Allow more time for the first Maven build. |

> The first run can take several minutes because Maven and npm download
> dependencies, and Keycloak builds its database for the first time. Later runs
> are much faster. Re-running `start-all.ps1` is safe: services that are already
> listening on their ports are left alone.

The manual steps below remain available if you prefer to start each service
yourself or run them from your IDE.

Start the services in the following order.

> First install the shared module once, otherwise Maven cannot resolve
> `com.fitness:fitness-common` when building the services:
>
> ```bash
> mvn -f fitness-common/pom.xml install -DskipTests
> ```
>
> (or `mvn install -DskipTests` from the repository root to build everything).

### 1. Config Server

```bash
cd configserver
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8888
```

### 2. Eureka Server

```bash
cd eureka
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8761
```

Eureka Dashboard:

```text
http://localhost:8761
```

### 3. User Service

```bash
cd userservice
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8081
```

> If PostgreSQL is running in a container, the JVM may send a timezone ID the container's tzdata
> does not recognise, and the service will fail to start. Pass an explicit zone in that case:
>
> ```bash
> mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Kolkata"
> ```
>
> See [Troubleshooting](#-troubleshooting).

### 4. Activity Service

```bash
cd activityservice
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8082
```

### 5. AI Service

```bash
cd aiservice
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8083
```

### 6. API Gateway

```bash
cd gateway
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8080
```

### 7. React Frontend

```bash
cd fitness-app-frontend

npm install

npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

# 🔌 API Endpoints

All frontend requests are routed through the API Gateway.

Every request needs a Keycloak access token:

```text
Authorization: Bearer <access-token>
```

Each service checks the token itself and identifies the caller from its `sub`
claim, so you can only see and change your own data. Requests without a valid
token get `401`, including requests that skip the gateway and call a service
port directly. Another user's activity or recommendation answers `404`.

## User Service

### Get My Profile

```http
GET /api/users/me
```

### Get User Profile (own profile only)

```http
GET /api/users/{userId}
```

### Internal endpoints

`POST /api/users/register` and `GET /api/users/{userId}/validate` are used by the
gateway and the Activity Service only. The gateway blocks them for browsers.
The profile is created automatically from the token on your first request.

---

## Activity Service

### Track Activity

```http
POST /api/activities
```

Example request:

```json
{
  "type": "RUNNING",
  "duration": 30,
  "caloriesBurned": 300
}
```

### Get My Activities

```http
GET /api/activities
```

### Get Activity

```http
GET /api/activities/{activityId}
```

### Delete Activity

```http
DELETE /api/activities/{activityId}
```

---

### Workout Analytics

Computed by MongoDB aggregation pipelines in the Activity Service
(`$group`, `$dateTrunc`, `$facet`), always for the logged-in user. Weeks start
on Monday; days follow the server's time zone (the one workout times are stored in).

| Endpoint | Returns |
|---|---|
| `GET /api/activities/analytics/dashboard` | Everything below in one response (used by the Analytics page) |
| `GET /api/activities/analytics/weekly-duration?weeks=12` | Minutes, workouts and calories per week (empty weeks included) |
| `GET /api/activities/analytics/monthly-calories?months=12` | Calories, workouts and minutes per month |
| `GET /api/activities/analytics/frequency?days=90` | Workouts per type (with share) and per weekday, workouts per week |
| `GET /api/activities/analytics/intensity?weeks=12` | Average kcal/min (weighted by duration) per week and overall, workouts per intensity level |
| `GET /api/activities/analytics/personal-bests` | Longest workout, most calories, highest intensity, best week and month, bests per type |
| `GET /api/activities/analytics/streaks` | Current and longest daily streak, weekly streak, active days, last 12 weeks by day |
| `GET /api/activities/analytics/month-comparison?month=2026-11` | A month vs the month before; the current month is compared month-to-date (1st to today vs the same days last month) |

Intensity levels (kcal/min): Low < 5, Moderate 5–8, High 8–11, Very high 11+.

Tests: the analytics are checked against a real MongoDB
with `AnalyticsIntegrationTest` (each figure compared with the same value
computed in plain Java) and `StreakCalculatorTest`.

---

## AI Recommendation Service

### Get My Recommendations

```http
GET /api/recommendations/user/{userId}
```

`{userId}` must be your own Keycloak user id.

### Get Activity Recommendation

```http
GET /api/recommendations/activity/{activityId}
```

---

# 🤖 AI Recommendation Flow

The AI recommendation pipeline works asynchronously and uses the
**Transactional Outbox pattern**, so a saved workout always reaches the AI
Service, even if RabbitMQ is down at the time:

```text
User creates activity
        │
        ▼
Activity Service ── one MongoDB transaction ──┐
        │                                     │
        ├── save activity      → activities    │
        └── save pending event → outbox_events ┘
        │
        ▼
Outbox Publisher (every 2 s)
        │   claims pending events, sends them, waits for RabbitMQ's
        │   publisher confirm, then marks them PUBLISHED
        │   (failures: retry with exponential backoff, FAILED after 10 tries)
        ▼
RabbitMQ  (activity.queue)
        │
        ▼
AI Service ── skips activities that already have a report (at-least-once delivery)
        │
        ▼
Google Gemini API (structured JSON output)
        │
        ▼
Report: score, intensity, analysis, strengths, prioritised improvements,
        next workouts, safety and recovery tips
        │
        ▼
MongoDB → Frontend
```

Why: saving to MongoDB and publishing to RabbitMQ are two separate systems.
Publishing directly after saving could lose the event (RabbitMQ down) or send
an event for an activity that was never saved. Writing both to MongoDB in one
transaction removes that gap.

MongoDB transactions need a replica set, so `docker-compose.yml` runs MongoDB
as a single-node replica set (`rs0`), and `start-all.ps1` initialises it.
Published events are deleted automatically after 7 days.

The outbox is covered by an integration test that runs real MongoDB and
RabbitMQ in Docker (Testcontainers):

```bash
cd activityservice
./mvnw test -Dtest=OutboxIntegrationTest
```

---

# 🔐 Security

The API Gateway uses Spring Security and OAuth2 Resource Server with JWT validation.

The frontend uses OAuth2 PKCE for authentication.

The gateway also synchronizes authenticated Keycloak users with the User Service.

> **Security Note:** Never commit API keys, database passwords, OAuth credentials, or other secrets to the repository. Use environment variables or a secure secrets manager.

---

# 🧪 Testing

You can test the system using:

* Postman
* Browser
* React frontend

First verify that all services are registered in Eureka:

```text
http://localhost:8761
```

Then test APIs through the Gateway:

```text
http://localhost:8080/api/...
```

To exercise the full chain from the command line, fetch a token and call the Gateway with it:

```powershell
$t = (Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8181/realms/fitness-oauth2/protocol/openid-connect/token" `
  -Body @{client_id='oauth2-pkce-client';username='testuser';password='test1234';
          grant_type='password';scope='openid profile email'}).access_token

Invoke-RestMethod -Uri "http://localhost:8080/api/activities" -Headers @{Authorization="Bearer $t"}
```

The same request without the `Authorization` header should return **401**.

---

# 🩺 Troubleshooting

**User Service fails with `invalid value for parameter "TimeZone": "Asia/Calcutta"`**

The JVM's default zone ID is not present in the PostgreSQL container's tzdata, so the JDBC
connection is rejected at startup. Start the service with an explicit zone:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Kolkata"
```

**AI Service starts but logs `GEMINI_API_KEY is not set`**

The AI Service only needs the Gemini key when it processes an activity. Without
`GEMINI_API_KEY` it still starts and consumes RabbitMQ messages, but the Gemini
call fails and no recommendations are generated. Set the key in `.env` (or the
environment) and restart the service.

**No recommendations appear for new activities**

The AI Service logs the raw Gemini response at INFO, so check that terminal first. Two common
causes, both reported in the response body:

* `400` with `API_KEY_INVALID` — the key is wrong. Copy it again from AI Studio using the copy
  button rather than selecting the text.
* `404` with `This model models/... is no longer available` — the model in `GEMINI_URL` has been
  retired. The message names the replacement; update `GEMINI_URL` and restart the service.

Messages that fail are requeued by RabbitMQ, so once the cause is fixed and the service restarts,
the backlog is processed automatically and earlier activities get their recommendations.

**Port already in use on 5432, 27017, 8080 or 5173**

Another Docker stack with a restart policy may claim these ports when Docker Desktop starts.
Identify and stop the container holding the port:

```powershell
docker ps --format "{{.Names}}`t{{.Ports}}"
docker stop <container-name>
```

**Services start but do not appear in Eureka**

Each service reads its Eureka URL from the Config Server, so start `configserver` (8888) and
`eureka` (8761) first and let them come up before launching the rest.

**CORS errors from the frontend**

`SecurityConfig` in the gateway allows origin `http://localhost:5173` only. If you run the
frontend on a different port, update the allowed origins there as well as `redirectUri` in
`fitness-app-frontend/src/authConfig.js` and the client's redirect URIs in Keycloak.

---

# 📌 Key Highlights

* Microservices architecture with independent Spring Boot services
* Service discovery using Eureka
* Centralized configuration using Spring Cloud Config
* API Gateway-based routing and security
* OAuth2 / Keycloak authentication
* Asynchronous event processing with RabbitMQ
* Polyglot persistence using PostgreSQL and MongoDB
* Google Gemini integration for AI-powered recommendations
* React-based frontend with OAuth2 PKCE

---

# 🚧 Future Improvements

* Dockerize all services and infrastructure
* Add Kubernetes deployment
* Add centralized logging and monitoring
* Add automated CI/CD pipeline
* Improve automated test coverage
* Add API documentation with OpenAPI/Swagger
* Move secrets to a dedicated secrets-management solution

---

## 👨‍💻 Author

**Aman Goyal**

Built as a full-stack microservices project to explore distributed systems, Spring Cloud, AI integration, asynchronous messaging, authentication, and modern React development.
