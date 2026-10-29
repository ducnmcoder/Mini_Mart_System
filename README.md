# MiniMart Management System

Ứng dụng web Spring Boot kết nối trực tiếp với database SQL Server `MiniMartSWD392`. Hệ thống có đăng nhập an toàn bằng BCrypt, điều hướng dashboard theo vai trò và quản lý thông tin cá nhân.

## Công nghệ

- Java 17, Spring Boot 3.5
- Spring MVC, Spring Security, Bean Validation
- Spring Data JPA, JDBC, Microsoft SQL Server Driver
- Thymeleaf, HTML5, CSS3 và JavaScript thuần
- Maven

## Kiến trúc hướng đối tượng và phân lớp

Luồng phụ thuộc được giữ một chiều:

```text
UI (Thymeleaf / CSS / JavaScript)
        ↓
Controller
        ↓
Service interface → Service implementation
        ↓
Repository
        ↓
Entity → SQL Server
```

```text
src/main/java/com/minimart/
├── config/              # Khởi tạo và cấu hình ứng dụng
├── controller/          # Nhận request, binding form, chọn view/redirect
├── dto/
│   ├── request/         # Dữ liệu đầu vào từ UI
│   └── response/        # Dữ liệu trả về cho UI
├── entity/              # Đối tượng nghiệp vụ và ánh xạ bảng database
├── repository/          # JPA repository và truy vấn thống kê
├── security/            # Xác thực, phân quyền, login handlers
├── service/             # Hợp đồng nghiệp vụ
│   └── impl/            # Phần triển khai nghiệp vụ
├── ui/                  # View name, assembler và view model
│   └── model/
└── validation/          # Annotation, validator và lỗi validation

src/main/resources/
├── templates/           # Lớp UI Thymeleaf
└── static/              # CSS, JavaScript và hình ảnh UI
```

Các nguyên tắc chính:

- Controller không gọi database trực tiếp.
- Service chỉ phụ thuộc vào repository interface và các service liên quan.
- Repository là nơi duy nhất chứa truy vấn dữ liệu.
- Entity đóng gói hành vi như cập nhật hồ sơ, đổi mật khẩu, ghi nhận đăng nhập và vô hiệu hóa tài khoản.
- DTO tách dữ liệu giao diện khỏi entity JPA.
- Validation tùy chỉnh kiểm tra số điện thoại và toàn bộ quy tắc đổi mật khẩu trước khi vào service.

## Chức năng đã có

- Kiểm tra tài khoản/mật khẩu không được để trống ở cả trình duyệt và server.
- Thông báo riêng khi tài khoản không tồn tại hoặc mật khẩu không đúng.
- BCrypt password, CSRF, session logout và phân quyền URL.
- Bốn dashboard: `ADMIN`, `OWNER`, `SALE_STAFF`, `INVENTORY_STAFF`.
- Số liệu dashboard đọc từ các bảng nghiệp vụ tương ứng trong database.
- Xem/sửa họ tên, thêm/sửa/xóa số điện thoại, đổi mật khẩu và lưu xuống `USER_ACCOUNT`.
- Ghi thay đổi hồ sơ xuống `AUDIT_LOG`.
- “Xóa tài khoản” được triển khai theo kiểu soft-delete (`status = INACTIVE`) để không làm hỏng các khóa ngoại/lịch sử giao dịch.
- Nút đăng xuất ở cuối menu bên trái trên cả bốn dashboard.
- Giao diện responsive cho desktop, tablet và điện thoại.

## 1. Chuẩn bị database

Mở SQL Server Management Studio và chạy **toàn bộ** file:

```text
C:\Users\vuan1\Downloads\MiniMartSWD392.sql
```

Script sẽ tạo database `MiniMartSWD392`, các bảng và dữ liệu mẫu. SQL Server service cần đang chạy và cho phép kết nối TCP/IP ở cổng `1433`.

## 2. Cấu hình kết nối

