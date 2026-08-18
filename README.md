<p align="center">
  <img src="docs/assets/logo/logo.png" alt="CheckPoint Logo" width="200"/>
</p>

<h1 align="center">CheckPoint</h1>

<p align="center">
  <strong>Your unified video game library tracker</strong>
</p>

<p align="center">
  <a href="#about">About</a> •
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#api-documentation">API Docs</a> •
  <a href="#testing--quality">Testing</a> •
  <a href="#documentation">Documentation</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#authors">Authors</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black" alt="React"/>
  <img src="https://img.shields.io/badge/coverage-%E2%89%A570%25-brightgreen?logo=jacoco&logoColor=white" alt="Coverage"/>
  <img src="https://img.shields.io/badge/PRs-welcome-blueviolet" alt="PRs welcome"/>
</p>

---

## About

**CheckPoint** addresses a concrete need identified among video game players: the fragmentation of their game libraries across multiple platforms. It offers a centralized solution allowing users to:

- Track their progress and manage their **backlog**
- Organize their **wishlist**
- Rate and review games
- Interact with a **community** of players
- Earn **XP and badges** through gamification

---

## Features

### For Players (Web Application)
- Secure authentication with 2FA support
- Personal game library management (in progress, finished, wishlist)
- Rating and review system
- Follow friends and see their activity
- Advanced fuzzy search with filters
- Game recommendations based on preferences
- Gamification with XP, levels, and badges

### For Administrators (Web Admin Panel)
- User management (block, promote, view history)
- Content moderation (review reports queue)
- API synchronization with MobyGames/IGDB
- Analytics dashboard with charts
- Manual game entry CRUD operations

---

## Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Spring Boot 3** | REST API framework |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | Database persistence |
| **Spring Batch** | Data import jobs |
| **Hibernate Search** | Full-text fuzzy search |
| **PostgreSQL** | Relational database |
| **SpringDoc OpenAPI (Swagger UI)** | Interactive API documentation |
| **JaCoCo** | Code coverage (enforced ≥ 70%) |

### Web Frontend
| Technology | Purpose |
|------------|---------|
| **TanStack Start** | React meta-framework with SSR |
| **React** | UI library |
| **Tailwind CSS** | Styling |
| **Shadcn UI** | Component library |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| **Nix + devenv** | Reproducible development environment |
| **Docker** | Containerization |
| **Traefik** | Reverse proxy & SSL |
| **Docker Swarm** | Container orchestration |

---

## Getting Started

Two setups are supported: a **Nix-based** one that pins the whole toolchain for
you, and a **manual** one where you install the tools yourself. Both use Docker
for the stateful dev services (PostgreSQL, MailHog).

### Setup with Nix (devenv)

