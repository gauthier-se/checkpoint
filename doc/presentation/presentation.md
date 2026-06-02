# CheckPoint — Support de présentation

> Document de travail (FR) servant de trame orale pour la soutenance.
> Auteurs : Enzo CHABOISSEAU · Gauthier SEYZERIAT--MEYER — CCI Campus Alsace.

---

## Table des matières

1. [Pitch & contexte](#1-pitch--contexte)
2. [Architecture du projet](#2-architecture-du-projet)
3. [Implémentations significatives](#3-implémentations-significatives)
4. [Parties de code complexes à mentionner](#4-parties-de-code-complexes-à-mentionner)
5. [Pensé cloud-native : stateless & scalabilité](#5-pensé-cloud-native--stateless--scalabilité)
6. [Gestion de projet : specs vs réalisé (écarts)](#6-gestion-de-projet--specs-vs-réalisé-écarts)
7. [Organisation : Linear, GitHub & GitFlow](#7-organisation--linear-github--gitflow)
8. [Stratégie qualité](#8-stratégie-qualité)
9. [Roadmap & évolutions futures](#9-roadmap--évolutions-futures)
10. [Chiffres clés & démo](#10-chiffres-clés--démo)

---

## 1. Pitch & contexte

**Le problème.** Les joueurs ont leur bibliothèque éclatée entre Steam, consoles, stores… Il n'existe pas de point unique pour suivre sa progression, son *backlog*, sa *wishlist* et discuter avec une communauté.

**La solution — CheckPoint.** Une plateforme centralisée, indépendante des stores, organisée autour de **trois composants hétérogènes** :

| Composant | Stack | Rôle |
|-----------|-------|------|
| **API REST** | Spring Boot 3.5 / Java 21 | Cœur métier : logique, sécurité, persistance. Consommée par les deux clients. |
| **Application Web** | TanStack Start / React 19 | Interface joueur (catalogue, bibliothèque, social, gamification). SSR pour le SEO. |
| **Application Desktop** | JavaFX / Java 24 | Console d'administration : modération, gestion du catalogue, import, analytics. |

**Deux acteurs** : l'**Utilisateur** (web) et l'**Administrateur** (desktop).

> 💡 *À dire à l'oral :* le vrai défi du projet n'est pas une fonctionnalité isolée, c'est **l'orchestration sécurisée de trois technologies très différentes autour d'une seule API**.

---

## 2. Architecture du projet

### 2.1. Vue d'ensemble (monorepo)

Un seul dépôt Git contient les trois modules, ce qui simplifie la cohérence et la traçabilité :

```
checkpoint/
├── api/        → Spring Boot (Maven)      — le backend
├── web/        → TanStack Start (pnpm)    — le client joueur
├── desktop/    → JavaFX (Maven)           — la console admin
├── doc/        → specs, UML, MCD, mockups, charte, fichiers .http
├── scripts/    → seed de production
└── .github/    → CI (api-ci.yml, web-ci.yml), templates PR/issues
```

### 2.2. Déploiement (production)

- Hébergement sur un **VPS** via **Dokploy** (PaaS auto-hébergé au-dessus de Docker).
- **Traefik** comme reverse-proxy : point d'entrée unique, terminaison **HTTPS/TLS** automatique.
- Routage : `checkpoint.com` → conteneur Web (Node) ; `checkpoint.com/api/v1` → conteneur API.
- **PostgreSQL** isolé sur un réseau Docker privé — jamais exposé à internet, seule l'API y accède (JDBC/TCP).

### 2.3. Architecture interne de l'API (en couches)

L'API suit une séparation stricte des responsabilités, visible dans l'arborescence des packages :

```
controllers/   → 41 contrôleurs REST (points d'entrée HTTP, validation, @PreAuthorize)
services/      → interfaces métier
services/impl/ → ~60 implémentations (logique métier, transactions)
repositories/  → Spring Data JPA
entities/      → 33 entités JPA (modèle relationnel)
dto/           → records de transfert (par domaine : auth, catalog, social…)
mapper/        → conversion entité ↔ DTO
security/      → JWT, filtres, OAuth2, WebSocket
events/ + listeners/ → bus d'événements applicatif (gamification, notifications)
jobs/ + tasks/ → import asynchrone + tâches planifiées
client/        → clients HTTP externes (IGDB, Steam, RSS)
```

> 💡 *Point fort à souligner :* l'API est codée **par interfaces** (`services/` vs `services/impl/`). Cela force le découplage, facilite le test (mocks) et illustre concrètement les principes **SOLID** (notamment l'inversion de dépendance).

### 2.4. Architecture du Desktop (JavaFX, SOLID)

- Structure **en couches** : `controller/` (un contrôleur par vue FXML) → `service/` (interfaces) → `service/impl/` (`*ApiClient` qui appellent l'API REST).
- **Conteneur d'injection de dépendances maison** (`di/DependencyContainer`) : on a recodé un mini-conteneur DI à la main (le desktop n'a pas Spring).
- `TokenManager` conserve le JWT de la session admin et l'injecte dans chaque appel.
- Vues en **FXML** + CSS qui imite la charte graphique du web.

### 2.5. Architecture du Web

- **TanStack Start** (méta-framework React) avec **SSR** pour la performance initiale et le SEO.
- Routing par fichiers (`src/routes/`), avec segments `_auth`, `_app`, `_protected` qui matérialisent les zones publiques / connectées / protégées.
- `queries/` (TanStack Query) + `services/` pour les appels API, `components/` (Shadcn UI / Tailwind).

---

## 3. Implémentations significatives

### 3.1. Authentification hybride & sécurité

- **Spring Security** avec **deux chaînes de filtres** (`SecurityConfig`) :
  1. Chaîne **WebSocket** (`/ws/**`, order 0) — l'authentification se fait au niveau STOMP.
  2. Chaîne **API** (`/api/v1/**`, order 1) — **stateless**, JWT.
- **JWT à double source** (`JwtAuthenticationFilter`) : le token est lu soit dans l'en-tête `Authorization: Bearer` (Desktop), soit dans le **cookie HttpOnly `checkpoint_token`** (Web) → un seul backend sert deux clients très différents.
- **RBAC** via `@EnableMethodSecurity` + règles d'URL (`/api/v1/admin/**` réservé à `ROLE_ADMIN`).
- **BCrypt** pour le hachage des mots de passe.
- **2FA par e-mail** (code à 6 chiffres) avec *token intermédiaire* (`isIntermediateToken`).
- **OAuth2 / OIDC** (login social) + **refresh tokens** persistés et nettoyés par tâche planifiée.
- **Connexion Steam** (OpenID) pour importer la bibliothèque Steam.

### 3.2. Moteur de recherche full-text (Hibernate Search + Lucene)

- Indexation **Lucene** locale via Hibernate Search.
- `SearchIndexer` (`CommandLineRunner`) reconstruit l'index complet au démarrage → recherche disponible immédiatement.
- Recherche **floue** (tolérante aux fautes de frappe) + filtres multicritères.

### 3.3. Import de catalogue asynchrone (IGDB)

- Bouton admin → lance un **job d'import en arrière-plan** (`ImportJobRunner`, `@Async("importExecutor")`).
- Import de jeux **top-rated** ou **récents** (jusqu'à ~5000 jeux), suivi de progression exposé via l'API.
- Chaque jeu est **commité indépendamment** (hors transaction globale) → résilience : un échec n'annule pas tout.
- **Idempotence** côté persistance (pas de doublons).

### 3.4. Gamification événementielle (XP, niveaux, badges)

- Architecture **event-driven** : `events/` (17 événements) + `listeners/`.
- Quand un utilisateur termine un jeu, écrit une review, suit quelqu'un… un événement est publié, un *listener* asynchrone attribue l'XP.
- **Anti-triche intégré** : déduplication par clé d'événement (suivre/se désabonner/resuivre ne re-crédite pas) + plafonds glissants (max 10 grants « review likée » par 24 h).
- Badges débloqués par événement (`BadgeListener`), y compris des **easter-eggs** cachés.

### 3.5. Recommandation & social

- **Jeux similaires** : algorithme *item-to-item* basé sur le recouvrement de tags (genres / plateformes / studios) + bonus de note et de récence.
- **Joueurs compatibles** : filtrage collaboratif basé sur les jeux terminés en commun.
- **Feed d'activité**, follow/followers, comparaison de profils.

### 3.6. Temps réel & tâches planifiées

- **WebSockets (STOMP)** pour les notifications en temps réel.
- Tâches **`@Scheduled`** : import de news (RSS/Steam), refresh des profils Steam, nettoyage des refresh tokens.
- **ShedLock** : verrou distribué pour que les tâches planifiées ne s'exécutent qu'une fois même avec plusieurs instances.

---

## 4. Parties de code complexes à mentionner

À l'oral, ce sont les morceaux qui montrent qu'on a dépassé le CRUD basique. Suggestion : en présenter **2 ou 3** avec le fichier ouvert.

| Sujet | Fichier(s) | Pourquoi c'est intéressant |
|-------|-----------|----------------------------|
| **Double chaîne de sécurité** | `config/SecurityConfig.java` | Deux `SecurityFilterChain` ordonnées, OAuth2 conditionnel (`ObjectProvider`), tout stateless. |
| **JWT à double source** | `security/JwtAuthenticationFilter.java` | Un même filtre sert header (desktop) ET cookie HttpOnly (web). |
| **Import asynchrone résilient** | `jobs/ImportJobRunner.java`, `services/impl/GamePersistenceServiceImpl.java` | Exécution hors transaction, commit par jeu, suivi de statut, gestion d'erreurs sans appelant HTTP. |
| **Gamification anti-abus** | `listeners/GamificationListener.java` | Déduplication par clé + plafonds glissants 24 h ; logique événementielle async. |
| **Reco item-to-item** | `services/impl/GameSimilarityServiceImpl.java`, `GameTagScorer.java` | Pré-filtrage SQL du pool de candidats, scoring pondéré, tri multi-critères. |
| **DI maison (desktop)** | `desktop/.../di/DependencyContainer.java` | Conteneur d'injection recodé à la main, sans Spring. |
| **Entités polymorphes** | `entities/Like.java`, `Comment.java`, `Report.java` | Un like/commentaire/report peut cibler plusieurs types (jeu, review, liste) — modélisation délicate. |

> 💡 *À dire :* l'import a été le plus dur — on est passé d'une approche transactionnelle classique à un modèle « commit par item » pour ne pas tout perdre sur une erreur réseau IGDB à mi-parcours.

---

## 5. Pensé cloud-native : stateless & scalabilité

> C'est le **fil conducteur architectural** du projet et la vraie raison derrière plusieurs de nos choix techniques. À présenter comme une décision d'ingénierie volontaire, pas comme une contrainte subie.

### 5.1. Pourquoi on a retiré les sessions

Le cahier des charges prévoyait une **authentification par session (stateful)** côté web. On a fait le choix inverse : **tout est stateless (JWT)**.

- Une **session stateful** stocke l'état d'authentification **en mémoire serveur** (ou dans un store de sessions partagé). Avec plusieurs instances de l'API derrière un load-balancer, il faut soit du *sticky session*, soit un store de sessions externe (Redis…) → complexité et point de contention.
- En **stateless**, l'API **ne garde aucun état** : le JWT porte l'identité, validé à chaque requête par `JwtAuthenticationFilter`. **N'importe quelle instance peut traiter n'importe quelle requête.**
- Conséquence directe : on peut **scaler horizontalement** (ajouter des instances) **sans aucune configuration de partage d'état**. C'est *la* condition pour un déploiement cloud multi-instances simple.
- On garde la sécurité web : le JWT est dans un **cookie HttpOnly + SameSite** (protégé du XSS), pas dans le localStorage.

> 💡 *Phrase clé :* « On a supprimé les sessions **pour que l'app soit nativement multi-instances** : aucune instance n'est spéciale, on peut en ajouter ou en retirer à chaud. »

### 5.2. Déploiement multi-instances avec Dokploy / Docker Swarm

- **Dokploy** s'appuie sur **Docker Swarm** pour l'orchestration.
- Un cluster Swarm se compose de :
  - **Manager nodes** (les *control planes*) — orchestrent le cluster, planifient les conteneurs ;
  - **Worker nodes** — exécutent les conteneurs applicatifs.
- On peut **ajouter des workers** (pour absorber plus de charge) ou des **managers** (pour la haute dispo du control plane) **sans retoucher le code**, justement parce que l'API est stateless.
- **Traefik** (le reverse-proxy) répartit alors le trafic entre les instances → load-balancing transparent.

### 5.3. Stockage & sauvegardes (vers le cloud)

- **Base de données** : PostgreSQL peut être **sauvegardée vers un bucket S3** (backups managés par Dokploy) → résilience et restauration.
- **Uploads (images, avatars)** : aujourd'hui sur le **disque local** via `LocalStorageServiceImpl`. Point fort : on a **abstrait le stockage derrière une interface** `StorageService`. Migrer vers **S3** = écrire une nouvelle implémentation (`S3StorageServiceImpl`) **sans toucher au reste du code** — c'est exactement l'intérêt du découplage par interface.
- 🔜 **Issue en cours** : migration des uploads vers S3. C'est aussi le **dernier point qui empêche le full multi-instances** : tant que les uploads sont sur disque local, deux instances ne partagent pas les fichiers. Une fois sur S3, le stockage devient lui aussi *stateless* → scalabilité complète.

> 💡 *À dire :* « Notre architecture est stateless **côté calcul**. Le seul état restant est dans des services externalisables — PostgreSQL et le stockage de fichiers — et la migration des uploads vers S3 finalise le tableau. »

---

## 6. Gestion de projet : specs vs réalisé (écarts)

> On a livré **l'intégralité du périmètre fonctionnel** des specs, et on est même allés au-delà. Les écarts sont surtout **techniques** (meilleurs choix d'outils en cours de route) et **assumés**. C'est un point à présenter honnêtement : un cahier des charges est une cible, pas un contrat figé.

### 5.1. Écarts techniques (outil différent, objectif atteint)

| Spec annoncée | Réalisé | Pourquoi |
|---------------|---------|----------|
| Import via **Spring Batch** | **Runner asynchrone maison** (`@Async` + `ImportJobRunner`) + **ShedLock** | Spring Batch était surdimensionné pour notre besoin (pas de reprise sur incident lourde, pas de chunking complexe). Un job async + commit par item donne la résilience voulue avec beaucoup moins de complexité. |
| Source de données **MobyGames** | **IGDB** (+ **Steam** pour les profils/news) | API IGDB plus riche et mieux documentée ; Steam ajoute l'import de bibliothèque, non prévu initialement. |
| Web en **session stateful** (cookies de session) | **JWT stateless** dans un **cookie HttpOnly** | Une seule chaîne de sécurité stateless pour les deux clients → API multi-instances sans état serveur (voir §5). Protection XSS via HttpOnly + SameSite. |
| Reco par **TF-IDF (Hibernate Search)** | Reco **item-to-item par overlap de tags** (SQL) | Plus simple à maîtriser et à expliquer, résultats pertinents sur notre volume. |
| Java **21** partout | API en **21**, Desktop en **24**, CI en **JDK 25** | JavaFX récent + contraintes de la machine de dev. |

### 5.2. Au-delà des specs (bonus livrés)

- **OAuth2 / OIDC** (login social) — non prévu.
- **Connexion & import Steam** (OpenID + bibliothèque + news).
- **Easter-eggs / badges cachés**.
- **Comparaison de profils** entre joueurs.
- **Leaderboard** et **streak de connexion**.
- **Export RGPD** des données (JSON) — conforme à l'esprit des specs (droit à la portabilité).

### 5.3. Conformité au planning

Les 4 phases des specs (Base technique → Cœur fonctionnel → Design/UI → Qualité & livraison) ont été suivies. La dernière phase (WebSockets temps réel, tests, polish responsive) est visible dans les commits récents (`TE-347`, `TE-351`, skeleton loaders, redesign…). Soutenance prévue **le 3 juin 2026**.

> 💡 *Message à faire passer :* aucun écart n'est une régression — chaque divergence est un **choix d'ingénierie justifié** qui a simplifié l'archi ou enrichi le produit.

---

## 7. Organisation : Linear, GitHub & GitFlow

### 6.1. Linear (gestion de projet) ↔ GitHub (code)

- **Linear** : backlog, user stories, cycles, milestones. Workspace `m2i-projet-java`, équipe **`TE`**, projet **Checkpoint**.
- Chaque ticket porte un identifiant **`TE-xxx`** que l'on retrouve **dans le nom de branche, le commit et la PR** → traçabilité de bout en bout (ticket → code → review → merge).
- **Labels** structurés : `api` / `web` / `desktop` (mutuellement exclusifs), `Feature` / `Bug` / `Improvement`, `security`…
- **Milestones** : ex. « Milestone 5 : Quality, Real-Time & Final Delivery ».
- **Discord** pour les points de synchro quotidiens.

### 6.2. Workflow Git — GitHub Flow

- Branche unique de vérité : **`main`** (toujours déployable).
- **Aucun push direct sur `main`** : tout passe par une **Pull Request**.
- Convention de nommage de branche :
  ```
  <username>/<ticket-id>-<description-kebab>
  ex. gseyzeriat1/te-258-docs-contributing-readme-http
  ```
- **Conventional Commits** :
  ```
  <type>(<scope>): <description> (<TICKET-ID>)
  ex. feat(web): add "Add to list" button on game detail page (TE-343)
       refactor(desktop): implement SOLID architecture (TE-262)
  ```
  Types : `feat`, `fix`, `chore`, `refactor`, `docs`, `test`… · Scope : `api` / `web` / `desktop`.

### 6.3. Règles de protection & revue

- ✅ **≥ 1 review approuvée** obligatoire avant merge.
- ✅ **Status checks verts** obligatoires (CI).
- Template de PR (`.github/PULL_REQUEST_TEMPLATE.md`) à remplir, lien vers le ticket (`Closes TE-xxx`).
- ~210 commits, **PR systématiques** (n° de PR visibles dans l'historique).

### 6.4. Intégration continue (GitHub Actions)

- **`api-ci.yml`** (déclenché si `api/**` change) : tests + **gate de couverture JaCoCo (≥ 70 %)**, build, upload du rapport en artifact.
- **`web-ci.yml`** (déclenché si `web/**` change) : `pnpm check:ci` (lint + prettier), tests Vitest, build.
- CI **scopée par chemin** → on ne relance pas tout le monorepo à chaque PR.

---

## 8. Stratégie qualité

| Type | Outils | Portée |
|------|--------|--------|
| Tests unitaires | JUnit 5, Mockito | Services métier isolés |
| Tests d'intégration | Spring Boot Test, Testcontainers | Contrôleurs REST, requêtes JPA |
| Tests web | Vitest | Composants / logique front |
| Automatisation | GitHub Actions | À chaque PR |

**Indicateurs :**
- **Couverture JaCoCo** : build **en échec si < 70 %** (gate stricte). **117 fichiers de test** côté API.
- **Documentation API** : **Swagger UI / OpenAPI** (SpringDoc) à `/swagger-ui.html` ; fichiers `.http` prêts à l'emploi dans `doc/http/`.
- **Javadoc** générée sur API et Desktop.
- Analyse statique (SonarLint), conventions de nommage (PascalCase / camelCase / snake_case).

---

## 9. Roadmap & évolutions futures

> Le produit est conçu pour vivre après la soutenance. La page **Roadmap** du site (`web/src/routes/_app/roadmap.tsx`) est publique et reflète notre vision en trois horizons. Les évolutions techniques sont, elles, suivies comme des **issues Linear**.

### 9.1. Évolutions techniques (issues en cours / à venir)

- 🔜 **Migration des uploads vers S3** (issue en cours) — finalise la scalabilité multi-instances (voir §5.3).
- Backups PostgreSQL automatisés vers S3.
- Montée en charge : ajout de workers Swarm selon le trafic.

### 9.2. Roadmap produit (page publique)

| Horizon | Fonctionnalités prévues |
|---------|-------------------------|
| **Court terme** | Traduction de l'application dans davantage de langues (i18n) |
| **Moyen terme** | Listes collaboratives entre amis · Suivre ses studios / développeurs préférés |
| **Long terme** | Messagerie privée entre utilisateurs · Application mobile native (iOS & Android) |

> 💡 *À dire :* « L'API REST stateless qu'on a construite est aussi ce qui rend une **app mobile native** réaliste demain : c'est exactement le même backend, consommé par un troisième type de client — comme on l'a déjà fait pour le web et le desktop. »

---

## 10. Chiffres clés & démo

**Quelques chiffres à citer :**
- 3 applications, 1 monorepo.
- API : **41 contrôleurs**, **~60 services**, **33 entités JPA**, **117 fichiers de test**.
- ~**210 commits**, workflow 100 % par PR avec review.
- Sécurité : JWT double source, 2FA, OAuth2, BCrypt, RBAC.

**Fil rouge suggéré pour la démo :**
1. **Web** — inscription, recherche floue d'un jeu, ajout au backlog, review → gain d'XP / badge.
2. **Desktop** — login admin (avec 2FA), lancement d'un **import IGDB** avec barre de progression, modération d'un report.
3. **Temps réel** — montrer une notification WebSocket qui arrive en direct.
4. **Code** — ouvrir 2 fichiers « complexes » (cf. §4) pour appuyer le propos technique.

> 💡 *Phrase de conclusion :* « CheckPoint, c'est trois clients hétérogènes derrière une seule API sécurisée et stateless ; on a tenu le périmètre fonctionnel, et nos écarts par rapport aux specs sont des choix d'ingénierie assumés qui ont simplifié l'architecture ou enrichi le produit. »
