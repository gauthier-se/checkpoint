# CheckPoint — Préparation aux questions sur le code

> Antisèche pour la soutenance. Pour chaque morceau de code présenté : ce qu'il
> fait, **pourquoi** on a fait ces choix, et les **questions-pièges** probables du
> jury avec une réponse prête. Les fichiers sont cités en `chemin:lignes`.

**Sommaire**

1. [JWT à double source + double chaîne de sécurité](#1-jwt-à-double-source--double-chaîne-de-sécurité)
2. [Import asynchrone résilient (commit par jeu)](#2-import-asynchrone-résilient-commit-par-jeu)
3. [Gamification événementielle anti-triche](#3-gamification-événementielle-anti-triche)
4. [Recommandation item-to-item](#4-recommandation-item-to-item)
5. [Bonus (non montrés en slide mais possibles en question)](#5-bonus-non-montrés-en-slide-mais-possibles-en-question)

---

## 1. JWT à double source + double chaîne de sécurité

**Fichiers :** `api/.../config/SecurityConfig.java` · `api/.../security/JwtAuthenticationFilter.java`

### Ce que ça fait

Une **seule API** sert deux clients très différents (le **web** React et le
**desktop** JavaFX). Le problème : ils transportent le jeton d'authentification (JWT)
différemment.

- Le **desktop** envoie l'en-tête HTTP `Authorization: Bearer <token>`.
- Le **web** envoie un **cookie `HttpOnly`** nommé `checkpoint_token`.

Le `JwtAuthenticationFilter` essaie **les deux sources, dans l'ordre** : header d'abord,
puis cookie en repli (`JwtAuthenticationFilter.java:82-98`).

```java
private String extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
        return header.substring(BEARER_PREFIX.length());   // Desktop
    }
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();                   // Web
            }
        }
    }
    return null;
}
```

Le reste du filtre (`doFilterInternal`, l. 42-73) : si un token est présent et **n'est
pas un token intermédiaire** (cas 2FA, voir plus bas), on extrait l'utilisateur, on
vérifie la validité, et on place l'authentification dans le `SecurityContextHolder`.
Le filtre hérite de `OncePerRequestFilter` → garanti **une seule exécution par requête**.

### Les deux chaînes de filtres (`SecurityConfig.java`)

On déclare **deux `SecurityFilterChain`** ordonnées :

- **Chaîne 0** (`@Order(0)`, `securityMatcher("/ws/**")`) : les WebSockets. On laisse
  passer le *handshake* HTTP ; l'authentification réelle se fait au niveau du protocole
  STOMP (dans `WebSocketAuthInterceptor`).
- **Chaîne 1** (`@Order(1)`, `securityMatcher("/api/v1/**")`) : l'API REST classique,
  avec `addFilterBefore(jwtAuthenticationFilter, ...)`.

Le point clé, présent dans **les deux** chaînes :

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

→ **aucune session serveur n'est créée.** C'est *la* ligne qui rend l'API stateless,
donc multi-instances (fil conducteur cloud-native).

### Pourquoi ces choix

- **Pourquoi stateless / pas de session ?** Pour pouvoir **scaler horizontalement** :
  comme l'API ne garde aucun état en mémoire, n'importe quelle instance peut traiter
  n'importe quelle requête. Pas besoin de *sticky sessions* ni de store de sessions
  partagé (Redis). Le cahier des charges prévoyait une session stateful ; on a fait le
  choix inverse, assumé.
- **Pourquoi un cookie HttpOnly pour le web ?** Un cookie `HttpOnly` **n'est pas
  lisible en JavaScript** → protégé contre le vol de token par XSS. C'est plus sûr que
  de stocker le JWT dans le `localStorage`.
- **Pourquoi un header Bearer pour le desktop ?** Une application JavaFX n'a pas de
  « jar de cookies » de navigateur ; le header est la façon standard et la plus simple
  côté client lourd.
- **Pourquoi CSRF désactivé ?** Le CSRF est une attaque qui exploite l'envoi
  **automatique** des cookies par le navigateur. On s'en protège avec `SameSite=Lax`
  sur le cookie (le navigateur ne l'envoie pas depuis un autre site), donc le jeton
  CSRF classique devient inutile. Côté desktop, pas de cookie automatique du tout.

### Questions-pièges probables

- **« Si quelqu'un vole le JWT, il peut tout faire ? »** Oui pendant la durée de vie du
  token — c'est pour ça qu'il est court et qu'on a des **refresh tokens** persistés (et
  nettoyés par tâche planifiée). Le cookie `HttpOnly` empêche justement le vol par XSS.
- **« Comment révoque-t-on un JWT puisqu'il est stateless ? »** Honnêtement, c'est la
  limite du stateless : on ne révoque pas un access token individuellement, on joue sur
  une **durée de vie courte** + la révocation du **refresh token** côté base. C'est un
  compromis assumé.
- **« C'est quoi un token intermédiaire ? »** Pour la **2FA** : après login/mot de passe
  on émet un token « à moitié authentifié » (`isIntermediateToken`) qui ne donne accès
  qu'à l'étape de validation du code à 6 chiffres, pas au reste de l'API. Le filtre
  refuse explicitement ce token (`JwtAuthenticationFilter.java:50`).
- **« Pourquoi deux chaînes et pas une ? »** Parce que WebSocket et REST ont des modèles
  d'authentification différents (handshake STOMP vs filtre HTTP par requête). Les séparer
  par `securityMatcher` + `@Order` évite des règles fourre-tout illisibles.

---

## 2. Import asynchrone résilient (commit par jeu)

**Fichiers :** `api/.../jobs/ImportJobRunner.java` · `api/.../services/impl/GameImportServiceImpl.java`
· `api/.../services/impl/GamePersistenceServiceImpl.java` · `api/.../config/AsyncConfig.java`

### Ce que ça fait

L'admin lance depuis le desktop l'import de **jusqu'à ~5000 jeux** depuis l'API externe
**IGDB**. C'est long (réseau, volume) → on ne peut pas faire ça dans la requête HTTP.

**Le flux :**

1. Le contrôleur démarre un job et **rend la main immédiatement** ; `ImportJobRunner.run()`
   tourne en arrière-plan grâce à `@Async("importExecutor")`.
2. `run()` récupère la liste des jeux depuis IGDB, puis délègue à
   `GameImportService.bulkImport(...)`.
3. `bulkImport` **itère** sur chaque jeu, et appelle `GamePersistenceService.importOne(...)`
   pour **chacun**.

### Le cœur de la résilience

**(a) `ImportJobRunner.run()` capture les erreurs au lieu de les propager** — il n'y a pas
d'appelant HTTP pour les recevoir, donc on les stocke dans l'objet `job` :

```java
@Async("importExecutor")
public void run(ImportJobStatus job) {
    job.setState(JobState.RUNNING);
    try {
        List<IgdbGameDto> games = ...;     // fetch IGDB
        gameImportService.bulkImport(games, job);
        job.setState(JobState.COMPLETED);
    } catch (Exception e) {
        job.setErrorMessage(...);
        job.setState(JobState.FAILED);     // on enregistre l'échec, on ne crashe pas
    } finally {
        job.setFinishedAt(Instant.now());
    }
}
```

**(b) Le service d'orchestration n'est PAS transactionnel**, et la boucle attrape les
erreurs **par jeu** (`GameImportServiceImpl.java:77-114`) :

```java
for (IgdbGameDto dto : games) {
    try {
        if (videoGameRepository.existsByIgdbId(dto.id())) { skipped++; continue; }
        gamePersistenceService.importOne(dto, timeToBeat.get(dto.id()));
        imported++;
    } catch (Exception e) {
        failed++;                          // un jeu rate → on continue les autres
        errors.add(...);
    } finally {
        progress.processed();              // suivi de progression
    }
}
```

**(c) Chaque jeu est commité dans sa propre transaction** (`GamePersistenceServiceImpl.java:60`) :

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public VideoGame importOne(IgdbGameDto dto, IgdbTimeToBeatDto timeToBeat) { ... }
```

`REQUIRES_NEW` = « ouvre une **nouvelle** transaction rien que pour ce jeu ». Donc si le
jeu n° 3001 échoue, **les 3000 déjà importés restent en base.**

**(d) Idempotence (upsert)** : `importOne` cherche d'abord par `findByIgdbId` ; s'il
existe il met à jour, sinon il crée → **pas de doublon**, et l'import est **relançable**.

### Pourquoi ces choix

- **Pourquoi pas une seule grosse transaction ?** Une transaction unique sur 5000 jeux,
  c'est : (1) tout est perdu si le dernier jeu échoue (un `rollback` annule tout), (2) une
  transaction ouverte plusieurs minutes verrouille des ressources et risque le timeout.
  Le « commit par item » donne la **résilience** sans ces inconvénients.
- **Pourquoi un `importExecutor` dédié et mono-thread ?** (`AsyncConfig.java:43-52`,
  `corePoolSize=1`, `maxPoolSize=1`) → il **sérialise** les imports (un seul à la fois,
  on évite deux imports concurrents qui se marchent dessus) et il est **séparé** de
  l'executor de la gamification pour qu'un gros import ne **bloque pas** le traitement
  des événements XP.
- **Pourquoi pré-charger les `timeToBeat` ?** (`prefetchTimeToBeat`, l. 148) → on fait
  **1 requête IGDB pour ~500 jeux** au lieu d'1 requête par jeu → on évite le problème
  N+1 côté API externe (et les quotas de l'API IGDB).

### Lien avec les écarts au cahier des charges

La spec prévoyait **Spring Batch**. On a jugé que c'était **surdimensionné** (chunking,
job repository, reprise sur incident lourde) pour notre besoin. Un `@Async` + commit par
item + suivi de statut nous donne la résilience voulue avec **beaucoup moins de
complexité**. C'est un écart **technique assumé**.

### Questions-pièges probables

- **« Et si l'appli redémarre pendant l'import ? »** L'état du job est en mémoire (objet
  `ImportJobStatus`) → un redémarrage perd le suivi en cours. Mais comme l'import est
  **idempotent** (upsert), il suffit de le **relancer** : les jeux déjà importés sont
  détectés et passés (`skipped`). En production, **ShedLock** garantit aussi qu'une tâche
  planifiée ne s'exécute pas en double sur plusieurs instances.
- **« Comment le desktop suit la progression ? »** Le `job` expose un compteur
  (`processed/imported/skipped/failed`) via une route API que le client interroge
  (polling) pour afficher la barre de progression.
- **« Pourquoi `existsByIgdbId` dans la boucle plutôt que dans `importOne` ? »** En import
  de masse on **saute** les jeux déjà présents (rapide). `importOne` fait quand même
  l'upsert complet, car il sert aussi à d'autres chemins (import ciblé par IDs, recherche).

---

## 3. Gamification événementielle anti-triche

**Fichiers :** `api/.../listeners/GamificationListener.java`
· `api/.../services/impl/GamificationServiceImpl.java`

### Ce que ça fait

Quand un utilisateur fait une action « méritante » (finir un jeu, écrire une review,
suivre quelqu'un…), il gagne de l'**XP** et peut monter de **niveau** / débloquer un
**badge**. On a fait ça en **architecture événementielle** : le code métier ne calcule
pas l'XP lui-même, il **publie un événement**, et un *listener* asynchrone réagit.

```java
@Async
@EventListener
public void onGameFinished(GameFinishedEvent event) {
    gamificationService.addXp(event.getUserId(), GAME_FINISHED_XP);  // 100 XP
}
```

- `@EventListener` = la méthode réagit à un type d'événement Spring.
- `@Async` = elle s'exécute **sur un autre thread** (l'executor `gamification-`), donc
  elle **ne ralentit pas** l'action de l'utilisateur (terminer un jeu répond tout de suite,
  l'XP est crédité en arrière-plan).

### L'anti-triche à deux niveaux

**Niveau 1 — dédup par contrainte d'unicité en base** (`GamificationServiceImpl.java:62-75`).
Chaque attribution est enregistrée comme un `XpGrant` avec une **clé unique
`(user_id, event_type, target_id)`**. On tente l'insertion et on attrape la violation de
contrainte :

```java
try {
    xpGrantRepository.saveAndFlush(new XpGrant(user, eventType, targetId, xpAmount));
} catch (DataIntegrityViolationException ex) {
    // déjà accordé pour cette cible → on ne crédite pas une 2ᵉ fois
    return;
}
applyXp(userId, xpAmount);
```

→ Suivre quelqu'un, se désabonner, le re-suivre **ne donne pas l'XP deux fois** : la clé
`(moi, USER_FOLLOWED, lui)` existe déjà. C'est la base qui garantit l'unicité, donc c'est
**robuste même en cas de concurrence** (deux requêtes simultanées).

**Niveau 2 — plafond glissant 24 h** (`GamificationListener.java:127-146`), pour l'XP
« ma review a été likée » (sinon on s'auto-like en boucle avec des faux comptes) :

```java
long grantsLast24h = xpGrantRepository.countByUserIdAndEventTypeAfter(
        event.getReviewAuthorId(), XpEventType.REVIEW_LIKED,
        LocalDateTime.now().minusHours(24));
if (grantsLast24h >= REVIEW_LIKED_DAILY_CAP) {   // max 10 / 24 h
    return;
}
```

### Pourquoi ces choix

- **Pourquoi événementiel et pas un appel direct ?** **Découplage** : le code qui termine
  un jeu n'a pas à connaître la gamification, les notifications, les badges… Il publie
  *un* événement, et **plusieurs** listeners y réagissent indépendamment
  (`GamificationListener` pour l'XP, `BadgeListener` pour les badges, `NotificationListener`
  pour les notifs). Ajouter une réaction = ajouter un listener, **sans toucher** au code
  métier → illustration du principe **ouvert/fermé** (SOLID).
- **Pourquoi `@Async` ?** L'utilisateur ne doit pas attendre le calcul d'XP. Et ça encaisse
  les pics (file d'attente, `queueCapacity=100`).
- **Pourquoi la dédup en base et pas en Java ?** Une vérification « est-ce que ça existe ? »
  en Java suivie d'une insertion a une **race condition** (deux requêtes passent le test en
  même temps). La **contrainte d'unicité** est atomique : la base tranche, c'est infaillible.
- **Le level-up est lui-même un événement** (`UserLeveledUpEvent`, publié dans `applyXp`)
  → qui déclenche notification + éventuels badges. L'architecture événementielle se chaîne.

### Questions-pièges probables

- **« Et si le listener async échoue ? L'utilisateur perd son XP ? »** Oui, dans ce cas
  l'XP de cet événement-là est perdu (c'est le compromis de l'asynchrone « fire and
  forget »). On l'accepte car l'XP n'est pas une donnée critique comme un paiement ; les
  erreurs sont loguées. On pourrait ajouter un retry / une dead-letter si besoin.
- **« Pourquoi un audit `XpGrant` en base et pas juste un compteur ? »** Parce qu'il sert à
  **deux choses** : la dédup (clé unique) **et** le plafond glissant (compter les grants
  des dernières 24 h). C'est une trace auditable de chaque gain.
- **« La formule de niveau ? »** `while (newXp >= newLevel * 1000) newLevel++` → palier de
  1000 XP × niveau (niveau 1→2 à 1000, 2→3 à 2000…). La boucle gère un gain qui ferait
  monter **plusieurs niveaux d'un coup**.

---

## 4. Recommandation item-to-item

**Fichiers :** `api/.../services/impl/GameSimilarityServiceImpl.java`
· `api/.../services/impl/GameTagScorer.java`

### Ce que ça fait

Sur la fiche d'un jeu, on affiche des **« jeux similaires »**. Approche **item-to-item**
basée sur le **recouvrement de tags** (genres, plateformes, studios), pas du machine
learning. Le principe : deux jeux se ressemblent s'ils partagent beaucoup de tags.

### Le pipeline (`GameSimilarityServiceImpl.getSimilarGames`)

1. **Charger le jeu source** avec ses relations (genres / plateformes / studios).
2. **Pré-filtrer en SQL** : `findSimilarCandidateIds(...)` ne ramène que les jeux qui
   partagent **au moins un genre ou un studio**, **plafonné à 50 candidats**
   (`CANDIDATE_POOL_CAP`). → On ne score pas toute la base, juste un petit pool pertinent.
3. **Scorer chaque candidat en mémoire** via `GameTagScorer.score(...)`.
4. **Trier** par score décroissant, puis par titre (départage stable), prendre le top N
   (12 par défaut, 30 max).
5. **Charger les cartes** (`GameCardDto`) des gagnants pour la réponse.

### Le scoring (`GameTagScorer.score`, l. 48-73)

Somme pondérée des tags partagés + bonus :

```java
double total = genreContribution      // genres × 1.0   (le plus discriminant)
             + platformContribution   // plateformes × 0.4
             + companyContribution    // studios × 0.6
             + rating * 0.2           // départage par la note moyenne
             + recency;               // +0.5 si sorti il y a < 2 ans
```

- **Poids** : genre (1.0) > studio (0.6) > plateforme (0.4) — un même **genre** est bien
  plus signifiant qu'une même **plateforme** (presque tout sort sur PC/PS5).
- **Tiebreaker note** (×0.2) : à tags égaux, on préfère le jeu mieux noté.
- **Boost récence** (+0.5) : à score proche, on remonte les sorties récentes.

### Le détail « monde réel » : `ensureNonEmpty` (l. 97-102)

```java
public static Collection<UUID> ensureNonEmpty(Set<UUID> ids) {
    if (ids.isEmpty()) {
        return List.of(new UUID(0L, 0L));   // UUID sentinelle qui ne matche rien
    }
    return ids;
}
```

Certains dialectes Hibernate **rejettent une clause SQL `IN ()` vide**. Si le jeu source
n'a aucun studio renseigné, on injecte un **UUID bidon** qui ne correspond à aucune ligne
→ la requête se réduit proprement à « faux » au lieu de **planter**. Petit garde-fou qui
évite un bug en production sur des données incomplètes.

### Pourquoi ces choix

- **Pourquoi pas du TF-IDF / Hibernate Search (prévu dans la spec) ?** Le tag-overlap est
  **plus simple à comprendre, à expliquer et à régler**, et donne des résultats pertinents
  sur **notre volume** de jeux. Écart technique assumé (TF-IDF aurait été de la complexité
  pour un gain non démontré chez nous).
- **Pourquoi pré-filtrer en SQL avant de scorer ?** **Performance** : scorer toute la base
  en mémoire serait coûteux. On laisse la base faire ce qu'elle fait de mieux (filtrer/
  indexer) et on ne ramène que ~50 candidats à scorer finement en Java.
- **Pourquoi `GameTagScorer` est une classe utilitaire `static` partagée ?** Parce que le
  **même** scoring sert deux services : les « jeux similaires » (graine = 1 jeu) **et** les
  recommandations personnalisées (graine = profil de goûts de l'utilisateur). On factorise
  → **DRY** (Don't Repeat Yourself), un seul endroit à maintenir/tester.

### Questions-pièges probables

- **« Comment as-tu choisi les poids 1.0 / 0.6 / 0.4 ? »** Empiriquement, par ressenti sur
  nos données (genre = signal fort, plateforme = signal faible). Ce sont des constantes
  isolées en haut de classe, **faciles à ajuster** sans toucher à la logique.
- **« C'est un vrai moteur de reco ? »** C'est du **filtrage par contenu** (content-based)
  simple, pas du collaboratif ni du ML. Pour les **joueurs compatibles**, en revanche, on
  fait du **filtrage collaboratif** (jeux terminés en commun) — autre algo.
- **« Et le cold start (jeu sans tags) ? »** Géré explicitement : si le jeu source n'a ni
  genre ni studio, on renvoie une liste vide (l. 65-68) plutôt qu'un résultat aléatoire.

---

## 5. Bonus (non montrés en slide mais possibles en question)

### `StorageService` — l'abstraction du stockage (cloud-native)

**Fichier :** `api/.../services/StorageService.java` (interface) + `LocalStorageServiceImpl`.

Interface à deux méthodes (`store`, `delete`). Implémentation actuelle :
`LocalStorageServiceImpl` (disque local). **Le point fort** : le reste du code dépend de
**l'interface**, jamais de l'implémentation. Migrer vers S3 = écrire un
`S3StorageServiceImpl` et changer **un seul branchement**, sans toucher aux appelants.

- C'est l'illustration concrète de **SOLID** : inversion de dépendance (DIP) + ouvert/fermé
  (OCP).
- C'est aussi **le dernier verrou** du full multi-instances : tant que les uploads sont sur
  disque local, deux instances ne partagent pas les fichiers. Sur S3, le stockage devient
  *stateless* → scalabilité complète. (Issue en cours.)
- **Question probable :** *« Pourquoi ne pas avoir fait S3 tout de suite ? »* → Priorisation :
  le périmètre fonctionnel d'abord ; l'**abstraction** était en place dès le départ
  justement pour rendre la migration triviale plus tard.

### Conteneur DI maison (desktop)

**Fichier :** `desktop/.../di/DependencyContainer.java`.

Le desktop n'a pas Spring. On a recodé un **mini-conteneur d'injection de dépendances** :
il instancie les services une fois (singleton) et **injecte les dépendances** dans les
contrôleurs JavaFX. Les services sont typés **par interfaces** (`AuthenticationService`,
`GameService`…), l'implémentation étant les `*ApiClient` qui appellent l'API REST.

- **Ce que ça illustre :** on a appliqué l'**inversion de dépendance** (les contrôleurs
  dépendent d'interfaces, pas des `ApiClient` concrets) → testable, découplé. Même
  architecture en couches que l'API (`controller` → `service` → `service/impl`).
- **Question-piège probable :** *« Pourquoi réinventer Spring / un conteneur DI ? »* →
  Réponse honnête : pour une appli desktop, embarquer Spring serait lourd ; on voulait
  surtout **démontrer qu'on maîtrise le concept** d'injection de dépendances et de couplage
  faible, pas dépendre d'un framework qui le fait « par magie ». Le câblage est explicite et
  centralisé en un seul endroit.
- ⚠️ **À ne pas mettre en avant :** la méthode `createController()` est une longue chaîne de
  `if/else`. Si on nous le reproche : c'est le compromis assumé d'un conteneur minimaliste
  fait main ; une version plus poussée utiliserait la réflexion/des annotations (ce que
  Spring fait), mais c'était hors périmètre.
