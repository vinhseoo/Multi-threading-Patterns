# 📑 ĐỀ CƯƠNG BÁO CÁO THUYẾT TRÌNH (15–20 PHÚT)
## ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
**Môn học:** Lập Trình Mạng (PTIT)  
**Giảng viên hướng dẫn:** TS. Đặng Ngọc Hùng (hungdn@ptit.edu.vn)  
**Người thực hiện:** Sinh viên bảo vệ Solo (1 thành viên)  
**Nền tảng:** Java 21 LTS (Loom Virtual Threads, Concurrency, Socket IO)

---

## 🎯 PHÂN BỔ THỜI GIAN THUYẾT TRÌNH (TỔNG: 18 PHÚT)
* **Phần 1: Đặt vấn đề & Bản chất Hệ điều hành** (00:00 – 04:30 | 4.5 phút)
* **Phần 2: 4 Kiến trúc luồng & Custom Thread Pool** (04:30 – 09:30 | 5 phút)
* **Phần 3: LIVE DEMO TRỰC QUAN TRÊN DASHBOARD** (09:30 – 15:00 | 5.5 phút)
* **Phần 4: Kết quả thực nghiệm, Kết luận & Q&A** (15:00 – 18:00 | 3 phút)

---

## 📊 CHI TIẾT 18 SLIDES BÁO CÁO CHUẨN MỰC

### SLIDE 1: Trang Tiêu Đề
* **Tiêu đề:** ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
* **Phụ đề:** Nghiên Cứu, Cài Đặt Thực Nghiệm 4 Mô Hình Đa Luồng và Ứng Dụng Java 21 Project Loom trong Lập Trình Mạng
* **Thông tin:** Sinh viên thực hiện | Giảng viên hướng dẫn: TS. Đặng Ngọc Hùng | Học viện Công nghệ Bưu chính Viễn thông (PTIT).
* **Lời nói mở đầu (30s):** "Kính thưa thầy Đặng Ngọc Hùng và các bạn. Trong kỷ nguyên dịch vụ số hiện đại, một máy chủ mạng phải đối mặt với hàng chục nghìn yêu cầu đồng thời mỗi giây. Hôm nay, em xin trình bày đề tài T45..."

---

### SLIDE 2: Đặt Vấn Đề: Nghẽn Mạng & Thách Thức C10K
* **Nội dung chính:**
  * Bài toán C10K: Khả năng phục vụ 10,000 kết nối đồng thời trên cùng một máy chủ.
  * Phân biệt rõ hai trạng thái tác vụ trong mạng:
    1. **I/O-Bound:** Luồng chờ dữ liệu mạng từ Socket, Database, Microservice (chiếm 80-90% thời gian).
    2. **CPU-Bound:** Tính toán nén dữ liệu, mã hóa SSL/TLS, giải thuật logic (chiếm 10-20%).
  * Điểm nghẽn: Nếu chỉ dùng đơn luồng tuần tự, thao tác I/O blocking sẽ làm tê liệt toàn bộ hệ thống.

---

### SLIDE 3: Bản Chất Tầng Thấp Của OS Platform Thread
* **Sơ đồ cấu trúc bộ nhớ của 1 luồng OS:**
  ```
  ┌────────────────────────────────────────────────────────┐
  │ OS Platform Thread (~1MB Memory Overhead)             │
  ├────────────────────────────────────────────────────────┤
  │ 1. Kernel Thread Control Block (TCB)                   │
  │    - Thread ID, CPU Registers State, PC, SP, State    │
  │ 2. Thread Stack Memory (-Xss1m trong JVM): ~1,024 KB   │
  │    - Stack Frames, Local Variables, Return Addresses   │
  │ 3. Kernel Scheduling Queue (Ready, Running, Blocked)   │
  └────────────────────────────────────────────────────────┘
  ```
* **Chi phí vật lý:** 2,000 threads = tiêu tốn ít nhất 2GB RAM chỉ để lưu Thread Stacks!

---

### SLIDE 4: Hiểm Họa Chuyển Ngữ Cảnh (Context Switching Overhead)
* **Cơ chế:** Khi $N_{threads} \gg N_{cores}$, bộ điều phối OS (OS Scheduler) liên tục ngắt luồng.
* **Hậu quả phần cứng:**
  * Lưu trữ/Phục hồi thanh ghi CPU & TCB.
  * **CPU Cache Pollution / Cache Trashing:** Dữ liệu L1/L2 Cache của Luồng A bị xóa sạch để nạp cho Luồng B.
  * Thời gian CPU dành cho Context Switch vượt quá thời gian xử lý dữ liệu mạng thực tế!

