# Archived modules

Two modules were removed from the working tree on 2026-08-18. They are no longer
maintained, no longer built, and no longer covered by CI or Dependabot. Nothing
was lost: both are recoverable in full from an annotated tag.

| Module | Was at | Tag | Last commit |
|--------|--------|-----|-------------|
| JavaFX admin console | `desktop/` | `desktop-final` | `584ca47` (2026-05-31) |
| Slidev soutenance deck | `doc/presentation/` | `presentation-final` | `2d8c213` (2026-06-02) |

## Getting the code back

```bash
# browse a module in place
git checkout desktop-final

# or restore it into the current branch
git checkout desktop-final -- desktop/
git checkout presentation-final -- doc/presentation/
```

Note that `doc/` was renamed to `docs/` in the same change, so the deck comes
back at its old `doc/presentation/` path.

## Why they were archived

**Desktop (JavaFX).** The admin console it provided has been rebuilt inside the
web app under `/admin` (user management, moderation queue, game catalog, news,
analytics). Keeping a second implementation of the same surface in a language
and UI toolkit nobody was touching meant maintaining it for no reader. Its
`pom.xml` also carried seven open jackson-databind advisories that would never
be triaged.

**Presentation (Slidev).** A one-off deck for the soutenance. It had no CI job
and no consumer after the defense, while its `pnpm-lock.yaml` accounted for 35
open advisories across dompurify, js-yaml, nanoid, image-size, mermaid, postcss
and linkify-it, several with no patched release available. The Netlify and
Vercel deployments were retired with it.

Between them these two modules were the source of every open Dependabot alert on
the repository. Removing them brings the count to zero, so the alerts that
remain from now on are ones that actually apply to shipped code.

## What this means for contributors

- The commit scopes are now `api` and `web`. `desktop` is retired.
- `devenv.nix` no longer defines `desktop-dev` or the JavaFX native-library
  plumbing (`CHECKPOINT_JAVAFX_LIB_PATH`).
- Java 24 is no longer a prerequisite; the API targets Java 21.
- If you need the archived code for reference, use the tags above rather than
  restoring it into `main`.
