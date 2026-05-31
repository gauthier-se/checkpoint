<h1 align="center">CheckPoint — API</h1>

<p align="center">
  <strong>The Spring Boot REST API powering CheckPoint</strong>
</p>

---

## Overview

The API module is the backend for CheckPoint. It exposes a REST API consumed by
the [web](../web) (players) and [desktop](../desktop) (admins) clients, handling
authentication, the game catalog, libraries, reviews, social features,
gamification, moderation, and external integrations (IGDB, Steam).

## Tech stack

| Technology | Purpose |
|------------|---------|
| [Spring Boot 3.5](https://spring.io/projects/spring-boot) | Application framework |
| [Spring Security](https://spring.io/projects/spring-security) | Authentication & authorization (JWT) |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | Persistence |
| [Spring Batch](https://spring.io/projects/spring-batch) | Data import jobs |
| [Hibernate Search](https://hibernate.org/search/) | Full-text fuzzy search (Lucene) |
| [PostgreSQL](https://www.postgresql.org/) | Relational database |
| [SpringDoc OpenAPI](https://springdoc.org/) | Swagger UI / OpenAPI docs |
| [JaCoCo](https://www.eclemma.org/jacoco/) | Code coverage |

## Prerequisites

- **Java 21**
- **Docker & Docker Compose** (PostgreSQL + MailHog)
- **Doppler CLI** (recommended) or a local `.env` (see [root README](../README.md))

## Getting started

```bash
cd api

# Start PostgreSQL (+ MailHog) with Docker
docker compose up -d

# Run the application (Doppler injects secrets)
doppler run -- ./mvnw spring-boot:run

# …or with a local .env exported into the shell
export $(grep -v '^#' ../.env | xargs) && ./mvnw spring-boot:run
```

The API runs at `http://localhost:8080`.

## API documentation (Swagger UI)

With the API running locally:

| Resource | URL |
|----------|-----|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI spec | `http://localhost:8080/v3/api-docs` |

Use the **Authorize** button to send a JWT via the `Authorization` header
(Desktop) or the `checkpoint_token` cookie (Web). Swagger is enabled by default
in development and disabled in production by setting `SWAGGER_ENABLED=false`.

For quick manual testing, see the ready-to-run requests in [`../doc/http/`](../doc/http/).

## Testing & coverage

```bash
# Run the full test suite and enforce the coverage gate
doppler run -- ./mvnw verify

# Run a single test
doppler run -- ./mvnw test -Dtest=AdminUserControllerTest
```

Coverage is measured with [JaCoCo](https://www.eclemma.org/jacoco/); the build
**fails if line coverage drops below 70%**. The HTML report is generated at
`target/site/jacoco/index.html`.

## Javadoc

```bash
./mvnw javadoc:javadoc      # output: target/site/apidocs/index.html
```

## Project structure

```
api/src/main/java/com/checkpoint/api/
├── controllers/   # REST controllers (@RestController)
├── services/      # Business logic (interfaces + impl)
├── repositories/  # Spring Data JPA repositories
├── entities/      # JPA entities
├── dto/           # Request/response records, grouped by domain
├── config/        # Spring configuration (security, OpenAPI, …)
└── ApiApplication.java
```

## Configuration

All externally configurable settings live in
`src/main/resources/application.properties` and read from environment variables
with development defaults. See [`.env.example`](../.env.example) for the full
list (IGDB, Steam, JWT, OAuth2, Swagger toggle, …).

## Related documentation

- [Root README](../README.md) — project overview and full setup
- [Contributing guide](../CONTRIBUTING.md) — conventions and workflow
- [HTTP files](../doc/http/) — ready-to-run admin requests
