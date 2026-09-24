# KẾ HOẠCH ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
## (BÀI TẬP LỚN MÔN LẬP TRÌNH MẠNG - HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG PTIT)

> **Môn học:** Lập Trình Mạng (Network Programming)  
> **Giảng viên hướng dẫn:** TS. Đặng Ngọc Hùng (hungdn@ptit.edu.vn)  
> **Chủ đề đăng ký:** **T45 — Multi-threading Patterns in Network Programming** (Nhóm VI: Concurrent Programming & Multiplayer Applications)  
> **Hình thức thực hiện:** **Cá nhân (1 thành viên - Tự chủ toàn bộ dự án từ A - Z)**  
> **Ngôn ngữ & Nền tảng:** **Java 21 LTS (Microsoft OpenJDK 21)**  
> **Thời lượng báo cáo:** 15–20 phút (Trình bày lý thuyết: 10–12 phút | Live Demo: 5–7 phút | Q&A: 3–5 phút)  
> **Tiêu chí chấm điểm:** Technical Depth (30%), Implementation Quality (25%), Presentation Skills (25%), Q&A Handling (20%)

---

## 1. TỔNG QUAN & MỤC TIÊU ĐẠT ĐIỂM XUẤT SẮC (A+)

### 1.1 Yêu Cầu Cốt Lõi Của Đề Tài T45
- **Tech Focus:** Thread pools, worker threads, thread-safe networking, synchronization primitives, task queues, context switching overhead.
- **Demo Target:** Xây dựng một Multi-threaded Network Server (HTTP/1.1 & TCP Socket) có khả năng xử lý đồng thời hàng nghìn kết nối, hỗ trợ chuyển đổi runtime giữa các mô hình đa luồng.
- **Innovation Factor (Điểm đột phá):** 
  1. Tự cài đặt một **Custom Thread Pool** (Worker Threads + Blocking Queue) để hiểu sâu nguyên lý tầng thấp trước khi đối chiếu với `ThreadPoolExecutor` chuẩn của Java.
  2. Đưa **Java 21 Virtual Threads (Project Loom)** vào so sánh thực nghiệm với Platform Threads truyền thống (đạt 10,000+ kết nối đồng thời với RAM cực thấp).
  3. Xây dựng **Real-time Web Dashboard** theo dõi trực quan trạng thái luồng, CPU, RAM, Latency và RPS.

### 1.2 Lợi Thế & Chiến Lược Cho Sinh Viên Làm Một Mình (Solo Project)
Khi làm 1 mình, giảng viên sẽ đánh giá rất cao tinh thần tự chủ nếu bạn thể hiện được sự bao quát:
- **Ưu thế:** Bạn nắm 100% mã nguồn, không bị tình trạng "phần này bạn khác làm em không rõ" khi thầy Hùng hỏi phản biện tầng sâu.
- **Tập trung:** Mã nguồn tinh gọn, hướng module rõ ràng, tự kiểm soát từ Socket, Parser, Threading, đến Benchmark và Slide.
- **Mục tiêu điểm số:** Tối đa hoá 30% Technical Depth và 20% Q&A bằng kiến thức nền tảng hệ điều hành vững chắc.

---

## 2. BỐN (04) MÔ HÌNH LUỒNG ĐỐI ĐẦU TRONG DỰ ÁN

Hệ thống sẽ cài đặt và cho phép chuyển đổi 4 mô hình luồng trên cùng 1 server:

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

### Chi Tiết Kỹ Thuật Từng Mô Hình:

#### Mô Hình 1: Single-Threaded Iterative Server (Mô hình tuần tự cơ bản)
- **Cơ chế:** Vòng lặp `serverSocket.accept()` -> `handleClient(socket)` -> `close()`.
- **Bản chất tầng thấp:** Chỉ có 1 Thread duy nhất. Khi Thread đang bị block ở thao tác I/O (`read()`, `write()`) hoặc đang xử lý tính toán CPU cho Client A, toàn bộ các kết nối từ Client B, C, D... bị giữ lại trong hàng đợi kết nối của hệ điều hành (**OS TCP Backlog Queue**). Nếu hàng đợi đầy, client sau sẽ nhận lỗi `Connection Refused`.
- **Vai trò trong bài:** Làm mốc đối chứng (baseline) để chứng minh sự cần thiết sống còn của đa luồng trong lập trình mạng.