[devenv](https://devenv.sh/) provisions Java, Node, pnpm, Maven and the Doppler
CLI at the exact versions CI uses, so a green local run means a green pipeline.

```bash
# One-time: install Nix and devenv
curl -fsSL https://install.determinate.systems/nix | sh -s -- install
nix profile install nixpkgs#devenv

# Enter the environment (first run downloads the toolchain)
devenv shell
```

With [direnv](https://direnv.net/) installed, `direnv allow` activates the
environment automatically whenever you `cd` into the repository.

The shell exposes a few commands:

| Command | What it does |
|---------|--------------|
| `services-up` / `services-down` | PostgreSQL + MailHog via `api/compose.yaml` |
| `services-logs` / `services-psql` | tail the service logs / open a `psql` shell |
| `services-reset` | drop the database volume and start over |
| `api-dev` / `api-test` | run the API on `:8080` / `./mvnw verify` |
| `web-dev` / `web-test` / `web-check` | run the web app on `:3000` / tests / lint + format |
| `with-secrets <cmd>` | run `<cmd>` with Doppler if configured, otherwise `.env` |
| `dev-doctor` | check that the toolchain matches CI |
| `devenv up` | run the API and the web app together |

`api-dev` starts the Docker services itself, and `with-secrets` is applied
automatically to the API commands — so a full stack is just:

```bash
devenv up
```

### Prerequisites (manual setup)

- **Java 21** (API)
- **Node.js 20+**
- **pnpm**
- **Docker & Docker Compose**
- **PostgreSQL** (or use Docker)
- **Doppler CLI** (recommended for secrets) — or a local `.env` file

### Environment Variables

You can provide secrets in two ways.

**Option A — Doppler (recommended).** This project uses [Doppler](https://www.doppler.com/) to manage environment variables and secrets.

```bash
# Install Doppler CLI (macOS)
brew install dopplerhq/cli/doppler

# Login to Doppler
doppler login

# Setup project (run in root directory)
doppler setup
```

**Option B — `.env` file.** Copy [`.env.example`](.env.example) to `.env` and fill in the values. Spring Boot does **not** load `.env` automatically, so export the variables before running the API:

```bash
cp .env.example .env
cd api && export $(grep -v '^#' ../.env | xargs) && ./mvnw spring-boot:run
```

All variables have development defaults in `api/src/main/resources/application.properties`, so the API boots without any of them — but external integrations (IGDB, Steam, OAuth2) require real credentials.

### API (Spring Boot)

```bash
cd api

# Start PostgreSQL with Docker
docker compose up -d

# Run the application with Doppler
doppler run -- ./mvnw spring-boot:run

# Run tests with Doppler (for IGDB integration tests)
doppler run -- ./mvnw test
```

The API will be available at `http://localhost:8080`

#### Required Environment Variables (via Doppler)

| Variable | Description |
|----------|-------------|
| `IGDB_CLIENT_ID` | Twitch Client ID for IGDB API |
| `IGDB_CLIENT_SECRET` | Twitch Client Secret for IGDB API |

### Web Application (TanStack Start)

```bash
cd web

# Install dependencies
pnpm install

# Start development server
pnpm dev
```

The web app will be available at `http://localhost:3000`

### Production Deployment

The application is deployed on a VPS using **Dokploy**, a self-hosted PaaS that simplifies Docker container management.

Dokploy handles:
- Automatic SSL/TLS certificates
- Reverse proxy configuration (Traefik)
- Container orchestration
- Zero-downtime deployments

---

## API Documentation

The API ships with interactive **Swagger UI** powered by SpringDoc OpenAPI. With the API running locally:

| Resource | URL |
|----------|-----|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI spec** | `http://localhost:8080/v3/api-docs` |

Use the **Authorize** button to authenticate with a JWT via the `Authorization` header or the `checkpoint_token` cookie.

For quick manual testing, ready-to-run REST client requests for the admin endpoints live in [`docs/http/`](docs/http/) (compatible with the VS Code REST Client and the IntelliJ HTTP client).

To generate the Javadoc:

```bash
cd api && ./mvnw javadoc:javadoc        # output: api/target/site/apidocs/index.html
```

---

## Testing & Quality

| Module | Command | Notes |
|--------|---------|-------|
| **API** | `doppler run -- ./mvnw verify` | Runs tests and enforces the JaCoCo coverage gate |
| **Web** | `pnpm test` · `pnpm check:ci` | Vitest + prettier/eslint checks |

Code coverage is measured with [**JaCoCo**](https://www.eclemma.org/jacoco/). The API build **fails if line coverage drops below 70%**; the HTML report is generated at `api/target/site/jacoco/index.html` and uploaded as a CI artifact on every pull request.

---

## Documentation

| Document | Description |
|----------|-------------|
| [Contributing Guide](CONTRIBUTING.md) | Setup, conventions, and PR workflow |
| [API HTTP files](docs/http/) | Ready-to-run REST requests for admin endpoints |
| [Specifications](docs/specifications/requirements.md) | Full project requirements and specifications |
| [Architecture](docs/architecture/) | System architecture diagram |
| [Entities](docs/entities/) | JPA entity documentation |
| [UML Diagrams](docs/uml/) | MCD and Use Case diagrams |
| [Design](docs/design/) | Graphic charter and style guide |
| [Mockups](docs/assets/mockups/) | UI/UX mockups |
| [Archived modules](ARCHIVE.md) | The retired JavaFX console and Slidev deck |

---

## Contributing

Contributions are welcome! Please read the [Contributing Guide](CONTRIBUTING.md) for branch naming, commit conventions (Conventional Commits), local setup, and the pull request process. When opening a PR, the [pull request template](.github/PULL_REQUEST_TEMPLATE.md) is loaded automatically.

---

## Authors

- **Enzo CHABOISSEAU**
- **Gauthier SEYZERIAT--MEYER**

---

## License

This project is released under the [MIT License](LICENSE) — see the `LICENSE`
file for details. It was originally built as part of an academic program at
**CCI Campus Alsace**.

## Community

- [Code of Conduct](CODE_OF_CONDUCT.md) — expectations for participation
- [Contributing Guide](CONTRIBUTING.md) — how to set up, build, and submit changes
- [Security Policy](SECURITY.md) — how to report vulnerabilities privately

---

<p align="center">
  Made with ❤️ for gamers
</p>
