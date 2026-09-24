# ⏱️ KỊCH BẢN LIVE DEMO 5.5 PHÚT (TỪNG BƯỚC BẤM GIỜ)
## ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
**Môn học:** Lập Trình Mạng (PTIT)  
**Giảng viên chấm:** TS. Đặng Ngọc Hùng  
**Mục tiêu:** Trình diễn trực quan, mượt mà, phơi bày rõ ràng bản chất kỹ thuật của 4 mô hình luồng, không để xảy ra thời gian chết (dead time).

---

## 🎯 BẢNG TỔNG QUAN TIẾN TRÌNH LIVE DEMO

| Thời Gian | Phân Cảnh | Thao Tác Kỹ Thuật | Hiện Tượng Trực Quan Cần Chỉ Cho Thầy Xem |
| :---: | :--- | :--- | :--- |
| **00:00 – 01:00**<br>(1 phút) | **Khởi Động & Giới Thiệu Dashboard** | - Chạy `run_server.bat 4`<br>- Mở trình duyệt `http://localhost:8080/dashboard` | - Giao diện Dark-tech thời gian thực.<br>- 6 thẻ KPI & 4 biểu đồ Canvas cập nhật mỗi 500ms.<br>- Mức nền tài nguyên: 0 RPS, ~10 OS Threads, 4MB RAM. |
| **01:00 – 02:15**<br>(1.25 phút) | **Đối Chứng: Single-Thread vs Đa Luồng** | - Chạy Mode 1 (`run_server.bat 1`) bắn tải.<br>- Chuyển sang Mode 2 (`run_server.bat 2`) bắn tải. | - Mode 1: 30 requests delay 100ms mất **3.0 giây** tuần tự.<br>- Mode 2: 30 requests delay 100ms xong ngay trong **110ms**.<br>- Thầy thấy ngay giá trị sống còn của đa luồng. |
| **02:15 – 03:45**<br>(1.5 phút) | **Thử Thách Tải Cao: C1000 Stress & Worker Pool** | - Bắn 1,000 requests vào Mode 2.<br>- Chuyển sang Mode 3 (`run_server.bat 3`) bắn 1,000 reqs. | - Mode 2: OS Threads vọt lên hàng nghìn, CPU nhảy vì Context Switch.<br>- Mode 3: Luồng bị khóa cứng ở 16 luồng, task xếp hàng trong Queue và tiêu thụ êm ái. |
| **03:45 – 05:00**<br>(1.25 phút) | **Đỉnh Cao: Java 21 Project Loom Virtual Threads** | - Bật Mode 4 (`run_server.bat 4`).<br>- Chạy `run_benchmark.bat` bắn 2,500 kết nối đồng thời. | - 2,500 kết nối xử lý trong chớp mắt.<br>- Đồ thị **Active Connections vọt lên cao nhưng OS Threads vẫn phẳng lì ở mức 15 luồng**!<br>- Thấy rõ chữ ký `"isVirtual": true`. |
| **05:00 – 05:30**<br>(0.5 phút) | **Kết Luận Live Demo** | - Mở file CSV `benchmark_results.csv`.<br>- Tóm tắt kết quả. | - Bảng số liệu khoa học chứng minh thông lượng tăng gấp nhiều lần.<br>- Chuyển lời mời thầy đặt câu hỏi phản biện Q&A. |

---

## 🎬 KỊCH BẢN CHI TIẾT TỪNG PHÚT (KÈM LỜI THOẠI MẪU)

### 📍 PHÂN CẢNH 1: KHỞI ĐỘNG MÁY CHỦ & GIỚI THIỆU WEB DASHBOARD (00:00 – 01:00)

* **Thao tác:**
  1. Mở cửa sổ Terminal/Command Prompt trong thư mục dự án.
  2. Gõ lệnh khởi động máy chủ:
     ```cmd
     run_server.bat 4
     ```
  3. Mở trình duyệt Chrome/Edge truy cập địa chỉ:
     `http://localhost:8080/dashboard`
