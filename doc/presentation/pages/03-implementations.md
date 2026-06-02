---
layout: section
number: "03"
---

# Implémentations significatives

Au-delà du CRUD : sécurité, recherche, import, gamification, social, temps réel.

---
layout: default
---

# <span class="cp-accent-bar">Authentification hybride &amp; sécurité</span>

<div class="grid grid-cols-2 gap-6 mt-2">

<div class="flex flex-col gap-2 text-[0.82rem]">
  <GlowCard icon="i-carbon-two-factor-authentication" title="JWT à double source">
  Un même filtre lit le token dans l'en-tête <code>Authorization: Bearer</code> (Desktop) <strong>ou</strong> le cookie <strong>HttpOnly</strong> <code>checkpoint_token</code> (Web).
  </GlowCard>
  <div class="grid grid-cols-2 gap-2">
    <div class="cp-card !p-2.5"><carbon:password class="inline"/> <strong>2FA e-mail</strong> — code 6 chiffres + token intermédiaire</div>
    <div class="cp-card !p-2.5"><carbon:user-role class="inline"/> <strong>RBAC</strong> — <code>@EnableMethodSecurity</code> + règles d'URL</div>
    <div class="cp-card !p-2.5"><carbon:locked class="inline"/> <strong>BCrypt</strong> — hachage des mots de passe</div>
    <div class="cp-card !p-2.5"><carbon:logo-discord class="inline"/> <strong>OAuth2 / OIDC</strong> + refresh tokens</div>
  </div>
</div>

<div>

```mermaid {scale: 0.66}
%%{init: {'theme':'base','themeVariables':{'primaryColor':'#2c2350','primaryTextColor':'#e7e7f2','primaryBorderColor':'#6d54c9','lineColor':'#8b7ad6','fontFamily':'Inter','clusterBkg':'#23283a66','clusterBorder':'#3a4055'}}}%%
flowchart TB
  W["🌐 Web"] -->|cookie HttpOnly| F
  D["🖥️ Desktop"] -->|Bearer header| F
  F["JwtAuthenticationFilter<br/><small>double source · stateless</small>"]
  F --> R{"ROLE ?"}
  R -->|ROLE_ADMIN| A["/api/v1/admin/**"]
  R -->|ROLE_USER| U["/api/v1/**"]
```

<div class="text-[0.74rem] cp-dim mt-1 text-center">Deux <code>SecurityFilterChain</code> ordonnées : WebSocket (order 0) · API (order 1).</div>

</div>

</div>

<div class="mt-2 text-[0.82rem] cp-card !p-2.5"><carbon:game-console class="inline"/> <strong>Connexion Steam</strong> (OpenID) pour importer la bibliothèque Steam du joueur.</div>

<!--
Le point fort : un seul backend, une seule chaîne stateless, mais deux modes
de transport du JWT pour deux clients aux contraintes différentes.
-->

---
layout: two-cols
layoutClass: gap-8
---

# <span class="cp-accent-bar">Recherche full-text</span>

**Hibernate Search + Lucene**

<div class="flex flex-col gap-2 mt-3 text-[0.82rem]">
  <GlowCard icon="i-carbon-search" title="Index Lucene local">
  Indexation via Hibernate Search, en local.
  </GlowCard>
  <div class="cp-card !p-2.5"><code>SearchIndexer</code> (<code>CommandLineRunner</code>) reconstruit l'index complet <strong>au démarrage</strong> → recherche dispo immédiatement.</div>
  <div class="cp-card !p-2.5"><carbon:character-whole-number class="inline"/> Recherche <strong>floue</strong> (tolérante aux fautes) + filtres multicritères.</div>
</div>

::right::

# <span class="cp-accent-bar">Recommandation &amp; social</span>

<div class="flex flex-col gap-2 mt-3 text-[0.82rem]">
  <GlowCard icon="i-carbon-network-4" title="Jeux similaires" color="oklch(0.64 0.15 233)">
  Algorithme <strong>item-to-item</strong> : recouvrement de tags (genres / plateformes / studios) + bonus note &amp; récence.
  </GlowCard>
  <GlowCard icon="i-carbon-collaborate" title="Joueurs compatibles" color="oklch(0.66 0.16 60)">
  Filtrage collaboratif sur les jeux <strong>terminés en commun</strong>.
  </GlowCard>
  <div class="cp-card !p-2.5"><carbon:activity class="inline"/> Feed d'activité, follow/followers, comparaison de profils.</div>
</div>

---
layout: default
---

# <span class="cp-accent-bar">Import de catalogue asynchrone (IGDB)</span>

<div class="flex justify-center mt-1">

