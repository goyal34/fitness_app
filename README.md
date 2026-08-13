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
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta
GEMINI_API_KEY=YOUR_API_KEY
```

**Never commit your actual API key to GitHub.**

The AI Service reads these values through:

```yaml
gemini:
  api:
    url: ${GEMINI_API_URL}
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
