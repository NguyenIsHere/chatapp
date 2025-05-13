# Chat App - Backend

Đây là phần backend cho ứng dụng Chat App, được xây dựng bằng Spring Boot. Ứng dụng hỗ trợ các tính năng chat real-time, bao gồm chat room, chat 1-1, xác thực người dùng, và quản lý trạng thái online.

## Các Công Nghệ Chính Sử Dụng

* **Java 17+**
* **Spring Boot 3.x+**
    * Spring Web (REST APIs)
    * Spring WebSocket (STOMP over WebSocket cho real-time messaging)
    * Spring Security (Xác thực JWT)
    * Spring Data MongoDB (Lưu trữ dữ liệu người dùng, tin nhắn)
    * Spring Data Redis (Quản lý trạng thái online của người dùng)
    * Spring Kafka (Message broker cho việc đồng bộ tin nhắn giữa các instance)
* **MongoDB:** Cơ sở dữ liệu NoSQL để lưu trữ thông tin người dùng, phòng chat, và lịch sử tin nhắn.
* **Redis:** In-memory data store, sử dụng để quản lý danh sách người dùng online và instance WebSocket mà họ đang kết nối.
* **Apache Kafka:** Distributed streaming platform, sử dụng làm message queue để xử lý tin nhắn bất đồng bộ và đồng bộ hóa giữa các instance backend.
* **Docker & Docker Compose:** Để đóng gói và triển khai ứng dụng cùng các services phụ thuộc (Redis, Kafka, Zookeeper, Nginx).
* **Nginx:** Web server, sử dụng làm Reverse Proxy và Load Balancer cho các instance backend.
* **JSON Web Tokens (JWT):** Cho việc xác thực và ủy quyền stateless.
* **Lombok:** Giảm thiểu code boilerplate.

## Tính Năng Chính

* **Xác thực người dùng:**
    * Đăng nhập bằng số điện thoại.
    * Đăng ký username nếu là người dùng mới.
    * Sử dụng JWT (Access Token & Refresh Token).
    * API cho refresh token.
    * API Logout.
* **Chat Real-time:**
    * **Chat Room:** Người dùng có thể tham gia vào các phòng chat và gửi/nhận tin nhắn.
    * **Chat 1-1 (Private Chat):** Người dùng có thể chat riêng tư với người dùng khác.
    * Sử dụng WebSocket với STOMP protocol.
* **Quản lý Trạng thái Online:**
    * Hiển thị trạng thái online/offline của người dùng.
    * Sử dụng Redis để lưu trữ trạng thái online và instance backend mà user đang kết nối WebSocket.
* **Tìm kiếm Người dùng:**
    * API cho phép tìm kiếm người dùng (ví dụ: theo username).
    * API lấy danh sách người dùng đang online.
* **Lưu trữ Lịch sử Chat:**
    * Lưu trữ tin nhắn private vào MongoDB.
    * API để tải lịch sử chat 1-1.
* **Khả năng mở rộng (Scalability):**
    * Hỗ trợ chạy nhiều instance backend.
    * Nginx làm load balancer (sử dụng thuật toán `least_conn` hoặc `round-robin`).
    * Kafka được sử dụng để broadcast tin nhắn đến tất cả các instance backend (mỗi instance có consumer group riêng cho topic tin nhắn), giúp các instance có thể gửi tin nhắn WebSocket đến client của mình.
    * Chống trùng lặp khi lưu tin nhắn vào DB (sử dụng `messageId` duy nhất và unique index trong MongoDB).

## Thiết Lập Môi Trường và Chạy Dự Án

### Yêu Cầu Cài Đặt:

* Java JDK 17 (hoặc phiên bản bạn đang dùng)
* Maven 3.x+ (hoặc Gradle)
* Docker và Docker Compose

### Biến Môi Trường Cần Thiết (Trong `docker-compose.yaml` hoặc file `.env`):

* `SPRING_DATA_MONGODB_URI`: URI kết nối đến MongoDB Atlas hoặc instance MongoDB cục bộ.
* `JWT_SECRET`: Khóa bí mật (nên là base64 encoded) để ký JWT.
* `JWT_ACCESS_EXPIRATION`: Thời gian sống của Access Token (milliseconds).
* `JWT_REFRESH_EXPIRATION`: Thời gian sống của Refresh Token (milliseconds).
* `INSTANCE_ID`: (Ví dụ: `chatapp_instance_1`, `chatapp_instance_2`) - Được đặt cho mỗi service backend trong `docker-compose.yaml`.
* `SPRING_KAFKA_BOOTSTRAP_SERVERS`: (Ví dụ: `kafka:9092`) - Địa chỉ Kafka brokers.
* `SPRING_KAFKA_CONSUMER_GROUP_ID`: (Ví dụ: `chat-group-instance-1`) - Đặt riêng cho mỗi instance backend để đảm bảo broadcast tin nhắn từ Kafka.
* `SPRING_DATA_REDIS_HOST`: (Ví dụ: `redis`) - Host của Redis.
* `SPRING_DATA_REDIS_PORT`: (Ví dụ: `6379`) - Port của Redis.

### Các Bước Chạy:

1.  **Clone repository (nếu có):**
    ```bash
    git clone <your-repo-url>
    cd <your-repo-directory>
    ```

2.  **Cấu hình MongoDB:**
    * Đảm bảo bạn có một instance MongoDB đang chạy (cục bộ hoặc trên Atlas).
    * Cập nhật `SPRING_DATA_MONGODB_URI` trong file `docker-compose.yaml` (trong phần `environment` của các service `chatapp*`) hoặc trong `application.properties` (nếu không chạy qua Docker Compose với override).