#### Mô Hình 2: Thread-per-Connection (Mô hình mỗi kết nối một luồng)
- **Cơ chế:** Mỗi khi `accept()` trả về một `Socket`, server lập tức khởi tạo:
  ```java
  new Thread(new ClientHandler(clientSocket)).start();
  ```
- **Ưu điểm:** Cực kỳ dễ lập trình, các client không bị block tuần tự.
- **Hạn chế trí mạng (Tử huyệt kỹ thuật):**
  1. **Chi phí bộ nhớ (Memory Overhead):** Mỗi OS Platform Thread trong Java tiêu chuẩn cấp phát riêng **1MB Stack memory** (`-Xss1m`). Với 2,000 kết nối đồng thời, server cần ít nhất 2GB RAM chỉ để lưu Thread Stacks!
  2. **Chi phí chuyển ngữ cảnh (Context Switching Cost):** Số luồng vượt xa số nhân CPU vật lý ($N_{threads} \gg N_{cores}$). CPU phải liên tục lưu/phục hồi TCB (Thread Control Block), thanh ghi, làm xả cache (CPU Cache Trashing). Thời gian CPU dành cho Context Switch lớn hơn thời gian xử lý dữ liệu mạng thực tế.
  3. **Khả năng sập:** Khi bị bắn tải đột biến (Spike Load), hệ thống ném ngoại lệ `java.lang.OutOfMemoryError: unable to create new native thread` và tiến trình bị hệ điều hành tiêu diệt.

#### Mô Hình 3: Worker Thread Pool với Task Queue (Chuẩn mực doanh nghiệp)
- **Cơ chế:**
  - Khởi tạo sẵn một số lượng Worker Threads cố định ($N_{workers}$).
  - Sử dụng một hàng đợi giới hạn (**Bounded Blocking Queue**, ví dụ kích thước 1,000 tasks).
  - Luồng `Acceptor` chỉ làm duy nhất nhiệm vụ lắng nghe kết nối và đẩy `SocketTask` vào Queue (Mô hình **Producer - Consumer**).
  - Các Worker Threads liên tục tranh thủ lấy kết nối ra xử lý và trả kết quả.
- **Tính toán số lượng Worker tối ưu:**
  - Tác vụ **CPU-bound:** $N_{threads} = N_{cpu} + 1$ (tránh lãng phí context switch).
  - Tác vụ **I/O-bound:** $N_{threads} = N_{cpu} \times (1 + \frac{W}{C})$, với $W$ là thời gian chờ I/O và $C$ là thời gian xử lý CPU.
- **Chính sách từ chối (Rejection Policies):**
  - Khi hàng đợi đầy: Sử dụng `AbortPolicy` (trả về HTTP 503 Service Unavailable) để tự bảo vệ hệ thống không bị crash, hoặc `CallerRunsPolicy` để tạo cơ chế **Backpressure (áp lực ngược)** tự nhiên làm chậm tốc độ nhận kết nối.

#### Mô Hình 4: Modern Concurrency — Java 21 Virtual Threads (Project Loom)
- **Cơ chế:**
  ```java
  Executors.newVirtualThreadPerTaskExecutor()
  ```
- **Điểm đột phá (Innovation):**
  - Virtual Threads là luồng ảo ở tầng người dùng (User-mode thread), được JVM quản lý thay vì OS kernel.
  - Kích thước ban đầu chỉ vài trăm bytes (thay vì 1MB như Platform Thread).
  - Khi một Virtual Thread thực hiện thao tác I/O bị block (ví dụ đọc socket), JVM sẽ tự động **unmount** Virtual Thread đó khỏi Carrier Thread (OS Thread) và gán Virtual Thread khác vào chạy tiếp. Khi socket có dữ liệu, Virtual Thread được **mount** trở lại.
  - Cho phép viết code theo phong cách đồng bộ (blocking I/O) dễ đọc, dễ debug nhưng hiệu năng và độ co giãn ngang ngửa với mô hình Non-blocking / Reactive phức tạp.

---

## 3. THIẾT KẾ KIẾN TRÚC MÃ NGUỒN (CLEAN ARCHITECTURE)

