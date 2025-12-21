<p align="center">
  <img src="doc/assets/logo/logo.png" alt="CheckPoint Logo" width="200"/>
</p>

<h1 align="center">CheckPoint</h1>

<p align="center">
  <strong>Your unified video game library tracker</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#documentation">Documentation</a> •
  <a href="#authors">Authors</a>
</p>

---

## About

**CheckPoint** addresses a concrete need identified among video game players: the fragmentation of their game libraries across multiple platforms. It offers a centralized solution allowing users to:

- Track their progress and manage their **backlog**
- Organize their **wishlist**
- Rate and review games
- Interact with a **community** of players
- Earn **XP and badges** through gamification

---

## Features

### For Players (Web Application)
- Secure authentication with 2FA support
- Personal game library management (in progress, finished, wishlist)
- Rating and review system
- Follow friends and see their activity
- Advanced fuzzy search with filters
- Game recommendations based on preferences
- Gamification with XP, levels, and badges

### For Administrators (Desktop Application)
- User management (block, promote, view history)
- Content moderation (review reports queue)
- API synchronization with MobyGames/IGDB
- Analytics dashboard with charts
- Manual game entry CRUD operations

---

## Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Spring Boot 3** | REST API framework |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | Database persistence |
| **Spring Batch** | Data import jobs |
| **Hibernate Search** | Full-text fuzzy search |
| **PostgreSQL** | Relational database |

### Web Frontend
| Technology | Purpose |
|------------|---------|
| **TanStack Start** | React meta-framework with SSR |
| **React** | UI library |
| **Tailwind CSS** | Styling |
| **Shadcn UI** | Component library |

### Desktop Frontend
| Technology | Purpose |
|------------|---------|
| **JavaFX** | Desktop UI framework |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Traefik** | Reverse proxy & SSL |
| **Docker Swarm** | Container orchestration |

---

## Getting Started

### Prerequisites

- **Java 21+**
- **Node.js 20+**
- **pnpm**
- **Docker & Docker Compose**
- **PostgreSQL** (or use Docker)

### API (Spring Boot)

```bash
cd api

# Start PostgreSQL with Docker
docker compose up -d

# Run the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

### Web Application (TanStack Start)

```bash
cd web

# Install dependencies
pnpm install

# Start development server
pnpm dev
```

The web app will be available at `http://localhost:3000`

### Production Deployment

The application is deployed on a VPS using **Dokploy**, a self-hosted PaaS that simplifies Docker container management.

Dokploy handles:
- Automatic SSL/TLS certificates
- Reverse proxy configuration (Traefik)
- Container orchestration
- Zero-downtime deployments

---

## Documentation

| Document | Description |
|----------|-------------|
| [Specifications](doc/specifications/requirements.md) | Full project requirements and specifications |
| [Architecture](doc/architecture/README.md) | System architecture details |
| [Entities](doc/entities/) | JPA entity documentation |
| [UML Diagrams](doc/uml/) | MCD and Use Case diagrams |
| [Design](doc/design/) | Graphic charter and style guide |
| [Mockups](doc/assets/mockups/) | UI/UX mockups |


---

## Authors

- **Enzo CHABOISSEAU**
- **Gauthier SEYZERIAT--MEYER**

---

## License

This project is part of an academic program at **CCI Campus Alsace**.

---

<p align="center">
  Made with ❤️ for gamers
</p>
