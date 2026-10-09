# 📑 ĐỀ CƯƠNG BÁO CÁO THUYẾT TRÌNH (15–20 PHÚT)
## ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
**Môn học:** Lập Trình Mạng (PTIT)  
**Giảng viên hướng dẫn:** TS. Đặng Ngọc Hùng (hungdn@ptit.edu.vn)  
**Người thực hiện:** Sinh viên bảo vệ Solo (1 thành viên)  
**Nền tảng:** Java 21 LTS (Loom Virtual Threads, Concurrency, Socket IO)

---

## 🎯 PHÂN BỔ THỜI GIAN THUYẾT TRÌNH (TỔNG: 18 PHÚT)
* **Phần 1: Đặt vấn đề & Bản chất Hệ điều hành** (00:00 – 04:30 | 4.5 phút)
* **Phần 2: 5 Kiến trúc luồng & Custom Thread Pool Tự Lập Trình** (04:30 – 09:30 | 5 phút)
* **Phần 3: LIVE DEMO TRỰC QUAN TRÊN DASHBOARD** (09:30 – 15:00 | 5.5 phút)
* **Phần 4: Kết quả thực nghiệm, Kết luận & Q&A** (15:00 – 18:00 | 3 phút)

---

## 📊 CHI TIẾT 18 SLIDES BÁO CÁO CHUẨN MỰC

### SLIDE 1: Trang Tiêu Đề
* **Tiêu đề:** ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
* **Phụ đề:** Nghiên Cứu, Cài Đặt Thực Nghiệm 5 Mô Hình Luồng Đối Đầu và Ứng Dụng Java 21 Project Loom trong Lập Trình Mạng
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

### SLIDE 5: Tổng Quan 5 Mô Hình Luồng Trong Dự Án
* **Sơ đồ kiến trúc tổng thể:**
  ```
                                [ Incoming Socket Connections ]
                                               │
        ┌──────────────┬───────────────┼───────────────┬──────────────┬──────────────┐
        ▼              ▼               ▼               ▼              ▼              ▼
   [ Mode 1 ]     [ Mode 2 ]      [ Mode 3 ]      [ Mode 5 ]     [ Mode 4 ]
   Iterative      Thread-Per-Conn Worker Pool     Custom Pool    Virtual Threads
   (Single)       (OS Threads)    (JDK Exec)      (Self-made)    (Java 21 Loom)
  ```

---

### SLIDE 6: Mode 1 - Mô Hình Đơn Luồng Xử Lý Tuần Tự (Baseline)
* **Quy trình 3 bước trực quan:**
  * Bước 1: Mở cửa đón kết nối đầu tiên.
  * Bước 2: Phục vụ trọn gói (đọc yêu cầu, tính toán hoặc chờ cơ sở dữ liệu). Trong suốt thời gian này, cửa ra vào bị khóa chặt!
  * Bước 3: Đóng kết nối, sau đó mới quay lại đón người tiếp theo.
* **Bản chất hiện tượng nghẽn mạng:** 29 khách còn lại bị giam lỏng ngoài hành lang (hàng đợi mạng TCP Backlog của Hệ điều hành). 30 yêu cầu delay 100ms mất tới 3.0 giây tuần tự. Thông lượng sụp đổ chỉ còn ~9.8 yêu cầu/giây.

---

### SLIDE 7: Mode 2 - Mỗi Kết Nối Một Luồng Riêng Biệt
* **Ý tưởng & Lợi ích:** Cứ có 1 kết nối đến ➔ Lập tức tạo riêng 1 nhân viên mới phục vụ riêng. 30 khách được 30 nhân viên xử lý song song, hoàn tất đồng loạt chỉ trong 115 mili-giây (~285 yêu cầu/giây).
* **Cái bẫy bùng nổ tài nguyên (Tại sao không thể mở rộng?):**
  * Số lượng luồng hệ điều hành tăng vọt bám sát số lượng kết nối mạng (Đường Đỏ dính chặt Đường Xanh).
  * Mỗi luồng ngốn 1 phòng riêng 1MB RAM. Khi có hàng nghìn kết nối ➔ Cạn kiệt bộ nhớ, máy chủ bị sập ngay lập tức.
  * Hệ điều hành kiệt quệ vì phải liên tục tráo đổi thanh ghi giữa hàng nghìn luồng.

---