```mermaid {scale: 0.52}
%%{init: {'theme':'base','themeVariables':{'primaryColor':'#2c2350','primaryTextColor':'#e7e7f2','primaryBorderColor':'#6d54c9','lineColor':'#8b7ad6','fontFamily':'Inter'}}}%%
sequenceDiagram
  participant Admin as 🖥️ Admin
  participant API
  participant Job as ImportJobRunner
  participant IGDB
  participant DB
  Admin->>API: lance l'import (top / récents)
  API-->>Admin: 202 · job démarré (@Async)
  loop par jeu (~5000)
    Job->>IGDB: fetch
    Job->>DB: commit indépendant
  end
  Admin->>API: GET statut (progression)
```

</div>

<div class="grid grid-cols-4 gap-3 mt-3 text-[0.76rem]">
  <div class="cp-card !p-2.5"><carbon:batch-job class="inline" style="color:oklch(0.7 0.15 286)"/> <strong>Job async</strong> — <code>@Async</code>, pas d'appelant HTTP qui attend.</div>
  <div class="cp-card !p-2.5"><carbon:checkmark class="inline" style="color:oklch(0.67 0.16 137)"/> <strong>Commit par jeu</strong> — un échec réseau n'annule pas tout.</div>
  <div class="cp-card !p-2.5"><carbon:repeat class="inline"/> <strong>Idempotence</strong> — pas de doublons en base.</div>
  <div class="cp-card !p-2.5"><carbon:progress-bar-round class="inline"/> <strong>Progression</strong> exposée via l'API.</div>
</div>

> 💡 Le plus dur : passer d'une approche transactionnelle classique à un modèle **« commit par item »** pour ne rien perdre sur une erreur IGDB à mi-parcours.

<!--
On a choisi un runner async maison plutôt que Spring Batch : même résilience,
beaucoup moins de complexité.
-->

<style>
.cp-card code { font-size: 0.72rem; }
</style>

---
layout: default
---

# <span class="cp-accent-bar">Gamification événementielle</span>

<div class="grid grid-cols-[1fr_1.1fr] gap-6 mt-2">

<div>

Architecture **event-driven** — `events/` (17 événements) + `listeners/`.

```mermaid {scale: 0.66}
%%{init: {'theme':'base','themeVariables':{'primaryColor':'#2c2350','primaryTextColor':'#e7e7f2','primaryBorderColor':'#6d54c9','lineColor':'#8b7ad6','fontFamily':'Inter'}}}%%
flowchart LR
  A["Termine un jeu<br/>Écrit une review<br/>Suit quelqu'un"] -->|publie| E((Event))
  E --> L1["GamificationListener<br/>→ XP / niveau"]
  E --> L2["BadgeListener<br/>→ badges + easter-eggs"]
```

</div>

<div class="flex flex-col gap-2 text-[0.82rem]">
  <GlowCard icon="i-carbon-trophy" title="XP, niveaux, badges">
  Un listener <strong>asynchrone</strong> attribue l'XP à chaque événement publié.
  </GlowCard>
  <GlowCard icon="i-carbon-security" title="Anti-triche intégré" color="oklch(0.58 0.18 27)">
  <strong>Déduplication par clé</strong> — resuivre ne re-crédite pas. <strong>Plafonds glissants</strong> : max 10 « review likée » / 24 h.
  </GlowCard>
  <div class="cp-card !p-2.5"><carbon:star class="inline" style="color:oklch(0.66 0.16 60)"/> Badges débloqués par événement — y compris des <strong>easter-eggs</strong> cachés.</div>
</div>

</div>

---
layout: default
---

# <span class="cp-accent-bar">Temps réel &amp; tâches planifiées</span>

<div class="grid grid-cols-3 gap-4 mt-6">

<GlowCard icon="i-carbon-notification" title="WebSockets (STOMP)" color="oklch(0.64 0.15 233)">
Notifications en <strong>temps réel</strong> poussées au client.
</GlowCard>

<GlowCard icon="i-carbon-time" title="Tâches @Scheduled" color="oklch(0.66 0.16 60)">
Import de news (RSS / Steam), refresh des profils Steam, nettoyage des refresh tokens.
</GlowCard>

<GlowCard icon="i-carbon-locked" title="ShedLock" color="oklch(0.58 0.21 286)">
Verrou <strong>distribué</strong> : une tâche planifiée ne s'exécute <strong>qu'une fois</strong>, même avec plusieurs instances.
</GlowCard>

</div>

<div class="mt-7 text-center cp-card !p-3 text-[0.88rem]">
<carbon:idea class="inline" style="color:oklch(0.66 0.18 286)"/> ShedLock est déjà un <strong>choix « multi-instances »</strong> : on prépare le terrain pour le scaling horizontal (voir §05).
</div>
