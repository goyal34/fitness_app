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
* Axios
* OAuth2 PKCE

---

## 📁 Project Structure

```text
fitness-app-microservices/
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

---

# ⚙️ Setup & Installation

## 1. Prerequisites

Install the following:

* Java 17+
* Maven 3.9+
* Node.js
* npm
* PostgreSQL
* MongoDB
* RabbitMQ
* Keycloak
* Google Gemini API key

---

## 2. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/fitness_app.git

cd fitness_app
```

---

## 3. Start PostgreSQL

Create the database used by the User Service:

```sql
CREATE DATABASE fitness_user_db;
```

Update the PostgreSQL username/password in your local configuration if required.

---

## 4. Start MongoDB

MongoDB is used by:

* Activity Service
* AI Service

The application expects MongoDB on:

```text
mongodb://localhost:27017
```

The databases used are:

```text
fitnessactivity
fitnessrecommendation
```

---

## 5. Start RabbitMQ

RabbitMQ is used for asynchronous communication between the Activity and AI services.

Default configuration:

```text
Host: localhost
Port: 5672
Username: guest
Password: guest
```

The application uses:

```text
Exchange: fitness.exchange
Queue: activity.queue
Routing Key: activity.tracking
```

When an activity is created, Activity Service publishes the activity to RabbitMQ. AI Service consumes the message and generates the recommendation.

---

## 6. Configure Gemini API

Create a Gemini API key using Google AI Studio.

Set these environment variables:

```text
GEMINI_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=
GEMINI_API_KEY=YOUR_API_KEY
```

`GeminiService` builds the request URI by concatenating the two values directly:

```java
.uri(geminiApiUrl + geminiApiKey)
```

so `GEMINI_URL` must be the full model endpoint and must end with the trailing `?key=`.

**Google retires older models periodically.** If the AI Service logs a `404` like
`This model models/... is no longer available`, the response names the current replacement — put
that model into `GEMINI_URL`. You can check a key and model in one call before starting the
service:

```powershell
$k = "YOUR_API_KEY"
$u = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$k"
Invoke-RestMethod -Method Post -Uri $u -ContentType "application/json" `
  -Body '{"contents":[{"parts":[{"text":"Say hello"}]}]}'
```

A `candidates` array means the key and model are both good. A `400` with `API_KEY_INVALID` means
the key is wrong; a `404` means the model name needs updating.

**Never commit your actual API key to GitHub.**

The AI Service reads these values through:

```yaml
gemini:
  api:
    url: ${GEMINI_URL}
    key: ${GEMINI_API_KEY}
```

---

## 7. Configure Keycloak

The application uses Keycloak for OAuth2 authentication.

The current configuration expects Keycloak at:

```text
http://localhost:8181
```

Realm:

```text
fitness-oauth2
```

The frontend uses OAuth2 PKCE with:

```text
Client ID: oauth2-pkce-client
```

Make sure the Keycloak realm, client, redirect URI, and user configuration match the application's authentication settings.

### Bootstrapping the realm

If you are starting from an empty Keycloak, run it and create the realm, the PKCE client and a
test user. These values must match `fitness-app-frontend/src/authConfig.js` and the
`jwk-set-uri` in `api-gateway.yml`:

```powershell
docker run -d --name fitness-keycloak -p 8181:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin `
  quay.io/keycloak/keycloak:26.0 start-dev
```

Wait ~30 seconds for Keycloak to boot, then:

```powershell
$KC = "http://localhost:8181"
$tok = (Invoke-RestMethod -Method Post -Uri "$KC/realms/master/protocol/openid-connect/token" `
  -Body @{client_id='admin-cli';username='admin';password='admin';grant_type='password'}).access_token
$H = @{ Authorization = "Bearer $tok"; 'Content-Type' = 'application/json' }

Invoke-RestMethod -Method Post -Uri "$KC/admin/realms" -Headers $H `
  -Body '{"realm":"fitness-oauth2","enabled":true}'

Invoke-RestMethod -Method Post -Uri "$KC/admin/realms/fitness-oauth2/clients" -Headers $H -Body @'
{"clientId":"oauth2-pkce-client","enabled":true,"publicClient":true,"standardFlowEnabled":true,
 "directAccessGrantsEnabled":true,"redirectUris":["http://localhost:5173/*","http://localhost:5173"],
 "webOrigins":["http://localhost:5173","+"],"attributes":{"pkce.code.challenge.method":"S256"}}
'@

Invoke-RestMethod -Method Post -Uri "$KC/admin/realms/fitness-oauth2/users" -Headers $H -Body @'
{"username":"testuser","email":"testuser@fitness.local","emailVerified":true,"firstName":"Test",
 "lastName":"User","enabled":true,"credentials":[{"type":"password","value":"test123","temporary":false}]}
'@
```

The gateway's `KeycloakUserSyncFilter` reads the `sub`, `email`, `given_name` and `family_name`
claims to auto-register users, so the test user needs an email, first name and last name set.

---

# ▶️ Running the Application

Start the services in the following order:

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

## User Service

### Register User

```http
POST /api/users/register
```

### Get User Profile

```http
GET /api/users/{userId}
```

### Validate User

```http
GET /api/users/{userId}/validate
```

---

## Activity Service

### Track Activity

```http
POST /api/activities
```

Required header:

```text
X-User-ID: <user-id>
```

Example request:

```json
{
  "type": "RUNNING",
  "duration": 30,
  "caloriesBurned": 300
}
```

### Get User Activities

```http
GET /api/activities
```

Required header:

```text
X-User-ID: <user-id>
```

### Get Activity

```http
GET /api/activities/{activityId}
```

---

## AI Recommendation Service

### Get User Recommendations

```http
GET /api/recommendations/user/{userId}
```

### Get Activity Recommendation

```http
GET /api/recommendations/activity/{activityId}
```

---

# 🤖 AI Recommendation Flow

The AI recommendation pipeline works asynchronously:

```text
User creates activity
        │
        ▼
Activity Service
        │
        ├── Save activity → MongoDB
        │
        ▼
RabbitMQ
        │
        ▼
AI Service
        │
        ▼
Google Gemini API
        │
        ▼
AI Analysis
        │
        ├── Performance analysis
        ├── Improvements
        ├── Workout suggestions
        └── Safety guidelines
        │
        ▼
MongoDB
        │
        ▼
Frontend
```

The AI service structures Gemini's response into recommendation data containing analysis, improvements, suggestions, and safety guidelines.

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
  -Body @{client_id='oauth2-pkce-client';username='testuser';password='test123';
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

**AI Service fails at startup with an unresolved placeholder**

`GeminiService` injects `${gemini.api.key}` with `@Value`, so `GEMINI_API_KEY` must be set in the
environment before launch or the context will not start. Without a valid key the service still
starts and consumes RabbitMQ messages, but the Gemini call fails and no recommendations are
generated.

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
