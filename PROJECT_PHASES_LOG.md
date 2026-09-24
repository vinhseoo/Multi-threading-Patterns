# 📋 NHẬT KÝ THEO DÕI TIẾN ĐỘ DỰ ÁN (PROJECT PHASES LOG)
**Đề tài T45:** Multi-threading Patterns in Network Programming  
**Môn học:** Lập Trình Mạng (PTIT)  
**Thời gian cập nhật:** 24/09/2026  
**Trạng thái chung:** 🟢 ĐÃ HOÀN THÀNH 100% (5/5 Phases hoàn thành)

---

## 📌 BẢNG TỔNG QUAN CÁC GIAI ĐOẠN

| Phase | Tên giai đoạn | Trạng thái | Tỉ lệ hoàn thành |
| :---: | :--- | :---: | :---: |
| **Phase 1** | Nền tảng cốt lõi & HTTP Protocol Engine | ✅ Hoàn thành | 100% |
| **Phase 2** | Bộ 3 Mô hình Đa luồng & Custom Thread Pool | ✅ Hoàn thành | 100% |
| **Phase 3** | Hệ thống Đo lường (Metrics) & Real-time Web Dashboard | ✅ Hoàn thành | 100% |
| **Phase 4** | Công cụ Benchmark & Kịch bản Bắn tải Thực nghiệm | ✅ Hoàn thành | 100% |
| **Phase 5** | Tài liệu Báo cáo, Kịch bản Demo 5.5 phút & Bộ Q&A Thầy Hùng | ✅ Hoàn thành | 100% |

---

## 🛠️ CHI TIẾT CÁC CHECKLIST ĐÁNH DẤU THEO PHASE

### 🔹 PHASE 1: Nền Tảng Cốt Lõi & HTTP Protocol Engine
> **Mục tiêu:** Xây dựng khung dự án Clean Architecture, bộ cấu hình, bộ phân tích gói tin HTTP/1.1 thủ công và mô hình xử lý tuần tự cơ bản (Mode 1).

- [x] **1.1. Cấu hình môi trường & Công cụ biên dịch**
  - [x] Tạo file `pom.xml` chuẩn Maven (Java 21 source/target).
  - [x] Tạo script `build.bat` biên dịch trực tiếp bằng JDK 21 (`C:\Users\maiduc.vinh\.jdks\ms-21.0.10`).
  - [x] Tạo script `run_server.bat` hỗ trợ chạy server 1-click không cần IDE.
- [x] **1.2. Module cấu hình máy chủ (`config`)**
  - [x] Tạo `ServerConfig.java` quản lý Port (`8080`), Core Pool Size, Max Pool Size, Queue Capacity, Backlog, Keep-Alive timeout.
- [x] **1.3. Module giao thức HTTP/1.1 thủ công (`http`)**
  - [x] Tạo `HttpRequest.java`: Parse method (`GET`, `POST`), URI, query parameters (`?ms=100`, `?n=30`), headers.
  - [x] Tạo `HttpResponse.java`: Builder tạo HTTP headers (`HTTP/1.1 200 OK`, `Content-Type`, `Content-Length`, `Connection`).
  - [x] Tạo `HttpHandler.java`: Định tuyến URL router và điều hướng xử lý logic.
- [x] **1.4. Lớp máy chủ cơ sở & Mô hình tuần tự (`server`)**
  - [x] Tạo `BaseHttpServer.java`: Khung trừu tượng định nghĩa lifecycle (`start()`, `stop()`, `isRunning()`).
  - [x] Tạo `IterativeServer.java` (**Mode 1**): Xử lý tuần tự trên 1 luồng duy nhất (mốc đối chứng baseline).
- [x] **1.5. Kiểm thử Phase 1**
  - [x] Biên dịch thành công 0 lỗi bằng `build.bat`.
  - [x] Chạy thử Mode 1, truy cập `http://localhost:8080/api/hello`, `/api/delay`, `/api/compute` phản hồi chuẩn xác.

---

### 🔹 PHASE 2: Bộ 3 Mô Hình Đa Luồng & Custom Thread Pool
> **Mục tiêu:** Cài đặt đầy đủ 3 mô hình xử lý song song, trong đó có phần tự viết Thread Pool từ con số 0 để lấy trọn 30% điểm Technical Depth.

