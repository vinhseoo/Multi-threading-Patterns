# ⏱️ KỊCH BẢN LIVE DEMO CHI TIẾT TỪNG BƯỚC (CHUẨN BẢO VỆ A+)
## ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
**Môn học:** Lập Trình Mạng (PTIT)  
**Giảng viên chấm:** TS. Đặng Ngọc Hùng  
**Hình thức:** Sinh viên bảo vệ Solo (Một mình trình diễn Live Demo & Phản biện Q&A)  
**Thời gian trình diễn:** 5 — 6 phút  

---

> [!IMPORTANT]
> **MỤC TIÊU CỐT LÕI CỦA BUỔI LIVE DEMO:**  
> Không chỉ chạy code cho vui, mà phải **chỉ rõ được sự biến thiên của các chỉ số hệ điều hành (OS Native Threads, Context Switching, TCP Backlog, Bounded Queue, và Virtual Threads M:N Mapping)** ngay trên màn hình Web Dashboard thời gian thực trước mắt thầy Đặng Ngọc Hùng!

---

## 🖥️ CHUẨN BỊ MÔI TRƯỜNG TRƯỚC KHI BẮT ĐẦU (00:00)

1. **Mở 2 cửa sổ Terminal (PowerShell hoặc CMD) tại thư mục dự án:**
   - **Terminal 1 (Server Console):** Dùng để bật/tắt các Mode máy chủ (`.\run_server.bat <mode>` hoặc `.\run_server.ps1 <mode>`).
   - **Terminal 2 (Benchmark Runner - Tùy chọn):** Dùng khi muốn bắn tải cực lớn bằng công cụ dòng lệnh (`benchmark\run_benchmark.bat`).
2. **Mở trình duyệt (Chrome/Edge):**
   - Truy cập sẵn đường dẫn: `http://localhost:8080/dashboard`
   - Nhấn `F11` (Full screen) để giao diện hiển thị trọn vẹn, chuyên nghiệp nhất.

---

## 🎯 BẢNG TRA CỨU NHANH HIỆN TƯỢNG TRỰC QUAN TRÊN DASHBOARD THEO TỪNG MODE

