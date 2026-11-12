# SyncFlow Work Log

Ghi chú bàn giao cho các phiên làm việc tiếp theo. Đọc file này trước khi sửa code, sau đó cập nhật mục **Latest session** khi hoàn thành thay đổi để phiên chat mới có thể tiếp tục từ trạng thái hiện tại.

## Project snapshot

- Repository hiện có backend Spring Boot, frontend Vite/React/TypeScript và PostgreSQL qua Docker Compose.
- Backend nằm trong `backend/`, dùng Java 25, Spring Boot 4.1.2, Spring Security, Spring Data JPA, PostgreSQL và Flyway.
- Database mặc định: `jdbc:postgresql://localhost:5432/syncflow_db` (user `postgres`). Có thể khởi động PostgreSQL bằng `docker compose up -d postgres` từ thư mục gốc.
- Chạy backend từ `backend/` bằng `./gradlew bootRun`.

## Product requirements and direction

The attached product brief sets the intended direction for SyncFlow. Treat it as the product requirements source when planning future work. It specifies a **modular monolith** in Spring Boot, React + TypeScript frontend, PostgreSQL/Flyway, and Docker Compose deployment; do not introduce microservices.

### Target product

- Multi-workspace Kanban with workspace membership and Owner/Member/Viewer permissions.
- Boards, ordered lists/cards, comments, labels/assignees, activity timeline, notifications, search/filtering, and real-time board updates via STOMP.
- Workspace authorization must be enforced server-side, including IDOR checks and validation of related resources (assignee belongs to workspace; destination list belongs to same board).
- Consistent REST status codes and RFC 7807 style errors; request DTO validation, pagination capped at 100, optimistic locking for conflicting card edits, and service/integration tests for security and concurrency.
- Public IDs should ultimately be UUIDs. Use indexed fractional/numeric card positions with occasional rebalance. Keep Flyway migrations authoritative and Hibernate `ddl-auto: validate`.
- Deployment target is Docker Compose with Nginx, backend, and PostgreSQL. Keep secrets in environment/secrets, not committed configuration. Exclude Google login, file uploads, email notifications, and Redis from the initial scope.

### Auth/security requirements from brief

- Register/login/logout/refresh, profile view/edit, and password change; normalize email to lowercase, enforce unique email and password minimum 8 chars with letters and numbers.
- Login errors must be generic. Logout revokes the current refresh token; password changes revoke all of the user's refresh tokens.
- Access JWT target: 15 minutes, HS256 with a strong environment secret, claims limited to `sub`, `exp`, `iat`, `jti`; workspace roles stay dynamic and out of token claims.
- Refresh token target: random opaque token, hash stored in DB, rotated on refresh, reuse detection revokes the entire token family. Preferred browser delivery is HttpOnly + Secure + SameSite=Strict cookie scoped to `/api/auth`; access token stays in frontend memory.
- Add rate limits on auth endpoints, strict CORS, security headers, safe logging, and WebSocket CONNECT/SUBSCRIBE authorization. Only expose health from Actuator if/when added.

### Suggested implementation order

1. Finish auth hardening and API conventions (validation, email normalization, 15-minute access token, refresh family/reuse, logout/password change, consistent errors, rate limits).
2. Workspace and membership module with centralized permission checks and tests for cross-workspace IDOR and Owner invariants.
3. Board/list/card modules, ordering, board aggregate fetch without N+1, and `@Version` conflict handling.
4. Comments, activity events, notifications, then STOMP board updates with CONNECT and SUBSCRIBE authorization.
5. Search/filter/pagination, frontend feature modules and automatic access-token refresh, then Docker/Nginx deployment and CI/security checks.

This roadmap is a planning aid; complete one vertical slice at a time and update this section/status as implementation progresses.

### Gaps versus the target brief

