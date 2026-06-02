---
layout: default
---

# <span class="cp-accent-bar">Pitch &amp; contexte — le problème, la solution</span>

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
<strong>trois composants hétérogènes</strong> : deux clients, une API.
</div>

</div>

<div class="flex flex-col gap-3 justify-center">

<GlowCard v-click="1" icon="i-carbon-cloud-foundry" title="API REST" color="oklch(0.58 0.21 286)">
<strong>Spring Boot 3.5 / Java 21.</strong> Cœur métier : logique, sécurité, persistance. Consommée par les deux clients.
</GlowCard>

<GlowCard v-click="2" icon="i-carbon-logo-react" title="Application Web — l'Utilisateur" color="oklch(0.64 0.15 233)">
<strong>TanStack Start / React 19.</strong> Catalogue, social, gamification. SSR pour le SEO.
</GlowCard>

<GlowCard v-click="3" icon="i-carbon-application" title="Application Desktop — l'Administrateur" color="oklch(0.66 0.16 60)">
<strong>JavaFX / Java 24.</strong> Modération, catalogue, import, analytics.
</GlowCard>

</div>

</div>

<div v-click="4" class="mt-5 text-center text-[0.95rem] cp-card !p-3">
<carbon:idea class="inline" style="color:oklch(0.66 0.18 286)"/> Le vrai défi n'est pas une fonctionnalité isolée : c'est <strong class="text-white">orchestrer trois technologies très différentes autour d'une seule API sécurisée et stateless</strong>.
</div>

<!--
Bonjour, nous sommes Enzo et Gauthier. CheckPoint répond à un problème concret :
les joueurs ont leur bibliothèque éclatée entre Steam, consoles, stores.
Le vrai défi du projet n'est pas une fonctionnalité isolée, c'est l'orchestration
sécurisée de trois technologies très différentes autour d'une seule API.
-->