Ứng dụng không lưu mật khẩu SQL Server trong source code. Trên PowerShell, đặt biến môi trường cho phiên terminal hiện tại:

```powershell
$env:DB_USERNAME="sa"
$env:DB_PASSWORD="mat-khau-sql-server-cua-ban"
$env:APP_DEMO_PASSWORD="MiniMart@123"
```

Nếu server/instance/cổng khác mặc định, đặt toàn bộ JDBC URL:

```powershell
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=MiniMartSWD392;encrypt=true;trustServerCertificate=true"
```

> Khuyến nghị dùng một SQL Login riêng chỉ có quyền trên database `MiniMartSWD392`, thay vì tài khoản `sa`, khi triển khai thật.

## 3. Chạy ứng dụng

```powershell
mvn spring-boot:run
```

Mở [http://localhost:8080](http://localhost:8080).

Ở lần chạy đầu, ứng dụng chỉ thay các giá trị `!SET_BCRYPT_HASH_VIA_APPLICATION!` bằng BCrypt hash của `APP_DEMO_PASSWORD` và chuyển các tài khoản `PENDING_PASSWORD` tương ứng sang `ACTIVE`. Mật khẩu đã đổi sau đó sẽ không bị ghi đè ở những lần chạy tiếp theo.

## Tài khoản mẫu

| Dashboard | Tài khoản |
|---|---|
| ADMIN DASHBOARD | `admin@minimart.example` |
| OWNER DASHBOARD | `owner@minimart.example` |
| SALE STAFF DASHBOARD | `cashier1@minimart.example` hoặc `cashier2@minimart.example` |
| INVENTORY STAFF DASHBOARD | `inventory@minimart.example` |

Mật khẩu ban đầu là giá trị của `APP_DEMO_PASSWORD` (mặc định cho môi trường học tập: `MiniMart@123`). Hãy đổi mật khẩu ngay trong phần **Quản lý thông tin cá nhân**.

## Build và kiểm thử

```powershell
mvn test
mvn clean package
```

File chạy được tạo tại `target/mini-mart-system-0.0.1-SNAPSHOT.jar`.

## Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | SQL Server localhost:1433 / MiniMartSWD392 | JDBC connection string |
| `DB_USERNAME` | `sa` | SQL Server login |
| `DB_PASSWORD` | rỗng | SQL Server password |
| `APP_BOOTSTRAP_DEMO` | `true` | Kích hoạt tài khoản demo còn placeholder |
| `APP_DEMO_PASSWORD` | `MiniMart@123` | Mật khẩu khởi tạo cho tài khoản demo |
| `SERVER_PORT` | `8080` | Cổng HTTP của ứng dụng |

## Xử lý lỗi schema SQL Server

Ứng dụng dùng `PhysicalNamingStrategyStandardImpl` để giữ nguyên tên bảng trong SQL Server như `ROLE`, `USER_ACCOUNT` và `AUDIT_LOG`. Cấu hình này cần thiết khi database sử dụng collation phân biệt chữ hoa/chữ thường. Trước khi Hibernate kiểm tra schema, file `db/schema-compatibility.sql` sẽ tạo `dbo.AUDIT_LOG` nếu database được tạo từ phiên bản script cũ chưa có bảng này; bảng và dữ liệu hiện có không bị thay đổi.

Các database cũ dùng role `WAREHOUSE_STAFF` cũng được hỗ trợ và tự ánh xạ sang `INVENTORY_STAFF` trong ứng dụng.

Nếu terminal vẫn báo `Schema-validation: missing table`, hãy kiểm tra ứng dụng đang kết nối đúng database và bảng thực sự tồn tại:

```sql
SELECT DB_NAME() AS current_database;
SELECT TABLE_SCHEMA, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('ROLE', 'USER_ACCOUNT', 'AUDIT_LOG');
```

Kết quả cần là database `MiniMartSWD392` và ba bảng thuộc schema `dbo`. Nếu thiếu bảng, chạy lại toàn bộ file `MiniMartSWD392.sql` trong SSMS.