Auth has since been hardened (see prior sessions). Remaining auth gaps: rate limiting is an in-memory fixed window per IP and endpoint, not per IP + email or shared across backend instances; security logs currently cover failed login and refresh reuse only; JWT secret must be supplied as `JWT_SECRET`; local PostgreSQL defaults remain in YAML/Compose; secure cookie defaults on (set `REFRESH_COOKIE_SECURE=false` only for local plain HTTP). Signed-in frontend supports workspace invitation links/acceptance, board/card loading and writes, comments, activity, notifications, board-scoped STOMP updates, and card search/filter. Invitation links are shareable but there is no outbound email delivery. Nginx deployment remains. Current IDs are numeric. Preserve these as explicit TODOs rather than assuming they are done.

## Latest session — 2026-11-12 (F5 comments, F6 activity, F7 notifications)

- Added Flyway migration `V10__comments_activity_notifications.sql`: comment edit timestamp/index, notification type/payload/index, and `activity_logs` with workspace/board/card/actor references and JSON metadata.
- Added comment CRUD and page-based listing at `/api/v1/cards/{cardId}/comments` and `/api/v1/comments/{id}`. Workspace membership is enforced; only the author may edit; author or Owner may delete. `@email` mentions resolve only current workspace members and create notifications.
- Added activity timelines at `/api/v1/cards/{cardId}/activities` and `/api/v1/boards/{boardId}/activities`. Card create/edit/move/delete, assignee changes, comment actions and workspace invite/join/role/remove/leave actions publish application events. `@TransactionalEventListener(AFTER_COMMIT)` persists activity and notification rows in a new transaction.
- Added notification list pagination, unread count, mark-one/read-all endpoints, and persisted notifications for assignment, mention, and invitation/member-join cases. WebSocket pushes are sent after notification commit to authenticated `/user/queue/notifications` subscribers.
- WebSocket STOMP endpoint `/ws` validates Bearer JWT on CONNECT and only allows the current user's notification queue on SUBSCRIBE. Frontend reconnects with backoff, displays the unread badge/dropdown, and provides older-page loading and read actions.
- Added task discussion modal with page-based comments and card activity; board timeline opens from the footer. Comments accept mentions as `@email` and the server only notifies matching workspace members.
- Updated invitation UX: notifications expose explicit Accept, Decline, and Decide later actions. Link-based invitations open a confirmation modal after sign-in and are no longer auto-accepted. Declining sets invitation status to `DECLINED`; ignoring leaves it `PENDING`. Added authenticated accept/decline endpoints keyed to the invited account and migration `V11__allow_declined_workspace_invitations.sql`.
- Added a **Leave workspace** action to the workspace switcher menu with confirmation. Non-Owners leave via `DELETE /api/v1/workspaces/{id}/members/me`, then the frontend reloads available workspaces. Owners see the existing transfer-ownership requirement and cannot confirm leaving.
- Verification: `backend/./gradlew compileJava` and `frontend/npm run build` succeeded after the invitation decision flow. No tests or servers were run; port 8080 was not intentionally opened. Flyway V10 and V11 will apply at next backend startup.
- Follow-up verification after adding leave-workspace UI: `frontend/npm run build` and `git diff --check` succeeded.
- Dropdown behavior: workspace switcher, task ellipsis, and notification popover now close on outside click or Escape and each has an X close control. Changed the profile action icon from X to a sign-out icon to avoid confusing it with dismiss. Frontend build and `git diff --check` succeeded.
- UI refresh based on the supplied Ignara reference: added persistent/system-default light and dark themes, a header theme switch, a clearer Inter + Manrope type scale, improved unread bell/badge styling, and dark surfaces for boards, cards, forms, menus, notifications and dialogs. The reference's template page describes dark/light modes and responsive navigation; its live hash-route demo was not available through the browser reader, so this is a SyncFlow-native visual adaptation rather than a pixel copy. Mobile topbar spacing was adjusted for the new controls.
- Final UI verification: `frontend/npm run build` and `git diff --check` succeeded. No tests or app servers were started.
- Remaining: richer member picker for mentions, email delivery for invitations, and Nginx deployment. Workspace invitation notification can only attach to a user account that already exists; the shareable link remains the invitation mechanism for unregistered addresses.
- F8/F9 follow-up: after-commit card events are broadcast to `/topic/boards/{boardId}`; STOMP CONNECT checks JWT and board SUBSCRIBE checks current workspace membership. The active board client subscribes, reloads on events, and reloads after reconnect. Search supports board/workspace scope, ILIKE title/description, assignee, board label, due range, priority, and page/size; V12 adds pg_trgm GIN indexes and board-scoped labels.
- UI follow-up: removed the top-right avatar control; workspace menu closes on outside click only (no X button), per latest request.
- Verification: backend `./gradlew compileJava` and frontend `npm run build` succeeded. No tests or servers were run; Flyway V12 will apply on next backend startup.

