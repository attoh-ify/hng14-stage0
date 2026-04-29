# Insighta Labs+ — Backend API

Secure REST API for the Insighta Labs+ profile intelligence platform. Built with Spring Boot, MySQL, and JWT-based authentication with GitHub OAuth (PKCE).

---

## Tech Stack

| Layer | Choice |
|---|---|
| Framework | Spring Boot 3 |
| Database | MySQL 8 + Spring Data JPA |
| Auth | GitHub OAuth 2.0 with PKCE |
| Tokens | JWT (access) + UUID refresh tokens |
| Rate Limiting | In-memory per-user bucket |
| Build | Maven |

---

## System Architecture

```
Client (CLI / Web Portal / Direct API)
        │
        ▼
  Spring Boot API (:8080)
        │
        ├── /auth/**         → Public OAuth endpoints
        ├── /api/profiles/** → Protected (JWT required)
        │        ├── GET    → admin + analyst
        │        ├── POST   → admin only
        │        └── DELETE → admin only
        │
        ├── JwtAuthenticationFilter  (validates Bearer token or cookie)
        ├── ApiVersionFilter         (enforces X-API-Version: 1)
        ├── RateLimitFilter          (10 req/min auth, 60 req/min others)
        └── RequestLoggingFilter     (method, path, status, duration)
```

---

## Authentication Flow

### Web Portal (Browser)
1. Portal redirects browser to `GET /auth/github?client=web`
2. Backend generates PKCE pair, stores in httpOnly cookies, redirects to GitHub
3. GitHub redirects to `GET /auth/github/callback` with code + state
4. Backend validates state cookie + PKCE verifier, exchanges code with GitHub
5. Backend redirects browser to `PORTAL_CALLBACK_URL?access_token=...&refresh_token=...&username=...`
6. Next.js sets httpOnly cookies and redirects user to dashboard

### CLI
1. CLI generates PKCE pair + state locally, starts local HTTP server on port 8976
2. CLI opens browser to `GET /auth/github?client=cli&redirect_uri=http://localhost:8976/callback&...`
3. GitHub redirects to `http://localhost:8976/callback` with code
4. CLI POSTs code + code_verifier to `POST /auth/github/cli/callback`
5. Backend returns `{ access_token, refresh_token, username }`
6. CLI stores tokens in `~/.insighta/credentials.json`

### Token Lifecycle
- **Access token**: JWT, expires in 3 minutes
- **Refresh token**: UUID stored in DB, expires in 5 minutes
- `POST /auth/refresh` invalidates the old refresh token and issues a new pair
- `POST /auth/logout` revokes the refresh token server-side

---

## Role Enforcement

| Role | Permissions |
|---|---|
| `admin` | GET, POST, DELETE on `/api/**` |
| `analyst` | GET only on `/api/**` |

Roles are enforced in `SecurityConfig` using Spring Security's `hasRole()` checks. Users with `is_active = false` receive `403 Forbidden` on all requests.

---

## API Endpoints

### Auth
| Method | Path | Description |
|---|---|---|
| GET | `/auth/github` | Redirect to GitHub OAuth |
| GET | `/auth/github/callback` | Web OAuth callback → redirects to portal |
| POST | `/auth/github/cli/callback` | CLI OAuth callback → returns JSON tokens |
| POST | `/auth/refresh` | Refresh token pair |
| POST | `/auth/logout` | Invalidate refresh token |
| GET | `/auth/me` | Get current user info |

### Profiles (all require `X-API-Version: 1` header + JWT)
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/profiles` | admin, analyst | List profiles (filters + pagination) |
| POST | `/api/profiles` | admin | Create profile |
| GET | `/api/profiles/:id` | admin, analyst | Get single profile |
| DELETE | `/api/profiles/:id` | admin | Delete profile |
| GET | `/api/profiles/search?q=` | admin, analyst | Natural language search |
| GET | `/api/profiles/export?format=csv` | admin, analyst | Export CSV |

---

## Natural Language Parsing

The `/api/profiles/search?q=` endpoint parses free-text queries into structured filters using keyword extraction. Examples: `"young males from Nigeria"` → `gender=male, country_id=NG, age_group=young`.

---

## Setup

### 1. Prerequisites
- Java 21+
- MySQL 8+
- Maven 3.9+

### 2. Create two GitHub OAuth Apps

**Web App** (in GitHub → Settings → Developer Settings → OAuth Apps):
- Homepage URL: `http://localhost:3000`
- Callback URL: `http://localhost:8080/auth/github/callback`

**CLI App**:
- Homepage URL: `http://localhost:8080`
- Callback URL: `http://localhost:8976/callback`

### 3. Configure environment

Copy `src/main/resources/application-dev.properties` and fill in:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nameclassifier?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=yourpassword

github.oauth.web.client-id=YOUR_WEB_CLIENT_ID
github.oauth.web.client-secret=YOUR_WEB_CLIENT_SECRET
github.oauth.web.redirect-uri=http://localhost:8080/auth/github/callback

github.oauth.cli.client-id=YOUR_CLI_CLIENT_ID
github.oauth.cli.client-secret=YOUR_CLI_CLIENT_SECRET

github.oauth.portal-callback-url=http://localhost:3000/api/auth/callback

jwt.secret=your_64_char_random_secret

app.cors.allowed-origins=http://localhost:3000
```

### 4. Run

```bash
mvn spring-boot:run
# → http://localhost:8080
```

---

## Deployment

```bash
mvn clean package -DskipTests
java -jar target/nameclassifier-*.jar
```

Set production environment variables:
```
GITHUB_WEB_CLIENT_ID=...
GITHUB_WEB_CLIENT_SECRET=...
GITHUB_CLI_CLIENT_ID=...
GITHUB_CLI_CLIENT_SECRET=...
GITHUB_WEB_REDIRECT_URI=https://api.yourdomain.com/auth/github/callback
PORTAL_CALLBACK_URL=https://app.yourdomain.com/api/auth/callback
JWT_SECRET=...
DB_USERNAME=...
DB_PASSWORD=...
CORS_ALLOWED_ORIGINS=https://app.yourdomain.com
```

---

## Rate Limiting

| Scope | Limit |
|---|---|
| `/auth/**` | 10 requests / minute |
| All other endpoints | 60 requests / minute per user |

Returns `429 Too Many Requests` when exceeded.