- [x] **2.1. Mô hình Thread-per-Connection (`server/ThreadPerConnServer.java`)**
  - [x] Cài đặt **Mode 2**: Khởi tạo 1 OS Thread mới cho mỗi kết nối client vào (`new Thread(...).start()`).
  - [x] Quản lý đóng Socket an toàn, ghi nhận log luồng.
- [x] **2.2. Tự lập trình Custom Thread Pool (`pool/`)**
  - [x] Tạo `CustomWorker.java`: Luồng thợ tự lập trình lấy task từ hàng đợi và thực thi.
  - [x] Tạo `CustomThreadPool.java`: Tự cài đặt cơ chế Producer-Consumer với Bounded Blocking Queue, worker pool size và hàm graceful shutdown.
  - [x] Tạo `CustomThreadPoolServer.java` (**Mode 5**): Máy chủ chạy trên Custom Thread Pool tự viết để kiểm chứng thực nghiệm.
- [x] **2.3. Mô hình Worker Thread Pool chuẩn doanh nghiệp (`server/WorkerThreadPoolServer.java`)**
  - [x] Cài đặt **Mode 3**: Tích hợp `ThreadPoolExecutor` của Java Concurrency.
  - [x] Cấu hình Bounded ArrayBlockingQueue.
  - [x] Cài đặt cơ chế Backpressure / Rejection Policy (`AbortPolicy` trả về HTTP 503 Service Unavailable kèm Retry-After).
- [x] **2.4. Mô hình hiện đại Java 21 Virtual Threads (`server/VirtualThreadServer.java`)**
  - [x] Cài đặt **Mode 4**: Sử dụng `Executors.newVirtualThreadPerTaskExecutor()`.
  - [x] Xử lý non-blocking mounting/unmounting ngầm định từ JVM Project Loom (`isVirtual: true`).
- [x] **2.5. Điểm vào ứng dụng (`Main.java`)**
  - [x] Menu tương tác Console cho phép người dùng chọn Mode (1, 2, 3, 4, 5) hoặc truyền argument khi khởi động.
- [x] **2.6. Kiểm thử Phase 2**
  - [x] Khởi động và kiểm tra hoạt động độc lập của cả 5 mode (Mode 1, Mode 2, Mode 3, Mode 4, Mode 5).
  - [x] Kiểm tra cơ chế đóng kết nối an toàn (Graceful Shutdown) và giải phóng Socket.

---

### 🔹 PHASE 3: Hệ Thống Đo Lường (Metrics) & Real-time Web Dashboard
> **Mục tiêu:** Xây dựng cơ chế thu thập metrics không khóa (Lock-free) và giao diện Web Dashboard trực quan theo dõi tức thời trạng thái CPU, RAM, RPS, Latency khi bắn tải.

- [x] **3.1. Engine thu thập chỉ số hiệu năng (`metrics`)**
  - [x] Tạo `ServerMetrics.java`: Dùng `LongAdder` và `AtomicLong` đếm Total Requests, Requests/sec (RPS), Active Threads, Error Count, Latency (Min, Max, Avg, P95).
  - [x] Tạo `SystemMetrics.java`: Dùng `OperatingSystemMXBean` và `MemoryMXBean` đo CPU usage %, JVM Heap RAM, Non-Heap RAM, OS Threads count.
- [x] **3.2. Bổ sung các API kịch bản thực nghiệm**
  - [x] `GET /api/hello`: Network I/O thuần túy đo thông lượng trần.
  - [x] `GET /api/delay?ms=...`: Giả lập I/O-bound (chờ I/O, database, microservice).
  - [x] `GET /api/compute?n=...`: Giả lập CPU-bound (tính toán đệ quy Fibonacci nặng).
  - [x] `GET /api/metrics`: Xuất toàn bộ dữ liệu thống kê dạng JSON cho Dashboard.
  - [x] `GET /dashboard`: Trả về giao diện web dashboard.
- [x] **3.3. Xây dựng Real-time Web Dashboard (`src/main/resources/web/`)**
  - [x] `index.html`: Bố cục Dashboard phong cách Dark-tech hiện đại, các thẻ chỉ số (KPI cards) và khu vực biểu đồ.
  - [x] `style.css`: Thiết kế giao diện cao cấp, dark mode, hiệu ứng chuyển động mượt mà, responsive.
  - [x] `dashboard.js`: Tự động gửi request đến `/api/metrics` mỗi 500ms, vẽ 4 biểu đồ Canvas thời gian thực (RPS, Latency, Concurrency vs OS Threads, CPU & RAM), tích hợp Live Demo Control Center.
