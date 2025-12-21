# Checkpoint API - Entity Documentation

This document provides a comprehensive overview of all JPA entities in the Checkpoint API.

## Table of Contents

1. [Core Entities](#core-entities)
   - [User](#user)
   - [VideoGame](#videogame)
   - [Review](#review)
   - [Platform](#platform)
   - [Genre](#genre)
   - [Series](#series)
   - [Company](#company)
   - [Picture](#picture)
2. [User-Related Entities](#user-related-entities)
   - [Role](#role)
   - [News](#news)
   - [Badge](#badge)
   - [Notification](#notification)
   - [SocialLink](#sociallink)
3. [Association Entities](#association-entities)
   - [UserGamePlay](#usergameplay)
   - [Backlog](#backlog)
   - [Rate](#rate)
   - [Wish](#wish)
   - [Favorite](#favorite)
   - [Like](#like)
   - [GameList](#gamelist)
   - [Comment](#comment)
   - [Report](#report)
4. [Enums](#enums)
   - [PlayStatus](#playstatus)

---

## Core Entities

### User

**Table:** `users`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| pseudo | String | NOT NULL, UNIQUE | Username |
| email | String | NOT NULL, UNIQUE | Email address |
| password | String | NOT NULL | Hashed password |
| bio | TEXT | - | User biography |
| picture | String | - | Profile picture URL |
| is_private | Boolean | NOT NULL, default: false | Privacy setting |
| xp_point | Integer | NOT NULL, default: 0 | Experience points |
| level | Integer | NOT NULL, default: 1 | User level |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `role` → Role (ManyToOne)
- `gamePlays` → UserGamePlay (OneToMany)
- `reviews` → Review (OneToMany)
- `news` → News (OneToMany)
- `badges` ↔ Badge (ManyToMany via `user_badges`)
- `notifications` → Notification (OneToMany)
- `socialLinks` → SocialLink (OneToMany)
- `following` ↔ User (ManyToMany via `user_follows`)
- `followers` ↔ User (ManyToMany inverse)
- `backlogs` → Backlog (OneToMany)
- `rates` → Rate (OneToMany)
- `wishes` → Wish (OneToMany)
- `favorites` → Favorite (OneToMany)
- `likes` → Like (OneToMany)
- `gameLists` → GameList (OneToMany)
- `comments` → Comment (OneToMany)
- `reports` → Report (OneToMany)

---

### VideoGame

**Table:** `video_games`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| title | String | NOT NULL | Game title |
| description | TEXT | - | Game description |
| release_date | LocalDate | - | Release date |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `series` → Series (ManyToOne, nullable)
- `parentGame` → VideoGame (ManyToOne, nullable) - For DLCs
- `dlcs` → VideoGame (OneToMany)
- `platforms` ↔ Platform (ManyToMany via `video_game_platforms`)
- `genres` ↔ Genre (ManyToMany via `video_game_genres`)
- `companies` ↔ Company (ManyToMany via `video_game_companies`)
- `gameLists` ↔ GameList (ManyToMany inverse)
- `gamePlays` → UserGamePlay (OneToMany)
- `reviews` → Review (OneToMany)
- `pictures` → Picture (OneToMany)
- `backlogs` → Backlog (OneToMany)
- `rates` → Rate (OneToMany)
- `wishes` → Wish (OneToMany)
- `favorites` → Favorite (OneToMany)
- `likes` → Like (OneToMany)

---

### Review

**Table:** `reviews`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| content | TEXT | NOT NULL | Review content |
| have_spoilers | Boolean | NOT NULL, default: false | Spoiler flag |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, NOT NULL)
- `comments` → Comment (OneToMany)
- `reports` → Report (OneToMany)
- `likes` → Like (OneToMany)

---

### Platform

**Table:** `platforms`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| name | String | NOT NULL, UNIQUE | Platform name |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Computed Fields:**
- `videoGamesCount` (Formula) - Count of video games on this platform

**Relationships:**
- `videoGames` ↔ VideoGame (ManyToMany inverse)
- `gamePlays` → UserGamePlay (OneToMany)

---

### Genre

**Table:** `genres`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| name | String | NOT NULL, UNIQUE | Genre name |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Computed Fields:**
- `videoGamesCount` (Formula) - Count of video games in this genre

**Relationships:**
- `videoGames` ↔ VideoGame (ManyToMany inverse)

---

### Series

**Table:** `series`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| name | String | NOT NULL, UNIQUE | Series name |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `videoGames` → VideoGame (OneToMany)

---

### Company

**Table:** `companies`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| name | String | NOT NULL, UNIQUE | Company name |
| description | TEXT | - | Company description |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Computed Fields:**
- `videoGamesCount` (Formula) - Count of video games created by this company

**Relationships:**
- `videoGames` ↔ VideoGame (ManyToMany inverse)

---

### Picture

**Table:** `pictures`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| url | String | NOT NULL | Picture URL |
| is_main | Boolean | NOT NULL, default: false | Main picture flag |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `videoGame` → VideoGame (ManyToOne, NOT NULL)

---

## User-Related Entities

### Role

**Table:** `roles`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| name | String | NOT NULL, UNIQUE | Role name |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `users` → User (OneToMany)

---

### News

**Table:** `news`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| title | String | NOT NULL | News title |
| description | TEXT | - | News content |
| published_at | LocalDateTime | - | Publication date |
| picture | String | - | News picture URL |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `author` → User (ManyToOne, NOT NULL)

---

### Badge

**Table:** `badges`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| name | String | NOT NULL, UNIQUE | Badge name |
| picture | String | - | Badge icon URL |
| description | TEXT | - | Badge description |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `users` ↔ User (ManyToMany inverse via `user_badges`)

---

### Notification

**Table:** `notifications`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| content | TEXT | NOT NULL | Notification content |
| is_read | Boolean | NOT NULL, default: false | Read status |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)

---

### SocialLink

**Table:** `social_links`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| url | String | NOT NULL | Social link URL |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)

---

## Association Entities

### UserGamePlay

**Table:** `user_game_plays`

Association entity between User and VideoGame representing a user playing a game.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| status | PlayStatus (Enum) | NOT NULL, default: ARE_PLAYING | Play status |
| is_replay | Boolean | NOT NULL, default: false | Replay flag |
| time_played | Integer | - | Time played in minutes |
| start_date | LocalDate | - | Start date |
| end_date | LocalDate | - | End date |
| ownership | String | - | Ownership type (owned, borrowed, subscription, etc.) |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, NOT NULL)
- `platform` → Platform (ManyToOne, NOT NULL)

---

### Backlog

**Table:** `backlogs`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Constraints:** UNIQUE(user_id, video_game_id)

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, NOT NULL)

---

### Rate

**Table:** `rates`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| score | Integer | NOT NULL | Rating score |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Constraints:** UNIQUE(user_id, video_game_id)

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, NOT NULL)

---

### Wish

**Table:** `wishes`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Constraints:** UNIQUE(user_id, video_game_id)

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, NOT NULL)

---

### Favorite

**Table:** `favorites`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| display_order | Integer | - | Order of the favorite (1-5) |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Constraints:** UNIQUE(user_id, video_game_id)

**Business Rule:** A user can have a maximum of 5 favorites (enforced at service level)

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, NOT NULL)

---

### Like

**Table:** `likes`

Polymorphic entity - a like can be on a VideoGame, Review, or GameList.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships (polymorphic - one of these will be set):**
- `user` → User (ManyToOne, NOT NULL)
- `videoGame` → VideoGame (ManyToOne, nullable)
- `review` → Review (ManyToOne, nullable)
- `gameList` → GameList (ManyToOne, nullable)

---

### GameList

**Table:** `lists`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| title | String | NOT NULL | List title |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Computed Fields:**
- `videoGamesCount` (Formula) - Count of video games in this list

**Relationships:**
- `user` → User (ManyToOne, NOT NULL)
- `videoGames` ↔ VideoGame (ManyToMany via `list_video_games`)
- `comments` → Comment (OneToMany)
- `likes` → Like (OneToMany)

---

### Comment

**Table:** `comments`

Polymorphic entity - a comment can be on a GameList or a Review.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| content | TEXT | NOT NULL | Comment content |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships (polymorphic - one of gameList/review will be set):**
- `user` → User (ManyToOne, NOT NULL)
- `gameList` → GameList (ManyToOne, nullable)
- `review` → Review (ManyToOne, nullable)
- `reports` → Report (OneToMany)

---

### Report

**Table:** `reports`

Polymorphic entity - a report can be against a Review or a Comment.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| content | TEXT | NOT NULL | Report reason/content |
| created_at | LocalDateTime | NOT NULL | Creation timestamp |
| updated_at | LocalDateTime | NOT NULL | Last update timestamp |

**Relationships (polymorphic - one of review/comment will be set):**
- `user` → User (ManyToOne, NOT NULL)
- `review` → Review (ManyToOne, nullable)
- `comment` → Comment (ManyToOne, nullable)

---

## Enums

### PlayStatus

**Location:** `com.checkpoint.api.enums.PlayStatus`

| Value | Description |
|-------|-------------|
| ARE_PLAYING | Currently playing |
| PLAYED | Has played |
| COMPLETED | Completed the game |
| RETIRED | Retired from playing |
| SHELVED | Put on hold |
| ABANDONED | Abandoned the game |

---