## Latest session — 2026-11-11 (frontend component refactor)

- Refactored the frontend view out of `App.tsx` into feature folders: `features/auth`, `features/board`, and `features/workspace`; shared layout/feedback components and view models live under `shared/`.
- Moved preview board data, view models, and API-to-task mapping into `frontend/src/shared/types.ts`; added feature barrel exports to keep imports short.
- Kept `App.tsx` responsible for session/workspace/board state, API calls, and event handlers; existing backend-backed board/card flows remain intact.
- Reformatted the TSX component markup and CSS into readable multiline code and added root `.editorconfig` for consistent two-space indentation and LF line endings.
- Verification: `cd frontend && npm run build` succeeded after formatting. No tests or backend server were run.

## Previous session — 2026-11-11 (F3 boards and lists)

- Added board CRUD and archive under workspace routes; Owner/Member can create, edit and archive; only Owner can delete. Archived boards are hidden by default and included with `?includeArchived=true`.
- Added board list create/rename/delete/move. Owner/Member can create, rename and move; Owner can delete. A non-empty list cannot be deleted; move or delete its cards first. Ordering uses fractional `double` positions and rebalances when adjacent gaps are under `0.001`.
- At the current implementation state, a new board starts with eight workflow lists: `Draft`, `Pending`, `In Progress`, `Blocked`, `In Review`, `Reopen`, `Done`, and `Cancelled`.
- Board detail returns all lists and cards nested in one HTTP response. Server fetches all lists and all cards in bulk queries (fixed query count, no per-card/list query loop); card CRUD and `@Version` are still F4.
- Added migration `V5__add_board_lists.sql`: create `board_lists`, archive flag, backfill existing card statuses into lists, add `cards.list_id`, convert card positions to double, and add `(list_id, position)` index. Board/list delete cascades to cards; application blocks deletion of a non-empty list.
- Added `WorkspacePermissionService` for shared member/editor/owner checks; nonmembers get 404, Viewer write attempts get 403.
- Verification: backend `compileJava` succeeded. `bootRun` applied Flyway through V5 and Hibernate initialized/validated entities, but Tomcat could not bind because port 8080 was already occupied. The run exited; I did not stop the unknown process. No tests were run. Frontend build last passed during the previous session; no frontend code changed in this F3 session.

### F3 limitations to carry forward

- Board detail has a fixed number of database queries but no benchmark/integration test for the 100-card / 300ms target.
- IDs remain BIGINT. Card response is read-only until F4; old `board_id` and `column_status` remain in the schema for migration compatibility.
- Current API is versioned `/api/v1/...`; board/list endpoints follow that prefix.

## Latest session — 2026-11-11 (frontend board/card persistence)

- Added card persistence endpoints and migration `V6__add_card_editing.sql` for card priority, optimistic-lock version, creator, and multiple assignees. Card create/update/delete/move/assignee operations validate workspace permissions; update/move/assignee writes require the current version and stale versions return 409.
- Frontend uses APIs to list boards and fetch a selected board with its lists/cards. Signed-in users can create boards, create cards with priority/due date, and move cards between lists; after writes, board data is reloaded from the API. The mock Kanban remains only for signed-out preview mode.
- Updated README and this work log. Card edit/assignee controls, list management UI, and F5-F9 features remain to be connected/implemented.
- Verification at implementation time: `./gradlew compileJava` and `npm run build` succeeded; no tests were run. Migration V6 was applied/validated in the follow-up startup check recorded below.