- [x] **3.4. Kiểm thử Phase 3**
  - [x] Mở trình duyệt `http://localhost:8080/dashboard` kiểm tra đồ thị và tài nguyên phản hồi 200 OK.
  - [x] Kiểm tra endpoint `/api/metrics` xuất đầy đủ thông số runtime của Mode, Server và System.

---

### 🔹 PHASE 4: Công Cụ Benchmark & Kịch Bản Bắn Tải Thực Nghiệm
> **Mục tiêu:** Tự động hóa đo đạc hiệu năng với công cụ benchmark thuần Java 21 (zero-dependency) và xuất kết quả đối sánh.

- [x] **4.1. Công cụ Benchmark tải đa luồng**
  - [x] Tạo `JavaLoadTester.java`: Dùng Java 21 Virtual Threads bắn đồng thời hàng nghìn request, đo đạc RPS, Min/Avg/P95/Max Latency, tỷ lệ lỗi (Error Rate). Không cần cài Python hay thư viện ngoài.
  - [x] Tạo `benchmark/load_tester.py`: Script Python bổ trợ cho bạn nào có cài sẵn môi trường Python.
- [x] **4.2. Bộ kịch bản kiểm thử (Test Scenarios)**
  - [x] Test Level 1 - Low Load: 50 concurrent connections vào `/api/delay?ms=100`.
  - [x] Test Level 2 - Medium Load: 200 concurrent connections.
  - [x] Test Level 3 - High Load (C1000): 1,000 concurrent connections.
  - [x] Test Level 4 - Spike/Stress: 2,500+ connections kiểm tra điểm sập của Mode 2 và sự chịu tải của Mode 3, Mode 4.
  - [x] Test Level 5 - CPU Stress: 64 concurrent connections vào `/api/compute?n=30`.
- [x] **4.3. Script tự động hóa 1-click**
  - [x] Tạo `benchmark/run_benchmark.bat`: Tự động gọi `JavaLoadTester` với menu 5 kịch bản và xuất kết quả vào `benchmark/benchmark_results.csv`.
- [x] **4.4. Kiểm thử Phase 4**
  - [x] Chạy thử nghiệm thực tế với 50 concurrent clients, đạt 361.19 RPS, ghi nhận thành công vào `benchmark/benchmark_results.csv`.

---

### 🔹 PHASE 5: Tài Liệu Báo Cáo, Kịch Bản Demo 5.5 Phút & Bộ Q&A Thầy Hùng
> **Mục tiêu:** Chuẩn bị trọn bộ tài liệu thuyết trình, kịch bản live demo và tài liệu giải trình phản biện để tự tin đạt điểm tối đa (A+).

- [x] **5.1. Đề cương Slide thuyết trình (`docs/presentation-outline.md`)**
  - [x] Xây dựng chi tiết 18 slides theo đúng cấu trúc 4 phần tiêu chí chấm của PTIT.
  - [x] Cung cấp các sơ đồ kỹ thuật: TCB, Context Switch, Queue Rejection, Virtual Thread Mount/Unmount.
- [x] **5.2. Kịch bản Live Demo 5.5 phút (`docs/demo-script.md`)**
  - [x] Phân bổ kịch bản bấm giờ từng phút (Bước 1: Khởi động & Dashboard -> Bước 2: Single vs Multi -> Bước 3: C1000 Stress -> Bước 4: Virtual Thread Loom chốt hạ).
- [x] **5.3. Cẩm nang bảo vệ phản biện Q&A (`docs/qa-defense-guide.md`)**
  - [x] Bộ câu trả lời mẫu cho 6 câu hỏi cốt lõi của TS. Đặng Ngọc Hùng (Stack 1MB, CAS Lock-free, Rejection Policy, Context Switching Overhead, Virtual Thread Carrier).
- [x] **5.4. Đóng gói & Hoàn thiện README**
  - [x] Cập nhật file `README.md` với đầy đủ hướng dẫn chạy, cấu trúc thư mục và bảng kết quả đối sánh định lượng mẫu.