| Chế Độ (Mode) | Lệnh Khởi Động | Huy Hiệu Header (Badge) | Nút Thử Nghiệm Bấm | Biến Thiên KPI Trên Dashboard | Hiện Tượng Biểu Đồ Số 3 (Key Chart) | Log Console Phía Dưới |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Mode 1**<br>Iterative Single-Thread | `.\run_server.bat 1` | `Mode 1: Iterative...`<br>*(Xám: Đơn luồng tuần tự)* | `⚡ Burst 30 Reqs (Delay 100ms)` | - **RPS:** ~9.8 RPS (Cực thấp)<br>- **Latency:** ~2,500ms - 3,000ms<br>- **OS Threads:** Giữ nguyên 7-10 luồng | **CẢ 2 ĐƯỜNG ĐỀU PHẲNG LÌ:**<br>- **Đường Xanh (Conns):** Giữ nguyên ở mức **1** (chỉ accept 1 kết nối tại 1 thời điểm).<br>- **Đường Đỏ (Threads):** Giữ nguyên **7-10 luồng**. | In ra lần lượt từng dòng rất chậm:<br>`[✓] HTTP 200 trong ...ms [Thread: main]` |
| **Mode 2**<br>Thread-per-Conn | `.\run_server.bat 2` | `Mode 2: Thread-per-Conn...`<br>*(Đỏ: 1 Thread Per Socket)* | `⚡ Burst 30 Reqs (Delay 100ms)`<br>Hoặc `🚀 Spike 100 Reqs` | - **RPS:** Vọt lên 250 - 280 RPS<br>- **Latency:** Xong ngay trong **110ms - 130ms**!<br>- **OS Threads:** **Tăng vọt thêm 30 - 100 luồng!** | **Đường Xanh (Conns) và Đường Đỏ (Threads) DÍNH CHẶT VÀO NHAU cùng nhảy vọt lên đỉnh!** | Bắn ra đồng loạt 30 dòng cùng lúc:<br>`[✓] HTTP 200 trong 115ms [Thread: ThreadPerConn-1]`<br>`[Thread: ThreadPerConn-2]...` |
| **Mode 3**<br>Worker Thread Pool | `.\run_server.bat 3` | `Mode 3: Worker Thread Pool...`<br>*(Vàng: Queue: 0/1000 \| Workers: 16/16)* | Click nút cam:<br>**`🔥 Benchmark 500 Clients (2,000 Reqs)`**<br>*(Hoặc Terminal: run_benchmark.bat)* | - **RPS:** Ổn định ~150 RPS<br>- **Latency:** ~210ms (16 worker chia 2 đợt)<br>- **OS Threads:** **BỊ KHÓA CỨNG Ở 16 LUỒNG!** | **Đường Xanh (Conns)** vọt lên 500,<br>nhưng **Đường Đỏ (Threads)** bị **chặn trần tuyệt đối ở mức 16 luồng**! | Các worker luân phiên tiêu thụ task:<br>`[Thread: WorkerPool-1]`<br>`[Thread: WorkerPool-2]...` |
| **Mode 4**<br>Java 21 Virtual Threads | `.\run_server.bat 4` | `Mode 4: Java 21 Virtual Threads...`<br>*(Xanh ngọc: OS Carrier Threads: 16)* | `🚀 Spike 100 Reqs`<br>Hoặc Benchmark 1,000 clients | - **RPS:** **Vọt lên 1,100+ RPS**<br>- **Latency:** Siêu tốc **70ms - 90ms**<br>- **OS Threads:** **HOÀN TOÀN PHẲNG LÌ Ở 15-16 LUỒNG!** | **Đường Xanh (Conns) vọt lên đỉnh chót vót (1,000 conns)**,<br>trong khi **Đường Đỏ (OS Threads) NẰM NGANG PHẲNG LÌ** dưới đáy! | In ra hàng loạt luồng ảo siêu nhẹ:<br>`[✓] HTTP 200 trong 72ms [Thread: VirtualThread-1] [Loom Virtual]` |
| **Mode 5**<br>Custom Thread Pool *(30% Depth)* | `.\run_server.bat 5` | `Mode 5: Custom Thread Pool...`<br>*(Tím: Queue: 0/1000 \| Workers: 16/16)* | `⚡ Burst 30 Reqs`<br>Hoặc `🔥 Benchmark 500 Clients` | - **RPS:** Ổn định ~148 RPS<br>- **Latency:** ~220ms<br>- **OS Threads:** **BỊ KHÓA CỨNG Ở 16 LUỒNG!** | **Đường Xanh vọt lên, Đường Đỏ chặn cứng ở 16 luồng** (giống Mode 3 nhưng tự viết 100% bằng mảng vòng + wait/notify). | Các worker tự lập trình tiêu thụ task:<br>`[Thread: CustomWorker-1]`<br>`[Thread: CustomWorker-2]...` |

---

## 🎬 KỊCH BẢN CHI TIẾT TỪNG PHÂN CẢNH (CÓ LỜI THOẠI MẪU)

---

### 📍 PHÂN CẢNH 1: KHỞI ĐỘNG HỆ THỐNG & GIỚI THIỆU TỔNG QUAN DASHBOARD (00:00 – 01:00)

#### 1. Thao tác kỹ thuật:
- Tại Terminal 1, khởi động máy chủ ở **Mode 4** (Chế độ hiện đại nhất):
  ```powershell
  .\run_server.bat 4
  ```
  *(Hoặc nếu chạy PowerShell: `.\run_server.ps1 4`)*
- Mở trình duyệt tại `http://localhost:8080/dashboard`.

#### 2. Chỉ tay vào màn hình Dashboard và giải thích:
- **Thanh Header trên cùng:**
  - **Badge Chế độ:** Đang hiển thị `Mode 4: Java 21 Virtual Threads (Project Loom)` với chấm tròn xanh đang nhấp nháy (Live Polling mỗi 500ms).
  - **Badge Luồng nền:** Hiển thị `OS Carrier Threads: 16 (M:N Mapping)`.
