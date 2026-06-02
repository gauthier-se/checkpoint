---
layout: section
number: "08"
---

# Stratégie qualité

Tests, couverture, documentation, analyse statique.

---
layout: default
---

# <span class="cp-accent-bar">Tests &amp; automatisation</span>

<div class="grid grid-cols-2 gap-6 mt-2">

<div>

| Type | Outils | Portée |
|------|--------|--------|
| Unitaires | JUnit 5, Mockito | Services métier isolés |
| Intégration | Spring Boot Test, Testcontainers | Contrôleurs REST, requêtes JPA |
| Web | Vitest | Composants / logique front |
| Automatisation | GitHub Actions | À chaque PR |

<div class="flex gap-2 mt-3 flex-wrap">
  <TechPill icon="i-carbon-document">Swagger UI / OpenAPI</TechPill>
  <TechPill icon="i-carbon-code">fichiers .http</TechPill>
  <TechPill icon="i-carbon-book">Javadoc</TechPill>
</div>

</div>

<div class="flex flex-col gap-3">
  <div class="grid grid-cols-2 gap-3">
    <StatCard :value="70" suffix="%" label="gate JaCoCo — build KO si en-dessous" icon="i-carbon-meter" />
    <StatCard :value="117" label="fichiers de test côté API" icon="i-carbon-test-tool" />
  </div>
  <GlowCard icon="i-carbon-meter-alt" title="Couverture — gate stricte">
  Le build <strong>échoue si &lt; 70 %</strong>. La couverture n'est pas indicative : c'est une <strong>barrière bloquante</strong> en CI.
  </GlowCard>
</div>

</div>

<div class="mt-3 text-[0.8rem] cp-dim text-center">
Documentation API à <code>/swagger-ui.html</code> · fichiers <code>.http</code> prêts à l'emploi dans <code>doc/http/</code> · conventions de nommage (PascalCase / camelCase / snake_case).
</div>

---
layout: default
---

# <span class="cp-accent-bar">Documentation vivante</span>

<div class="grid grid-cols-[1fr_1.55fr] gap-6 mt-2 items-start">

<div class="flex flex-col gap-2.5">

<GlowCard icon="i-carbon-api" title="Swagger / OpenAPI" color="oklch(0.67 0.16 137)">
SpringDoc génère la doc interactive de tous les endpoints — testable depuis le navigateur.
</GlowCard>

<GlowCard icon="i-carbon-http" title="Fichiers .http" color="oklch(0.64 0.15 233)">
Requêtes prêtes à l'emploi dans <code>doc/http/</code> — pour tester l'API sans Postman.
</GlowCard>

<GlowCard icon="i-carbon-book" title="Javadoc" color="oklch(0.66 0.16 60)">
Générée sur l'API <strong>et</strong> le Desktop — documentation du code à jour.
</GlowCard>

</div>

<div>
  <Placeholder label="Swagger UI — /swagger-ui.html" src="swagger-ui.png" ratio="16 / 10" />
  <div class="text-[0.72rem] cp-dim mt-1.5 text-center">Swagger UI à <code>/swagger-ui.html</code> — endpoints versionnés <code>/api/v1</code>.</div>
</div>

</div>
