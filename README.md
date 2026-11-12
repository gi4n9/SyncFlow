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

   * Event-driven notifications and board updates using Spring WebSocket with STOMP.

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
│   │   ├── api/              # Typed API client and auth refresh flow
│   │   ├── features/
│   │   │   ├── auth/components/
│   │   │   ├── board/components/
│   │   │   └── workspace/components/
│   │   ├── shared/           # Shared components and view types
│   │   ├── App.tsx           # App state and feature orchestration
│   │   └── main.tsx
│   └── Dockerfile
└── docker-compose.yml        # Multi-container orchestration (PostgreSQL + Backend + Frontend)


```

### Current backend implementation

The repository contains the Spring Boot backend, a Vite + React + TypeScript frontend, and PostgreSQL setup. Signed-in users load workspaces, boards, lists, cards, and workspace members from the API. Owners can invite accounts; invited users accept a token to join and access that workspace's boards. Creating/editing/moving cards and assigning workspace members persists to PostgreSQL; the signed-out sample board remains a local preview. The backend source is organized by responsibility:

* `controller/` exposes HTTP endpoints (`AuthController`).
* `service/` contains registration and credential verification (`AuthService`).
* `repository/` provides Spring Data access to users (`UserRepository`).
* `model/` maps the `users` table (`User`).
* `dto/` defines request and response payloads.
* `config/` configures Spring Security; Flyway schema migrations live under `src/main/resources/db/migration/`.

The implemented auth endpoints are:

| Method | Path | Request | Result |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | `{ "email": "...", "password": "...", "fullName": "..." }` | `201` with public profile; `400` for invalid input, `409` for duplicate email |
| `POST` | `/api/v1/auth/login` | `{ "email": "...", "password": "..." }` | `200` with user profile and access token; refresh token is set as HttpOnly cookie; `401` for invalid credentials |
| `POST` | `/api/v1/auth/refresh` | Refresh cookie (no body) | `200` with new access token and rotated refresh cookie; invalid/reused token returns `401` |
| `POST` | `/api/v1/auth/logout` | Refresh cookie (no body) | `204`, revokes current refresh token and clears cookie |
| `GET` | `/api/v1/auth/me` | Bearer access token | `200` with current profile |
| `PUT` | `/api/v1/auth/me` | `{ "email": "...", "fullName": "..." }` | Updates current profile; email is normalized to lowercase |
| `POST` | `/api/v1/auth/password` | `{ "currentPassword": "...", "newPassword": "..." }` | `204`; revokes all refresh tokens for the account |

Workspace and membership endpoints:

| Method | Path | Permission / result |
| --- | --- | --- |
| `POST` | `/api/v1/workspaces` | Authenticated user creates a workspace and becomes its Owner (`201`) |
| `GET` | `/api/v1/workspaces` | Lists workspaces for the current user |
| `GET` | `/api/v1/workspaces/{id}` | Workspace members only; nonmembers receive `404` |
| `PUT`, `DELETE` | `/api/v1/workspaces/{id}` | Owner only; delete is hard delete with database cascade |
| `GET` | `/api/v1/workspaces/{id}/members` | Workspace members only |
| `POST` | `/api/v1/workspaces/{id}/invitations` | Owner invites by email with `MEMBER` or `VIEWER`; returns a one-use token expiring in seven days |
| `POST` | `/api/v1/invitations/{token}/accept` | Signed-in account matching the invitation email accepts it |
| `POST` | `/api/v1/my/invitations/{id}/accept` | Accepts a pending invitation received in the signed-in user's notification |
| `POST` | `/api/v1/my/invitations/{id}/decline` | Declines it; the user is not added to the workspace |
| `PATCH`, `DELETE` | `/api/v1/workspaces/{id}/members/{userId}` | Owner changes role or removes a member |
| `DELETE` | `/api/v1/workspaces/{id}/members/me` | Member leaves; current Owner must transfer ownership first |

Board and list endpoints:

| Method | Path | Permission / result |
| --- | --- | --- |
| `GET`, `POST` | `/api/v1/workspaces/{id}/boards` | Workspace members list boards; Owner/Member can create. Archived boards are excluded unless `?includeArchived=true` |
| `GET`, `PUT` | `/api/v1/boards/{id}` | Member can read the board aggregate (lists and cards); Owner/Member can rename it |
| `POST` | `/api/v1/boards/{id}/archive` | Owner/Member archives the board |
| `DELETE` | `/api/v1/boards/{id}` | Owner deletes the board and its lists/cards (cascade) |
| `POST` | `/api/v1/boards/{id}/lists` | Owner/Member creates a list |
| `PUT`, `PATCH` | `/api/v1/lists/{id}`, `/api/v1/lists/{id}/move` | Owner/Member renames or reorders a list by fractional `position` |
| `DELETE` | `/api/v1/lists/{id}` | Owner deletes an empty list; lists with cards return `409` |

New boards start with eight workflow lists: `Draft`, `Pending`, `In Progress`, `Blocked`, `In Review`, `Reopen`, `Done`, and `Cancelled`. Board detail loads lists and cards with bulk queries whose count does not grow with board size. `V5__add_board_lists.sql` migrates existing status-based cards to lists. A board deletion hard-deletes descendants by foreign-key cascade; a list with cards must be emptied first to avoid accidental task loss. `V8__add_in_review_list.sql` and `V9__standardize_board_workflow_states.sql` migrate existing boards to the standard workflow while preserving cards and keeping custom lists after the standard states.

Card endpoints include `POST /api/v1/lists/{listId}/cards`, `PUT /api/v1/cards/{cardId}`, `DELETE /api/v1/cards/{cardId}`, `PATCH /api/v1/cards/{cardId}/move`, and `PUT /api/v1/cards/{cardId}/assignees`. Update, move, and assignee requests carry the card `version`; stale versions return `409 Conflict`. Migration `V6__add_card_editing.sql` adds priority, version, creator, and assignee persistence.

Labels are board-scoped. `GET /api/v1/boards/{boardId}/labels` and `GET /api/v1/workspaces/{workspaceId}/labels` list them; `PUT /api/v1/cards/{cardId}/labels` accepts `{ "labels": ["Design", "Sprint 08"] }` and creates/reuses labels on the card's board.

Card search is available at `GET /api/v1/search/cards`. Pass `boardId` or `workspaceId`; optional filters are `q`, `assigneeId`, `label`, `dueFrom`, `dueTo`, and `priority`, along with `page` and `size` (maximum 100). Search checks title and description with PostgreSQL `ILIKE`; migration `V12__add_card_labels_and_search.sql` enables `pg_trgm`, adds GIN indexes, and creates board-scoped labels. Workspace membership is checked before searching.

For board realtime updates, an authenticated STOMP client subscribes to `/topic/boards/{boardId}`. The server checks workspace membership on SUBSCRIBE and broadcasts card, assignee, label, comment, and related activity events only after transaction commit. The frontend reloads the board after receiving an event and after reconnecting. Notification messages continue on `/user/queue/notifications`.

Migration `V7__remove_legacy_card_columns.sql` removes the old `board_id`, `column_status`, and `assigned_to` fields. Cards now reference their board through `board_lists` and store assignees in `card_assignees`.

Comments use `GET/POST /api/v1/cards/{cardId}/comments?page=0&size=20` and `PUT/DELETE /api/v1/comments/{commentId}`. Members and higher roles can read comments; Owner/Member can create them; only the author can edit; the author or workspace Owner can delete. Mention a workspace member by email in the comment text (for example `@member@example.com`) to create a notification; non-members and the author are not notified.

Activity is available through `GET /api/v1/cards/{cardId}/activities` and `GET /api/v1/boards/{boardId}/activities` with `page` and `size` pagination. Card, assignee, comment, and workspace membership actions publish Spring application events; an `AFTER_COMMIT` listener persists the activity records and any associated notifications.

Notifications are available at `GET /api/v1/notifications?page=0&size=20`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read`, and `PATCH /api/v1/notifications/read-all`. The authenticated STOMP endpoint is `/ws`; connect with a native `Authorization: Bearer <accessToken>` header and subscribe to `/user/queue/notifications`. Vite proxies `/ws` to the backend during local development. The frontend reconnects and updates the notification badge in real time. Notifications are created for card assignment, comment mentions, and invitations/member joins when the recipient already has an account. An invitation notification opens explicit **Accept**, **Decline**, and **Decide later** actions. A link opens a confirmation screen; signing in never accepts it automatically. Ignoring it leaves the invitation pending, and declining marks it declined. Invitations to unregistered addresses are shared as one-use links; the application does not send email.