- **6 Thẻ KPI Thời Gian Thực:**
  - `THROUGHPUT (RPS)`: Đo thông lượng tức thời bằng cửa sổ trượt Lock-free (LongAdder).
  - `TOTAL REQUESTS`: Tổng số request đã nhận và phân loại Thành công / Thất bại.
  - `RESPONSE LATENCY`: Độ trễ trung bình, Min và phân vị **P95 Latency**.
  - `ACTIVE CONNECTIONS`: Số socket kết nối mạng đang mở đồng thời.
  - `OS NATIVE THREADS`: **Số luồng vật lý cấp hệ điều hành** lấy trực tiếp từ JMX `ThreadMXBean`.
  - `JVM HEAP & CPU`: Đo tiêu hao % CPU và RAM Heap của tiến trình máy chủ.
- **4 Biểu Đồ Canvas Native 60fps:**
  - Biểu đồ 1 (Xanh lơ Cyan): Throughput RPS theo thời gian.
  - Biểu đồ 2 (Tím Violet): Biến thiên độ trễ mạng (Latency ms).
  - **Biểu đồ 3 (Trọng tâm đề tài):** Đối đầu trực diện giữa **Active Connections (Xanh lá)** và **OS Native Threads (Đỏ hồng)**.
  - Biểu đồ 4 (Xanh dương / Vàng): Mức độ ngốn CPU và RAM.

#### 3. Lời thoại trình bày:
> *"Kính thưa thầy Đặng Ngọc Hùng, để phơi bày trực quan và đo lường chính xác hành vi của các mô hình đa luồng ở mức hệ điều hành, em đã tự tay xây dựng toàn bộ Web Dashboard thời gian thực cập nhật chu kỳ 500ms bằng 100% Native Canvas không dùng bất kỳ thư viện bên ngoài nào.*  
> *Mục tiêu xuyên suốt của buổi demo hôm nay là chứng minh sự chuyển dịch kiến trúc: Từ mô hình Đơn luồng tuần tự bế tắc, qua Đa luồng truyền thống gây cạn kiệt tài nguyên OS Thread, chuẩn hóa bằng Thread Pool doanh nghiệp, và đạt đỉnh cao hiệu năng với Java 21 Virtual Threads."*

---

### 📍 PHÂN CẢNH 1.5: PHÂN ĐỊNH 3 DẠNG TẢI TRÊN DEMO CONTROL CENTER (PURE I/O vs I/O-BOUND vs CPU-BOUND) (01:00 – 01:30)

#### 1. Thao tác kỹ thuật trên Dashboard:
Lần lượt click 3 nút test đơn lẻ trên thanh công cụ Demo Control Center:
1. Click **`🟢 GET /api/hello (1 req - Pure I/O)`**:
   - Log Console phản hồi tức thì: `[✓] HTTP 200 trong 0ms [Thread: VirtualThread-...] [Loom Virtual]`.
   - Mục đích: Đo trần thông lượng Network I/O không chịu tải trễ.
2. Click **`🟡 GET /api/delay?ms=150 (1 req - I/O Bound)`**:
   - Log Console phản hồi sau 150ms: Mô phỏng gọi Database / API bên thứ ba bằng `Thread.sleep(150)`.
3. Click **`🔴 GET /api/compute?n=32 (1 req - CPU Bound)`**:
   - Quan sát **Biểu đồ số 4 (CPU % & RAM)**: Kim CPU nhảy vọt tức thì lên cao do thuật toán đệ quy Fibonacci(32).

#### 2. Lời thoại giải thích bản chất (Ghi điểm lý thuyết sâu):
> *"Thưa thầy, hệ thống hỗ trợ đo lường cả 3 dạng tải mạng: Pure I/O, I/O-Bound và CPU-Bound.*  
> *- Với **I/O-Bound** (`/api/delay`): Luồng chủ yếu chờ đợi socket/database, đây chính là 'sân khấu' để Java 21 Virtual Threads phát huy tối đa sức mạnh unmount luồng.*  
> *- Với **CPU-Bound** (`/api/compute`): Thuật toán đệ quy chiếm dụng 100% chu kỳ xung nhịp CPU, do đó Virtual Threads sẽ không giúp tăng tốc độ xử lý hơn so với số core vật lý có sẵn."*

---

### 📍 PHÂN CẢNH 2: ĐỐI CHỨNG SINGLE-THREAD (MODE 1) VS THREAD-PER-CONNECTION (MODE 2) (01:30 – 02:45)