3.  **Build ứng dụng Spring Boot:**
    Sử dụng Maven:
    ```bash
    mvn clean package -DskipTests
    ```
    Hoặc sử dụng Gradle:
    ```bash
    ./gradlew build -x test
    ```
    Thao tác này sẽ tạo ra file `.jar` trong thư mục `target/` (cho Maven) hoặc `build/libs/` (cho Gradle). Đảm bảo tên file này khớp với tên file trong `Dockerfile` (ví dụ: `chatapp-0.0.1-SNAPSHOT.jar`).

4.  **Chuẩn bị file `nginx.conf`:**
    Đặt file `nginx.conf` (đã được cung cấp và cấu hình `least_conn`) ở cùng thư mục gốc với `docker-compose.yaml`.

5.  **Chạy với Docker Compose:**
    Từ thư mục gốc của dự án (nơi chứa `docker-compose.yaml`):
    ```bash
    docker-compose up --build -d
    ```
    * `--build`: Xây dựng lại image nếu có thay đổi trong `Dockerfile` hoặc source code.
    * `-d`: Chạy ở chế độ detached (chạy nền).

6.  **Kiểm tra trạng thái các container:**
    ```bash
    docker ps -a
    ```
    Đảm bảo các container `chatapp1-container`, `chatapp2-container`, `nginx-loadbalancer`, `kafka`, `zookeeper`, `my-redis-container` đều đang ở trạng thái `Up`.

7.  **Truy cập ứng dụng:**
    * Backend API (qua Nginx) sẽ có thể truy cập tại: `http://localhost` (nếu Nginx đang map port 80) hoặc `http://localhost:PORT_NGINX` (nếu bạn đổi port cho Nginx trong `docker-compose.yaml`). Ví dụ: `http://localhost/api/v1/auth/login`.
    * WebSocket endpoint (qua Nginx): `ws://localhost/ws`.

8.  **Xem logs:**
    ```bash
    docker-compose logs -f chatapp1 # Xem log của instance 1
    docker-compose logs -f chatapp2 # Xem log của instance 2
    docker-compose logs -f nginx-lb # Xem log của Nginx
    docker-compose logs -f kafka
    docker-compose logs -f redis
    ```
    Hoặc dùng `docker logs <container_name>`.

9.  **Dừng ứng dụng:**
    ```bash
    docker-compose down
    ```
    Để xóa cả volumes (dữ liệu của Redis, Kafka), thêm cờ `-v`:
    ```bash
    docker-compose down -v
    ```

## API Endpoints Chính (Ví dụ)

* **Authentication (`/api/v1/auth`):**
    * `POST /login`: Đăng nhập, trả về Access Token và Refresh Token.
    * `POST /register`: Đăng ký username cho người dùng mới (sau khi login lần đầu không thành công và nhận được trạng thái "NEW_USER").
    * `POST /refresh`: Làm mới Access Token bằng Refresh Token.
    * `POST /logout`: Đăng xuất.
* **Users (`/api/v1/users`):**
    * `GET /search?usernameQuery={query}`: Tìm kiếm người dùng.
    * `GET /online`: Lấy danh sách người dùng đang online.
    * `GET /{phoneNumber}`: Lấy thông tin chi tiết của một người dùng.
* **Rooms (`/api/v1/rooms` - nếu bạn đã implement đầy đủ):**
    * `POST /`: Tạo phòng mới.
    * `GET /{roomId}`: Tham gia/Lấy thông tin phòng.
    * `GET /{roomId}/messages`: Lấy lịch sử tin nhắn của phòng.
* **Chat History (`/api/v1/chat-history` - nếu bạn đã implement):**
    * `GET /private/{user1PhoneNumber}/{user2PhoneNumber}`: Lấy lịch sử chat 1-1.

## WebSocket Endpoints (STOMP)

* **Kết nối WebSocket ban đầu:** `ws://<your_nginx_host_port>/ws`
* **Client gửi tin nhắn:**
    * Room chat: `/app/room.sendMessage/{roomId}`
    * Private chat: `/app/private.sendMessage`
* **Client subscribe để nhận tin nhắn:**
    * Room chat: `/topic/room/{roomId}`
    * Private chat (cho user cụ thể): `/user/queue/private-messages` (Spring sẽ tự động xử lý prefix `/user` để định tuyến đến đúng session của người dùng đã xác thực).

## Các Điểm Cần Lưu Ý và Cải Thiện Tiềm Năng

* **Xử lý lỗi chi tiết hơn:** Thêm các exception handler tùy chỉnh.
* **Validation:** Validate DTO đầu vào.
* **Bảo mật:** Review kỹ cấu hình Spring Security, JWT. Cân nhắc các biện pháp chống tấn công phổ biến.
* **Phân trang và Sắp xếp:** Áp dụng cho các API trả về danh sách.
* **Testing:** Viết unit test và integration test.
* **Dead Letter Queue (DLQ) cho Kafka:** Xử lý các tin nhắn không thể tiêu thụ được.
* **Push Notifications:** Cho người dùng offline.
* **Tối ưu hóa hiệu năng:** Cho các truy vấn DB, xử lý Kafka.
* **Document API:** Sử dụng Swagger/OpenAPI.

---

Cảm ơn bạn đã cung cấp thông tin chi tiết về dự án. Hy vọng file README này sẽ hữu ích!