Dự án được cấu trúc theo dạng Maven chuẩn hóa, không phụ thuộc framework cồng kềnh (thuần `java.net` và `java.util.concurrent` để thể hiện trình độ Network Programming cốt lõi):

```
NetworkProgramming-T45/
├── pom.xml                                    # Cấu hình Maven (Java 21)
├── KE_HOACH_DE_TAI_T45.md                     # Tài liệu kế hoạch chi tiết (File này)
├── README.md                                  # Hướng dẫn build, run và kịch bản demo
├── docs/
│   ├── presentation-outline.md                # Đề cương slide 15 phút
│   └── demo-script.md                         # Kịch bản demo bấm giờ từng phút
├── benchmark/
│   ├── load_tester.py                         # Tool bắn tải đa luồng đo RPS & Latency
│   └── run_benchmark.bat                      # Script tự động chạy lần lượt 4 mode & ghi CSV
└── src/
    ├── main/
    │   ├── java/vn/ptit/network/
    │   │   ├── Main.java                      # Điểm khởi động, cho phép chọn Mode (1, 2, 3, 4)
    │   │   ├── config/
    │   │   │   └── ServerConfig.java          # Cấu hình Port, Pool Size, Queue Capacity
    │   │   ├── server/
    │   │   │   ├── BaseHttpServer.java        # Lớp trừu tượng quản lý Socket Server chung
    │   │   │   ├── IterativeServer.java       # Mode 1: Single Thread
    │   │   │   ├── ThreadPerConnServer.java   # Mode 2: Thread-per-connection
    │   │   │   ├── WorkerThreadPoolServer.java# Mode 3: Thread Pool + Bounded Queue
    │   │   │   └── VirtualThreadServer.java   # Mode 4: Java 21 Virtual Threads
    │   │   ├── pool/
    │   │   │   ├── CustomThreadPool.java      # Tự viết ThreadPool (Worker + BlockingQueue)
    │   │   │   └── CustomWorker.java          # Luồng thợ tự lập trình
    │   │   ├── http/
    │   │   │   ├── HttpRequest.java           # Parser HTTP/1.1 (Method, Path, Headers)
    │   │   │   ├── HttpResponse.java          # Builder HTTP Response chuẩn (Status, Content-Type)
    │   │   │   └── HttpHandler.java           # Định tuyến router & xử lý nghiệp vụ
    │   │   └── metrics/
    │   │       ├── ServerMetrics.java         # Atomic counters (RPS, Active Threads, Latency)
    │   │       └── SystemMetrics.java         # CPU %, RAM MB, OS Threads count
    │   └── resources/
    │       └── web/
    │           ├── index.html                 # Giao diện Web Dashboard thời gian thực
    │           ├── dashboard.js               # Biểu đồ tự vẽ bằng Canvas/Chart.js
    │           └── style.css                  # Giao diện Dark mode hiện đại
    └── test/
        └── java/vn/ptit/network/              # Unit Tests kiểm tra HTTP Parser & Thread Pool
```

---

## 4. BỘ ENDPOINTS PHỤC VỤ THỰC NGHIỆM VÀ DEMO

Server cung cấp sẵn 4 API endpoints được thiết kế chuyên biệt để bộc lộ rõ ràng ưu nhược điểm của từng mô hình luồng:

1. `GET /api/hello` *(Network I/O thuần)*
   - Trả về ngay chuỗi JSON `{"status": "ok", "message": "Hello from T45 Server"}`.
   - Mục đích: Đo thông lượng socket thuần túy (RPS tối đa) khi không bị nghẽn CPU hay I/O.
2. `GET /api/compute?n=32` *(Giả lập CPU-bound)*
   - Thực hiện thuật toán đệ quy tính số Fibonacci thứ $n$.
   - Mục đích: Thử nghiệm xem các luồng có tận dụng được đa nhân CPU hay không; chứng minh tác hại của việc tạo quá nhiều luồng gây nghẽn CPU vì Context Switching.
3. `GET /api/delay?ms=150` *(Giả lập I/O-bound)*
   - Thread thực hiện `Thread.sleep(150)` (mô phỏng truy vấn cơ sở dữ liệu hoặc gọi API microservice bên ngoài).
   - Mục đích: Chứng minh sự sụp đổ của Single-Thread và sự vượt trội thần kỳ của Java 21 Virtual Threads khi xử lý I/O chờ.