#### 1. Bước A: Chạy Mode 1 (Single-Threaded Iterative Server)
- **Thao tác:**
  - Tại Terminal 1, bấm `Ctrl + C` để dừng server, gõ:
    ```powershell
    .\run_server.bat 1
    ```
  - Chuyển sang trình duyệt (Dashboard tự động nhận diện `Mode 1: Iterative Single-Threaded Server`).
  - Tại mục **🎮 DEMO CONTROL CENTER**, click nút:
    `⚡ Burst 30 Reqs (Delay 100ms)`
- **Hiện tượng trên màn hình cần chỉ cho thầy xem:**
  1. **Log Console:** Các dòng log nhảy rất chậm chạp, lần lượt từng request một, mỗi dòng cách nhau đúng 100ms:
     `[✓] HTTP 200 trong 105ms [Thread: main]`  
     `[✓] HTTP 200 trong 210ms [Thread: main]`...
     Đặc biệt, tên luồng luôn cố định là `[Thread: main]`.
  2. **Thẻ KPI Response Latency:** Trung bình độ trễ vọt lên **2,500ms - 3,000ms**, P95 chạm đỉnh **3,000ms**!
  3. **Biểu đồ số 3 (Khoảnh khắc cốt lõi giải thích bản chất nghẽn mạng):**
     - **Cả 2 đường đều phẳng lì đứng yên:** Đường xanh lá (Active Connections) luôn giữ ở mức **1**, đường đỏ (OS Threads) giữ nguyên ở mức **7** luồng.
     - **Giải thích tầng sâu kiến trúc mạng:** Tại sao bắn 30 requests mà Active Connections không nhảy lên 30?
       + Vì máy chủ chỉ có **DUY NHẤT 1 luồng `main`**, nó vừa làm nhiệm vụ `accept()` vừa trực tiếp xử lý `processConnection()`.
       + Khi nhận Request số 1, luồng `main` bị **block cứng 100ms** để xử lý. Trong suốt 100ms đó, luồng `main` **chưa hề quay lại vòng lặp để gọi `accept()` cho 29 request còn lại**!
       + 29 request còn lại hoàn toàn bị **giam lỏng trong hàng đợi TCP Backlog của Kernel Hệ điều hành** (chưa hề được đưa lên tầng Application của Java).
       + Xử lý xong Request 1 -> đóng socket -> mới `accept()` tiếp Request 2. Do đó ở tầng ứng dụng, tại mọi thời điểm **Active Connections chỉ có tối đa 1 kết nối duy nhất**!
  4. **Đối chiếu với Mode 2 tiếp theo:** Khi chuyển sang Mode 2 (Thread-per-Conn), luồng Acceptor chỉ làm `accept()` rồi ủy thác ngay cho luồng mới, nên cả 30 socket mới được accept đồng thời và kéo cả 2 đường cùng nhảy vọt lên 30!

#### 2. Bước B: Chuyển sang Mode 2 (Thread-per-Connection)
- **Thao tác:**
  - Tại Terminal 1, bấm `Ctrl + C`, gõ:
    ```powershell
    .\run_server.bat 2
    ```
  - Trên Dashboard, Header đổi sang viền đỏ: `Mode 2: Thread-per-Connection Server`.
  - Click lại nút:
    `⚡ Burst 30 Reqs (Delay 100ms)`
- **Hiện tượng bùng nổ trên màn hình:**
  1. **Log Console:** Ngay lập tức, **toàn bộ 30 requests hoàn tất đồng loạt trong chỉ 115ms**!
     Dòng thông báo: `[🔥 HOÀN TẤT TẢI] 30 reqs trong 118ms (Thành công: 30, Lỗi: 0)`.
     Tên luồng được phân nhánh độc lập: `[Thread: ThreadPerConn-1]`, `[Thread: ThreadPerConn-2]`...
  2. **Thẻ KPI OS Native Threads:** Con số lập tức nhảy tăng thêm **30 luồng**!
  3. **Biểu đồ số 3 (Khoảnh khắc ấn tượng):** Đường màu xanh lá (Connections) và đường màu đỏ (OS Threads) **nhảy vọt song song cùng nhau lên đỉnh 30 luồng**!

