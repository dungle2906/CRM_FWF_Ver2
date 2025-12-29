# Hướng dẫn thiết lập biến môi trường trong Cursor IDE

## Cách 1: Sử dụng file `.env` (Khuyến nghị)

1. **Tạo file `.env`** ở thư mục gốc của project với nội dung:

```env
# Database Configuration
JDBC_CONNECTION_DB_URL=jdbc:mysql://localhost:3306/your_database_name
JDBC_CONNECTION_DB_NAME=your_username
JDBC_CONNECTION_DB_PASSWORD=your_password

# Mail Configuration
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# JWT Configuration
JWT_SECRET_KEY=your_jwt_secret_key_here_make_it_long_and_secure

# Stock Configuration
STOCK_ID=your_stock_id

# URL Configuration
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000
```

2. **Cài đặt extension** trong Cursor IDE:
   - Tìm và cài extension: **"DotENV"** hoặc **"env"** trong Extensions (Ctrl+Shift+X)

3. **Cập nhật file `.vscode/launch.json`**:
   - Mở file `.vscode/launch.json`
   - Thay thế các giá trị trong phần `"env"` bằng giá trị thực tế của bạn
   - Hoặc sử dụng extension để tự động load từ file `.env`

## Cách 2: Set trực tiếp trong launch.json

1. Mở file `.vscode/launch.json`
2. Tìm phần `"env"` và thay thế các giá trị placeholder bằng giá trị thực tế:

```json
"env": {
  "JDBC_CONNECTION_DB_URL": "jdbc:mysql://localhost:3306/your_database",
  "JDBC_CONNECTION_DB_NAME": "your_username",
  "JDBC_CONNECTION_DB_PASSWORD": "your_password",
  ...
}
```

## Cách 3: Set trong Terminal (Windows PowerShell)

Trước khi chạy ứng dụng, set các biến môi trường trong terminal:

```powershell
$env:JDBC_CONNECTION_DB_URL="jdbc:mysql://localhost:3306/your_database"
$env:JDBC_CONNECTION_DB_NAME="your_username"
$env:JDBC_CONNECTION_DB_PASSWORD="your_password"
$env:MAIL_USERNAME="your_email@gmail.com"
$env:MAIL_PASSWORD="your_app_password"
$env:JWT_SECRET_KEY="your_jwt_secret_key"
$env:STOCK_ID="your_stock_id"
$env:BACKEND_URL="http://localhost:8080"
$env:FRONTEND_URL="http://localhost:3000"

# Sau đó chạy ứng dụng
mvn spring-boot:run
```

## Cách 4: Sử dụng file `.env` với dotenv-java (Nâng cao)

Nếu bạn muốn Spring Boot tự động đọc từ file `.env`, bạn cần tích hợp dotenv-java vào `BasicCrmFwfApplication.java`:

```java
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class BasicCrmFwfApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();
        
        // Load các biến vào System properties
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        
        SpringApplication.run(BasicCrmFwfApplication.class, args);
    }
}
```

## Lưu ý

- File `.env` đã được thêm vào `.gitignore` để không bị commit lên git
- Không commit file `.env` chứa thông tin nhạy cảm
- Có thể tạo file `.env.example` (không chứa giá trị thực) để làm mẫu cho team