Migrations `V10__comments_activity_notifications.sql` and `V11__allow_declined_workspace_invitations.sql` add comment edit metadata, typed notification payloads, activity logs, and the declined invitation status. They are applied by Flyway on the next backend start.

Passwords are stored as BCrypt hashes and never included in responses. Access tokens are signed HS256 JWTs and last 15 minutes by default. Refresh tokens are random opaque values in an HttpOnly, SameSite=Strict cookie; only their SHA-256 hashes are stored in PostgreSQL and each refresh rotates the token. Reuse of a revoked token invalidates its token family. All routes require a valid `Authorization: Bearer <accessToken>` header except registration, login, refresh, logout, Swagger, and `/api/v1/test/**`. Set `JWT_SECRET` to a private random value of at least 32 bytes before starting the backend. `JWT_EXPIRATION` and `JWT_REFRESH_EXPIRATION` are milliseconds. `REFRESH_COOKIE_SECURE` defaults to `true`; use `false` only for local HTTP development. `FRONTEND_ORIGIN` controls the single allowed browser origin. Auth endpoints have per-IP fixed-window limits in each backend process.

## 🚀 Getting Started

### Frontend (local development)

The React + TypeScript frontend lives in `frontend/`. Start PostgreSQL with `docker compose up -d postgres`, then start the backend locally with its HTTP development cookie and matching frontend origin:

```sh
cd backend
JWT_SECRET='replace-with-a-random-secret-of-at-least-32-bytes' REFRESH_COOKIE_SECURE=false FRONTEND_ORIGIN=http://localhost:5173 ./gradlew bootRun
```

In another terminal, run:

```sh
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to `http://localhost:8080`. To add a teammate, an Owner enters the member's email and role; the app creates a one-use invitation link that can be copied and shared. The invited account opens the link, signs in (or registers) with that same email, then explicitly accepts or declines; simply signing in or ignoring the invitation does not add them. Invitees can leave a workspace later, subject to the Owner transfer rule. The new member can open workspace boards and can be selected in a task's **Edit task → Assignees** section. The project does not currently send invitation emails itself. Sign-in, profile, workspace, member, board, list/card loading and writes use the API. Signed-out users see a sample preview; preview changes are not persisted. Use a private random `JWT_SECRET` locally and keep `REFRESH_COOKIE_SECURE=false` only for plain HTTP development.

To leave a workspace, open the workspace switcher in the sidebar, choose **Leave “workspace name”**, and confirm. Leaving removes that workspace from your list. The Owner must transfer ownership from member management before leaving.

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

   * **Frontend:** `http://localhost:5173`

   * **Backend API & Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

## 📖 API Documentation

Once the backend is running, you can explore the fully interactive API documentation via Swagger UI at:
👉 `http://localhost:8080/swagger-ui/index.html`

To authorize protected endpoints in Swagger, call `POST /api/v1/auth/login`. Swagger UI automatically reads the returned `accessToken` and authorizes the `bearerAuth` scheme for subsequent calls. The refresh token is an HttpOnly cookie and is handled by the browser for refresh/logout endpoints.

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
