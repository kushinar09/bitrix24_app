Đây là backend của ứng dụng kết nối với Bitrix24, được phát triển bằng Spring Boot.

---

## 1. Yêu cầu hệ thống

- Java 21 hoặc mới hơn
- Maven 3.6+

---

## 2. Clone project

```bash
git clone https://github.com/kushinar09/bitrix24_app
```

## 3. Cấu hình ứng dụng

File cấu hình chính nằm tại:

src/main/resources/application.yml

Bạn cần chỉnh sửa các giá trị sau trước khi chạy:
```
server:
  port: 8080
  
spring:
  profiles:
    active: local
  application:
    name: Bitrix24
    url: http://localhost:8080
    fe-url: # <Your Frontend URL> e.g. 'http://localhost:5174'
    ngrok-url: # <Your Ngrok URL> e.g. 'https://kangaroo-cute-halibut.ngrok-free.app'
    
bitrix:
  client-id: #<Your Client ID>
  client-secret: #<Your Client Secret>
  domain: #<Your Bitrix24 Domain> e.g. 'https://abc.bitrix24.com'
```
Lưu ý: Các giá trị như client-id, client-secret, domain phải được điền đúng theo ứng dụng bạn đã đăng ký với Bitrix24.

## 3.1. Cách lấy client-id, client-secret qua Bitrix24.
https://apidocs.bitrix24.com/local-integrations/serverside-local-app-with-no-ui.html

## 4. Build và chạy ứng dụng
Cài đặt dependencies và build project:
```
mvn clean install
```
Chạy ứng dụng:
```
mvn spring-boot:run
```
Ứng dụng sẽ chạy tại địa chỉ:
```
http://localhost:8080
```
## 5. Sử dụng ngrok để truy cập đến backend
  1. Đăng nhập/đăng ký ngrok qua link: https://ngrok.com/
  2. Chọn mục Domain
  ![image](https://github.com/user-attachments/assets/efc2a43e-7698-4599-98a1-e85b8adc9500)
  3. Tạo mới Domain (hoặc sử dụng Domain đã có sẵn)
  4. Khởi chạy truy cập -> **Start Tunnel**
  ![image](https://github.com/user-attachments/assets/a9a9233c-c134-48ab-ae46-fdb27e42621d)
  5. Sử dụng commend line qua termial để chỉ đến cổng backend (ví dụ: 8080)
## 6. API Endpoint
Các endpoint của API sẽ được sử dụng bởi frontend (ReactJS) tại địa chỉ: http://localhost:5174

## 7. Ghi chú

Nếu bạn dùng ngrok cho webhook hoặc xác thực OAuth, đảm bảo ngrok-url trong application.yml là địa chỉ công khai mới nhất.
Đảm bảo Bitrix24 được cấu hình đúng để trỏ về domain ngrok tương ứng.

## 8. Liên hệ

Mọi thắc mắc hoặc lỗi phát sinh, vui lòng liên hệ qua email: phongpd109.work@gmail.com
