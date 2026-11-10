# SyncFlow Work Log

Ghi chú bàn giao cho các phiên làm việc tiếp theo. Đọc file này trước khi sửa code, sau đó cập nhật mục **Latest session** khi hoàn thành thay đổi để phiên chat mới có thể tiếp tục từ trạng thái hiện tại.

## Project snapshot

- Repository hiện có backend Spring Boot và PostgreSQL qua Docker Compose; frontend được nhắc trong README nhưng chưa có trong working tree.
- Backend nằm trong `backend/`, dùng Java 25, Spring Boot 4.1.2, Spring Security, Spring Data JPA, PostgreSQL và Flyway.
- Database mặc định: `jdbc:postgresql://localhost:5432/syncflow_db` (user `postgres`). Có thể khởi động PostgreSQL bằng `docker compose up -d postgres` từ thư mục gốc.
- Chạy backend từ `backend/` bằng `./gradlew bootRun`.

## Latest session — 2026-11-10

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
