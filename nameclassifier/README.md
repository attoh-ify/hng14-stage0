# 🔍 Name Classifier API

> A lightweight REST API that predicts the gender of a given name using the [Genderize.io](https://genderize.io) API, enriched with confidence scoring and structured responses.

[![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![HNG14](https://img.shields.io/badge/HNG14-Stage%200-FF6B35?style=flat-square)](https://hng.tech)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Response Examples](#-response-examples)
- [Error Handling](#-error-handling)
- [Deployment](#-deployment)

---

## 🌐 Overview

The **Name Classifier API** exposes a single `GET` endpoint that accepts a name as a query parameter, calls the external Genderize.io API, processes the raw response, and returns a clean, structured JSON payload — including a computed `is_confident` field and a live `processed_at` timestamp.

**Key behaviours:**
- Validates the `name` parameter (presence, type, format)
- Calls Genderize.io and maps `count` → `sample_size`
- Computes `is_confident`: `true` only when `probability ≥ 0.7` **AND** `sample_size ≥ 100`
- Generates `processed_at` dynamically in UTC ISO 8601 on every request
- Returns structured errors for all failure modes
- CORS-enabled (`Access-Control-Allow-Origin: *`)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| HTTP Client | Spring `RestClient` |
| Build Tool | Maven |
| External API | [Genderize.io](https://genderize.io) |

---

## 📁 Project Structure

```
nameclassifier/
├── src/
│   └── main/
│       ├── java/hng14/stage0/nameclassifier/
│       │   ├── client/
│       │   │   └── GenderizeClient.java          # Genderize.io HTTP client
│       │   ├── config/
│       │   │   ├── CorsConfig.java               # CORS configuration
│       │   │   └── RestClientConfig.java         # RestClient bean setup
│       │   ├── controller/
│       │   │   └── ClassifyController.java       # GET /api/classify endpoint
│       │   ├── dto/
│       │   │   ├── external/
│       │   │   │   └── GenderizeResponse.java    # Genderize API response model
│       │   │   └── response/
│       │   │       ├── ClassifyDataResponse.java # Success data payload
│       │   │       ├── SuccessResponse.java      # Success envelope
│       │   │       └── ErrorResponse.java        # Error envelope
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java   # Centralised error handling
│       │   │   ├── ApiException.java
│       │   │   ├── BadRequestException.java      # 400
│       │   │   ├── UnprocessableEntityException.java  # 422
│       │   │   ├── NoPredictionException.java    # No gender data
│       │   │   └── UpstreamServiceException.java # 502
│       │   ├── service/
│       │   │   ├── ClassifyService.java          # Service interface
│       │   │   └── impl/ClassifyServiceImpl.java # Business logic
│       │   └── NameclassifierApplication.java    # Entry point
│       └── resources/
│           └── application.properties
└── pom.xml
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — bundled via `mvnw` wrapper (no install needed)

### Clone & Run

```bash
# 1. Clone the repository
git clone https://github.com/attoh-ify/hng14-stage0.git
cd hng14-stage0/nameclassifier

# 2. Run with the Maven wrapper (builds & starts on port 8080)
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

The server starts at `http://localhost:8080`.

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | Port the server listens on |

To override the port:

```bash
PORT=3000 ./mvnw spring-boot:run
```

Or set it in your deployment platform's environment configuration.

---

## 📡 API Reference

### `GET /api/classify`

Classifies the gender of a given name.

#### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `name` | `string` | ✅ Yes | The name to classify (letters only) |

#### Request

```http
GET /api/classify?name=john
```

---

## 📦 Response Examples

### ✅ 200 — Success

```json
{
  "status": "success",
  "data": {
    "name": "john",
    "gender": "male",
    "probability": 0.99,
    "sample_size": 1234,
    "is_confident": true,
    "processed_at": "2026-04-10T10:30:00Z"
  }
}
```

**`is_confident` logic:**

```
is_confident = (probability >= 0.7) AND (sample_size >= 100)
```

Both conditions must be true. If either fails, `is_confident` is `false`.

---

## ⚠️ Error Handling

All error responses follow a consistent structure:

```json
{
  "status": "error",
  "message": "<human-readable message>"
}
```

| HTTP Status | Scenario | Message |
|---|---|---|
| `400 Bad Request` | `name` parameter is missing or empty | `Missing or empty name parameter` |
| `422 Unprocessable Entity` | `name` contains non-letter characters | `name must be a valid string` |
| `422 Unprocessable Entity` | No gender prediction available | `No prediction available for the provided name` |
| `500 Internal Server Error` | Unexpected server-side failure | Internal error message |
| `502 Bad Gateway` | Genderize.io is unreachable or returns an error | Upstream error message |

### Edge Case: No Prediction Available

When Genderize.io returns `gender: null` or `count: 0`:

```json
{
  "status": "error",
  "message": "No prediction available for the provided name"
}
```

---

## ☁️ Deployment

The API can be deployed on any platform that supports Java. Recommended options:

- **Railway** — `railway up`
- **Heroku** — `git push heroku main`
- **AWS Elastic Beanstalk** — deploy the JAR via EB CLI
- **Vercel** (via Java serverless adapter)

### Build the JAR

```bash
./mvnw clean package -DskipTests
java -jar target/nameclassifier-0.0.1-SNAPSHOT.jar
```

### Health Check

Once deployed, confirm the endpoint is live:

```bash
curl "https://your-deployed-url.app/api/classify?name=james"
```

Expected: a `200 OK` response with the JSON payload shown above.

---

## 🧪 Quick Test

```bash
# Valid name
curl "http://localhost:8080/api/classify?name=amara"

# Missing name — expect 400
curl "http://localhost:8080/api/classify"

# Invalid characters — expect 422
curl "http://localhost:8080/api/classify?name=john123"
```

---

## 📄 License

This project was built as part of the **HNG Internship 14 — Backend Track, Stage 0** assessment.
