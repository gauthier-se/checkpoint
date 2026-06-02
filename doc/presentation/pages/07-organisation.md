---
layout: default
---

# <span class="cp-accent-bar">Organisation — Linear ↔ GitHub, traçabilité de bout en bout</span>

<div class="grid grid-cols-2 gap-6 mt-6">

<div class="flex flex-col gap-4 text-[0.82rem]">
  <GlowCard icon="i-carbon-roadmap" title="Linear — issues &amp; gestion de projet" color="oklch(0.64 0.15 233)">
  Backlog, user stories, cycles, milestones. Workspace <code>m2i-projet-java</code>, équipe <strong>TE</strong>, projet <strong>Checkpoint</strong>.
  </GlowCard>
  <div class="cp-card cp-card-ic !p-2.5"><carbon:tag class="inline"/> Labels <code>api</code> / <code>web</code> / <code>desktop</code> (exclusifs), <code>Feature</code> / <code>Bug</code> / <code>Improvement</code>, <code>security</code>…</div>
  <div class="cp-card cp-card-ic !p-2.5"><carbon:milestone class="inline"/> Milestones — ex. « Quality, Real-Time &amp; Final Delivery ».</div>
  <div class="cp-card cp-card-ic !p-2.5"><carbon:logo-discord class="inline"/> Discord pour les points de synchro quotidiens.</div>
</div>

<div>
  <Placeholder label="Board Linear — équipe TE" src="linear.png" ratio="16 / 9" />
  <div class="text-[0.78rem] cp-dim mt-1.5 text-center">Backlog, cycles &amp; milestones suivis dans Linear.</div>
</div>

</div>

<div v-click class="flex items-center justify-center gap-1.5 mt-4 text-[0.74rem] flex-wrap">
  <span class="cp-chip"><carbon:ticket class="inline"/> Ticket <code>TE-343</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:branch class="inline"/> Branche <code>te-343-…</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:commit class="inline"/> Commit <code>… (TE-343)</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:pull-request class="inline"/> PR <code>Closes TE-343</code></span>
  <carbon:arrow-right class="opacity-40" />
  <span class="cp-chip"><carbon:checkmark-filled class="inline" style="color:oklch(0.67 0.16 137)"/> Merge <code>main</code></span>
</div>

<!--
Le même identifiant TE-xxx traverse tout : ticket, branche, commit, PR, merge.
Traçabilité de bout en bout entre la gestion de projet et le code.
-->

---
layout: default
---

# <span class="cp-accent-bar">Workflow Git — GitHub Flow</span>

<div class="grid grid-cols-2 gap-6 mt-6">

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

<!--
GitHub Flow : main toujours déployable, tout par PR, conventional commits avec
l'ID de ticket. Aucun push direct, au moins une review + CI verte avant merge.
-->