## Troubleshooting — 2026-11-12 (task creation report)

- Rechecked the create-task UI/API path: it submits `title`, enum `priority`, optional ISO `dueDate` to `POST /api/v1/lists/{listId}/cards`; the route exists and checks workspace editor permission.
- `./gradlew bootRun` connected to the configured PostgreSQL DB and validated all 6 Flyway migrations / initialized the current JPA model, but could not bind port 8080 because another process already owns it. Started the current backend on an ephemeral port to confirm startup, then stopped that process. Do not kill or replace the unknown process on 8080 without the user's authorization.
- Could not inspect the live API response: sandbox denied localhost socket access. The active 8080 process may be a stale backend build; restart the backend process using the current source before testing card creation. Need capture the exact UI/API error if creation still fails after restart.
- No application code changed in this troubleshooting pass; no tests were run. Frontend build remains successful from the prior session.

## Swagger JWT authorization — 2026-11-12

- Added an HTTP Bearer JWT scheme and global security requirement to the generated OpenAPI document, enabling Swagger UI's **Authorize** dialog for protected routes.
- Marked register/login/refresh/logout as public OpenAPI operations. A custom Springdoc index transformer adds a Swagger UI response interceptor: after successful `/api/v1/auth/login`, it reads `accessToken` and calls Swagger UI's Bearer preauthorization action. Users no longer need to copy/paste the token.
- Refresh still uses its HttpOnly cookie. Documented the automatic authorization behavior in README. `./gradlew compileJava` succeeded; no tests or server were run, so port 8080 was left untouched.

## Task creation database constraint — 2026-11-12

- Root cause of the reported `Dữ liệu xung đột với tài khoản hiện có`: V1 defined `cards.board_id` as `NOT NULL`; V5 moved card ownership to `list_id`, but card creation only writes `list_id`, leaving the obsolete required `board_id` empty and violating the DB constraint.
- Added Flyway `V7__remove_legacy_card_columns.sql` to remove obsolete `board_id`, `column_status`, and `assigned_to`. Their current equivalents are `board_lists.board_id`, `cards.list_id`, and `card_assignees`; V5/V6 already migrate old data before V7.
- Changed the generic integrity error text so it no longer incorrectly implies an account collision.
- Verification: `./gradlew compileJava` succeeded. No tests or server were run. Next step: restart backend to apply V7, then retry task creation. The existing process on port 8080 must be stopped/restarted by the user; this session did not change it.

## Task card actions and editing UI — 2026-11-12

- Changed the card's three-dot button from an immediate move action to an action menu with explicit **Edit task** and **Move to next list** items.
- Added edit modal for title, description, priority, and due date. Signed-in edits call `PUT /api/v1/cards/{id}` with optimistic-lock version then reload the board; preview edits remain local.
- Added styling for the popover menu and description field. `cd frontend && npm run build` succeeded. No tests were run.

## Add In review list — 2026-11-12

- Added `In review` as a default board list and Flyway `V8__add_in_review_list.sql` to add it to existing boards before `Done` when available (otherwise after the current final list). Existing cards and list positions are preserved.
- Updated README's board defaults. Backend compilation succeeded; no tests or server were run. Restart backend to apply V8.

## Standardize task workflow states — 2026-11-12

- Changed new-board defaults and the signed-out frontend preview to the eight requested states: Draft, Pending, In Progress, Blocked, In Review, Reopen, Done, Cancelled. Preview move action follows this order.
- Added Flyway V9 to normalize known old list names (for example `To do` → `Pending`, `Completed` → `Done`), retain cards, position standard states in workflow order, and preserve custom lists after them.
- Updated README. Backend compile and frontend production build succeeded. `./gradlew bootRun --args=--server.port=0` started successfully (Flyway applied V9 and Hibernate validated the model); I stopped that process afterward. No tests were run, and the existing process on port 8080 was left untouched.

