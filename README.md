# 🚀 ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
**Môn học:** Lập Trình Mạng (Network Programming) - Học viện Công nghệ Bưu chính Viễn thông (PTIT)  
**Giảng viên hướng dẫn:** TS. Đặng Ngọc Hùng (hungdn@ptit.edu.vn)  
**Sinh viên thực hiện:** 1 thành viên (Solo Project - Tự chủ toàn bộ dự án từ A - Z)  
**Nền tảng & Ngôn ngữ:** Java 21 LTS (Microsoft OpenJDK 21, Project Loom Virtual Threads, Concurrency, Socket IO)  
**GitHub Repository:** [https://github.com/vinhseoo/Multi-threading-Patterns.git](https://github.com/vinhseoo/Multi-threading-Patterns.git)

---

## 📌 TỔNG QUAN DỰ ÁN

Dự án nghiên cứu, cài đặt từ con số 0 và đánh giá thực nghiệm **4 mô hình kiến trúc luồng (Multi-threading Patterns)** đối đầu trong lập trình ứng dụng máy chủ mạng:

1. **Mode 1: Iterative Single-Threaded Server** (Xử lý tuần tự trên luồng chính - Mốc đối chứng Baseline).
2. **Mode 2: Thread-per-Connection Server** (Mỗi kết nối 1 luồng OS - Naive Multi-threading).
3. **Mode 3: Worker Thread Pool Server** (Hàng đợi Bounded Blocking Queue + Fixed Workers + Backpressure Rejection - Enterprise Standard).
4. **Mode 4: Java 21 Virtual Threads Server** (Project Loom User-space Threads - Đỉnh cao High-concurrency hiện đại).
5. **Mode 5 (Innovation Factor - Điểm kỹ thuật 30%): Custom Thread Pool Server** (Tự lập trình trọn vẹn Worker Threads và Bounded Blocking Queue không dùng thư viện có sẵn).

```
                                  [ Incoming Client Connections ]
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 ▼                              ▼                              ▼                              ▼
      [ Mode 1: Iterative ]           [ Mode 2: Thread-Per-Conn ]     [ Mode 3: Worker Thread Pool ]  [ Mode 4: Virtual Threads ]
       - 1 Main Thread                 - 1 Acceptor Thread             - 1 Acceptor Thread             - 1 Acceptor Thread
       - Xử lý tuần tự từng client     - Spawns new thread/client      - Bounded Blocking Queue        - Java 21 Loom Virtual Threads
       - Block hoàn toàn kết nối sau   - Dễ crash khi C1000+           - Fixed pool (N workers)        - 100K+ lightweight threads
       - Đo độ trễ xếp hàng            - Đo chi phí Context Switch     - Đo cơ chế Backpressure        - Đỉnh cao hiệu năng I/O
```

---

## 🌟 ĐIỂM NỔI BẬT & TÍNH ĐỘT PHÁ CỦA DỰ ÁN

* **Zero External Runtime Dependency:** Xây dựng máy chủ HTTP/1.1 thủ công (Request parser, Response builder, Router) bằng 100% Java Standard Library (`java.net`, `java.util.concurrent`, `java.lang.management`).
* **Hệ thống Đo lường Lock-Free (Lock-free Metrics Engine):** Dùng `LongAdder`, `AtomicLong` với lệnh máy CAS (Compare-And-Swap) – hàng nghìn luồng ghi nhận số liệu song song với chi phí bằng 0.
* **Real-time Web Dashboard (`/dashboard`):** Giao diện Dark-tech thời gian thực với 4 biểu đồ Canvas 60fps mượt mà, giám sát trực quan RPS, Latency, Active Connections, CPU, RAM và số lượng **OS Platform Threads thực tế**.
* **Công cụ Benchmark thuần Java 21 (`JavaLoadTester.java`):** Dùng chính Virtual Threads để bắn đồng thời hàng nghìn request, đo đạc RPS và phân vị độ trễ (Min, Avg, P50, P95, P99, Max) không phụ thuộc Python hay phần mềm bên ngoài.
* **Tự lập trình Custom Thread Pool (`pool/`):** Tự tay hiện thực hóa mẫu Producer - Consumer để bảo vệ điểm xuất sắc trước hội đồng.

---

## 📂 CẤU TRÚC THƯ MỤC DỰ ÁN

```
Multi-threading-Patterns/
├── pom.xml                                    # Cấu hình Maven (Java 21 LTS + Lombok)
├── build.bat                                  # Script biên dịch 1-click tự động bằng JDK 21
├── run_server.bat                             # Script khởi chạy máy chủ (chọn Mode 1 - 5)
├── README.md                                  # Hướng dẫn tổng quan & kết quả dự án
├── PROJECT_PHASES_LOG.md                      # Nhật ký theo dõi tiến độ chi tiết 5 Phase
├── KE_HOACH_DE_TAI_T45.md                     # Kế hoạch & Mục tiêu đề tài BTL
├── lib/
│   └── lombok.jar                              # Thư viện Lombok phục vụ compile độc lập
├── docs/
│   ├── presentation-outline.md                # Đề cương Slide thuyết trình 15-20 phút (18 slides)
│   ├── demo-script.md                         # Kịch bản Live Demo 5.5 phút bấm giờ từng thao tác
│   └── qa-defense-guide.md                    # Cẩm nang giải trình 6 câu hỏi phản biện của thầy Hùng
├── benchmark/
│   ├── run_benchmark.bat                      # Script chạy bắn tải tự động với 5 kịch bản
│   ├── load_tester.py                         # Tool bắn tải bằng Python (dự phòng)
│   └── benchmark_results.csv                  # Dữ liệu kết quả thực nghiệm đo đạc thực tế
└── src/
    └── main/
        ├── java/vn/ptit/network/
        │   ├── Main.java                      # Điểm khởi động, menu điều hướng 5 mode
        │   ├── config/
        │   │   └── ServerConfig.java          # Cấu hình Port, Pool Size, Queue, Timeout
        │   ├── http/
        │   │   ├── HttpRequest.java           # Bộ phân tích gói tin HTTP/1.1 thủ công
        │   │   ├── HttpResponse.java          # Bộ xây dựng HTTP Response chuẩn
        │   │   └── HttpHandler.java           # Định tuyến router & xử lý nghiệp vụ
        │   ├── server/
        │   │   ├── BaseHttpServer.java        # Khung trừu tượng quản lý ServerSocket
        │   │   ├── IterativeServer.java       # Mode 1: Single Thread (Baseline)
        │   │   ├── ThreadPerConnServer.java   # Mode 2: Mỗi kết nối 1 luồng OS
        │   │   ├── WorkerThreadPoolServer.java# Mode 3: ThreadPool chuẩn + Backpressure
        │   │   ├── VirtualThreadServer.java   # Mode 4: Java 21 Project Loom
        │   │   └── CustomThreadPoolServer.java# Mode 5: Custom Thread Pool tự viết
        │   ├── pool/
        │   │   ├── CustomWorker.java          # Luồng thợ tự lập trình
        │   │   └── CustomThreadPool.java      # Thread Pool tự viết từ con số 0
        │   ├── metrics/
        │   │   ├── ServerMetrics.java         # Bộ đếm Lock-free (LongAdder, CAS, RPS, P95)
        │   │   └── SystemMetrics.java         # JMX MXBean (CPU %, RAM MB, OS Threads)
        │   └── benchmark/
        │       └── JavaLoadTester.java        # Tool bắn tải đa luồng thuần Java 21 Loom
        └── resources/
            └── web/
                ├── index.html                 # Giao diện Web Dashboard thời gian thực
                ├── style.css                  # Giao diện Dark-tech cao cấp, glassmorphism
                └── dashboard.js               # Động cơ Canvas 60fps & Demo Control Center
```

---

## ⚡ HƯỚNG DẪN KHỞI CHẠY NHANH (1-CLICK)

### 1. Biên dịch dự án:
Mở cửa sổ Command Prompt / PowerShell trong thư mục dự án và chạy:
```cmd
build.bat
```
*(Script sẽ tự động nhận diện JDK 21 tại `C:\Users\maiduc.vinh\.jdks\ms-21.0.10` và biên dịch toàn bộ mã nguồn).*

### 2. Khởi chạy máy chủ (Lựa chọn Mode):
Chạy script `run_server.bat` kèm số thứ tự Mode muốn kiểm thử:
```cmd
run_server.bat 1      # Khởi động Mode 1: Iterative Single-Thread
run_server.bat 2      # Khởi động Mode 2: Thread-per-Connection
run_server.bat 3      # Khởi động Mode 3: Worker Thread Pool chuẩn
run_server.bat 4      # Khởi động Mode 4: Java 21 Virtual Threads [Khuyên Dùng]
run_server.bat 5      # Khởi động Mode 5: Custom Thread Pool tự lập trình
```

### 3. Mở Web Dashboard giám sát:
Truy cập trình duyệt tại địa chỉ:
👉 **`http://localhost:8080/dashboard`**

---

## 🌐 DANH SÁCH CÁC ENDPOINTS PHỤC VỤ THỰC NGHIỆM

| Endpoint | Mục đích thực nghiệm | Mô tả hành vi kỹ thuật |
| :--- | :--- | :--- |
| `GET /dashboard` | Giám sát trực quan | Trả về Web Dashboard Dark-tech với 4 biểu đồ Canvas thời gian thực. |
| `GET /api/metrics` | Dữ liệu thống kê JSON | Xuất toàn bộ số liệu: Total Requests, RPS, Latency (Avg, P95), Active Conns, CPU %, RAM MB, OS Threads. |
| `GET /api/hello` | Đo Network I/O thuần túy | Phản hồi JSON ngay lập tức để đo trần thông lượng (Maximum Throughput). |
| `GET /api/delay?ms=150` | Giả lập I/O-Bound | Gọi `Thread.sleep(ms)` mô phỏng truy vấn CSDL; phơi bày điểm nghẽn của Mode 1 và sức mạnh của Loom Mode 4. |
| `GET /api/compute?n=32` | Giả lập CPU-Bound | Tính toán đệ quy Fibonacci thứ $n$; đo đạc chi phí chuyển ngữ cảnh (Context Switching) của CPU. |

---

## 📊 KẾT QUẢ ĐỐI SÁNH THỰC NGHIỆM ĐỊNH LƯỢNG

Dữ liệu đo đạc thực tế thu thập bằng công cụ `JavaLoadTester` trên cùng một cấu hình máy tính (16 Logical Cores, Windows 11):

| Chỉ Số Đánh Giá | Mode 1 (Iterative) | Mode 2 (Thread-per-conn) | Mode 3 (Worker Pool) | Mode 4 (Virtual Threads) |
| :--- | :---: | :---: | :---: | :---: |
| **Throughput (RPS)** | ~9.8 RPS | ~185.4 RPS | ~340.2 RPS | **~1,250+ RPS** |
| **Avg Latency** | 3,210 ms (xếp hàng) | 165 ms | 115 ms | **58 ms** |
| **P95 Latency** | 4,800 ms | 420 ms | 180 ms | **85 ms** |
| **Số OS Threads khi tải 1,000 conn** | Duy nhất 1 | **1,000 luồng** (nguy cơ crash) | **16 luồng cố định** | **~15 luồng ổn định** |
| **Tiêu hao RAM Heap** | Thấp | Rất cao (~1GB stack) | Trung bình (ổn định) | **Rất thấp (~15MB)** |
| **Hiện tượng khi quá tải** | Chờ vô hạn ở Backlog | Tràn Stack / Treo máy | Hàng đợi giữ task / 503 an toàn | **Phản hồi tức thì, mượt mà** |

---

## 📚 TÀI LIỆU PHỤC VỤ BÁO CÁO & BẢO VỆ ĐỒ ÁN

* 📑 **[Đề cương Slide thuyết trình 15-20 phút](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/presentation-outline.md)**: Chi tiết 18 slides chuẩn barem chấm điểm của PTIT.
* ⏱️ **[Kịch bản Live Demo 5.5 phút](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/demo-script.md)**: Bấm giờ chi tiết từng câu thoại, từng thao tác mở tab và click demo.
* 🛡️ **[Cẩm nang phản biện Q&A](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/qa-defense-guide.md)**: Bí kíp trả lời 6 câu hỏi bản chất hệ điều hành của TS. Đặng Ngọc Hùng.
* 📋 **[Nhật ký tiến độ từng Phase](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/PROJECT_PHASES_LOG.md)**: Theo dõi tiến độ hoàn thành 100% của 5 giai đoạn dự án.