### SLIDE 8: Mode 3 - Đội Ngũ Nhân Viên Cố Định & Hàng Ghế Chờ (Chuẩn Doanh Nghiệp)
* **Mô hình Nhà hàng chuyên nghiệp (Producer - Consumer):**
  * 1 Nhân viên Lễ tân (Acceptor): Chỉ chuyên đứng cửa tiếp nhận kết nối và phát số thứ tự vào hàng ghế chờ.
  * Hàng ghế chờ có giới hạn (1,000 chỗ): Lưu giữ các yêu cầu đang đợi tới lượt một cách trật tự.
  * Đội ngũ 16 Nhân viên cố định (16 Workers): Tuyển sẵn từ đầu, luân phiên lấy việc từ hàng ghế chờ ra xử lý.
* **Lợi ích kiểm soát:** Dù có 500 hay 1,000 khách ồ ạt tràn vào, số luồng hệ điều hành luôn được chặn cứng ở mức 16 luồng cố định, bảo vệ máy chủ an toàn tuyệt đối.

---

### SLIDE 9: Cơ Chế Hàng Ghế Chờ Giới Hạn & Van Xả An Toàn (Backpressure)
* **Nguy cơ của hàng chờ vô hạn:** Nếu cho xếp hàng vô tận, bộ nhớ sẽ phình to cho đến khi nổ tung (tràn bộ nhớ sập máy chủ).
* **Giải pháp Van xả an toàn (Backpressure):**
  * Đặt giới hạn trần 1,000 chỗ chờ.
  * Khi hàng chờ chạm ngưỡng 1,000 ➔ Kích hoạt van xả an toàn: Lịch sự từ chối ngay lập tức và hẹn quay lại sau (Mã HTTP 503 Service Unavailable).
  * Ý nghĩa: Máy chủ chủ động từ chối an toàn các yêu cầu vượt ngưỡng để bảo vệ 100% các khách hàng đang được phục vụ bên trong.

---

### SLIDE 10: [INNOVATION 30%] Mode 5 - Tự Thiết Kế Thread Pool Từ Con Số 0
* **3 Trụ cột kỹ thuật tự lập trình (Không dùng thư viện có sẵn):**
  1. **Băng chuyền xoay vòng $O(1)$:** Hàng đợi hoạt động như băng chuyền sushi hình tròn khép kín. Đặt việc vào một đầu và lấy việc ra ở đầu đối diện liên tục, tốc độ xử lý tức thì không phải dịch chuyển mảng.
  2. **Cơ chế Thức giấc & Ngủ đông:** Có việc mới thì lập tức đánh thức công nhân dậy làm; hết việc trên băng chuyền thì công nhân tự động đi ngủ đông để tiết kiệm 100% năng lực CPU.
  3. **Quản lý vòng đời & Dừng an toàn:** Điều phối 16 công nhân, hỗ trợ dừng mềm dẻo (đợi các việc đang làm dở hoàn tất trọn vẹn mới nghỉ).
* **Giá trị khoa học:** Chứng minh năng lực làm chủ thuật toán và cấu trúc dữ liệu đa luồng tầng thấp.

---

### SLIDE 11: Mode 4 - Đỉnh Cao Java 21 Virtual Threads (Project Loom)
* **Cơ chế "Bàn làm việc dùng chung" (Mount / Unmount luồng ảo):**
  * Giai đoạn 1 (Làm việc): Luồng ảo siêu nhẹ được gắn vào một lõi xử lý vật lý để tính toán.
  * Giai đoạn 2 (Tạm nghỉ khi chờ đợi - Unmount): Khi luồng ảo gặp thao tác phải chờ (đọc mạng, truy vấn CSDL, delay), máy ảo tự động nhấc luồng này ra cất gọn vào bộ nhớ và nhường ngay lõi xử lý cho luồng khác vào làm việc.
  * Giai đoạn 3 (Quay lại liền mạch - Mount): Khi mạng có dữ liệu, luồng ảo được đặt lại lên một lõi xử lý đang rảnh để chạy tiếp như chưa hề bị ngắt!
* **Tại sao đạt hiệu năng kỷ lục?** Mỗi luồng ảo chỉ tốn vài trăm Bytes (nhẹ hơn hàng nghìn lần so với luồng hệ điều hành). Đạt thông lượng kỷ lục ~1,145+ yêu cầu/giây với độ trễ siêu thấp dưới 75ms.

---

