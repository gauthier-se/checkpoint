---
layout: section
number: "01"
---

# Pitch &amp; contexte

Le problème, la solution, les acteurs.

---
layout: default
---

# <span class="cp-accent-bar">Le problème &amp; la solution</span>

<div class="grid grid-cols-2 gap-6 mt-3">

<div>

<h3 class="flex items-center gap-2"><carbon:warning-alt class="text-amber-400" /> Le problème</h3>

<div class="cp-card mt-2 !p-4 text-[0.92rem] leading-relaxed">
Les joueurs ont leur bibliothèque <strong>éclatée</strong> entre Steam,
consoles, stores… Aucun point unique pour suivre sa <strong>progression</strong>,
son <strong>backlog</strong>, sa <strong>wishlist</strong> et discuter avec une
communauté.
</div>

<h3 class="flex items-center gap-2"><carbon:checkmark-filled style="color: oklch(0.67 0.16 137)" /> La solution</h3>

<div class="cp-card mt-2 !p-4 text-[0.92rem] leading-relaxed">
<strong>CheckPoint</strong> — une plateforme <strong>centralisée</strong> et
<strong>indépendante des stores</strong>, organisée autour de
<strong>trois composants hétérogènes</strong>.
</div>

</div>

<div class="flex flex-col gap-3 justify-center">

<GlowCard icon="i-carbon-cloud-foundry" title="API REST" color="oklch(0.58 0.21 286)">
<strong>Spring Boot 3.5 / Java 21.</strong> Cœur métier : logique, sécurité, persistance. Consommée par les deux clients.
</GlowCard>

<GlowCard icon="i-carbon-logo-react" title="Application Web" color="oklch(0.64 0.15 233)">
<strong>TanStack Start / React 19.</strong> Interface joueur (catalogue, social, gamification). SSR pour le SEO.
</GlowCard>

<GlowCard icon="i-carbon-application" title="Application Desktop" color="oklch(0.66 0.16 60)">
<strong>JavaFX / Java 24.</strong> Console d'administration : modération, catalogue, import, analytics.
</GlowCard>

</div>

</div>

<!--
Le vrai défi du projet n'est pas une fonctionnalité isolée, c'est
l'orchestration sécurisée de trois technologies très différentes autour
d'une seule API.
-->

---
layout: default
---

# <span class="cp-accent-bar">Deux acteurs, une API</span>

<div class="grid grid-cols-[1fr_auto_1fr] items-center gap-6 mt-8">

<div class="flex flex-col gap-3">
  <GlowCard icon="i-carbon-user" title="L'Utilisateur" color="oklch(0.64 0.15 233)">
  Joue, suit sa progression, note, échange. <br/>→ via l'<strong>application Web</strong>.
  </GlowCard>
  <div class="flex gap-2 flex-wrap justify-center">
    <TechPill icon="i-carbon-search">Recherche floue</TechPill>
    <TechPill icon="i-carbon-bookmark">Backlog / Wishlist</TechPill>
    <TechPill icon="i-carbon-trophy">XP &amp; badges</TechPill>
  </div>
</div>

<div class="flex flex-col items-center text-center px-2">
  <img :src="'/logo.svg'" class="w-16 h-16 mb-2" />
  <div class="cp-chip">API REST sécurisée</div>
  <carbon:arrow-left class="text-2xl mt-3 opacity-50" />
  <carbon:arrow-right class="text-2xl opacity-50" />
</div>

<div class="flex flex-col gap-3">
  <GlowCard icon="i-carbon-user-admin" title="L'Administrateur" color="oklch(0.66 0.16 60)">
  Modère, gère le catalogue, lance les imports. <br/>→ via la <strong>console Desktop</strong>.
  </GlowCard>
  <div class="flex gap-2 flex-wrap justify-center">
    <TechPill icon="i-carbon-rule">Modération</TechPill>
    <TechPill icon="i-carbon-download">Import IGDB</TechPill>
    <TechPill icon="i-carbon-analytics">Analytics</TechPill>
  </div>
</div>

</div>

<div class="text-center mt-9 text-[0.95rem] cp-dim">
Un <strong class="text-white">seul backend</strong> sert deux clients très différents — c'est tout l'enjeu architectural.
</div>
