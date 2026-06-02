# Contributing to CheckPoint

Thanks for taking the time to contribute! This guide explains how to set up the
project locally, the conventions we follow, and how to get your changes merged.

CheckPoint is a monorepo with three modules:

| Module | Stack | Path |
|--------|-------|------|
| **API** | Spring Boot 3.5 · Java 21 · PostgreSQL · Maven | [`api/`](api) |
| **Web** | TanStack Start · React 19 · Vite · pnpm | [`web/`](web) |
| **Desktop** | JavaFX admin app · Java 24 · Maven | [`desktop/`](desktop) |

## Code of conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md) based on the
Contributor Covenant. By participating, you are expected to uphold it. Please
report unacceptable behavior to the contacts listed there.

Found a security issue? **Do not** open a public issue — follow the
[Security Policy](SECURITY.md) to report it privately.

## Prerequisites

- **Java 21** (API) and **Java 24** (Desktop)
- **Node.js 20+** and **pnpm** (Web)
- **Docker & Docker Compose** (PostgreSQL, MailHog)
- **Doppler CLI** (recommended for secrets) — or a local `.env` file (see below)

### Secrets and environment variables

The recommended way to provide secrets is [Doppler](https://www.doppler.com/):

```bash
brew install dopplerhq/cli/doppler
doppler login
doppler setup            # run in the repository root
```

If you prefer not to use Doppler, copy [`.env.example`](.env.example) to `.env`
and fill in the values. Spring Boot does **not** read `.env` automatically, so
export the variables into your shell before running the API:

```bash
cp .env.example .env
cd api && export $(grep -v '^#' ../.env | xargs) && ./mvnw spring-boot:run
```

All variables have sensible development defaults in
`api/src/main/resources/application.properties`, so the API boots without any of
them — but external integrations (IGDB, Steam, OAuth2) need real credentials.

## Running each module locally

### API (Spring Boot)

```bash
cd api
docker compose up -d                      # PostgreSQL (+ MailHog)
doppler run -- ./mvnw spring-boot:run     # or use exported .env vars
```

The API runs at `http://localhost:8080`. Interactive API docs (Swagger UI) are
available at `http://localhost:8080/swagger-ui.html`.

### Web (TanStack Start)

```bash
cd web
pnpm install
pnpm dev
```

The web app runs at `http://localhost:3000`.

### Desktop (JavaFX)

```bash
cd desktop
mvn clean javafx:run
```

See [`desktop/README.md`](desktop/README.md) for the view inventory and
architecture. The desktop app expects the API to be running locally.

## Testing & quality gates

CI runs these on every pull request — please run them locally first.

### API

```bash
cd api
doppler run -- ./mvnw verify    # runs tests + enforces JaCoCo coverage ≥ 70%
```

We use [JaCoCo](https://www.eclemma.org/jacoco/) for code coverage. The build
**fails** if line coverage drops below **70%**. After a run, open the HTML report
at `api/target/site/jacoco/index.html`.

To run a single test:

```bash
doppler run -- ./mvnw test -Dtest=AdminUserControllerTest
```

### Web

```bash
cd web
pnpm test        # Vitest
pnpm check:ci    # prettier --check + eslint (must pass in CI)
pnpm check       # auto-fix formatting
```

## Generating documentation

### API & Desktop Javadoc

Both Java modules ship with Javadoc comments. Generate the HTML docs with:

```bash
cd api && ./mvnw javadoc:javadoc        # output: api/target/site/apidocs/index.html
cd desktop && mvn javadoc:javadoc       # output: desktop/target/site/apidocs/index.html
```

### Manual API testing

Ready-to-run REST client requests for the admin endpoints live in
[`doc/http/`](doc/http/). Open them with the VS Code REST Client or IntelliJ HTTP
client, set the `@token` variable to an admin JWT, and send the requests.

## Branching & commits

### Branch naming

```
<username>/<ticket-id>-<short-description>
```

Example: `gseyzeriat1/te-258-docs-contributing-readme-http`

Use the ticket id in lowercase and a concise kebab-case description.

### Commit messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description> (<TICKET-ID>)
```

- **type**: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`, `style`, `perf`
- **scope**: `api`, `web`, or `desktop` (optional, but encouraged)

Examples (from the project history):

```
feat(web): add "Add to list" button on game detail page (TE-343)
fix(web): add favicon and per-route SEO titles/meta via head() (TE-264)
refactor(desktop): implement SOLID architecture, UI fixes, and unit tests (TE-262)
docs: add contributing guide, PR template, desktop README, and API http files (TE-258)
```

## Pull request process

1. Create a branch from `main` following the naming convention above.
2. Make your changes, keeping commits scoped and descriptive.
3. Run the tests and quality gates for the modules you touched.
4. Push and open a pull request. The
   [pull request template](.github/PULL_REQUEST_TEMPLATE.md) is loaded
   automatically — fill in every section.
5. Link the related issue (e.g. `Closes TE-258`).
6. Address review feedback, then squash/merge once approved and CI is green.
