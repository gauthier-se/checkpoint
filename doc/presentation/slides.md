---
theme: default
title: CheckPoint — Soutenance
info: |
  ## CheckPoint
  Support de soutenance — plateforme de suivi de bibliothèque de jeux vidéo.
  Enzo CHABOISSEAU · Gauthier SEYZERIAT--MEYER — CCI Campus Alsace.
author: Enzo CHABOISSEAU · Gauthier SEYZERIAT--MEYER
colorSchema: dark
fonts:
  sans: Inter
  mono: JetBrains Mono
  weights: '300,400,500,600,700,800'
transition: slide-left
drawings:
  persist: false
seoMeta:
  ogTitle: CheckPoint — Soutenance
  ogDescription: Trois clients hétérogènes autour d'une API REST sécurisée et stateless.
duration: 35min
---

<!-- ===================== COVER ===================== -->

<div class="flex items-center gap-5">
  <img :src="'/logo.svg'" class="w-20 h-20 drop-shadow-lg" alt="CheckPoint" />
  <div>
    <h1 class="!text-6xl cp-text-gradient !mb-0 !leading-none font-extrabold">CheckPoint</h1>
    <p class="!mt-2 text-lg cp-dim">Your unified video game library tracker</p>
  </div>
</div>

<p class="max-w-2xl text-base leading-relaxed opacity-90 mt-6">
Trois clients hétérogènes — une <strong>application Web</strong>, une <strong>console Desktop</strong> et une <strong>API REST</strong> — orchestrés autour d'une seule plateforme, sécurisée et <strong>stateless</strong>.
</p>

<div class="flex gap-2 mt-6 flex-wrap max-w-3xl">
  <TechPill icon="i-carbon-cloud-foundry">Spring Boot 3.5 · Java 21</TechPill>
  <TechPill icon="i-carbon-logo-react">TanStack Start · React 19</TechPill>
  <TechPill icon="i-carbon-application">JavaFX · Java 24</TechPill>
  <TechPill icon="i-carbon-data-base">PostgreSQL</TechPill>
  <TechPill icon="i-carbon-container-services">Dokploy · Docker Swarm</TechPill>
</div>

<div class="mt-8 pt-4 border-t border-[var(--cp-border)] text-sm cp-dim max-w-3xl">
  <span class="opacity-90">Enzo CHABOISSEAU · Gauthier SEYZERIAT--MEYER</span>
  &nbsp;—&nbsp; CCI Campus Alsace &nbsp;·&nbsp; Soutenance 03/06/2026
</div>

<!--
Bonjour, nous sommes Enzo et Gauthier. CheckPoint répond à un problème concret :
les joueurs ont leur bibliothèque éclatée entre Steam, consoles, stores…
Notre vrai défi technique : orchestrer trois technologies très différentes
autour d'une seule API sécurisée et stateless.
-->

---
layout: default
class: cp-agenda
---

# Sommaire

<div class="grid grid-cols-2 gap-x-8 gap-y-2.5 mt-5">

<div class="cp-agenda__item"><span class="cp-agenda__n">01</span><carbon:idea /> Pitch &amp; contexte</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">02</span><carbon:network-3 /> Architecture du projet</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">03</span><carbon:checkmark-outline /> Implémentations significatives</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">04</span><carbon:code /> Parties de code complexes</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">05</span><carbon:cloud /> Pensé cloud-native : stateless</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">06</span><carbon:compare /> Specs vs réalisé (écarts)</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">07</span><carbon:flow /> Organisation : Linear &amp; GitFlow</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">08</span><carbon:test-tool /> Stratégie qualité</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">09</span><carbon:roadmap /> Roadmap &amp; évolutions</div>
<div class="cp-agenda__item"><span class="cp-agenda__n">10</span><carbon:chart-line /> Chiffres clés &amp; démo</div>

</div>

<style>
.cp-agenda__item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  font-size: 1.05rem;
  padding: 0.55rem 0.85rem;
  border-radius: 10px;
  border: 1px solid var(--cp-border);
  background: var(--cp-card);
}
.cp-agenda__item svg { color: oklch(0.7 0.15 286); }
.cp-agenda__n {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  color: oklch(0.62 0.2 286);
  font-size: 0.95rem;
}
</style>

<!--
Voici notre fil de présentation. Le fil conducteur, c'est l'architecture
cloud-native stateless qui justifie la plupart de nos choix techniques.
-->

---
src: ./pages/01-pitch.md
---

---
src: ./pages/02-architecture.md
---

---
src: ./pages/03-implementations.md
---

---
src: ./pages/04-complex-code.md
---

---
src: ./pages/05-cloud-native.md
---

---
src: ./pages/06-specs.md
---

---
src: ./pages/07-organisation.md
---

---
src: ./pages/08-quality.md
---

---
src: ./pages/09-roadmap.md
---

---
src: ./pages/10-chiffres-demo.md
---

---
layout: cover
class: text-center
---

<img :src="'/logo.svg'" class="w-16 h-16 mx-auto mb-5" alt="CheckPoint" />

<h1 class="cp-text-gradient !text-5xl">Merci !</h1>

<div class="mt-8 text-2xl font-semibold cp-text-gradient">Questions ?</div>

<!--
Phrase de conclusion : CheckPoint, c'est trois clients hétérogènes derrière une
seule API sécurisée et stateless ; on a tenu le périmètre fonctionnel, et nos
écarts par rapport aux specs sont des choix d'ingénierie assumés.
-->
