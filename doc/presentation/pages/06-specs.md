---
layout: section
number: "06"
---

# Specs vs réalisé

On a livré tout le périmètre fonctionnel — et au-delà. Les écarts sont techniques et assumés.

---
layout: default
---

# <span class="cp-accent-bar">Écarts techniques — outil différent, objectif atteint</span>

| Spec annoncée | Réalisé | Pourquoi |
|---------------|---------|----------|
| Import via **Spring Batch** | Runner **async maison** (`@Async`) + **ShedLock** | Spring Batch surdimensionné ; job async + commit par item = même résilience, bien moins de complexité. |
| Source **MobyGames** | **IGDB** (+ **Steam** profils/news) | API IGDB plus riche &amp; mieux documentée ; Steam ajoute l'import de bibliothèque. |
| Web **session stateful** | **JWT stateless** en cookie HttpOnly | Une seule chaîne pour deux clients → API multi-instances sans état serveur. |
| Reco **TF-IDF** | Reco **item-to-item** (overlap de tags, SQL) | Plus simple à maîtriser/expliquer, pertinent sur notre volume. |

<div class="mt-3 text-[0.82rem] cp-dim text-center">Aucun écart n'est une régression — chacun a <strong class="text-white">simplifié l'archi</strong> ou <strong class="text-white">enrichi le produit</strong>.</div>

---
layout: two-cols
layoutClass: gap-8
---

# <span class="cp-accent-bar">Au-delà des specs</span>

Bonus livrés, non prévus initialement :

<div class="flex flex-col gap-2 mt-3 text-[0.82rem]">
  <div class="cp-card !p-2.5"><carbon:logo-discord class="inline"/> <strong>OAuth2 / OIDC</strong> — login social</div>
  <div class="cp-card !p-2.5"><carbon:game-console class="inline"/> <strong>Connexion &amp; import Steam</strong> (OpenID + bibliothèque + news)</div>
  <div class="cp-card !p-2.5"><carbon:star class="inline"/> <strong>Easter-eggs</strong> / badges cachés</div>
  <div class="cp-card !p-2.5"><carbon:compare class="inline"/> <strong>Comparaison de profils</strong></div>
  <div class="cp-card !p-2.5"><carbon:trophy class="inline"/> <strong>Leaderboard</strong> &amp; streak de connexion</div>
  <div class="cp-card !p-2.5"><carbon:data-share class="inline"/> <strong>Export RGPD</strong> des données (JSON)</div>
</div>

::right::

# <span class="cp-accent-bar">Suivi du planning</span>

On a **suivi les 4 phases** des specs. Le planning n'a pas été tenu à 100 % — il y a eu quelques retards en route — mais **au final, tout est livré** :

<div class="flex flex-col gap-2 mt-3 text-[0.82rem]">
  <div class="cp-card !p-2.5 flex items-center gap-2"><span class="cp-chip">1</span> Base technique</div>
  <div class="cp-card !p-2.5 flex items-center gap-2"><span class="cp-chip">2</span> Cœur fonctionnel</div>
  <div class="cp-card !p-2.5 flex items-center gap-2"><span class="cp-chip">3</span> Design / UI</div>
  <div class="cp-card !p-2.5 flex items-center gap-2"><span class="cp-chip">4</span> Qualité &amp; livraison</div>
</div>

<div class="mt-3 text-[0.78rem] cp-dim">
Phase finale (WebSockets, tests, polish responsive) visible dans les commits récents : <code>TE-347</code>, <code>TE-351</code>, skeleton loaders, redesign…
</div>

<div class="mt-3 cp-chip"><carbon:calendar class="inline"/> &nbsp;Soutenance — 03/06/2026</div>
