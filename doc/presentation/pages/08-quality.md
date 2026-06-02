---
layout: default
---

# <span class="cp-accent-bar">Qualité &amp; intégration continue</span>

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

| Type | Outils | Portée |
|------|--------|--------|
| Unitaires | JUnit 5, Mockito | Services métier isolés |
| Intégration | Spring Boot Test, Testcontainers | Contrôleurs REST, JPA |
| Web | Vitest | Composants / logique front |

<div class="flex gap-2 mt-3 flex-wrap">
  <TechPill icon="i-carbon-document">Swagger UI / OpenAPI</TechPill>
  <TechPill icon="i-carbon-code">fichiers .http</TechPill>
  <TechPill icon="i-carbon-book">Javadoc (API + Desktop)</TechPill>
</div>

</div>

<div class="flex flex-col gap-2.5">
  <div class="grid grid-cols-2 gap-3">
    <StatCard :value="70" suffix="%" label="gate JaCoCo — build KO sinon" icon="i-carbon-meter" />
    <StatCard :value="117" label="fichiers de test (API)" icon="i-carbon-test-tool" />
  </div>
  <GlowCard icon="i-carbon-meter-alt" title="Couverture = barrière bloquante">
  Le build <strong>échoue si &lt; 70 %</strong> en CI — la couverture n'est pas indicative.
  </GlowCard>
</div>

</div>

<div class="grid grid-cols-3 gap-3 mt-4 text-[0.8rem]">
  <div class="cp-card cp-card-ic !p-2.5"><carbon:flow class="inline" style="color:oklch(0.66 0.18 286)"/> <strong>CI scopée par chemin</strong> — <code>api-ci</code> / <code>web-ci</code> ne relancent que le module modifié.</div>
  <div class="cp-card cp-card-ic !p-2.5"><carbon:deploy class="inline" style="color:oklch(0.66 0.18 286)"/> <strong>Déploiement auto</strong> — Dokploy détecte les commits sur <code>main</code> et redéploie (Git-driven).</div>
  <div class="cp-card cp-card-ic !p-2.5"><carbon:api class="inline" style="color:oklch(0.66 0.18 286)"/> <strong>Doc vivante</strong> — Swagger à <code>/swagger-ui.html</code>, endpoints <code>/api/v1</code>.</div>
</div>

<!--
La couverture n'est pas un chiffre indicatif : c'est une barrière bloquante, le
build échoue sous 70 %. La CI est scopée par chemin, et le déploiement est
automatique sur main via Dokploy — c'est notre brique automatisation cloud-native.
-->