---

### SLIDE 5: Tổng Quan 4 Mô Hình Luồng Trong Dự Án
* **Sơ đồ kiến trúc tổng thể:**
  ```
                        [ Incoming Socket Connections ]
                                       │
        ┌──────────────┬───────────────┴───────────────┬──────────────┐
        ▼              ▼                               ▼              ▼
   [ Mode 1 ]     [ Mode 2 ]                      [ Mode 3 ]     [ Mode 4 ]
   Iterative      Thread-Per-Conn                 Worker Pool    Virtual Threads
   (Single)       (OS Threads)                    (Queue + N)    (Java 21 Loom)
  ```

---

### SLIDE 6: Mode 1 - Single-Threaded Iterative Server (Baseline)
* **Nguyên lý:** Vòng lặp `serverSocket.accept()` -> `handleClient()` -> `close()` trên 1 luồng duy nhất.
* **Bản chất tầng mạng:** Khi luồng bận xử lý Client A (ví dụ delay 150ms), các kết nối từ Client B, C, D... bị giam trong **OS TCP Backlog Queue**.
* **Đánh giá:** Làm mốc đối chứng (Baseline) chứng minh sự sụp đổ khi có tải đồng thời.

---

### SLIDE 7: Mode 2 - Thread-per-Connection Server
* **Nguyên lý:** Mỗi khi `accept()` một socket, lập tức khởi tạo `new Thread(...).start()`.
* **Ưu điểm:** Khử nghẽn tuần tự của Mode 1, các client chạy độc lập.
* **Tử huyệt kỹ thuật:** Khi bị bắn tải đột biến (Spike 2,000+ conn), hệ thống ném ngoại lệ:
  `java.lang.OutOfMemoryError: unable to create new native thread` làm crash toàn bộ tiến trình.

---

### SLIDE 8: Mode 3 - Worker Thread Pool Server (Chuẩn Doanh Nghiệp)
* **Mô hình Producer - Consumer:**
  * **Producer:** Luồng `Acceptor` chỉ chuyên lắng nghe mạng và đẩy socket vào Queue.
  * **Consumer:** $N_{workers}$ luồng thợ liên tục tranh chấp an toàn để rút task ra xử lý.
* **Công thức tính số Worker tối ưu:**
  * CPU-bound: $N = N_{cpu} + 1$ (tránh lãng phí context switch).
  * I/O-bound: $N = N_{cpu} \times (1 + \frac{W}{C})$.

---

### SLIDE 9: Cơ Chế Bounded Queue & Backpressure (Chống Sập)
* **Kích thước hàng đợi có giới hạn:** `ArrayBlockingQueue(1000)`.
* **Chính sách từ chối (Rejection Policies):**
  * `AbortPolicy`: Trả về ngay mã HTTP `503 Service Unavailable` kèm header `Retry-After: 2` (Fail-fast tự bảo vệ).
  * `CallerRunsPolicy`: Luồng Acceptor tự chạy task, tạm dừng gọi `accept()`, tạo ra **áp lực ngược (Backpressure)** tự nhiên làm chậm tốc độ nhận kết nối.

---

### SLIDE 10: [INNOVATION] Custom Thread Pool Tự Lập Trình (Technical Depth 30%)
* **Mục tiêu:** Không dựa dẫm vào `java.util.concurrent.ThreadPoolExecutor`, tự cài đặt từ con số 0:
  * `CustomWorker.java`: Luồng thợ sống lâu (Long-lived Thread) chạy vòng lặp vô tận `taskQueue.take()`.
  * `CustomThreadPool.java`: Tự cài đặt cơ chế đồng bộ luồng, quản lý danh sách Worker và Graceful Shutdown.
* **Giá trị khoa học:** Chứng minh hiểu sâu sắc cấu trúc dữ liệu Producer-Consumer tầng thấp.

---

### SLIDE 11: Mode 4 - Modern Concurrency: Java 21 Virtual Threads (Loom)
* **Khái niệm:** Virtual Threads là luồng ảo chạy ở không gian người dùng (User-space), được JVM quản lý.
* **Cơ chế Mount / Unmount kỳ diệu:**
  ```
  [ Virtual Thread ] ──(Chạy tính toán)──> [ Carrier OS Thread ] (Gắn / Mounted)
           │
           ▼ (Gặp I/O Blocking: read socket, Thread.sleep)
  [ JVM Unmounts VT ] ──> Lưu Stack vào Heap ──> Carrier OS Thread rảnh tiếp nhận VT khác
           │
           ▼ (Socket có tín hiệu dữ liệu sẵn sàng)
  [ JVM Mounts VT ]  ──> Nạp lại Call Stack  ──> Tiếp tục thực thi liền mạch
  ```

