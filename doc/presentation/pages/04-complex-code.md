---
layout: section
number: "04"
---

# Parties de code complexes

Les morceaux qui montrent qu'on a dépassé le CRUD basique.

---
layout: default
---

# <span class="cp-accent-bar">Carte des points techniques</span>

| Sujet | Fichier(s) | Pourquoi c'est intéressant |
|-------|-----------|----------------------------|
| **Double chaîne de sécurité** | `config/SecurityConfig` | Deux `SecurityFilterChain` ordonnées, OAuth2 conditionnel, stateless. |
| **JWT à double source** | `security/JwtAuthenticationFilter` | Un filtre sert header (desktop) **et** cookie HttpOnly (web). |
| **Import async résilient** | `jobs/ImportJobRunner` · `GamePersistenceServiceImpl` | Hors transaction, commit par jeu, suivi de statut. |
| **Gamification anti-abus** | `listeners/GamificationListener` | Déduplication par clé + plafonds glissants 24 h. |
| **Reco item-to-item** | `GameSimilarityServiceImpl` · `GameTagScorer` | Pré-filtrage SQL, scoring pondéré, tri multi-critères. |
| **DI maison (desktop)** | `di/DependencyContainer` | Conteneur d'injection recodé à la main, sans Spring. |
| **Entités polymorphes** | `entities/Like` · `Comment` · `Report` | Une cible multi-types (jeu, review, liste) — modélisation délicate. |

---
layout: default
---

# <span class="cp-accent-bar">① JWT à double source</span>

<div class="text-[0.85rem] cp-dim mb-2"><code>JwtAuthenticationFilter.java</code> — un même filtre sert deux clients aux transports différents.</div>

```java {all|2-4|7-14|16}{lines:true}
private String extractToken(HttpServletRequest request) {
    // 1. Desktop → en-tête Authorization: Bearer <token>
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
        return header.substring(BEARER_PREFIX.length());
    }
    // 2. Web → cookie HttpOnly "checkpoint_token" (protégé du XSS)
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
    }
    return null; // pas de token → requête anonyme
}
```

<div class="text-[0.78rem] cp-dim mt-1"><carbon:idea class="inline"/> Header d'abord, cookie en fallback → le backend reste agnostique du type de client.</div>

---
layout: default
---

# <span class="cp-accent-bar">② Entités polymorphes</span>

<div class="grid grid-cols-2 gap-6 mt-2">

<div>

Un `Like`, un `Comment` ou un `Report` peut cibler **plusieurs types** d'objets (jeu, review, liste…). On modélise la cible par un couple **type + id**.

```java {all|3-4|6-7}{lines:true}
@Entity
public class Like {
    @Enumerated(EnumType.STRING)
    private LikeTargetType targetType; // GAME, REVIEW, LIST…

    private Long targetId;             // l'id de la cible
    private Long userId;
    // + contrainte d'unicité (user, type, id)
}
```

</div>

<div class="flex flex-col gap-2 text-[0.82rem] justify-center">
  <GlowCard icon="i-carbon-types" title="Pourquoi délicat ?">
  Pas de clé étrangère « classique » vers une seule table → l'intégrité repose sur la <strong>logique applicative</strong> + une contrainte d'unicité composite.
  </GlowCard>
  <div class="cp-card !p-2.5"><carbon:checkmark class="inline" style="color:oklch(0.67 0.16 137)"/> Un seul mécanisme de like/commentaire/report réutilisé partout.</div>
  <div class="cp-card !p-2.5"><carbon:warning class="inline" style="color:oklch(0.66 0.16 60)"/> En échange : déduplication &amp; validation côté service.</div>
</div>

</div>