4. `GET /api/metrics` *(Dữ liệu giám sát)*
   - Trả về dữ liệu JSON chứa: Số active threads, queue size, requests/sec, average latency, RAM usage, CPU load.
5. `GET /dashboard` *(Giao diện trực quan)*
   - Trả về file HTML/JS Dashboard để người xem và thầy giáo nhìn thấy thông số nhảy trực tiếp khi bắn tải.

---

## 5. LỊCH TRÌNH THỰC HIỆN CHI TIẾT (SOLO ROADMAP)

Lộ trình được tối ưu hóa cho 1 người thực hiện trong vòng 10–14 ngày (khoảng 35–45 giờ làm việc):

| Giai đoạn | Nhiệm vụ cụ thể | Đầu ra kiểm tra |
| :--- | :--- | :--- |
| **Ngày 1–2** | **Core Socket & HTTP Parser**<br>- Khởi tạo project Maven với Java 21.<br>- Viết `HttpRequest` parser và `HttpResponse` builder.<br>- Hoàn thiện `IterativeServer` (Mode 1) chạy thử trên trình duyệt. | Trình duyệt truy cập `http://localhost:8080/api/hello` trả về 200 OK chuẩn xác. |
| **Ngày 3–5** | **Triển khai 3 mô hình đa luồng**<br>- Viết `ThreadPerConnServer` (Mode 2).<br>- Tự lập trình `CustomThreadPool` (BlockingQueue + Worker Threads).<br>- Viết `WorkerThreadPoolServer` (Mode 3) dùng `ThreadPoolExecutor`.<br>- Viết `VirtualThreadServer` (Mode 4) dùng Java 21 Virtual Threads. | Cả 4 Mode đều khởi động được và chuyển đổi mượt mà qua tham số `Main.java`. |
| **Ngày 6–7** | **Metrics Collector & Web Dashboard**<br>- Dùng `AtomicLong`, `LongAdder` để đếm request không bị lock.<br>- Tích hợp `OperatingSystemMXBean` lấy CPU/RAM.<br>- Xây dựng trang `/dashboard` có biểu đồ thời gian thực. | Dashboard mở trên trình duyệt tự động cập nhật biểu đồ mỗi 500ms. |
| **Ngày 8–9** | **Viết công cụ Benchmark & Thực nghiệm số liệu**<br>- Viết script Python `load_tester.py` dùng `concurrent.futures`.<br>- Chạy 4 kịch bản tải (50, 200, 1000, 3000 connections).<br>- Thu thập số liệu RPS, P95 Latency, RAM, tạo bảng đối sánh. | Có bảng dữ liệu và 3 biểu đồ so sánh chuẩn để đưa vào Slide. |
| **Ngày 10–11** | **Soạn thảo Slide báo cáo (15–20 phút)**<br>- Thiết kế 18–20 slides theo đúng cấu trúc tiêu chí chấm.<br>- Vẽ sơ đồ TCB, Context Switch, Queue Rejection Policy. | File slide chuẩn chỉnh, nội dung súc tích, hình vẽ chuyên nghiệp. |
| **Ngày 12** | **Luyện tập Live Demo & Chuẩn bị kịch bản Q&A**<br>- Tập dượt demo đúng 5–7 phút (không vấp, không lỗi mạng).<br>- Học thuộc và hiểu sâu 6 câu hỏi phản biện cốt lõi của thầy Hùng. | Tự tin bảo vệ 1 mình trước hội đồng. |

---

## 6. KỊCH BẢN THUYẾT TRÌNH & LIVE DEMO CHUẨN 15–20 PHÚT

### 6.1 Phân Bổ Thời Gian Thuyết Trình
- **00:00 – 02:00 (2 phút): Mở đầu & Bài toán thực tế**
  - Đặt vấn đề: Tại sao server mạng cần xử lý đồng thời? Khái niệm Concurrency vs Parallelism.
- **02:00 – 05:00 (3 phút): Bản chất tầng thấp của Hệ điều hành**
  - OS Thread là gì? Thread Control Block (TCB), Stack 1MB, chi phí Context Switching của CPU.
  - Các hiểm họa: Race condition, Deadlock, Thread Starvation và giải pháp Lock-free / Atomic.