## Workspace member invite and task assignment flow — 2026-11-12

- Added a frontend **Join with invitation** flow for a signed-in account. It calls the one-use invitation accept API, refreshes workspace membership, and selects the joined workspace; backend requires the signed-in email to match the invite.
- Added multi-select workspace members to the task edit modal and connected it to `PUT /api/v1/cards/{id}/assignees` using the latest card version. Backend already validates that each assignee belongs to the card's workspace.
- Task cards now show real member initials and names in avatar tooltips. README documents Owner invite → invited account joins → member assigned to task.
- Verification: frontend `npm run build` and backend `./gradlew compileJava` succeeded. No tests or backend server were run in this session.

## Shareable invitation links — 2026-11-12

- Replaced raw invitation-token toast with an invite modal that creates a URL containing the one-use invitation token and offers a Copy link action.
- Opening the invitation URL preserves the token through sign-in/sign-up; the frontend now shows an explicit confirmation modal after sign-in. Acceptance only happens when the user presses Join.
- Manual “Join with invitation” now accepts a pasted link as a fallback. No email/notification transport is configured, so the Owner shares the copied URL through their preferred channel.
- `cd frontend && npm run build` succeeded. No tests or backend server were run.

## Invitation account mismatch — 2026-11-12

- A 403 when accepting an invitation can happen when the shared link opens in a browser authenticated as the Owner/another account. The invitation remains pending and requires a session for the invited email.
- Added a dedicated safe 403 message that says the signed-in email does not match the invite without exposing the invited address. Frontend opens the login modal and retains the link so the user can review and explicitly accept after switching accounts.
- Backend `./gradlew compileJava` and frontend `npm run build` both succeeded. No tests or server were run; restart backend/frontend processes to load the changes.

## Previous session — 2026-11-11 (auth integration and workspace foundation)

### Frontend API connection — 2026-11-11

- Extended `frontend/src/api/client.ts` with typed profile, workspace, member, and invitation API calls using the existing in-memory access token, refresh-cookie flow, and shared 401 retry.
- On app load, the frontend restores the session via `/api/v1/auth/me`, then loads `/api/v1/workspaces`; the workspace switcher selects real workspaces and loads their members.
- Connected workspace creation and invitation creation to the backend, and wired account sign-out to `/api/v1/auth/logout`.
- Kept the Kanban task board as sample data because the backend currently has no board/task endpoints. Updated task feedback/footer so it no longer implies sample changes are persisted.
- Verification: `cd frontend && npm run build` succeeded. No backend services were started; port 8080 state was not changed.

- Updated the attached F1-F9 requirements as the source of truth. Existing backend routes use `/api/v1/...` (versioned) rather than the brief's unversioned `/api/...` examples.
- Frontend auth now uses `frontend/src/api/client.ts`: access token stays in module memory; refresh token is an HttpOnly cookie; simultaneous 401s share one refresh request; the failed API request retries once after refresh. Removed localStorage token writes.
- Added missing frontend React type packages; TypeScript and Vite production build now complete. Frontend auth helper clears old localStorage token keys when it loads.
- Added auth DTO validation, email normalization, BCrypt strength 12, 15-minute JWT with `sub/iat/exp/jti`, refresh cookie, rotation/reuse detection, logout, profile endpoints and password change revocation. Added IP/endpoint rate limit, single-origin CORS and ProblemDetail responses.
- Added workspace CRUD and membership APIs, owner/member/viewer roles, invite creation/acceptance with one-use hashed token and seven-day expiry, role update/transfer, member removal and leave.
- Workspace authorization is checked in `WorkspaceService`; nonmembers get 404. Owner-only actions are locked on the workspace row. The service preserves one Owner; ownership transfer demotes the prior owner; the Owner cannot leave/remove/demote themself first.
- Workspace deletion is hard delete, relying on the existing foreign keys' `ON DELETE CASCADE`. Invitation schema and member index are in migration V4. Expired pending invitations are marked expired so a new invitation can be issued.
- Public IDs are still BIGINT (the attached brief requests UUID). Switching the existing schema and all foreign keys to UUID remains a planned migration; do not claim this requirement is complete.
- Build verification then: `backend/./gradlew compileJava` succeeded after adding F2. `frontend/npm run build` first exposed missing `@types/react` packages; added `@types/react` and `@types/react-dom`, then the build succeeded. No test suite was run in that session.

