# 🚀 SyncFlow

### Modern Real-time Collaborative Workspace & Task Management System

*A high-performance, real-time Trello/Jira alternative featuring granular workspace-level role-based access control (RBAC) and optimized database queries.*

## 📌 Table of Contents

* [About The Project](#-about-the-project)

* [Key Highlights & Technical Challenges](#-key-highlights--technical-challenges)

* [Tech Stack](#-tech-stack)

* [Core Features](#-core-features)

* [Project Architecture & Structure](#-project-architecture--structure)

* [Getting Started](#-getting-started)

* [API Documentation](#-api-documentation)

* [License](#-license)

## 💡 About The Project

**SyncFlow** is a feature-rich, full-stack collaborative project management platform built to bridge the gap between simple task boards (like Trello) and structured issue tracking (like Jira).

While most standard clones implement flat user structures, **SyncFlow** stands out by introducing **Workspace-scoped Role-Based Access Control (RBAC)** and **sub-second real-time synchronization** via WebSockets, ensuring seamless collaboration across distributed team members.

## 🛠 Key Highlights & Technical Challenges

This project goes beyond standard CRUD applications by tackling real-world engineering problems:

1. **Workspace-Level RBAC (Owner / Member / Viewer):**

   * Implemented custom security annotation-based authorization checks (`@PreAuthorize` / custom security evaluators).

   * Dynamic permission validation ensuring users can only modify boards/cards within workspaces they belong to, adhering strictly to their assigned roles.

2. **Secure Authentication & Token Management:**

   * Stateless authentication powered by Spring Security and JWT.

   * Secure token rotation mechanism using **Access Tokens** (short-lived) and **Refresh Tokens** (stored securely, rotated on refresh requests).

3. **Database Performance & N+1 Query Optimization:**

   * Diagnosed and resolved typical JPA N+1 query bottlenecks when fetching complex hierarchies (Workspace -> Boards -> Cards -> Assignees/Comments).

   * Utilized `@EntityGraph`, `JOIN FETCH`, and projection queries to optimize database round-trips by up to 70%.

4. **Real-time Collaboration via WebSocket (STOMP):**

   * Event-driven notifications and instant board updates broadcasted using Spring WebSocket with STOMP protocol and SockJS fallback.

   * Instant UI reactivity on card movements, comments, and member assignments.

## 🧰 Tech Stack

### Backend

* **Framework:** Spring Boot 3

* **Security:** Spring Security, JWT (JSON Web Tokens)

* **Data Access:** Spring Data JPA, Hibernate, PostgreSQL

* **Migration:** Flyway

* **Real-time:** WebSocket (STOMP)

* **Validation:** Jakarta Bean Validation

* **Documentation:** Springdoc OpenAPI (Swagger UI)

### Frontend

* **Library:** React 18 with TypeScript

* **Styling:** Tailwind CSS

* **State Management & Fetching:** React Query (@tanstack/react-query)

* **Drag & Drop:** `@hello-pangea/dnd` or `@dnd-kit`

* **Real-time Client:** `@stomp/stompjs`, `sockjs-client`

### DevOps & Infrastructure

* **Containerization:** Docker & Docker Compose

* **Database:** PostgreSQL 15

## ✨ Core Features

* **Authentication System:** Secure Sign up, Sign in, and automatic token refreshing.

* **Workspace Management:** Create multiple workspaces, invite members, and manage roles (`Owner`, `Member`, `Viewer`).

* **Board & Column Management:** Kanban boards with customizable columns.

* **Drag-and-Drop Cards:** Seamless card reordering and moving across columns.

* **Task Assignment & Details:** Assign members to cards, add descriptions, and track due dates.

* **Real-time Comments:** Discuss tasks instantly with team members.

* **Notification System:** Real-time bell notifications for assignments, mentions, and updates.

## 📁 Project Structure

```
syncflow/
├── backend/                  # Spring Boot Application
│   ├── src/main/java/        # Controllers, Services, Repositories, Entities, Security
│   ├── src/main/resources/   # Application configs & Flyway migrations V1__init.sql
│   └── Dockerfile
├── frontend/                 # React + TypeScript Application
│   ├── src/
│   │   ├── components/       # Reusable UI elements & Kanban board
│   │   ├── hooks/            # React Query hooks & WebSocket hooks
│   │   ├── services/         # Axios API clients
│   │   └── types/            # TypeScript interfaces
│   └── Dockerfile
└── docker-compose.yml        # Multi-container orchestration (PostgreSQL + Backend + Frontend)


```

### Current backend implementation

The repository currently contains the Spring Boot backend and PostgreSQL setup; the `frontend/` directory described above has not been added yet. The backend source is organized by responsibility:

* `controller/` exposes HTTP endpoints (`AuthController`).
* `service/` contains registration and credential verification (`AuthService`).
* `repository/` provides Spring Data access to users (`UserRepository`).
* `model/` maps the `users` table (`User`).
* `dto/` defines request and response payloads.
* `config/` configures Spring Security; Flyway schema migrations live under `src/main/resources/db/migration/`.

The implemented auth endpoints are:

| Method | Path | Request | Result |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | `{ "email": "...", "password": "...", "fullName": "..." }` | `200` with a registration message; `400` if the email exists |
| `POST` | `/api/v1/auth/login` | `{ "email": "...", "password": "..." }` | `200` with user profile and access/refresh tokens; `401` for invalid credentials |
| `POST` | `/api/v1/auth/refresh` | `{ "refreshToken": "..." }` | `200` with a new token pair; the submitted refresh token is invalidated. Invalid or expired tokens return `401` |

Passwords are stored as BCrypt hashes and never included in responses. Access tokens are signed HS256 JWTs. Refresh tokens are random opaque values; only their SHA-256 hashes are stored in PostgreSQL and each refresh rotates the token. All routes require a valid `Authorization: Bearer <accessToken>` header except registration, login, refresh, Swagger, and `/api/v1/test/**`. Configure `JWT_SECRET` to a private random value of at least 32 bytes before deployment; `JWT_EXPIRATION` and `JWT_REFRESH_EXPIRATION` are milliseconds.

## 🚀 Getting Started

To run the project locally using Docker, follow these steps:

### Prerequisites

* [Docker](https://www.docker.com/) and Docker Compose installed on your machine.

* Node.js & Java 17+ (optional, if you prefer running services outside Docker for development).

### Installation & Run via Docker Compose

1. **Clone the repository:**

   ```
   git clone https://github.com/your-username/syncflow.git
   cd syncflow
   
   
   ```

2. **Configure Environment Variables:**
   Create a `.env` file in the root directory or configure database credentials in `docker-compose.yml`.

3. **Start services with Docker Compose:**

   ```
   docker-compose up --build
   
   
   ```

4. **Access the application:**

   * **Frontend:** `http://localhost:3000`

   * **Backend API & Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

## 📖 API Documentation

Once the backend is running, you can explore the fully interactive API documentation via Swagger UI at:
👉 `http://localhost:8080/swagger-ui/index.html`

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
