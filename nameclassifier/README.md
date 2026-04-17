# 🧬 Name Profile API

> A RESTful API that enriches a given name with gender, age, and nationality predictions by integrating three external classification services, persisting the results, and exposing full CRUD endpoints with filtering support.

[![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![HNG14](https://img.shields.io/badge/HNG14-Stage%201-FF6B35?style=flat-square)](https://hng.tech)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [API Reference](#-api-reference)
- [Classification Rules](#-classification-rules)
- [Error Handling](#-error-handling)
- [Deployment](#-deployment)

---

## 🌐 Overview

The **Name Profile API** accepts a name, fans out to three free external APIs to enrich it with gender, age, and nationality data, applies classification logic, and stores the result in a PostgreSQL database. Duplicate submissions are detected by name (case-insensitive) and return the existing profile rather than creating a new record.

**Key behaviours:**
- Calls Genderize, Agify, and Nationalize on each new name
- Derives `age_group` (`child`, `teenager`, `adult`, `senior`) from the Agify age
- Selects the top-probability country from the Nationalize response as `country_id`
- Idempotent `POST` — the same name always returns the same stored profile
- Filterable `GET /api/profiles` with `gender`, `country_id`, and `age_group` query params (all case-insensitive)
- IDs are **UUID v7** (time-ordered epoch) for natural sort ordering
- All timestamps are **UTC ISO 8601**
- CORS-enabled (`Access-Control-Allow-Origin: *`)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| HTTP Client | Spring `RestClient` |
| UUID Generation | `uuid-creator` v5 (UUID v7) |
| Build Tool | Maven |
| External APIs | Genderize.io · Agify.io · Nationalize.io |

---

## 📁 Project Structure

```
nameclassifier/
├── src/
│   └── main/
│       ├── java/hng14/stage0/nameclassifier/
│       │   ├── aspect/
│       │   │   └── LoggingAspect.java              # Cross-cutting request/response logging
│       │   ├── client/
│       │   │   ├── AgifyClient.java                # HTTP client → api.agify.io
│       │   │   ├── GenderizeClient.java            # HTTP client → api.genderize.io
│       │   │   └── NationalizeClient.java          # HTTP client → api.nationalize.io
│       │   ├── config/
│       │   │   ├── CorsConfig.java                 # Global CORS: Access-Control-Allow-Origin: *
│       │   │   └── RestClientConfig.java           # RestClient bean configuration
│       │   ├── controller/
│       │   │   └── ProfileController.java          # POST · GET · GET all · DELETE
│       │   ├── dto/
│       │   │   ├── error/ErrorResponse.java
│       │   │   ├── external/
│       │   │   │   ├── AgifyResponse.java
│       │   │   │   ├── CountryPayload.java
│       │   │   │   ├── GenderizeResponse.java
│       │   │   │   └── NationalizeResponse.java
│       │   │   ├── payload/CreatePayload.java      # Request body: { "name": "..." }
│       │   │   ├── response/
│       │   │   │   ├── AgeGroup.java               # Enum: child | teenager | adult | senior
│       │   │   │   ├── CompactProfileDto.java      # Slim DTO for GET all list items
│       │   │   │   └── ProfileDto.java             # Full DTO for single-profile responses
│       │   │   └── success/
│       │   │       ├── CreateSuccessResponse.java
│       │   │       ├── GetAllSuccessResponse.java
│       │   │       └── GetSuccessResponse.java
│       │   ├── entities/
│       │   │   └── Profile.java                    # JPA entity → `profiles` table
│       │   ├── exception/
│       │   │   ├── ApiException.java
│       │   │   ├── BadRequestException.java        # 400
│       │   │   ├── GlobalExceptionHandler.java     # Centralised @RestControllerAdvice
│       │   │   ├── NotFoundException.java          # 404
│       │   │   ├── UnprocessableEntityException.java # 422
│       │   │   └── UpstreamServiceException.java   # 502
│       │   ├── mappers/
│       │   │   ├── ProfileMapper.java
│       │   │   └── impl/ProfileMapperImpl.java
│       │   ├── repositories/
│       │   │   └── ProfileRepository.java          # JPA repo with dynamic filter query
│       │   ├── service/
│       │   │   ├── ProfileService.java
│       │   │   └── impl/ProfileServiceImpl.java    # Core business logic
│       │   └── NameclassifierApplication.java
│       └── resources/
│           └── application.properties
└── pom.xml
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — bundled via `mvnw` wrapper (no install needed)
- **PostgreSQL** — a running instance with a database created

### Clone & Run

```bash
# 1. Clone the repository
git clone https://github.com/attoh-ify/hng14-stage0.git
cd hng14-stage1

# 2. Set required environment variables (see section below)
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=name_classifier
export PGUSER=postgres
export PGPASSWORD=secret

# 3. Run with the Maven wrapper
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

The server starts at `http://localhost:8080`.

> The schema is auto-managed by Hibernate (`ddl-auto=update`) — no migration scripts are needed.

---

## 🔧 Environment Variables

| Variable | Description | Example |
|---|---|---|
| `PORT` | Port the server listens on (default: `8080`) | `8080` |
| `PGHOST` | PostgreSQL host | `localhost` |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `name_classifier` |
| `PGUSER` | Database username | `postgres` |
| `PGPASSWORD` | Database password | `secret` |

---

## 📡 API Reference

### Base URL

```
https://hng14-stage0-production.up.railway.app
```

---

### 1. `POST /api/profiles` — Create Profile

Accepts a name, calls all three external APIs, and persists the enriched profile. If the name already exists (case-insensitive match), returns the stored record without calling any external API.

**Request Body**

```json
{ "name": "ella" }
```

**Response — 201 Created (new profile)**

```json
{
  "status": "success",
  "data": {
    "id": "b3f9c1e2-7d4a-4c91-9c2a-1f0a8e5b6d12",
    "name": "ella",
    "gender": "female",
    "gender_probability": 0.99,
    "sample_size": 1234,
    "age": 46,
    "age_group": "adult",
    "country_id": "DRC",
    "country_probability": 0.85,
    "created_at": "2026-04-01T12:00:00Z"
  }
}
```

**Response — 200 OK (duplicate name)**

```json
{
  "status": "success",
  "message": "Profile already exists",
  "data": { ...existing profile... }
}
```

---

### 2. `GET /api/profiles/{id}` — Get Single Profile

**Response — 200 OK**

```json
{
  "status": "success",
  "data": {
    "id": "b3f9c1e2-7d4a-4c91-9c2a-1f0a8e5b6d12",
    "name": "emmanuel",
    "gender": "male",
    "gender_probability": 0.99,
    "sample_size": 1234,
    "age": 25,
    "age_group": "adult",
    "country_id": "NG",
    "country_probability": 0.85,
    "created_at": "2026-04-01T12:00:00Z"
  }
}
```

---

### 3. `GET /api/profiles` — Get All Profiles

Returns all stored profiles. Supports optional, case-insensitive filtering via query parameters. Filters can be combined freely.

**Query Parameters**

| Parameter | Type | Notes |
|---|---|---|
| `gender` | string | `male` or `female` — case-insensitive |
| `country_id` | string | ISO country code, e.g. `NG`, `US` — case-insensitive |
| `age_group` | string | `child`, `teenager`, `adult`, or `senior` — case-insensitive |

**Example**

```http
GET /api/profiles?gender=male&country_id=NG
```

**Response — 200 OK**

```json
{
  "status": "success",
  "count": 2,
  "data": [
    {
      "id": "id-1",
      "name": "emmanuel",
      "gender": "male",
      "age": 25,
      "age_group": "adult",
      "country_id": "NG"
    },
    {
      "id": "id-2",
      "name": "david",
      "gender": "male",
      "age": 32,
      "age_group": "adult",
      "country_id": "NG"
    }
  ]
}
```

---

### 4. `DELETE /api/profiles/{id}` — Delete Profile

Returns **204 No Content** on success. Returns **404** if the profile does not exist.

---

## 📊 Classification Rules

### Age Group (from Agify)

| Age Range | Group |
|---|---|
| 0 – 12 | `child` |
| 13 – 19 | `teenager` |
| 20 – 59 | `adult` |
| 60+ | `senior` |

### Nationality (from Nationalize)

The country with the **highest probability** in the Nationalize response array is selected as `country_id`.

---

## ⚠️ Error Handling

All errors follow a consistent structure:

```json
{ "status": "error", "message": "<human-readable message>" }
```

| HTTP Status | Scenario | Message |
|---|---|---|
| `400 Bad Request` | `name` is missing or empty | `Missing or empty name` |
| `422 Unprocessable Entity` | `name` contains non-letter characters | `name is not a string` |
| `422 Unprocessable Entity` | Request body is malformed / wrong type | `Invalid type` |
| `422 Unprocessable Entity` | Invalid `age_group` filter value | `Invalid age_group` |
| `404 Not Found` | Profile ID does not exist | `Profile not found` |
| `502 Bad Gateway` | Genderize returns `null` gender or zero count | `Genderize returned an invalid response` |
| `502 Bad Gateway` | Agify returns `null` age | `Agify returned an invalid response` |
| `502 Bad Gateway` | Nationalize returns no country data | `Nationalize returned an invalid response` |
| `500 Internal Server Error` | Unexpected server-side failure | `Internal server error` |

> Profiles are **never stored** when any external API returns an invalid response.

---

## ☁️ Deployment

The API can be deployed to any platform that supports Java. Recommended options:

- **Railway** — connect your repo, set env vars, deploy
- **Heroku** — `git push heroku main`
- **AWS Elastic Beanstalk** — deploy the JAR via EB CLI

### Build the JAR

```bash
./mvnw clean package -DskipTests
java -jar target/nameclassifier-0.0.1-SNAPSHOT.jar
```

### Smoke Test

Once deployed, confirm all endpoints are live:

```bash
# Create a profile
curl -X POST https://hng14-stage0-production.up.railway.app/api/profiles \
  -H "Content-Type: application/json" \
  -d '{"name": "james"}'

# Submit the same name again — should return 200 with "Profile already exists"
curl -X POST https://hng14-stage0-production.up.railway.app/api/profiles \
  -H "Content-Type: application/json" \
  -d '{"name": "james"}'

# List all profiles
curl https://hng14-stage0-production.up.railway.app/api/profiles

# Filter by gender and country
curl "https://hng14-stage0-production.up.railway.app/api/profiles?gender=male&country_id=NG"

# Delete a profile
curl -X DELETE https://hng14-stage0-production.up.railway.app/api/profiles/<id>
```

---

## 📄 License

Built as part of the **HNG Internship 14 — Backend Track, Stage 1** assessment.