* **Lời thoại trình bày:**
  > *"Kính thưa thầy, để trực quan hóa toàn bộ hành vi đa luồng ở mức hệ điều hành, em đã tự tay xây dựng một Web Dashboard thời gian thực cập nhật chu kỳ 500ms bằng 100% Native Canvas không dùng bất kỳ thư viện bên ngoài nào.*  
  > *Như thầy có thể quan sát trên màn hình:*  
  > *- Phía trên là Badge trạng thái hiển thị máy chủ đang chạy ở **Mode 4: Java 21 Virtual Threads**.*  
  > *- 6 thẻ KPI phía trên phản ánh thông lượng RPS, số lượng kết nối đang mở, độ trễ phản hồi, và đặc biệt là chỉ số **OS Native Threads** lấy trực tiếp từ JMX.*  
  > *- Phía dưới là 4 biểu đồ thời gian thực và trung tâm điều khiển Live Demo."*

---

### 📍 PHÂN CẢNH 2: ĐỐI CHỨNG SINGLE-THREAD (MODE 1) VS MULTI-THREAD (MODE 2) (01:00 – 02:15)

* **Thao tác:**
  1. Nhấn `Ctrl + C` tắt server hiện tại, khởi động **Mode 1: Iterative Server**:
     ```cmd
     run_server.bat 1
     ```
  2. Refresh lại Dashboard.
  3. Tại mục **🎮 DEMO CONTROL CENTER**, click nút:
     `⚡ Burst 30 Reqs (Delay 100ms)`
  4. Quan sát Terminal Log và đồ thị.
* **Lời thoại trình bày:**
  > *"Đầu tiên, em khởi động **Mode 1 - Single-Threaded Iterative Server**. Em sẽ bắn 30 requests vào endpoint `/api/delay?ms=100` mô phỏng truy vấn cơ sở dữ liệu.*  
  > *Thầy có thể thấy trên màn hình: Vì chỉ có 1 luồng duy nhất xử lý tuần tự, tổng thời gian hoàn tất mất đúng **3,000ms (tức 3 giây)**! Trong lúc luồng bận xử lý request số 1, thì 29 request còn lại bị giam lỏng hoàn toàn trong hàng đợi TCP Backlog của Hệ điều hành.*  
  > *Bây giờ, em chuyển sang **Mode 2 - Thread-per-Connection**:*  
  > *(Khởi động `run_server.bat 2` và click lại nút Burst 30 Reqs)*  
  > *Ngay lập tức, 30 requests hoàn tất trong chỉ **110ms**! Đồ thị Active Connections và OS Threads nhảy đồng thời 30 luồng. Đây là minh chứng rõ nhất vì sao lập trình mạng bắt buộc phải ứng dụng đa luồng."*

---

### 📍 PHÂN CẢNH 3: THỬ THÁCH GIỚI HẠN TẢI CAO (C1000) & WORKER THREAD POOL (02:15 – 03:45)

* **Thao tác:**
  1. Khi server đang chạy ở Mode 2, click nút:
     `🚀 Spike 100 Reqs Đồng Thời!` hoặc chạy terminal benchmark:
     ```cmd
     benchmark\run_benchmark.bat -c 500 -n 1000 --url http://localhost:8080/api/delay?ms=100
     ```
  2. Chỉ cho thầy thấy: Số lượng **OS Native Threads** trên Dashboard tăng vọt lên hàng trăm luồng, RAM Heap tăng nhanh, CPU bắt đầu giật lag vì Context Switching.
  3. Dừng Mode 2, chuyển sang **Mode 3: Worker Thread Pool**:
     ```cmd
     run_server.bat 3
     ```
  4. Bắn lại đúng gói tải 500 clients, 1,000 requests đó.
