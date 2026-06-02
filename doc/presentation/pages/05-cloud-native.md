---
layout: section
number: "05"
---

# Pensé cloud-native

Stateless &amp; scalabilité — le fil conducteur architectural du projet.

---
layout: default
---

# <span class="cp-accent-bar">Pourquoi on a retiré les sessions</span>

<div class="text-[0.85rem] mb-3">Le cahier des charges prévoyait une auth <strong>stateful</strong> (session). On a fait le choix inverse : <strong>tout est stateless (JWT)</strong>.</div>

<div class="grid grid-cols-2 gap-6">

<GlowCard icon="i-carbon-data-base-alt" title="Stateful (session)" color="oklch(0.58 0.18 27)">

État d'auth stocké **en mémoire serveur**. Avec plusieurs instances :

<div class="flex flex-col gap-1.5 mt-2 text-[0.78rem]">
  <div>❌ sticky sessions <strong>ou</strong> store externe (Redis…)</div>
  <div>❌ complexité &amp; point de contention</div>
  <div>❌ une instance devient « spéciale »</div>
</div>

</GlowCard>

<GlowCard icon="i-carbon-cloud" title="Stateless (JWT) — notre choix">

L'API **ne garde aucun état** : le JWT porte l'identité, validé à chaque requête.

<div class="flex flex-col gap-1.5 mt-2 text-[0.78rem]">
  <div>✅ n'importe quelle instance traite n'importe quelle requête</div>
  <div>✅ <strong>scaling horizontal</strong> sans config de partage d'état</div>
  <div>✅ sécurité web : cookie <strong>HttpOnly + SameSite</strong> (pas de localStorage)</div>
</div>

</GlowCard>

</div>

> 💡 On a supprimé les sessions **pour que l'app soit nativement multi-instances** : aucune instance n'est spéciale, on en ajoute ou retire à chaud.

---
layout: default
---

# <span class="cp-accent-bar">Déploiement multi-instances</span>

<div class="grid grid-cols-[1.2fr_1fr] gap-6 mt-2">

<div>

```mermaid {scale: 0.66}
%%{init: {'theme':'base','themeVariables':{'primaryColor':'#2c2350','primaryTextColor':'#e7e7f2','primaryBorderColor':'#6d54c9','lineColor':'#8b7ad6','fontFamily':'Inter','clusterBkg':'#23283a66','clusterBorder':'#3a4055'}}}%%
flowchart TB
  T["Traefik · load-balancing"]
  subgraph swarm["Docker Swarm (via Dokploy)"]
    direction LR
    subgraph m["Manager nodes"]
      M1["control plane"]
    end
    subgraph w["Worker nodes"]
      A1["API #1"]
      A2["API #2"]
      A3["API #3"]
    end
  end
  T --> A1 & A2 & A3
  M1 -.orchestre.-> w
```

</div>

<div class="flex flex-col gap-2 text-[0.82rem]">
  <div class="cp-card !p-2.5"><strong>Manager nodes</strong> — orchestrent le cluster, planifient les conteneurs.</div>
  <div class="cp-card !p-2.5"><strong>Worker nodes</strong> — exécutent les conteneurs applicatifs.</div>
  <GlowCard icon="i-carbon-scale" title="Scaler = ajouter des nœuds">
  On ajoute des <strong>workers</strong> (charge) ou des <strong>managers</strong> (HA) <strong>sans retoucher le code</strong> — parce que l'API est stateless.
  </GlowCard>
</div>

</div>

---
layout: default
---

# <span class="cp-accent-bar">Stockage découplé — vers S3</span>

<div class="grid grid-cols-[1fr_1fr] gap-6 mt-1">

<div>

Les uploads sont aujourd'hui sur **disque local**, mais **derrière une interface** `StorageService`. Migrer vers S3 = **une nouvelle implémentation**, sans toucher au reste.

````md magic-move {lines: true}
```java
// 1. Le contrat — tout le code dépend de CETTE interface
public interface StorageService {
    String store(MultipartFile file, String directory);
    void delete(String path);
}
```

```java
// 2. Aujourd'hui — disque local
@Service
public class LocalStorageServiceImpl implements StorageService {
    public String store(MultipartFile file, String directory) {
        // écrit le fichier sur le système de fichiers local
    }
    public void delete(String path) { /* … */ }
}
```

```java
// 3. Demain — S3, AUCUN autre fichier ne change
@Service
public class S3StorageServiceImpl implements StorageService {
    public String store(MultipartFile file, String directory) {
        // PutObject vers un bucket S3 → stockage stateless
    }
    public void delete(String path) { /* … */ }
}
```
````

</div>

<div class="flex flex-col gap-2 text-[0.82rem] justify-center">
  <div class="cp-card !p-2.5"><carbon:data-backup class="inline"/> <strong>PostgreSQL</strong> sauvegardée vers un <strong>bucket S3</strong> (backups Dokploy).</div>
  <GlowCard icon="i-carbon-cloud-satellite" title="Le dernier verrou" color="oklch(0.66 0.16 60)">
  Tant que les uploads sont sur disque local, deux instances ne partagent pas les fichiers. <strong>Une fois sur S3 → scalabilité complète.</strong>
  </GlowCard>
  <div class="cp-chip"><carbon:in-progress class="inline"/> &nbsp;Issue en cours</div>
</div>

</div>

> 💡 Notre architecture est stateless **côté calcul**. Le seul état restant est dans des services externalisables — Postgres &amp; le stockage de fichiers — et la migration des uploads vers S3 finalise le tableau.
