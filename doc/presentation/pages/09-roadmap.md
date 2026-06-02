---
layout: section
number: "09"
---

# Roadmap &amp; évolutions

Le produit est conçu pour vivre après la soutenance.

---
layout: default
---

# <span class="cp-accent-bar">Évolutions techniques &amp; produit</span>

<div class="grid grid-cols-2 gap-6 mt-2">

<div>

<h3 class="flex items-center gap-2"><carbon:tools /> Technique (issues Linear)</h3>

<div class="flex flex-col gap-2 mt-2 text-[0.82rem]">
  <GlowCard icon="i-carbon-cloud-satellite" title="Migration uploads → S3" color="oklch(0.66 0.16 60)">
  <span class="cp-chip">en cours</span> — finalise la scalabilité multi-instances (voir §05).
  </GlowCard>
  <div class="cp-card !p-2.5"><carbon:data-backup class="inline"/> Backups PostgreSQL automatisés vers S3.</div>
  <div class="cp-card !p-2.5"><carbon:scale class="inline"/> Montée en charge : ajout de workers Swarm selon le trafic.</div>
</div>

</div>

<div>

<h3 class="flex items-center gap-2"><carbon:roadmap /> Produit (page publique)</h3>

<div class="flex flex-col gap-2 mt-2 text-[0.82rem]">
  <div class="cp-card !p-2.5"><span class="cp-chip">Court terme</span> Traduction multi-langues (i18n).</div>
  <div class="cp-card !p-2.5"><span class="cp-chip">Moyen terme</span> Listes collaboratives entre amis · suivre ses studios / développeurs préférés.</div>
  <div class="cp-card !p-2.5"><span class="cp-chip">Long terme</span> Messagerie privée · <strong>application mobile native</strong> (iOS &amp; Android).</div>
</div>

<div class="text-[0.74rem] cp-dim mt-2">Page Roadmap publique : <code>web/src/routes/_app/roadmap.tsx</code></div>

</div>

</div>

> 💡 L'API REST stateless qu'on a construite est aussi ce qui rend une **app mobile native** réaliste demain : exactement le même backend, consommé par un 3ᵉ type de client — comme déjà fait pour le web et le desktop.