---

### SLIDE 12: Bảng Đối Sánh Lý Thuyết 4 Kiến Trúc
| Tiêu chí | Mode 1 (Iterative) | Mode 2 (Thread-per-conn) | Mode 3 (Worker Pool) | Mode 4 (Virtual Threads) |
| :--- | :---: | :---: | :---: | :---: |
| **Số luồng OS** | Duy nhất 1 | Bằng số Client ($N$) | Cố định ($16-32$) | Rất ít (~$N_{cores}$) |
| **Bộ nhớ Stack/luồng** | 1MB | 1MB / kết nối | 1MB $\times$ CorePool | Vài trăm Bytes (Heap) |
| **Context Switch** | Không có | Cực kỳ nghiêm trọng | Thấp, kiểm soát được | Siêu nhẹ (JVM-managed) |
| **Giới hạn kết nối** | $C \approx 1$ | $C \approx 1,000-2,000$ | $C \approx Queue + Workers$ | **$C > 100,000$** |

---

### SLIDE 13: Hệ Thống Đo Lường Lock-Free & Real-Time Dashboard
* **Lock-free Metrics Engine:** Sử dụng `LongAdder`, `AtomicLong` (lệnh vi xử lý CPU CAS) – không gây nghẽn tại điểm đếm.
* **JMX Hardware Monitor:** Giám sát % CPU, RAM Heap, và số luồng OS Native Threads thực tế.
* **Giao diện Web Dashboard:** 4 biểu đồ Canvas thời gian thực cập nhật chu kỳ 500ms.

---

### SLIDE 14: LIVE DEMO (5.5 PHÚT)
*(Chuyển sang màn hình trình duyệt `http://localhost:8080/dashboard` - Xem chi tiết trong file `demo-script.md`)*
* Bước 1: Giới thiệu Dashboard & baseline tài nguyên.
* Bước 2: Đối chứng Single-thread vs Đa luồng qua `/api/delay`.
* Bước 3: Thử thách tải cao C1000 và cơ chế hàng đợi Mode 3.
* Bước 4: Đỉnh cao Virtual Threads xử lý nghìn kết nối mà OS Threads vẫn phẳng lì.

---

### SLIDE 15: Kết Quả Đo Đạc Thực Nghiệm (Benchmark Data)
*(Trình chiếu số liệu thực tế thu thập được từ `JavaLoadTester` và file `benchmark_results.csv`)*
* Biểu đồ so sánh Throughput (RPS):
  * Mode 1: ~9.8 RPS (bị nghẽn delay tuần tự).
  * Mode 2: ~180 RPS (suy hao do context switch).
  * Mode 3: ~320 RPS (hàng đợi điều tiết ổn định).
  * **Mode 4: ~1,250+ RPS (Virtual Threads bứt phá vượt trội)**.

---

### SLIDE 16: Phân Tích Phân Vị Độ Trễ (Latency Percentiles P50, P95, P99)
* Tại sao độ trễ P95/P99 quan trọng hơn độ trễ trung bình (Avg)?
  * Hiện tượng "Head-of-Line Blocking" ở Mode 1 và Mode 3 khi Queue đầy.
  * Mode 4 giữ cho P99 phẳng lì ngay cả dưới tải lớn nhờ tính chất Non-blocking ngầm định của Loom.

---

### SLIDE 17: Kết Luận & Đóng Góp Của Đề Tài
1. **Làm chủ nguyên lý:** Hiểu cặn kẽ từ socket mạng, HTTP/1.1 thủ công đến chi phí chuyển ngữ cảnh của Hệ điều hành.
2. **Năng lực tự chủ (Solo Project):** Tự tay lập trình trọn vẹn Custom Thread Pool, Lock-free Engine, Web Dashboard và Công cụ Benchmark.
3. **Hiện đại hóa:** Áp dụng thành công công nghệ tiên tiến nhất của Java 21 (Virtual Threads) vào giải quyết bài toán C10K mạng.

---

### SLIDE 18: Lời Cảm Ơn & Sẵn Sàng Q&A
* "Em xin chân thành cảm ơn thầy TS. Đặng Ngọc Hùng đã tận tình hướng dẫn trong suốt môn học Lập Trình Mạng."
* "Em xin sẵn sàng lắng nghe câu hỏi nhận xét và phản biện từ thầy!"