* **Lời thoại trình bày:**
  > *"Tuy Mode 2 giải quyết được bài toán tuần tự, nhưng khi số lượng kết nối tăng vọt lên hàng trăm hay hàng nghìn, mỗi luồng OS ngốn 1MB Stack Memory, làm nảy sinh chi phí chuyển ngữ cảnh (Context Switching) đắt đỏ khiến CPU kiệt quệ.*  
  > *Để khắc phục, các hệ thống doanh nghiệp sử dụng **Mode 3 - Worker Thread Pool với Hàng đợi Bounded Queue** mà em đã lập trình ở đây.*  
  > *Như thầy thấy trên biểu đồ số 3:*  
  > *- Dù có 500 kết nối gửi đến, số lượng **OS Platform Threads được khống chế nghiêm ngặt ở mức 16 luồng cố định** (Core Pool Size).*  
  > *- Các task tự động xếp hàng ngăn nắp trong Queue và được 16 worker tiêu thụ tuần tự.*  
  > *- RAM phẳng lì, CPU không hề bị nghẽn Context Switch. Nếu hàng đợi bị quá tải 1,000 tasks, server kích hoạt Rejection Policy trả về ngay HTTP 503 Service Unavailable để tự bảo vệ, không bao giờ bị crash tiến trình."*

---

### 📍 PHÂN CẢNH 4: ĐỈNH CAO JAVA 21 PROJECT LOOM VIRTUAL THREADS (03:45 – 05:00)

* **Thao tác:**
  1. Tắt Mode 3, khởi động **Mode 4: Java 21 Virtual Threads**:
     ```cmd
     run_server.bat 4
     ```
  2. Mở một cửa sổ dòng lệnh thứ 2, chạy kịch bản thử thách cực đại:
     ```cmd
     benchmark\run_benchmark.bat -c 1000 -n 3000 --url http://localhost:8080/api/delay?ms=50
     ```
  3. Chỉ tay lên Dashboard vào **Biểu đồ số 3 (Active Connections vs OS Native Threads)**.
* **Lời thoại trình bày (Điểm chốt hạ ấn tượng nhất):**
  > *"Và thưa thầy, đây là điểm đột phá lớn nhất của đề tài: **Mode 4 - Java 21 Virtual Threads (Project Loom)**.*  
  > *Em vừa phát sinh đồng thời **1,000 kết nối đồng thời** bắn 3,000 requests.*  
  > *Xin thầy hãy quan sát kỹ **Biểu đồ số 3**:*  
  > *- Đường màu xanh lá (Active Connections) vọt lên đỉnh 1,000 kết nối.*  
  > *- Nhưng đường màu đỏ (**OS Native Threads**) hoàn toàn nằm phẳng lì ở mức **chỉ 15–16 luồng**!*  
  > *- Nguyên lý: Khi một Virtual Thread bị block ở thao tác mạng hoặc delay, JVM tự động unmount nó khỏi Carrier Thread của OS và đưa luồng khác vào chạy. Khi có dữ liệu, nó mount trở lại.*  
  > *Kết quả đo đạc: Thông lượng đạt hơn **1,000 Requests/giây (RPS)**, độ trễ P95 chỉ vài chục mili-giây, và RAM chỉ tiêu tốn vỏn vẹn vài chục MB! Hệ thống chạy êm ái, đạt chuẩn bài toán C10K hiện đại."*

---

### 📍 PHÂN CẢNH 5: TỔNG KẾT LIVE DEMO & MỜI PHẢN BIỆN (05:00 – 05:30)

* **Thao tác:**
  1. Mở file [benchmark/benchmark_results.csv](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/benchmark/benchmark_results.csv) trên màn hình.
  2. Chỉ vào các cột so sánh Throughput RPS và Latency giữa 4 Mode.
* **Lời thoại trình bày:**
  > *"Toàn bộ kết quả thực nghiệm của các Mode đều đã được công cụ `JavaLoadTester` tự động lưu lại vào file CSV với đầy đủ dấu ấn thời gian và phân vị độ trễ.*  
  > *Phần trình diễn Live Demo của em đến đây là kết thúc. Em xin kính mời thầy Đặng Ngọc Hùng đặt câu hỏi phản biện ạ!"*
