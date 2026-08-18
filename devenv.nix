{ pkgs, ... }:

{
  # ---------------------------------------------------------------------------
  # Toolchain — versions mirror the CI workflows so that "works on my machine"
  # and "passes CI" mean the same thing.
  #
  #   api      targets Java 21, its CI job runs JDK 25
  #   web      Node.js 22 + pnpm 11 (.github/workflows/web-ci.yml)
  # ---------------------------------------------------------------------------
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk25;
    maven.enable = true;
  };

  languages.javascript = {
    enable = true;
    package = pkgs.nodejs_22;
    pnpm.enable = true;
    pnpm.package = pkgs.pnpm;
  };

  packages = with pkgs; [
    docker-compose # fallback when the docker CLI ships without the compose plugin
    doppler # secrets manager used by the project (see README)
    git
    jq
  ];

  # ---------------------------------------------------------------------------
  # Docker-backed dev services (PostgreSQL + MailHog), defined in
  # api/compose.yaml. That file is also the one Spring Boot's docker-compose
  # support boots automatically, so it stays the single source of truth.
  # ---------------------------------------------------------------------------
  scripts.compose.exec = ''
    set -euo pipefail
    cd "$DEVENV_ROOT"
    if docker compose version >/dev/null 2>&1; then
      exec docker compose -f api/compose.yaml "$@"
    elif command -v docker-compose >/dev/null 2>&1; then
      exec docker-compose -f api/compose.yaml "$@"
    else
      echo "error: no Docker Compose available. Install Docker and start the daemon." >&2
      exit 1
    fi
  '';

  scripts.services-up.exec = ''
    set -euo pipefail
    compose up -d "$@"
    echo "postgres  -> localhost:5432 (myuser/secret, db mydatabase)"
    echo "mailhog   -> smtp localhost:1025, web UI http://localhost:8025"
  '';

  scripts.services-down.exec = ''exec compose down "$@"'';

  scripts.services-logs.exec = ''exec compose logs -f "$@"'';

  # Drops the volumes: the schema is recreated by Hibernate on the next boot.
  scripts.services-reset.exec = ''
    set -euo pipefail
    compose down -v
    services-up
  '';

  scripts.services-psql.exec = ''
    exec compose exec -e PGPASSWORD=secret postgres psql -U myuser -d mydatabase "$@"
  '';

  # ---------------------------------------------------------------------------
  # Secrets: Doppler when it is configured for this directory, otherwise the
  # local .env file (see .env.example). Neither is required to boot the API.
  # ---------------------------------------------------------------------------
  scripts.with-secrets.exec = ''
    set -euo pipefail
    if [ -f "$DEVENV_ROOT/.env" ]; then
      set -a
      # shellcheck disable=SC1091
      . "$DEVENV_ROOT/.env"
      set +a
    fi
    if command -v doppler >/dev/null 2>&1 &&
       [ -n "$(doppler configure get project --plain 2>/dev/null || true)" ]; then
      exec doppler run -- "$@"
    fi
    exec "$@"
  '';

  # ---------------------------------------------------------------------------
  # Per-module workflows
  # ---------------------------------------------------------------------------
  scripts.api-dev.exec = ''
    set -euo pipefail
    services-up >/dev/null
    cd "$DEVENV_ROOT/api"
    exec with-secrets ./mvnw spring-boot:run "$@"
  '';

  scripts.api-test.exec = ''
    set -euo pipefail
    cd "$DEVENV_ROOT/api"
    exec with-secrets ./mvnw verify --batch-mode "$@"
  '';

  scripts.web-dev.exec = ''
    set -euo pipefail
    cd "$DEVENV_ROOT/web"
    [ -d node_modules ] || pnpm install
    exec pnpm dev "$@"
  '';

  scripts.web-test.exec = ''
    set -euo pipefail
    cd "$DEVENV_ROOT/web"
    [ -d node_modules ] || pnpm install
    exec pnpm test "$@"
  '';

  scripts.web-check.exec = ''
    set -euo pipefail
    cd "$DEVENV_ROOT/web"
    [ -d node_modules ] || pnpm install
    exec pnpm check:ci "$@"
  '';

  # Sanity check that the toolchain matches what CI runs.
  scripts.dev-doctor.exec = ''
    set -euo pipefail
    java -version 2>&1 | grep -q '"25' || { echo "expected JDK 25"; exit 1; }
    node --version | grep -q '^v22\.' || { echo "expected Node.js 22"; exit 1; }
    pnpm --version | grep -q '^11\.' || { echo "expected pnpm 11"; exit 1; }
    mvn --version >/dev/null
    docker info >/dev/null 2>&1 || echo "warning: the Docker daemon is not reachable"
    echo "toolchain ok"
  '';

  scripts.dev-help.exec = ''
    cat <<'EOF'
    CheckPoint dev environment

      services-up / services-down / services-logs   postgres + mailhog (docker)
      services-reset                                wipe the database volume
      services-psql                                 psql shell on the dev database

      api-dev / api-test                            Spring Boot API (:8080)
      web-dev / web-test / web-check                TanStack Start web app (:3000)

      with-secrets <cmd>                            run <cmd> with Doppler or .env
      dev-doctor                                    check the toolchain versions
      devenv up                                     API + web together
    EOF
  '';

  # `devenv up` runs the whole stack; api-dev brings the Docker services up.
  processes.api.exec = "api-dev";
  processes.web.exec = "web-dev";

  enterShell = ''
    echo "CheckPoint dev environment ready — run 'dev-help' for the available commands."
  '';
}