#### 3. Lời thoại trình bày:
> *"Thưa thầy, đây là minh chứng rõ ràng nhất cho giá trị sống còn của lập trình mạng đa luồng: Cùng một khối lượng 30 requests mô phỏng I/O 100ms, Mode 1 mất tới 3.0 giây tuần tự, trong khi Mode 2 hoàn tất chỉ trong 115 mili-giây — nhanh hơn gấp gần 30 lần!*  
> *Tuy nhiên, như thầy thấy ở Biểu đồ số 3: Cứ mỗi kết nối vào, Hệ điều hành lại phải đẻ ra một luồng native mới (`new Thread()`). Điều này dẫn tới điểm nghẽn nghiêm trọng khi tải tăng cao."*

---

### 📍 PHÂN CẢNH 3: THỬ THÁCH GIỚI HẠN TẢI CAO (C1000 STRESS) & WORKER THREAD POOL (02:45 – 04:00)

#### 1. Bước A: Phơi bày tử huyệt của Mode 2 dưới tải lớn
- **Thao tác:**
  - Khi server đang chạy ở Mode 2, click nút:
    `🚀 Spike 100 Reqs Đồng Thời!`
    *(Hoặc tại Terminal 2 chạy: `cmd.exe /c "benchmark\run_benchmark.bat -c 500 -n 1000 --url http://localhost:8080/api/delay?ms=100"`)*
- **Hiện tượng trên màn hình:**
  1. Thẻ **OS Native Threads** tăng vọt lên hàng trăm luồng!
  2. Biểu đồ số 4: CPU bắt đầu nhảy mạnh do **Context Switching Overhead** (Hệ điều hành liên tục tráo đổi thanh ghi và xóa sạch L1/L2 Cache của CPU).
  3. Bộ nhớ RAM tăng vọt vì mỗi OS Thread trong JVM ngốn mặc định 1MB Stack Memory (`-Xss1m`).

#### 2. Bước B: Kích hoạt Mode 3 (Worker Thread Pool chuẩn doanh nghiệp)
- **Thao tác:**
  - Tại Terminal 1, bấm `Ctrl + C`, khởi động Mode 3:
    ```powershell
    .\run_server.bat 3
    ```
  - Quan sát Header Dashboard: Hiển thị ngay badge màu vàng:  
    `📦 Queue: 0/1000 | Workers: 16/16`.
  - Tại mục **🎮 DEMO CONTROL CENTER**, click trực tiếp nút cam:  
    👉 **`🔥 Benchmark 500 Clients (2,000 Reqs)`**  
    *(Máy chủ sẽ tự động gọi ngầm `JavaLoadTester` phát động 500 luồng TCP đồng thời bắn 2,000 requests vào máy chủ mà bạn không cần mở terminal gõ tay!)*
- **Hiện tượng kiểm soát tài nguyên tuyệt đối cần chỉ cho thầy xem:**
  1. **Thẻ KPI OS Native Threads:** Con số **bị khóa cứng ở mức 16 luồng cố định** (cộng vài luồng nền JVM là ~26 luồng), tuyệt đối không bao giờ vượt quá!
  2. **Huy hiệu Queue trên Header:** Nhảy số phản ánh tác vụ xếp hàng trong Bounded ArrayBlockingQueue và được 16 Worker Threads tiêu thụ nhịp nhàng.
  3. **Biểu đồ số 3:** Đường xanh lá (Connections) vọt lên 100-500, nhưng **đường đỏ OS Threads nằm ngang chặn trần ở mức 16 luồng**!
  4. **Log Console:** Tên luồng quay vòng có kiểm soát: `[Thread: WorkerPool-1]`, `[Thread: WorkerPool-2]`... `[Thread: WorkerPool-16]`.
  5. **Minh họa cơ chế Backpressure (HTTP 503 Rejection):**
     - Giải thích: Nếu hàng đợi 1,000 tasks bị tràn, máy chủ kích hoạt `sendServiceUnavailable()` trả về ngay mã HTTP 503 Service Unavailable để bảo vệ hệ thống khỏi sập bộ nhớ. Thẻ KPI hiển thị số lượng requests lỗi tăng lên mà tiến trình JVM vẫn sống an toàn!

