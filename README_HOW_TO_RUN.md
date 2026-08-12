# How to Run — Bank Card Management Service

This guide gets the project running locally from a clean checkout: infrastructure (PostgreSQL + Redis), the Spring Boot app, and a smoke test via Swagger UI.

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker + Docker Compose | any recent version | `docker -v` |

> This project has no Maven Wrapper (`mvnw`) committed, so use your system's `mvn`.

---

## 1. Start Infrastructure (PostgreSQL + Redis)

From the project root:

```bash
docker-compose up -d
```

This starts two containers, defined in `docker-compose.yml`:

| Service | Container | Port | Purpose |
|---|---|---|---|
| PostgreSQL 15 | `bankcard-postgres` | `5432` | Application database (`bankdb`) |
| Redis 7 | `bankcard-redis` | `6379` | Refresh-token storage & access-token blacklist (logout) |

Verify both are healthy:

```bash
docker ps
# STATUS column should show "healthy" for both containers after ~10-20s
```

Default credentials (see `docker-compose.yml` / `application.yml` — fine for local dev, **do not use in production**):
- Postgres: `postgres` / `postgres`, database `bankdb`
- Redis: no password required locally

---

## 2. Build the Project

```bash
mvn clean install -DskipTests
```

(Or drop `-DskipTests` to also run the test suite — see [Running Tests](#running-tests) below.)

---

## 3. Run the Application

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080** (configured in `application.yml`).

On startup, Liquibase automatically runs the migrations in `src/main/resources/db/migration/`:
- `001-create-users-table.yaml`
- `002-seed-default-admin.yaml` — seeds a default admin account (see below)
- `003-create-cards-table.yaml`

You should see log lines like:
```
Liquibase: ChangeSet ... ran successfully
Tomcat started on port 8080
Started BankcardsApplication in X.XXX seconds
```

### Default Seeded Admin Account

| Field | Value |
|---|---|
| Email | `admin@bankcard.local` |
| Password | `Admin123!` |
| Role | `ADMIN` |

Use this to log in immediately without registering — see [Testing via Swagger](#4-explore--test-via-swagger-ui).

---

## 4. Explore & Test via Swagger UI

Open: **http://localhost:8080/swagger-ui/index.html**

### Authenticate once, test everything

1. Expand **Authentication → POST /api/auth/login**, click **Try it out**, and send:
   ```json
   {
     "email": "admin@bankcard.local",
     "password": "Admin123!"
   }
   ```
2. Copy the `accessToken` from the response.
3. Click the **🔓 Authorize** button (top-right of the page).
4. Paste **just the token** (no `Bearer ` prefix) → **Authorize** → **Close**.
5. Every subsequent "Try it out" call across **Authentication**, **Cards**, and **Admin - Cards** now automatically sends `Authorization: Bearer <token>` for you.

### Suggested test flow

1. **Admin - Cards → POST /api/admin/cards** — issue a card to a user (`ownerId: 1` for the seeded admin, or register a new user first and use their id).
2. **Cards → GET /api/cards** — list your own cards.
3. **Cards → POST /api/cards/transfer** — transfer funds between two of your own cards.
4. **Authentication → POST /api/auth/refresh** — pass your `refreshToken` via the `X-Refresh-Token` header to get a new token pair.
5. **Authentication → POST /api/auth/logout** — blacklists the access token and revokes the refresh token.

Full request/response schemas, field descriptions, and example payloads are documented directly in Swagger UI for every endpoint.

---

## Configuration Reference

Main config file: `src/main/resources/application.yml`

| Setting | Default | Notes |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/bankdb` | Change if Postgres runs elsewhere |
| `spring.data.redis.host` / `port` | `localhost` / `6379` | Redis connection |
| `jwt.secret` | dev default (override via `JWT_SECRET` env var) | HMAC signing key |
| `jwt.access-token-expiry` | `900000` ms (15 min) | Access token lifetime |
| `jwt.refresh-token-expiry` | `604800000` ms (7 days) | Refresh token lifetime (stored in Redis) |
| `app.encryption.card-key` | dev default (override via `CARD_ENCRYPTION_KEY` env var) | AES-256-GCM key used to encrypt stored card numbers |

⚠️ **Before deploying anywhere beyond your own machine**, override `JWT_SECRET` and `CARD_ENCRYPTION_KEY` via environment variables, and change the Postgres/Redis credentials.

---

## Running Tests

```bash
mvn test
```

---

## Stopping / Resetting

**Stop the app:** `Ctrl+C` in the terminal running `mvn spring-boot:run`.

**Stop infrastructure (keep data):**
```bash
docker-compose stop
```

**Stop and wipe all data (fresh database + Redis on next start):**
```bash
docker-compose down -v
```

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Port 8080 was already in use` | Another instance of the app (or another process) is already running | Find and stop it: `lsof -i :8080` then stop that process, or change `server.port` |
| App fails to connect to Postgres/Redis | Containers not started/healthy yet | Run `docker ps`, wait for `healthy` status, then restart the app |
| `ValidationFailedException` from Liquibase (changeset checksum mismatch) | A migration file was edited after already being applied to your local DB | For local dev, reset the database: `docker-compose down -v && docker-compose up -d` |
| Login fails with `401` on the seeded admin | Migrations didn't run (check startup logs for Liquibase errors) | Reset the database as above and restart the app |
