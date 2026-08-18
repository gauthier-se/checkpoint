# Contributing to CheckPoint

Thanks for taking the time to contribute. This guide is the technical reference
for the project: how to set it up locally, how to run and test each module, and
the conventions your change needs to follow to get merged.

CheckPoint is a monorepo with two modules:

| Module | Stack | Path |
|--------|-------|------|
| **API** | Spring Boot 3.5 · Java 21 · PostgreSQL · Maven | [`api/`](api) |
| **Web** | TanStack Start · React 19 · Vite · pnpm | [`web/`](web) |

Two modules that used to live here, a JavaFX admin console and a Slidev deck,
were archived and are no longer maintained. See [ARCHIVE.md](ARCHIVE.md) if you
need them.

## Code of conduct and security

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md) based on the
Contributor Covenant. By participating, you are expected to uphold it.

Found a security issue? **Do not** open a public issue: follow the
[Security Policy](SECURITY.md) to report it privately.

## Getting started

Two setups are supported. The Nix one pins the whole toolchain for you; the
manual one leaves you to install the tools yourself. Both use Docker for the
stateful dev services (PostgreSQL, MailHog).

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
automatically to the API commands, so a full stack is just:

```bash
devenv up
```

### Manual setup

Without Nix, you need:

- **Java 21** (API)
- **Node.js 20+** and **pnpm** (Web)
- **Docker & Docker Compose** (PostgreSQL, MailHog)
- **PostgreSQL** (or use Docker)
- **Doppler CLI** (recommended for secrets), or a local `.env` file

### Secrets and environment variables

**Option A: Doppler (recommended).** This project uses
[Doppler](https://www.doppler.com/) to manage environment variables and secrets.

```bash
brew install dopplerhq/cli/doppler
doppler login
doppler setup            # run in the repository root
```

**Option B: a `.env` file.** Copy [`.env.example`](.env.example) to `.env` and
fill in the values. Spring Boot does **not** read `.env` automatically, so
export the variables into your shell before running the API:

```bash
cp .env.example .env
cd api && export $(grep -v '^#' ../.env | xargs) && ./mvnw spring-boot:run
```

All variables have development defaults in
`api/src/main/resources/application.properties`, so the API boots without any of
them, but external integrations (IGDB, Steam, OAuth2) need real credentials:

| Variable | Description |
|----------|-------------|
| `IGDB_CLIENT_ID` | Twitch Client ID for the IGDB API |
| `IGDB_CLIENT_SECRET` | Twitch Client Secret for the IGDB API |

## Running each module locally

### API (Spring Boot)

```bash
cd api
docker compose up -d                      # PostgreSQL (+ MailHog)
doppler run -- ./mvnw spring-boot:run     # or use exported .env vars
```

The API runs at `http://localhost:8080`.

### Web (TanStack Start)

```bash
cd web
pnpm install
pnpm dev
```

The web app runs at `http://localhost:3000`.

## API documentation

The API ships with interactive **Swagger UI** powered by SpringDoc OpenAPI. With
the API running locally:

| Resource | URL |
|----------|-----|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI spec** | `http://localhost:8080/v3/api-docs` |

Use the **Authorize** button to authenticate with a JWT via the `Authorization`
header or the `checkpoint_token` cookie.

For quick manual testing, ready-to-run REST client requests for the admin
endpoints live in [`docs/http/`](docs/http/). Open them with the VS Code REST
Client or the IntelliJ HTTP client, set the `@token` variable to an admin JWT,
and send the requests.

To generate the Javadoc:

```bash
cd api && ./mvnw javadoc:javadoc        # output: api/target/site/apidocs/index.html
```

## Testing and quality gates

CI runs these on every pull request, so please run them locally first.

### API

```bash
cd api
doppler run -- ./mvnw verify    # runs tests + enforces JaCoCo coverage ≥ 70%
```

Coverage is measured with [JaCoCo](https://www.eclemma.org/jacoco/). The build
**fails** if line coverage drops below **70%**. After a run, open the HTML report
at `api/target/site/jacoco/index.html`; CI also uploads it as an artifact on
every pull request.

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

Note that CI does not typecheck. `pnpm exec tsc --noEmit` currently reports
pre-existing errors on `main`, so if you run it, compare your error set against
`main` rather than reading the total.

## Branching and commits

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
- **scope**: `api` or `web` (optional, but encouraged)

Examples (from the project history):

```
feat(web): add "Add to list" button on game detail page (TE-343)
fix(web): add favicon and per-route SEO titles/meta via head() (TE-264)
feat(web): admin analytics dashboard (#548)
docs: add contributing guide, PR template, and API http files (TE-258)
```

## Pull request process

1. Create a branch from `main` following the naming convention above.
2. Make your changes, keeping commits scoped and descriptive.
3. Run the tests and quality gates for the modules you touched.
4. Push and open a pull request. The
   [pull request template](.github/PULL_REQUEST_TEMPLATE.md) is loaded
   automatically, so fill in every section.
5. Link the related issue (e.g. `Closes TE-258`).
6. Address review feedback, then squash and merge once approved and CI is green.

CI is path-filtered: `api-ci.yml` runs on `api/**` and `web-ci.yml` on `web/**`.
A pull request touching only root-level files runs neither, so verify it locally
and say so in the description.

## Deployment

The application is deployed on a VPS using **Dokploy**, a self-hosted PaaS that
sits on top of Docker. Dokploy handles:

- Automatic SSL/TLS certificates
- Reverse proxy configuration (Traefik)
- Container orchestration
- Zero-downtime deployments

The API and the web app are two separate Dokploy applications. Only the web app
is exposed publicly: it serves the front end and proxies `/api/**` to the API
container, so the API is never reachable directly from the internet.

### How a deployment is triggered

Dokploy's "auto deploy on push" is **off** on both applications. Rollouts go
through [`cd.yml`](.github/workflows/cd.yml) instead, which runs on every push
to `main` and:

1. Diffs the push to decide whether the API, the web app, or both changed.
2. Re-runs `api-ci.yml` and `web-ci.yml` on the merge commit. The pull request
   runs cover the branch, not the commit that actually lands, and a push to
   `main` triggers neither workflow on its own.
3. Calls the Dokploy deploy hook of each changed application, API first. A
   commit touching both must ship the API ahead of the web app: the front end
   calls endpoints that have to exist already, and it is also the proxy in
   front of the API.
4. Waits for the deployment to be observably live.

That last step matters. A deploy hook only acknowledges the request, so a build
that fails inside Dokploy still answers with a success code. For the API the
workflow polls `/api/v1/actuator/info` until the reported `build.time` is newer
than the run, which is what proves the container restarted on a new image. The
web app has no equivalent stamp, so the workflow waits for the site to answer
and reports whether the bundle hash moved.

To redeploy without pushing, run the **CD** workflow manually from the Actions
tab and pick `api`, `web`, or `both`.

### Required repository secrets

| Secret                    | Purpose                        |
| ------------------------- | ------------------------------ |
| `DOKPLOY_API_DEPLOY_HOOK` | Deploy hook URL of the API app |
| `DOKPLOY_WEB_DEPLOY_HOOK` | Deploy hook URL of the web app |

These URLs are credentials: anyone holding one can trigger a production
deployment. Regenerate them in Dokploy if they are ever exposed.