- **05:00 – 08:00 (3 phút): 4 Mô hình kiến trúc luồng trong Lập trình mạng**
  - Trình bày tuần tự: Iterative -> Thread-per-connection -> Worker Thread Pool -> Virtual Threads.
  - Phân tích cơ chế Producer - Consumer và Bounded Queue Rejection Policies.
- **08:00 – 11:00 (3 phút): Kết quả thực nghiệm và phân tích định lượng**
  - Trình chiếu bảng đối sánh Throughput (RPS), Latency P99 và mức tiêu hao RAM/CPU.
- **11:00 – 16:30 (5.5 phút): LIVE DEMO TRỰC QUAN (Xem kịch bản chi tiết bên dưới)**
- **16:30 – 20:00 (3.5 phút): Kết luận, Hướng mở rộng BTL & Trả lời phản biện (Q&A)**

### 6.2 Kịch Bản Live Demo 5.5 Phút (Ấn tượng nhất, không thời gian chết)
1. **Bước 1 (1 phút): Khởi động Server & Trình diễn Dashboard**
   - Chạy server ở chế độ mặc định, mở trình duyệt `http://localhost:8080/dashboard`.
   - Giới thiệu các chỉ số đang ở mức nền: 0 active threads, 0% CPU, 45MB RAM.
2. **Bước 2 (1.5 phút): Đối chứng Single-Thread vs Thread-per-Connection**
   - Bắn 50 requests vào `/api/delay?ms=100`:
   - Single-thread: Mất tổng cộng $50 \times 0.1s = 5$ giây mới xong.
   - Chuyển sang Thread-per-Connection: Phản hồi tức thì trong 100ms. Thầy thấy ngay giá trị của đa luồng.
3. **Bước 3 (1.5 phút): Thử thách giới hạn tải cao (C1000 Stress Test)**
   - Bắn tải 2,000 requests đồng thời vào Thread-per-Connection:
   - Dashboard hiển thị số lượng Thread tăng vọt lên hàng nghìn, RAM nhảy vọt, CPU giật lag do Context Switch, server bắt đầu từ chối kết nối.
   - Chuyển sang Worker Thread Pool: Số luồng được khống chế nghiêm ngặt ở mức 16 threads, RAM phẳng lì, task tự động xếp hàng trong Queue và tiêu thụ đều đặn, không có kết nối nào bị crash.
4. **Bước 4 (1.5 phút): Đỉnh cao Virtual Threads (Java 21 Project Loom)**
   - Bật Mode Virtual Threads: Bắn một đợt 10,000 requests.
   - Server xử lý 10,000 kết nối đồng thời trong chớp mắt, RAM chỉ tăng vài chục MB, không hề có lỗi out-of-thread. Đây là cú "chốt hạ" ghi điểm A+ tuyệt đối.

---

## 7. BỘ CÂU HỎI PHẢN BIỆN (Q&A) THẦY HÙNG & HƯỚNG DẪN TRẢ LỜI

Khi bạn làm 1 mình, thầy Hùng sẽ hỏi sâu để chắc chắn bạn tự viết và hiểu bản chất:

### Câu 1: "Tại sao không tăng số lượng Worker Threads lên 500 hoặc 1000 cho chạy nhanh hơn mà lại để 16 hay 32?"
> **Trả lời:**  
> "Thưa thầy, số lượng thread không thể tăng vô hạn vì hai rào cản vật lý:
> 1. **Chi phí bộ nhớ:** Mỗi OS thread chiếm 1MB stack memory. 1,000 threads sẽ ngốn 1GB RAM chỉ để lưu stack frames.
> 2. **Chi phí Context Switch:** Máy tính chỉ có số nhân CPU hữu hạn (ví dụ 8 cores). Nếu có 1,000 luồng tranh chấp 8 cores, bộ điều phối của OS (OS Scheduler) phải liên tục ngắt luồng, lưu trạng thái thanh ghi và nạp luồng mới. Thời gian CPU làm việc chuyển đổi này vượt quá thời gian xử lý nghiệp vụ thực tế.
> Công thức chuẩn là: Với CPU-bound, cấu hình $N_{threads} = N_{cpu} + 1$. Với I/O-bound, $N_{threads} = N_{cpu} \times (1 + \frac{Wait}{Compute})$."