#### 3. Lời thoại trình bày:
> *"Thưa thầy, để khắc phục triệt để nguy cơ sập bộ nhớ và nghẽn CPU của Mode 2, ở **Mode 3 - Worker Thread Pool**, em đã áp dụng mẫu thiết kế Producer-Consumer chuẩn doanh nghiệp:*  
> *- Acceptor thread nhận socket và đẩy vào Bounded Blocking Queue sức chứa 1,000 tasks.*  
> *- 16 Worker Threads được tạo sẵn (Pre-spawned) liên tục rút task ra xử lý.*  
> *Như thầy thấy trên Biểu đồ số 3: Dù tải tăng đột biến, số lượng OS Platform Threads vẫn được khống chế nghiêm ngặt ở 16 luồng, CPU không bị lãng phí Context Switch. Nếu hàng đợi vượt quá 1,000 tasks, máy chủ kích hoạt Rejection Policy trả về ngay HTTP 503 Service Unavailable để tự bảo vệ, không bao giờ bị Crash tiến trình."*

---

### 📍 PHÂN CẢNH 3.5: [INNOVATION 30%] CHỨNG MINH ĐỘ SÂU KỸ THUẬT VỚI CUSTOM THREAD POOL & BOUNDED QUEUE (MODE 5) (04:00 – 04:45)

#### 1. Thao tác kỹ thuật:
- Tại Terminal 1, bấm `Ctrl + C`, khởi động **Mode 5** (Chế độ tự lập trình 100% không dùng thư viện ngoài):
  ```powershell
  .\run_server.bat 5
  ```
- Quan sát Header Dashboard: Hiển thị badge màu tím:
  `Mode 5: Custom Thread Pool Server (Self-implemented Pool)`  
  `📦 Queue: 0/1000 | Workers: 0/16`
- Tại mục **🎮 DEMO CONTROL CENTER**, click nút:
  `⚡ Burst 30 Reqs (Delay 100ms)` hoặc `🔥 Benchmark 500 Clients`

#### 2. Hiện tượng và điểm nhấn kỹ thuật cần chỉ cho thầy xem:
1. **Log Console:** Tên luồng in ra chính xác tên các worker tự tạo:
   `[Thread: CustomWorker-1]`, `[Thread: CustomWorker-2]`... `[Thread: CustomWorker-16]`.
2. **Biểu đồ số 3:** Tương tự Mode 3 chuẩn, số luồng OS Native Threads bị **khống chế cứng ở 16 luồng**.
3. **Mở file code trực tiếp cho thầy xem (Chốt trọn 30% điểm Technical Depth):**
   - Mở file [CustomBlockingQueue.java](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/src/main/java/vn/ptit/network/pool/CustomBlockingQueue.java): Chỉ cho thầy thấy cấu trúc **Mảng vòng (Circular Array Buffer)** đạt độ phức tạp $O(1)$, cùng cặp hàm nguyên bản `synchronized`, `wait()` và `notifyAll()`.
   - Mở file [CustomWorker.java](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/src/main/java/vn/ptit/network/pool/CustomWorker.java): Chỉ cho thầy thấy luồng kế thừa trực tiếp từ `Thread`, chạy vòng lặp rút task từ hàng đợi tự viết.

#### 3. Lời thoại trình bày:
> *"Thưa thầy, để đạt điểm tuyệt đối về Technical Depth, ở **Mode 5**, em đã tự tay xây dựng toàn bộ hệ thống Thread Pool từ con số 0 mà không dùng bất kỳ lớp nào trong `java.util.concurrent`.*  
> *- Em tự cài đặt `CustomBlockingQueue` bằng mảng vòng (Circular Array) $O(1)$, áp dụng Java Monitor Pattern với `synchronized`, `wait()` và `notifyAll()`.*  
> *- Khi hàng đợi đầy 1,000 tasks, hàm `offer()` trả về `false` ngay (Non-blocking) để trả về mã lỗi HTTP 503 tự bảo vệ hệ thống.*  
> *- 16 `CustomWorker` kế thừa trực tiếp từ `Thread` liên tục gọi `take()` và tự động ngủ đông khi không có task để giải phóng CPU.*  
> *Kết quả đo đạc trên Dashboard cho thấy Mode 5 tự viết đạt hiệu năng và độ ổn định tương đương hoàn toàn với `ThreadPoolExecutor` chuẩn của Oracle."*

---

### 📍 PHÂN CẢNH 4: ĐỈNH CAO JAVA 21 PROJECT LOOM VIRTUAL THREADS (04:45 – 05:30)

