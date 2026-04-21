# 🔍 Insighta Labs — Profile Intelligence API

> A demographic intelligence API that stores 2,026 enriched name profiles and exposes advanced filtering, sorting, pagination, and plain-English natural language search — all backed by indexed SQL queries with no full-table scans.

[![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![HNG14](https://img.shields.io/badge/HNG14-Stage%202-FF6B35?style=flat-square)](https://hng.tech)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [API Reference](#-api-reference)
- [Natural Language Search](#-natural-language-search)
- [Error Handling](#-error-handling)
- [Deployment](#-deployment)

---

## 🌐 Overview

The **Profile Intelligence API** is an upgrade to the Stage 1 name-classifier system. It ships pre-seeded with 2,026 demographic profiles and adds four major capabilities on top of the existing CRUD:

- **Advanced filtering** across 7 dimensions (gender, age range, country, probability thresholds, age group)
- **Sorting** by age, created date, or gender probability in either direction
- **Pagination** with configurable page size (max 50) and stable total counts
- **Natural language search** that parses plain English queries into typed filter combinations using rule-based token matching — no AI, no LLMs

All filters are combinable. Queries are executed via JPA Specifications so only the predicates that are actually provided hit the database. IDs are UUID v7. All timestamps are UTC ISO 8601.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate + JPA Specifications |
| HTTP Client | Spring `RestClient` |
| UUID Generation | `uuid-creator` v5 (UUID v7) |
| Build Tool | Maven |
| External APIs | Genderize.io · Agify.io · Nationalize.io |

---

## 🗄 Database Schema

The `profiles` table follows the exact structure required by the assessment:

| Column | Type | Notes |
|---|---|---|
| `id` | `VARCHAR` (UUID v7) | Primary key |
| `name` | `VARCHAR` | Unique, lowercase-normalised |
| `gender` | `VARCHAR` | `"male"` or `"female"` |
| `gender_probability` | `FLOAT` | Confidence score from Genderize |
| `age` | `INT` | Exact predicted age |
| `age_group` | `VARCHAR` | `child`, `teenager`, `adult`, `senior` |
| `country_id` | `VARCHAR(2)` | ISO 3166-1 alpha-2 code (e.g. `NG`) |
| `country_name` | `VARCHAR` | Full country name (e.g. `Nigeria`) |
| `country_probability` | `FLOAT` | Confidence score from Nationalize |
| `created_at` | `TIMESTAMP` | Auto-set to UTC on insert |

**Age group classification:**

| Age Range | Group |
|---|---|
| 0 – 12 | `child` |
| 13 – 19 | `teenager` |
| 20 – 59 | `adult` |
| 60+ | `senior` |

The schema is auto-managed by Hibernate (`ddl-auto=update`). The seed runs at startup via `DataSeeder` — duplicate names are detected before insertion so re-running is safe.

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — bundled via `mvnw` wrapper
- **PostgreSQL** — running instance with a database created

### Clone & Run

```bash
# 1. Clone the repository
git clone https://github.com/attoh-ify/hng14-stage0.git
cd hng14-stage0

# 2. Set environment variables
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=name_classifier
export PGUSER=postgres
export PGPASSWORD=secret

# 3. Run — the seed loads automatically on first start
./mvnw spring-boot:run
```

The server starts at `http://localhost:8080`. The 2,026 profiles from `src/main/resources/data/seed_profiles.json` are loaded on startup. Re-running never creates duplicates.

---

## 🔧 Environment Variables

| Variable | Description | Example |
|---|---|---|
| `PORT` | Server port (default `8080`) | `8080` |
| `PGHOST` | PostgreSQL host | `localhost` |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `name_classifier` |
| `PGUSER` | Database username | `postgres` |
| `PGPASSWORD` | Database password | `secret` |

---

## 📡 API Reference

### Base URL

```
https://your-deployed-app.domain.app
```

---

### 1. `POST /api/profiles` — Create Profile

Calls Genderize, Agify, and Nationalize for a new name and stores the result. Returns the existing record if the name has already been classified.

**Request Body**

```json
{ "name": "ella" }
```

**Responses:** `201 Created` (new) · `200 OK` (duplicate)

---

### 2. `GET /api/profiles/{id}` — Get Single Profile

Returns `200` with the full profile, or `404` if not found.

---

### 3. `GET /api/profiles` — Get All Profiles (with filtering, sorting, pagination)

**Filters** — all optional, all combinable:

| Parameter | Type | Description |
|---|---|---|
| `gender` | string | `male` or `female` |
| `country_id` | string | ISO code, e.g. `NG` |
| `age_group` | string | `child` · `teenager` · `adult` · `senior` |
| `min_age` | integer | Inclusive lower bound on age |
| `max_age` | integer | Inclusive upper bound on age |
| `min_gender_probability` | float | 0.0 – 1.0 |
| `min_country_probability` | float | 0.0 – 1.0 |

**Sorting:**

| Parameter | Values | Default |
|---|---|---|
| `sort_by` | `age` · `created_at` · `gender_probability` | `created_at` |
| `order` | `asc` · `desc` | `asc` |

**Pagination:**

| Parameter | Default | Max |
|---|---|---|
| `page` | `1` | — |
| `limit` | `10` | `50` |

**Example**

```http
GET /api/profiles?gender=male&country_id=NG&min_age=25&sort_by=age&order=desc&page=1&limit=10
```

**Response — 200 OK**

```json
{
  "status": "success",
  "page": 1,
  "limit": 10,
  "total": 312,
  "data": [
    {
      "id": "b3f9c1e2-7d4a-4c91-9c2a-1f0a8e5b6d12",
      "name": "emmanuel",
      "gender": "male",
      "gender_probability": 0.99,
      "age": 34,
      "age_group": "adult",
      "country_id": "NG",
      "country_name": "Nigeria",
      "country_probability": 0.85,
      "created_at": "2026-04-01T12:00:00Z"
    }
  ]
}
```

**Validation rules:**
- `min_age` and `max_age` must be ≥ 0; `min_age` must not exceed `max_age`
- Probability filters must be between 0.0 and 1.0
- `page` must be ≥ 1; `limit` must be between 1 and 50
- Invalid `sort_by` or `order` values return `422`

---

### 4. `GET /api/profiles/search` — Natural Language Search

Parse a plain-English query into filter conditions and return matching profiles. Supports the same `page` and `limit` parameters as `GET /api/profiles`.

**Parameters**

| Parameter | Required | Description |
|---|---|---|
| `q` | ✅ | Plain-English query string |
| `page` | No | Default `1` |
| `limit` | No | Default `10`, max `50` |

**Example**

```http
GET /api/profiles/search?q=young males from nigeria&page=1&limit=10
```

See the [Natural Language Search](#-natural-language-search) section below for full keyword reference.

---

### 5. `DELETE /api/profiles/{id}` — Delete Profile

Returns `204 No Content` on success. Returns `404` if the profile does not exist.

---

## 🧠 Natural Language Search

### How it works

Queries are parsed by `Helpers.parse()` — a pure rule-based token scanner written in Java. The query string is lower-cased and checked against a fixed set of keyword rules in order. There is no AI, no LLM, no regex-heavy NLP library. Each rule extracts one filter dimension independently, so rules do not interfere with each other.

The parsed result is a `ParsedSearchQuery` record holding up to five optional fields: `gender`, `ageGroup`, `minAge`, `maxAge`, and `countryId`. This is passed directly into the same `getAll()` pipeline used by the standard filter endpoint, so all JPA Specification logic and pagination apply identically.

If no supported keyword produces at least one non-null filter, the parser throws `422 Unprocessable Entity` with the message `"Unable to interpret query"`.

---

### Supported keywords and their mappings

#### Gender

| Query contains | Resolves to |
|---|---|
| `male` or `males` (without `female`) | `gender = male` |
| `female` or `females` (without `male`) | `gender = female` |
| Both `male` and `female` | No gender filter (returns all genders) |

#### Age group

| Query contains | Resolves to |
|---|---|
| `child` or `children` | `age_group = child` |
| `teenager` or `teenagers` | `age_group = teenager` |
| `adult` or `adults` | `age_group = adult` |
| `senior` or `seniors` | `age_group = senior` |

#### Age bounds

| Query pattern | Resolves to |
|---|---|
| `above <N>` or `over <N>` | `min_age = N` |
| `below <N>` or `under <N>` | `max_age = N` |
| `young` | `min_age = 16`, `max_age = 24` |

> `young` is a parser-only keyword. It is not a stored age group and does not map to any `age_group` value.

#### Country

The parser scans the query for any known country name from a built-in map of ~250 countries and resolves it to its ISO 3166-1 alpha-2 code. Both `"from nigeria"` and just `"nigeria"` are matched.

**Example country mappings:**

| Query | Resolves to |
|---|---|
| `from nigeria` | `country_id = NG` |
| `people from kenya` | `country_id = KE` |
| `angola` | `country_id = AO` |
| `from the united states` | `country_id = US` |

---

### Example query mappings

| Query | Extracted filters |
|---|---|
| `young males` | `gender=male`, `min_age=16`, `max_age=24` |
| `females above 30` | `gender=female`, `min_age=30` |
| `people from angola` | `country_id=AO` |
| `adult males from kenya` | `gender=male`, `age_group=adult`, `country_id=KE` |
| `male and female teenagers above 17` | `age_group=teenager`, `min_age=17` |
| `seniors below 75` | `age_group=senior`, `max_age=75` |
| `young females from nigeria` | `gender=female`, `min_age=16`, `max_age=24`, `country_id=NG` |

---

### Limitations

The parser is deliberately simple. The following cases are **not handled**:

- **Age ranges expressed as spans** — `"between 25 and 40"` is not parsed; only `above N` and `below N` work.
- **ISO code input** — typing `"NG"` instead of `"Nigeria"` is not resolved (only full country names are matched).
- **Alternate country names** — `"UK"` and `"Britain"` are not mapped; the parser expects `"United Kingdom"`.
- **Relative terms other than `young`** — words like `"old"`, `"elderly"`, `"middle-aged"` produce no filter.
- **Negation** — `"not male"` or `"excluding seniors"` is not supported and will produce incorrect results.
- **Spelling variations and typos** — `"femal"`, `"nigera"`, `"teeneger"` do not match.
- **Multiple countries** — `"from nigeria or ghana"` will match the first country found; `"ghana"` may or may not be picked up depending on scan order.
- **Ordinal phrases** — `"people in their 30s"` produces no filter.
- **Combined `above`/`below` in the same query** — supported, but only when the numbers appear as a single token after the keyword (e.g. `"above 25 below 40"`).
- **Queries in languages other than English** — not supported.
- **Probability filters** — `min_gender_probability` and `min_country_probability` are not available via natural language; use the standard filter endpoint for those.

---

## ⚠️ Error Handling

All errors follow a consistent structure:

```json
{ "status": "error", "message": "<human-readable message>" }
```

| HTTP Status | Scenario | Message |
|---|---|---|
| `400 Bad Request` | `name` or `q` is missing or empty | `Missing or empty parameter` |
| `422 Unprocessable Entity` | Invalid `age_group`, `sort_by`, `order`, or probability range | `Invalid query parameters` |
| `422 Unprocessable Entity` | Query string produces no parseable filters | `Unable to interpret query` |
| `404 Not Found` | Profile ID does not exist | `Profile not found` |
| `502 Bad Gateway` | Any external API returns invalid data | `<ApiName> returned an invalid response` |
| `500 Internal Server Error` | Unexpected server failure | `Internal server error` |

---

## ☁️ Deployment

### Build the JAR

```bash
./mvnw clean package -DskipTests
java -jar target/nameclassifier-0.0.1-SNAPSHOT.jar
```

### Smoke Tests

```bash
# Paginated + filtered list
curl "https://hng14-stage0-production.up.railway.app/api/profiles?gender=female&country_id=NG&sort_by=age&order=asc&page=1&limit=5"

# Natural language search
curl "https://hng14-stage0-production.up.railway.app/api/profiles/search?q=young males from nigeria"

# Combined age range + gender
curl "https://hng14-stage0-production.up.railway.app/api/profiles?gender=male&min_age=25&max_age=40&limit=10"

# Probability threshold
curl "https://hng14-stage0-production.up.railway.app/api/profiles?min_gender_probability=0.9&sort_by=gender_probability&order=desc"

# NL search with age bound
curl "https://hng14-stage0-production.up.railway.app/api/profiles/search?q=female seniors above 65"
```

---

## 📄 License

Built as part of the **HNG Internship 14 — Backend Track, Stage 2** assessment.