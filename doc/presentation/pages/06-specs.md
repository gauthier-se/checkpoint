---
layout: default
---

# <span class="cp-accent-bar">Gestion de projet — specs vs réalisé</span>

<div class="text-[0.82rem] mt-6 mb-4">On a livré <strong>tout le périmètre fonctionnel</strong> — et au-delà. Les écarts sont <strong>techniques et assumés</strong> : meilleur outil, même objectif.</div>

| Spec annoncée | Réalisé | Pourquoi |
|---------------|---------|----------|
| Import via **Spring Batch** | Runner **async maison** (`@Async`) + **ShedLock** | Surdimensionné ; commit par item = même résilience, moins de complexité. |
| Source **MobyGames** | **IGDB** (+ **Steam** profils/news) | API plus riche ; Steam ajoute l'import de bibliothèque. |
| Web **session stateful** | **JWT stateless** en cookie HttpOnly | Une chaîne pour deux clients → API multi-instances sans état serveur. |
| Reco **TF-IDF** | Reco **item-to-item** (overlap de tags) | Plus simple à maîtriser/expliquer, pertinent sur notre volume. |

<div class="grid grid-cols-[1.4fr_1fr] gap-4 mt-8">

<div>
<div class="text-[0.78rem] cp-dim mb-2"><carbon:add-alt class="inline" style="color:oklch(0.67 0.16 137)"/> <strong>Au-delà des specs (bonus non prévus) :</strong></div>
<div class="flex flex-wrap gap-1.5 text-[0.74rem]">
  <span class="cp-chip">OAuth2 / OIDC</span>
  <span class="cp-chip">Connexion &amp; import Steam</span>
  <span class="cp-chip">Easter-eggs</span>
  <span class="cp-chip">Comparaison de profils</span>
  <span class="cp-chip">Leaderboard &amp; streak</span>
  <span class="cp-chip">Export RGPD</span>
</div>
</div>

<div class="cp-card !p-2.5 text-[0.76rem]">
<carbon:calendar class="inline"/> Les <strong>4 phases</strong> des specs ont été suivies. Le planning n'a pas été tenu à 100 % (quelques retards), mais <strong>tout est livré</strong>. Soutenance — 03/06/2026.
</div>

</div>

<div class="mt-2 text-[0.8rem] cp-dim text-center">Aucun écart n'est une régression — chacun a <strong class="text-white">simplifié l'archi</strong> ou <strong class="text-white">enrichi le produit</strong>.</div>

<!--
Message clé : un cahier des charges est une cible, pas un contrat figé. Chaque
divergence est un choix d'ingénierie justifié. On est honnêtes sur le planning :
des retards en route, mais le périmètre est livré en entier, et même au-delà.
-->
