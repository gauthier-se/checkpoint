---
layout: section
number: "10"
---

# Chiffres clés &amp; démo

Ce qu'on retient, et le fil rouge de la démonstration.

---
layout: default
---

# <span class="cp-accent-bar">Chiffres clés</span>

<div class="grid grid-cols-4 gap-4 mt-6">
  <StatCard :value="3" label="applications · 1 monorepo" icon="i-carbon-box" />
  <StatCard :value="41" label="contrôleurs REST" icon="i-carbon-api" />
  <StatCard :value="60" prefix="~" label="services métier" icon="i-carbon-gears" />
  <StatCard :value="33" label="entités JPA" icon="i-carbon-data-table" />
  <StatCard :value="117" label="fichiers de test" icon="i-carbon-test-tool" />
  <StatCard :value="210" prefix="~" label="commits" icon="i-carbon-commit" />
  <StatCard :value="100" suffix="%" label="par PR avec review" icon="i-carbon-pull-request" />
  <StatCard :value="70" suffix="%" label="gate de couverture" icon="i-carbon-meter" />
</div>

<div class="mt-7 cp-card !p-3 text-center text-[0.9rem]">
<carbon:security class="inline" style="color:oklch(0.66 0.18 286)"/> Sécurité : <strong>JWT double source</strong> · <strong>2FA</strong> · <strong>OAuth2</strong> · <strong>BCrypt</strong> · <strong>RBAC</strong>
</div>

---
layout: cover
class: text-center
---

<div class="i-carbon-play-filled-alt text-7xl mx-auto" style="color: oklch(0.66 0.18 286)"></div>

<h1 class="cp-text-gradient !text-7xl mt-3">Démo</h1>

<p class="text-lg cp-dim mt-4 max-w-xl mx-auto">
Démonstration live de l'application — Web, Desktop &amp; temps réel.
</p>

<!--
On bascule sur l'app : inscription / recherche floue / backlog / review → XP & badge,
puis côté admin login 2FA + import IGDB + modération, et une notification WebSocket en direct.
-->