### Câu 2: "Trong Thread Pool, khi Hàng đợi (Work Queue) bị đầy thì hệ thống xử lý như thế nào?"
> **Trả lời:**  
> "Thưa thầy, khi task queue đầy, `ThreadPoolExecutor` sẽ kích hoạt **RejectedExecutionHandler**. Em đã cài đặt và thử nghiệm các chính sách:
> - `AbortPolicy`: Lập tức ném ngoại lệ và trả về mã HTTP `503 Service Unavailable` kèm header `Retry-After`. Đây là chiến lược Fail-fast nhằm bảo vệ hệ thống không bị quá tải.
> - `CallerRunsPolicy`: Luồng Acceptor (luồng chính) sẽ tự đứng ra thực thi task đó. Khi luồng chính bận thực thi, nó tạm thời ngừng gọi `accept()`, tạo ra cơ chế **Backpressure (áp lực ngược)** tự nhiên làm chậm tốc độ tiếp nhận kết nối mới từ mạng."

### Câu 3: "Khi có hàng trăm worker threads cùng cập nhật số liệu thống kê (Total Requests, Latency), em làm sao để đảm bảo Thread-safe mà không làm tụt hiệu năng?"
> **Trả lời:**  
> "Thưa thầy, nếu dùng từ khóa `synchronized` hoặc `ReentrantLock` bao quanh biến đếm, tất cả các worker threads sẽ bị xếp hàng chờ (Lock Contention) tại điểm ghi metrics, biến hệ thống thành đơn luồng tại điểm nghẽn đó.  
> Em giải quyết bằng cách sử dụng các cấu trúc **Lock-free** trong gói `java.util.concurrent.atomic`, cụ thể là `LongAdder` và `AtomicLong`. Các lớp này sử dụng lệnh vi xử lý cấp phần cứng **Compare-And-Swap (CAS)** và kỹ thuật phân tán cell bộ nhớ (Striped64) giúp hàng trăm luồng ghi nhận số liệu song song với chi phí cực thấp."

### Câu 4: "Sự khác biệt bản chất giữa Virtual Threads trong Java 21 và OS Platform Threads truyền thống là gì?"
> **Trả lời:**  
> "Thưa thầy, OS Platform Thread được quản lý trực tiếp bởi nhân hệ điều hành (1:1 với kernel thread), stack cố định ~1MB, chi phí tạo và chuyển ngữ cảnh đắt đỏ.  
> Trong khi đó, **Virtual Thread (M:N)** là luồng ảo chạy trong không gian người dùng (User-space) do JVM quản lý. Hàng triệu Virtual Threads có thể chia sẻ một số ít Carrier Threads (OS threads). Khi Virtual Thread thực hiện thao tác I/O bị nghẽn (như đọc socket), JVM sẽ unmount nó và lưu call stack vào Heap, nhường Carrier thread cho virtual thread khác. Khi socket có tín hiệu dữ liệu, JVM đưa nó trở lại chạy tiếp. Điều này mang lại thông lượng cực cao mà vẫn giữ được mô hình lập trình blocking tuần tự, dễ debug."

---

## 8. HƯỚNG MỞ RỘNG THÀNH BÀI TẬP LỚN TOÀN DIỆN (BTL EXTENSION)
Theo gợi ý trong đề cương môn học của thầy Hùng, đề tài T45 này có thể mở rộng phát triển thành đồ án BTL quy mô lớn:
1. **Tích hợp SSL/TLS (HTTPS):** Bổ sung `SSLSocket` với Keystore tự ký để phân tích chi phí mã hóa handshake đa luồng.
2. **Hỗ trợ HTTP/1.1 Persistent Connections (Keep-Alive):** Quản lý vòng đời socket khi 1 kết nối gửi nhiều request liên tục mà không đóng socket ngay.
3. **Module Rate Limiting đa luồng:** Áp dụng thuật toán Token Bucket / Leaky Bucket bảo vệ server chống DoS/DDoS.
4. **Viết Báo Cáo Kỹ Thuật (Technical Paper):** Đóng gói thành báo cáo khoa học so sánh hiệu năng chi tiết với biểu đồ nhiệt (Heatmap) và biểu đồ phân vị độ trễ (Latency Percentiles P50, P90, P99).
