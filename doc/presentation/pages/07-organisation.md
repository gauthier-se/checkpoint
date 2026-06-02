---
layout: section
number: "07"
---

# Organisation

Linear ↔ GitHub, GitHub Flow, protection de branche &amp; CI.

---
layout: default
---

# <span class="cp-accent-bar">Linear ↔ GitHub — traçabilité de bout en bout</span>

<div class="grid grid-cols-2 gap-6 mt-2">

<div class="flex flex-col gap-2 text-[0.82rem]">
  <GlowCard icon="i-carbon-roadmap" title="Linear — gestion de projet" color="oklch(0.64 0.15 233)">
  Backlog, user stories, cycles, milestones. Workspace <code>m2i-projet-java</code>, équipe <strong>TE</strong>, projet <strong>Checkpoint</strong>.
  </GlowCard>
  <div class="cp-card !p-2.5"><carbon:tag class="inline"/> Labels <code>api</code> / <code>web</code> / <code>desktop</code> (exclusifs), <code>Feature</code> / <code>Bug</code> / <code>Improvement</code>, <code>security</code>…</div>
  <div class="cp-card !p-2.5"><carbon:milestone class="inline"/> Milestones — ex. « Quality, Real-Time &amp; Final Delivery ».</div>
  <div class="cp-card !p-2.5"><carbon:logo-discord class="inline"/> Discord pour les points de synchro quotidiens.</div>
</div>

<div>
  <Placeholder label="Board Linear — équipe TE" src="linear.png" ratio="16 / 9" />
  <div class="text-[0.78rem] cp-dim mt-1.5 text-center">Backlog, cycles &amp; milestones suivis dans Linear.</div>
</div>

</div>

<div class="flex items-center justify-center gap-1.5 mt-4 text-[0.74rem] flex-wrap">
  <span class="cp-chip"><carbon:ticket class="inline"/> Ticket <code>TE-343</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:branch class="inline"/> Branche <code>te-343-…</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:commit class="inline"/> Commit <code>feat(web): … (TE-343)</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:pull-request class="inline"/> PR <code>Closes TE-343</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:checkmark-filled class="inline" style="color:oklch(0.67 0.16 137)"/> Merge <code>main</code></span>
</div>

---
layout: default
---

# <span class="cp-accent-bar">GitHub Flow &amp; conventions</span>

<div class="grid grid-cols-2 gap-6 mt-2">

<div>

**Branche unique de vérité : `main`** (toujours déployable). Aucun push direct — tout passe par une **PR**.

```bash
# Nommage de branche
<username>/<ticket-id>-<description-kebab>
gseyzeriat1/te-258-docs-contributing-readme-http
```

```bash
# Conventional Commits
<type>(<scope>): <description> (<TICKET-ID>)
feat(web): add "Add to list" button (TE-343)
refactor(desktop): implement SOLID architecture (TE-262)
```

<div class="text-[0.76rem] cp-dim mt-1">Types : <code>feat</code> · <code>fix</code> · <code>chore</code> · <code>refactor</code> · <code>docs</code> · <code>test</code> — Scope : api / web / desktop.</div>

</div>

<div class="flex flex-col gap-2 text-[0.82rem]">
  <GlowCard icon="i-carbon-rule" title="Règles de protection">
  <div class="flex flex-col gap-1 mt-1">
    <div>✅ ≥ 1 review approuvée avant merge</div>
    <div>✅ status checks verts obligatoires (CI)</div>
    <div>✅ template de PR + lien <code>Closes TE-xxx</code></div>
  </div>
  </GlowCard>
  <div class="grid grid-cols-2 gap-2">
    <StatCard :value="210" prefix="~" label="commits" icon="i-carbon-commit" />
    <StatCard :value="100" suffix="%" label="par PR avec review" icon="i-carbon-pull-request" />
  </div>
</div>

</div>

---
layout: default
---

# <span class="cp-accent-bar">Intégration &amp; déploiement continus</span>

<div class="grid grid-cols-2 gap-6 mt-3">

<GlowCard icon="i-carbon-cloud-foundry" title="api-ci.yml" color="oklch(0.58 0.21 286)">

Déclenché si `api/**` change :

<div class="flex flex-col gap-1.5 mt-2 text-[0.8rem]">
  <div><carbon:test-tool class="inline"/> tests JUnit + Mockito + Testcontainers</div>
  <div><carbon:meter class="inline"/> <strong>gate JaCoCo ≥ 70 %</strong> — build en échec sinon</div>
  <div><carbon:document class="inline"/> build + upload du rapport en artifact</div>
</div>

</GlowCard>

<GlowCard icon="i-carbon-logo-react" title="web-ci.yml" color="oklch(0.64 0.15 233)">

Déclenché si `web/**` change :

<div class="flex flex-col gap-1.5 mt-2 text-[0.8rem]">
  <div><carbon:checkmark class="inline"/> <code>pnpm check:ci</code> — lint + prettier</div>
  <div><carbon:test-tool class="inline"/> tests Vitest</div>
  <div><carbon:build-tool class="inline"/> build</div>
</div>

</GlowCard>

</div>

<div class="grid grid-cols-2 gap-4 mt-4">
  <div class="cp-card !p-3 text-[0.84rem]"><carbon:flow class="inline" style="color:oklch(0.66 0.18 286)"/> CI <strong>scopée par chemin</strong> → on ne relance pas tout le monorepo à chaque PR.</div>
  <div class="cp-card !p-3 text-[0.84rem]"><carbon:deploy class="inline" style="color:oklch(0.66 0.18 286)"/> <strong>Déploiement automatique</strong> : Dokploy détecte les commits sur <code>main</code> et redéploie les conteneurs — pas de release manuelle.</div>
</div>