### SLIDE 12: Bảng Đối Sánh Lý Thuyết 5 Kiến Trúc Luồng
| Tiêu chí | Mode 1 (Iterative) | Mode 2 (Thread-per-conn) | Mode 3 (Worker Pool) | Mode 5 (Custom Pool) | Mode 4 (Virtual Threads) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Số luồng OS** | Duy nhất 1 | Bằng số Client ($N$) | Cố định ($16-32$) | Cố định ($16-32$) | Rất ít (~$N_{cores}$) |
| **Bộ nhớ Stack/luồng** | 1MB | 1MB / kết nối | 1MB $\times$ CorePool | 1MB $\times$ CorePool | Vài trăm Bytes (Heap) |
| **Hàng đợi Blocking** | Không có | Không có | `ArrayBlockingQueue` | `CustomBlockingQueue` (Circular) | VirtualThread Scheduler |
| **Context Switch** | Không có | Cực kỳ nghiêm trọng | Thấp, kiểm soát được | Thấp, kiểm soát được | Siêu nhẹ (JVM-managed) |
| **Giới hạn kết nối** | $C \approx 1$ | $C \approx 1,000-2,000$ | $C \approx Queue + Workers$ | $C \approx Queue + Workers$ | **$C > 100,000$** |

---

### SLIDE 13: Hệ Thống Đo Lường Lock-Free & Real-Time Dashboard
* **Lock-free Metrics Engine:** Sử dụng `LongAdder`, `AtomicLong` (lệnh vi xử lý CPU CAS) – không gây nghẽn tại điểm đếm.
* **JMX Hardware Monitor:** Giám sát % CPU, RAM Heap, và số luồng OS Native Threads thực tế.
* **Giao diện Web Dashboard:** 4 biểu đồ Canvas thời gian thực cập nhật chu kỳ 500ms + Bảng đối sánh Benchmark đa luồng + Hỗ trợ Sắp xếp lịch sử test linh hoạt.

---

### SLIDE 14: LIVE DEMO THỰC CHIẾN (5.5 — 6 PHÚT)
*(Chuyển sang màn hình trình duyệt `http://localhost:8080/dashboard` - Chi tiết xem tại [demo-script.md](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/demo-script.md))*
* **Bước 1 (00:00):** Giới thiệu kiến trúc Dashboard: 6 thẻ KPI, 4 biểu đồ Native Canvas 60fps, cơ chế Polling chu kỳ 500ms không reload.
* **Bước 1.5 (01:00):** Kiểm thử 3 tính chất tải qua Demo Control Center:
  - `GET /api/hello` (Pure I/O): Phản hồi < 1ms, in định danh Thread và cờ `[Loom Virtual]`.
  - `GET /api/delay` (I/O-Bound): Mô phỏng trễ Database/Network 150ms.
  - `GET /api/compute` (CPU-Bound): Đẩy CPU lên cao trên Biểu đồ 4 để phân biệt bản chất tối ưu I/O của Virtual Threads.
* **Bước 2 (01:30):** Đối chứng Mode 1 (Nghẽn TCP Backlog, latency ~3s) vs Mode 2 (Tăng vọt 30 luồng, Đường Đỏ dính chặt Đường Xanh).
* **Bước 3 (02:45):** Thử thách tải cao C1000 và kích hoạt Mode 3:
  - Khóa cứng OS Threads ở 16 luồng cố định.
  - Cơ chế Backpressure: Tự bảo vệ bằng HTTP 503 Service Unavailable khi hàng đợi đầy.
* **Bước 3.5 (04:00):** [30% Depth] Trình diễn Mode 5 Custom Thread Pool tự lập trình: Mở trực tiếp mã nguồn `CustomBlockingQueue.java` (Circular Array $O(1)$) và `CustomWorker.java` (`wait()`/`notifyAll()`).
* **Bước 4 (04:45):** Đỉnh cao Java 21 Virtual Threads (Mode 4): Bắn 1,000 clients, Active Connections vọt đỉnh nhưng OS Threads hoàn toàn phẳng lì ở 15-16 Carrier Threads, RPS đạt ~1,145+.
* **Bước 5 (05:30):** Bảng ma trận đối sánh Benchmark, đồ thị cột trực quan, tính năng Sort lịch sử test và tải file CSV.

---

### SLIDE 15: Kết Quả Đo Đạc Thực Nghiệm (Benchmark Data)
*(Trình chiếu số liệu thực tế thu thập được từ `JavaLoadTester` và file `benchmark_results.csv`)*
* Biểu đồ so sánh Throughput (RPS):
  * Mode 1: ~9.8 RPS (bị nghẽn delay tuần tự ở TCP Backlog).
  * Mode 2: ~285.4 RPS (suy hao do context switch và bộ nhớ stack).
  * Mode 3: ~152.0 RPS (hàng đợi Bounded Queue điều tiết ổn định).
  * Mode 5: ~148.6 RPS (Custom Pool tự viết mảng vòng đạt hiệu năng ngang ngửa Mode 3 chuẩn).
  * **Mode 4: ~1,145+ RPS (Virtual Threads bứt phá vượt trội)**.

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
