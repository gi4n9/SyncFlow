# SyncFlow Work Log

Ghi chú bàn giao cho các phiên làm việc tiếp theo. Đọc file này trước khi sửa code, sau đó cập nhật mục **Latest session** khi hoàn thành thay đổi để phiên chat mới có thể tiếp tục từ trạng thái hiện tại.

## Project snapshot

- Repository hiện có backend Spring Boot và PostgreSQL qua Docker Compose; frontend được nhắc trong README nhưng chưa có trong working tree.
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
4. Comments, activity events, notifications, then STOMP real-time updates with CONNECT and SUBSCRIBE authorization.
5. Search/filter/pagination, frontend feature modules and automatic access-token refresh, then Docker/Nginx deployment and CI/security checks.

This roadmap is a planning aid; complete one vertical slice at a time and update this section/status as implementation progresses.

### Gaps versus the target brief

Auth has since been hardened (see the latest session below). Remaining auth gaps: rate limiting is currently an in-memory fixed window per IP and endpoint, not per IP + email and not shared across multiple backend instances; security events are not yet logged; JWT secret must be supplied as `JWT_SECRET`; local PostgreSQL defaults remain in YAML/Compose; secure cookie defaults on (set `REFRESH_COOKIE_SECURE=false` only for local plain HTTP). There are no workspace/board/card APIs, WebSocket, Nginx deployment, or frontend auth integration yet. Current IDs are numeric. Preserve these as explicit TODOs rather than assuming they are done.

## Latest session — 2026-11-10 (auth hardening)

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
- Added sign-in/sign-up modal wired to the existing `/api/v1/auth/login` and `/api/v1/auth/register`; login tokens are saved in local storage. Vite proxies `/api` to the local backend on port 8080.
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
- `backend/src/main/java/com/syncflow/backend/model/RefreshToken.java` and `repository/RefreshTokenRepository.java` — persistence cho refresh token.
- `backend/src/main/resources/db/migration/V1__init_schema.sql` and `V2__create_refresh_tokens.sql` — schema database.
- `backend/src/main/resources/application.yaml` — database và JWT config; giá trị JWT mặc định chỉ dành cho môi trường local.

## Known gaps / next considerations

- Chưa có logout/revoke endpoint cho refresh token; hiện token refresh bị thu hồi khi refresh thành công hoặc khi hết hạn.
- Chưa triển khai RBAC/role cho workspace; các route yêu cầu đăng nhập mới chỉ xác thực user.
- Chưa có frontend trong repository hiện tại.
- Chưa chạy test suite. Lần xác nhận gần nhất là chạy `bootRun` thành công; chưa kiểm thử toàn bộ luồng đăng ký, đăng nhập, refresh qua HTTP.
- `application.yaml` hiện vẫn chứa cấu hình local. Không ignore toàn bộ file này vì backend cần cấu hình mặc định; tạo `application-local.yaml` hoặc chuyển secret local sang env nếu cần cấu hình riêng.

## Update this file after each coding session

Khi hoàn thành một phiên thay đổi code, cập nhật ngày và ghi ngắn gọn:

1. Tính năng/sửa lỗi đã làm cùng các file chính.
2. Lệnh build/test/run đã thực hiện và kết quả (nếu có).
3. Tiến trình/dịch vụ đang chạy hay đã dừng, đặc biệt cổng 8080.
4. Việc còn thiếu hoặc quyết định cần giữ lại cho phiên sau.