#### 1. Thao tác kỹ thuật:
- Tại Terminal 1, chuyển sang **Mode 4**:
  ```powershell
  .\run_server.bat 4
  ```
- Tại Terminal 2, chạy kịch bản thử thách cực đại (1,000 clients đồng thời, 3,000 requests):
  ```powershell
  cmd.exe /c "benchmark\run_benchmark.bat -c 1000 -n 3000 --url http://localhost:8080/api/delay?ms=100"
  ```
- Quay sang màn hình Dashboard quan sát trực tiếp.

#### 2. Hiện tượng ĐỘT PHÁ trên màn hình (Điểm chốt hạ lấy trọn điểm A+):
1. **Biểu đồ số 3 (Khoảnh khắc đắt giá nhất của buổi bảo vệ):**
   - Đường màu xanh lá (**Active Connections**) vọt lên đỉnh cao chót vót: **1,000 kết nối đồng thời**!
   - Nhưng đường màu đỏ (**OS Native Threads**) **hoàn toàn nằm ngang phẳng lì ở mức chỉ 15–16 luồng Carrier Threads**!
2. **Thẻ KPI Thông Lượng (Throughput RPS):**
   - Vọt lên con số ấn tượng: **1,100+ Requests/giây**!
3. **Thẻ KPI Response Latency:**
   - Dù có 1,000 client cùng bắn tải, độ trễ P95 chỉ ở mức **90ms - 95ms**!
4. **Log Console:**
   - Từng dòng log in ra kèm chữ ký xác thực:  
     `[✓] HTTP 200 trong 72ms [Thread: VirtualThread-142] [Loom Virtual]`.
5. **Tiêu hao phần cứng (KPI Card 6):**
   - RAM Heap chỉ tiêu tốn vỏn vẹn **~70MB**, không hề bị quá tải.

#### 3. Lời thoại trình bày:
> *"Và thưa thầy, đây chính là điểm đột phá công nghệ cao nhất của đề tài em: **Mode 4 — Java 21 Virtual Threads (Project Loom)**.*  
> *Em vừa phát sinh **1,000 kết nối đồng thời** bắn 3,000 requests vào máy chủ.*  
> *Xin thầy hãy quan sát kỹ **Biểu đồ số 3** trên màn hình:*  
> *- Đường màu xanh lá (Active Connections) vọt lên đỉnh 1,000 kết nối.*  
> *- Nhưng đường màu đỏ (**OS Platform Threads**) hoàn toàn nằm phẳng lì ở mức **chỉ 15–16 luồng**!*  
> *- **Bản chất kỹ thuật:** Virtual Thread là luồng ảo chạy trong User-space do JVM quản lý với mô hình ánh xạ M:N. Khi một luồng ảo bị block ở thao tác mạng hoặc I/O delay, JVM tự động unmount nó khỏi Carrier Thread và gán luồng ảo khác vào chạy tiếp. Khi có dữ liệu, nó mount trở lại.*  
> *Nhờ đó, ta đạt được thông lượng kỷ lục hơn 1,100 RPS với chi phí bộ nhớ chỉ vài KB mỗi luồng, lập trình blocking tuần tự dễ hiểu nhưng hiệu năng tương đương non-blocking phức tạp!"*

---

### 📍 PHÂN CẢNH 5: BẢNG ĐỐI SÁNH BENCHMARK TRỰC TIẾP TRÊN DASHBOARD & KẾT LUẬN (05:30 – 06:00)

#### 1. Thao tác kỹ thuật:
- Cuộn chuột xuống ngay bên dưới Demo Control Center tới mục:  
  **📊 BẢNG ĐỐI SÁNH HIỆU NĂNG THỰC NGHIỆM (5 MÔ HÌNH LUỒNG ĐỐI ĐẦU)**.
- Nhấp nút **`🔄 Làm Mới Dữ Liệu`** (Dashboard tự động gọi `/api/benchmark` và hiển thị kết quả).
- Chỉ cho thầy thấy tính năng **Sắp xếp thời gian (Sort Mới nhất trước ▼)** và khả năng click vào bất kỳ cột nào để đối sánh trực tiếp.

