# ⚙️ Spring Boot Backend Services

Dịch vụ backend được xây dựng trên nền tảng **Spring Boot 4.0.6** và **Java 21**, cung cấp toàn bộ API nghiệp vụ cho ứng dụng TikTok Clone, bao gồm xác thực người dùng, tải/xử lý video, livestreaming, thương mại điện tử (TikTok Shop), tìm kiếm toàn văn và hệ thống khuyến nghị video được tăng cường bởi AI.

---

## 🛠️ Công Nghệ Chủ Chốt

* **Ngôn ngữ & Runtime**: Java 21, Spring Boot 4.0.6, Gradle.
* **Xác thực & Bảo mật**: Spring Security, OAuth2 Client (Google & Facebook login), JWT (jjwt 0.12.6).
* **Cơ sở dữ liệu**:
  * **MySQL**: Lưu trữ dữ liệu quan hệ (người dùng, video, sản phẩm, đơn hàng, bình luận).
  * **Redis**: Caching dữ liệu, xử lý phiên làm việc (Session) và cơ chế khóa phân tán.
  * **OpenSearch**: Động cơ tìm kiếm phân tán cho video, hashtag và tài khoản người dùng.
* **Tích Hợp AI**: LangChain4j + Google Gemini API (dùng để duyệt video tự động và xếp hạng đề xuất video cá nhân hóa).
* **Xử lý Livestream**: Tích hợp LiveKit Server Java SDK để quản lý phòng live và sinh mã token phát sóng.
* **Lưu trữ đám mây**: AWS S3 SDK tương thích với Cloudflare R2 (lưu trữ file video) và Cloudinary (lưu trữ hình ảnh).
* **Nhận diện nhạc**: ACRCloud, Shazam API và AudD để phân tích và nhận diện nhạc nền trong video.
* **Cổng thanh toán**: Stripe SDK và PayPal SDK để thực hiện thanh toán đơn hàng trên TikTok Shop.

---

## 📂 Các Phân Hệ Nghiệp Vụ (Business Modules)

Mã nguồn backend nằm trong thư mục `src/main/java/com/back/` và được tổ chức thành các gói nghiệp vụ riêng biệt:

| Module | Mô tả chức năng chính |
| :--- | :--- |
| `user` | Đăng ký, đăng nhập (JWT, OAuth2), quản lý hồ sơ và cập nhật thông tin cá nhân. |
| `video` | Xử lý tải video lên, tích hợp nhận diện nhạc nền và đẩy video lên Cloudflare R2. |
| `moderation` | Hệ thống kiểm duyệt video tự động bằng Gemini AI trước khi đưa lên bảng tin công cộng. |
| `recommendation`| Giải thuật gợi ý video thông minh kết hợp LangChain4j (re-ranking) dựa trên điểm tương tác (Likes, Saves, Views) và độ mới của nội dung. |
| `livestream` | Quản lý trạng thái phòng live, sinh LiveKit Token cho streamer và người xem. |
| `shop` | Hệ thống TikTok Shop: quản lý cửa hàng, đăng sản phẩm, giỏ hàng, đơn hàng và thanh toán. |
| `comment` | Viết bình luận, phản hồi bình luận phân cấp và lượt thích bình luận. |
| `follow` | Theo dõi và hủy theo dõi tài khoản khác. |
| `hashtag` | Quản lý thẻ hashtag trong mô tả video và tìm kiếm theo hashtag. |
| `notification` | Gửi thông báo đẩy thời gian thực về các tương tác mới (follow, like, comment). |
| `search` | Tìm kiếm tối ưu hóa hiệu năng cao sử dụng OpenSearch. |

---

## 🔑 Cấu Hình Biến Môi Trường (`.env`)

Để khởi chạy backend, bạn cần tạo hoặc chỉnh sửa file `.env` nằm trong thư mục `/back`. Dưới đây là bảng đặc tả các biến số cấu hình cần thiết:

### 1. Cơ sở dữ liệu & Caching
* `DB_URL`: Đường dẫn kết nối MySQL (ví dụ: `jdbc:mysql://localhost:3306/toptop?...`).
* `DB_USERNAME` & `DB_PASSWORD`: Tài khoản đăng nhập MySQL.
* `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`: Thông tin kết nối Upstash Redis hoặc Redis local.

### 2. Cấu hình bảo mật & OAuth2
* `SECRET_KEY`: Khóa ký mã hóa JWT (chuỗi ký tự ngẫu nhiên dài tối thiểu 256 bits).
* `ACCESS_TOKEN_EXPIRATION_TIME`: Thời hạn hiệu lực của Access Token (ms).
* `REFRESH_TOKEN_EXPIRATION_TIME`: Thời hạn hiệu lực của Refresh Token (ms).
* `GOOGLE_CLIENT_ID` & `GOOGLE_CLIENT_SECRET`: Khóa xác thực ứng dụng Google Console.
* `FACEBOOK_APP_ID`: ID ứng dụng Facebook Developers.

### 3. Lưu trữ đám mây Cloudflare R2
* `R2_ACCOUNT_ID`: ID tài khoản Cloudflare.
* `R2_ACCESS_KEY` & `R2_SECRET_KEY`: Cặp khóa API truy cập R2 Bucket.
* `R2_BUCKET_NAME`: Tên thùng chứa lưu trữ video (`toptop-storage`).
* `R2_ENDPOINT`: Endpoint Cloudflare R2.
* `R2_PUBLIC_URL`: Tên miền công cộng để phát trực tuyến video.

### 4. Trí tuệ nhân tạo (AI) & Nhận diện âm nhạc
* `GEMINI_API_KEY`: API Key kết nối Google Gemini.
* `GEMINI_MODEL`: Model Gemini sử dụng (ví dụ: `gemini-2.5-flash`).
* `ACRCLOUD_HOST`, `ACRCLOUD_ACCESS_KEY`, `ACRCLOUD_ACCESS_SECRET`: Cấu hình nhận diện nhạc nền ACRCloud.

### 5. Livestream & Tìm kiếm
* `LIVEKIT_URL`: Địa chỉ máy chủ LiveKit (Giao thức `wss://`).
* `LIVEKIT_API_KEY` & `LIVEKIT_API_SECRET`: Khóa kết nối máy chủ LiveKit.
* `OPENSEARCH_URL`, `OPENSEARCH_USERNAME`, `OPENSEARCH_PASSWORD`: URL và thông tin đăng nhập OpenSearch.

---

## 🚀 Hướng Dẫn Vận Hành

### Chạy ứng dụng ở chế độ phát triển:
```bash
./gradlew bootRun
```

### Chạy các ca kiểm thử (Tests):
```bash
./gradlew test
```

### Biên dịch ra file JAR phân phối:
```bash
./gradlew build -x test
```
File thực thi JAR sau khi build thành công sẽ nằm ở thư mục `build/libs/back-0.0.1-SNAPSHOT.jar`.
