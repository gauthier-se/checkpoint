---
layout: default
---

# <span class="cp-accent-bar">Bilan &amp; chiffres clés</span>

<div class="grid grid-cols-4 gap-4 mt-5">
  <StatCard :value="3" label="applications · 1 monorepo" icon="i-carbon-box" />
  <StatCard :value="41" label="contrôleurs REST" icon="i-carbon-api" />
  <StatCard :value="60" prefix="~" label="services métier" icon="i-carbon-gears" />
  <StatCard :value="33" label="entités JPA" icon="i-carbon-data-table" />
  <StatCard :value="117" label="fichiers de test" icon="i-carbon-test-tool" />
  <StatCard :value="210" prefix="~" label="commits" icon="i-carbon-commit" />
  <StatCard :value="100" suffix="%" label="par PR avec review" icon="i-carbon-pull-request" />
  <StatCard :value="70" suffix="%" label="gate de couverture" icon="i-carbon-meter" />
</div>

<div v-click class="mt-6 cp-card !p-4 text-center text-[0.95rem]">
<carbon:idea class="inline" style="color:oklch(0.66 0.18 286)"/> <strong>Trois clients hétérogènes derrière une seule API sécurisée et stateless.</strong> On a tenu le périmètre fonctionnel, et nos écarts par rapport aux specs sont des <strong class="text-white">choix d'ingénierie assumés</strong> qui ont simplifié l'architecture ou enrichi le produit.
</div>

<!--
Phrase de conclusion : CheckPoint, c'est trois clients hétérogènes derrière une
seule API sécurisée et stateless ; on a tenu le périmètre fonctionnel, et nos
écarts par rapport aux specs sont des choix d'ingénierie assumés.
-->