#### 2. Chỉ vào các phần tử trực quan trên màn hình Benchmark:
1. **Bảng Ma Trận Đối Sánh Tổng Hợp (Benchmark Matrix Table):**
   - Chỉ vào hàng **Mode 1 (Iterative)**: RPS 9.8, P95 3,050ms -> Điểm nghẽn tuần tự ở TCP Backlog.
   - Chỉ vào hàng **Mode 2 (Thread-per-Connection)**: RPS 285.4, OS Threads 500+ luồng, RAM 550MB -> Điểm nghẽn Context Switching và nguy cơ OOM Crash.
   - Chỉ vào hàng **Mode 3 (Worker Thread Pool)**: RPS 152.0, OS Threads 16 luồng cố định, RAM 65MB -> An toàn, ổn định với Bounded Queue.
   - Chỉ vào hàng **Mode 4 (Virtual Threads Loom)**: RPS 1,145.2, P95 95ms, OS Threads 15-16 luồng phẳng -> Đỉnh cao thông lượng.
   - Chỉ vào hàng **Mode 5 (Custom Thread Pool)**: Mô hình tự viết BlockingQueue và Worker từ đầu (RPS ~148.6, P95 ~495ms) để chứng minh hiểu sâu internals.
2. **Hai Thanh Đồ Thị Đối Sánh Cột (Visual Bars):**
   - Thanh màu xanh ngọc (Mode 4) chiếm ưu thế áp đảo về Throughput (1,145 RPS).
   - Thanh độ trễ P95 của Mode 4 ngắn nhất (chỉ 95ms so với 3,050ms của Mode 1).
3. **Bảng Lịch Sử Các Lần Test (Trích xuất từ `benchmark_results.csv`):**
   - Chỉ cho thầy thấy từng lần đo đạc thực tế có dấu ấn thời gian (Timestamp), số Client, số Request thành công 100%, và phân vị độ trễ P50/P95.
   - Nút **`📥 Tải File CSV`** cho phép tải file dữ liệu thô về máy bất cứ lúc nào.

#### 3. Lời thoại kết luận và chuyển sang phần Q&A:
> *"Thưa thầy, toàn bộ các số liệu đo đạc khoa học từ công cụ `JavaLoadTester` đều đã được hiển thị trực quan trên Bảng đối sánh Benchmark của Dashboard và tự động đồng bộ vào file CSV.*  
> *Đề tài đã hoàn thành xuất sắc mục tiêu: Đối chứng toàn diện 5 mô hình luồng (từ tuần tự, đa luồng hệ điều hành, Thread Pool tự viết đến Java 21 Project Loom) để giải bài toán nghẽn mạng High-Concurrency.*  
> *Phần trình diễn Live Demo của em đến đây là kết thúc. Em xin kính mời thầy Đặng Ngọc Hùng đặt câu hỏi phản biện ạ!"*

---

## 💡 GỢI Ý MẸO PHẢN XẠ NHANH KHI GẶP TÌNH HUỐNG TRÊN LỚP

- **Nếu thầy bảo: "Em đổi cổng server sang 9090 xem sao?":**  
  Chạy ngay: `.\run_server.bat 4 9090` (máy chủ đã hỗ trợ tham số port thứ 2).
- **Nếu thầy bảo: "Em thử chứng minh cơ chế từ chối tải 503 của Thread Pool xem?":**  
  Bật Mode 3 (hoặc Mode 5), dùng terminal bắn tải vượt quá 1,000 tasks trong queue, server sẽ lập tức trả về mã HTTP 503 kèm header `Retry-After: 2` hiển thị trên màn hình.
- **Nếu thầy bảo: "Em có tự viết Thread Pool không hay chỉ dùng thư viện có sẵn?":**  
  Bật ngay **Mode 5** (`.\run_server.bat 5`) và mở trực tiếp các file [CustomBlockingQueue.java](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/src/main/java/vn/ptit/network/pool/CustomBlockingQueue.java), [CustomThreadPool.java](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/src/main/java/vn/ptit/network/pool/CustomThreadPool.java) và [CustomWorker.java](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/src/main/java/vn/ptit/network/pool/CustomWorker.java) giải thích cơ chế Mảng vòng O(1), Monitor Pattern `synchronized`, `wait()`, `notifyAll()`. Thầy sẽ chấm trọn điểm A+ ngay lập tức!