### Remaining auth/workspace caveats

- Rate limits are in-memory per IP and route, not per IP+email and not shared between backend instances.
- Invitation creation returns the raw token to the Owner in the API response because email delivery is out of scope; caller must share the token/link.
- Access-token refresh helper is ready for API calls, but current board UI still uses sample data and is not wired to workspace/task APIs.
- The desired refresh-cookie path in the brief is `/api/auth`; current path remains `/api/v1/auth` to match the versioned API.
- WebSocket/role-based operation permissions within board/card modules are not implemented yet.

- Normalize email to lowercase on register, login, and profile update; request DTOs validate email, name, and password (8+ chars, letter + number).
- Set BCrypt strength to 12. Wrong login credentials now use a generic Vietnamese RFC 7807 `ProblemDetail` 401 response.
- Access JWT lifetime is 15 minutes by default; claims are limited to `sub`, `iat`, `exp`, and `jti`.
- Refresh token moved out of JSON into HttpOnly, SameSite=Strict cookie at `/api/v1/auth`; cookie Secure is configurable and defaults true. DB stores SHA-256 hash, family ID, expiry, and revoked time. Rotation locks token row; reuse revokes active tokens in that family.
- Add logout (revoke current refresh cookie), profile read/update, and password change (revoke all user's refresh tokens).
- Add IP + endpoint fixed-window limits: 10 requests/minute for register/login, 20/minute for refresh. This is in-memory and per backend instance; it does not yet key by email.
- Add single-origin CORS via `FRONTEND_ORIGIN`; default `http://localhost:5173`.
- Controller validation/auth errors and Spring Security 401/403 responses use RFC 7807 `ProblemDetail` JSON. Generic failed login message is `Email hoặc mật khẩu không đúng`.
- Add migration `V3__add_refresh_token_families.sql`.
- `JWT_SECRET` is now required; set a random value of at least 32 bytes. Database settings can be overridden using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Verification: `./gradlew compileJava` succeeded. No tests were run. Backend was not started, so port 8080 remains free.

## Initial implementation session — 2026-11-10

### Frontend foundation

- Added `frontend/` as a Vite + React + TypeScript app with a responsive SyncFlow project dashboard and Kanban board.
- Added sample task board, search and priority filtering, task creation modal, status movement, workspace navigation, project summary cards, and responsive styling.
- Added sign-in/sign-up modal wired to `/api/v1/auth/login` and `/api/v1/auth/register`; its old localStorage token handling was replaced by the in-memory/cookie client noted in the latest session. Vite proxies `/api` to the local backend on port 8080.
- Added local frontend run instructions to README. Frontend uses sample board data because backend workspace/task APIs are not implemented yet.
- No dependency installation or build was run in this session.

### Đã triển khai

- Thêm đăng ký tại `POST /api/v1/auth/register`; mật khẩu được băm bằng BCrypt.
- Thêm đăng nhập tại `POST /api/v1/auth/login`; thông tin không hợp lệ trả `401`.
- Login trả thông tin user công khai, access token JWT HS256, refresh token và `expiresIn` (giây).
- Access token có subject là user ID, thời hạn mặc định một ngày.
- Refresh token là chuỗi ngẫu nhiên; chỉ hash SHA-256 được lưu trong DB. `POST /api/v1/auth/refresh` xoay token, làm token cũ mất hiệu lực. Mặc định hết hạn sau bảy ngày.
- Tạo migration `V2__create_refresh_tokens.sql` cho bảng `refresh_tokens`.
- Thêm `JwtAuthenticationFilter`; các route mặc định yêu cầu `Authorization: Bearer <accessToken>`. Public routes: register, login, refresh, Swagger/OpenAPI và `/api/v1/test/**`.
- Thêm cấu hình môi trường `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`. Cần thay secret mặc định bằng secret riêng, ngẫu nhiên, dài tối thiểu 32 byte trước khi deploy.
- Cập nhật README mô tả cấu trúc backend và các endpoint auth.
- Bổ sung `.gitignore` để bỏ qua `.env`, cấu hình Spring local và file log. Quy tắc ignore đã kiểm tra với `git check-ignore`.

### Sự cố `bootRun` và cách xử lý

- Lần chạy đầu lỗi vì `JwtService` inject Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper`, nhưng Spring Boot 4 mặc định cấu hình Jackson 3 và không có bean Jackson 2 đó.
- Đã sửa `JwtService` tự tạo mapper Jackson 2 dùng riêng cho việc encode/parse JWT.
- Chạy lại `./gradlew bootRun`: biên dịch thành công, kết nối PostgreSQL, Flyway xác thực 2 migration và Spring báo ứng dụng đã start.
- Đã dừng tiến trình `bootRun` sau đó theo yêu cầu để giải phóng cổng 8080.

## Important implementation files

- `backend/src/main/java/com/syncflow/backend/service/AuthService.java` — đăng ký, xác thực login, phát token.
- `backend/src/main/java/com/syncflow/backend/service/JwtService.java` — tạo và kiểm tra JWT.
- `backend/src/main/java/com/syncflow/backend/service/RefreshTokenService.java` — tạo, hash, kiểm tra và xoay refresh token.
- `backend/src/main/java/com/syncflow/backend/config/JwtAuthenticationFilter.java` — xác thực Bearer token cho request.
- `backend/src/main/java/com/syncflow/backend/config/SecurityConfig.java` — cấu hình stateless security và routes public.
- `backend/src/main/java/com/syncflow/backend/service/WorkspaceService.java` — workspace/member permissions, owner transfer invariants and invitations.
- `backend/src/main/java/com/syncflow/backend/controller/WorkspaceController.java` and `InvitationController.java` — F2 REST endpoints.
- `backend/src/main/java/com/syncflow/backend/model/RefreshToken.java` and `repository/RefreshTokenRepository.java` — persistence cho refresh token.
- `backend/src/main/resources/db/migration/` — V1 baseline schema, V2 refresh-token table, V3 token families and V4 workspace invitation/index changes.
- `backend/src/main/resources/application.yaml` — database và JWT config; giá trị JWT mặc định chỉ dành cho môi trường local.

## Known gaps / next considerations

- No workspace tests or end-to-end HTTP verification have been run. Keep workspace CRUD and role/invitation invariants on the test backlog.
- Card mutation, comments, activity, notifications, search, STOMP, Nginx deployment, and production frontend integration remain unimplemented.
- Current IDs are BIGINT rather than the UUIDs in the brief.
- Local PostgreSQL settings still have development defaults; override via `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. `JWT_SECRET` is mandatory. `application.yaml` stays tracked as shared config.

## Update this file after each coding session

Khi hoàn thành một phiên thay đổi code, cập nhật ngày và ghi ngắn gọn:

1. Tính năng/sửa lỗi đã làm cùng các file chính.
2. Lệnh build/test/run đã thực hiện và kết quả (nếu có).
3. Tiến trình/dịch vụ đang chạy hay đã dừng, đặc biệt cổng 8080.
4. Việc còn thiếu hoặc quyết định cần giữ lại cho phiên sau.
