# ⚡ HƯỚNG DẪN DEMO NHANH CÁC CHẾ ĐỘ (5 MODES DEMO GUIDE)
## Đề Tài T45: Multi-Threading Patterns in Network Programming

> [!TIP]
> **Mục đích tài liệu:** Hướng dẫn ngắn gọn, trực quan cách khởi chạy và thực hiện demo từng mô hình kiến trúc luồng (Mode 1 đến Mode 5), các nút bấm trên Web Dashboard và hiện tượng mấu chốt cần quan sát.

---

## 🛠️ 1. CHUẨN BỊ MÔI TRƯỜNG (30 GIÂY)

### Bước 1: Biên dịch mã nguồn (chỉ cần chạy 1 lần)
Mở PowerShell hoặc CMD tại thư mục gốc dự án:
```powershell
.\build.bat
```

### Bước 2: Mở Web Dashboard giám sát
Mở trình duyệt (Chrome / Edge) và truy cập:
👉 **`http://localhost:8080/dashboard`** *(Bấm `F11` để bật chế độ toàn màn hình chuyên nghiệp)*

---

## 🚀 2. HƯỚNG DẪN DEMO TỪNG MODE CHI TIẾT

```
Mode 1 (Iterative)  ──►  Mode 2 (Thread/Conn)  ──►  Mode 3 (Thread Pool)  ──►  Mode 5 (Custom Pool)  ──►  Mode 4 (Virtual Threads)
   (Nghẽn cổ chai)         (Bùng nổ OS Thread)        (Chặn trần an toàn)       (Tự viết hàng đợi)           (Đỉnh cao 1,000+ RPS)
```

---

### 1️⃣ MODE 1: Iterative Single-Threaded Server (Tuần tự - Baseline)

* **Lệnh khởi động:**
  ```powershell
  .\run_server.bat 1
  # hoặc PowerShell: .\run_server.ps1 1
  ```
* **Nút bấm Demo trên Dashboard:**
  Click nút vàng: **`⚡ Burst 30 Reqs (Delay 100ms)`**
* **Hiện tượng quan sát trên Dashboard:**
  * **RPS (Throughput):** Tụt dốc thảm hại, chỉ đạt **~9.8 RPS**.
  * **Latency (Độ trễ):** Lên tới **~2,800ms – 3,000ms** (do 30 request phải nối đuôi nhau: $30 \times 100\text{ms}$).
  * **Biểu đồ số 3 (Conns vs Threads):** Cả 2 đường đều phẳng lì dưới đáy (Active Conns chỉ nhận 1 kết nối tại 1 thời điểm; OS Threads giữ nguyên 7–10 luồng).
  * **Log Console:** In ra lần lượt từng dòng rất chậm: `[✓] HTTP 200 trong ...ms [Thread: main]`.
* **💡 Điểm mấu chốt giải thích (Punchline):**
  > *"Hiện tượng **Head-of-Line Blocking**: Chỉ 1 luồng `main` duy nhất, 1 client bị chậm thì toàn bộ client phía sau bị treo cứng trong hàng đợi TCP Backlog của OS."*

---

### 2️⃣ MODE 2: Thread-per-Connection Server (Mỗi kết nối 1 luồng OS)

* **Lệnh khởi động:**
  ```powershell
  .\run_server.bat 2
  ```
* **Nút bấm Demo trên Dashboard:**
  Click nút đỏ: **`🚀 Spike 100 Reqs Đồng Thời!`** *(hoặc `⚡ Burst 30 Reqs`)*
* **Hiện tượng quan sát trên Dashboard:**
  * **RPS & Latency:** Phản hồi rất nhanh (**~110ms – 130ms**, ~280 RPS) vì mỗi request có luồng riêng để xử lý song song.
  * **Biểu đồ số 3 (ĐẶC BIỆT CHÚ Ý):** **Đường Đỏ (OS Native Threads) và Đường Xanh (Active Conns) DÍNH CHẶT VÀO NHAU**, cùng nhảy vọt dựng đứng lên đỉnh (từ 10 luồng vọt lên 40–110 luồng).
  * **Log Console:** Bắn ra 30–100 dòng cùng 1 tích tắc: `[Thread: ThreadPerConn-1]`, `[Thread: ThreadPerConn-2]...`
* **💡 Điểm mấu chốt giải thích (Punchline):**
  > *"Tốc độ nhanh trong ngắn hạn nhưng **không thể scale**. Mỗi kết nối ngốn 1MB stack bộ nhớ và 1 Kernel Thread. Khi tải 1,000+ kết nối, server sẽ sập vì OutOfMemory hoặc nghẽn do OS Context Switching bão hòa."*

---

### 3️⃣ MODE 3: Worker Thread Pool Server (Chuẩn doanh nghiệp)

* **Lệnh khởi động:**
  ```powershell
  .\run_server.bat 3
  ```
* **Nút bấm Demo trên Dashboard:**
  Click nút cam: **`🔥 Benchmark 500 Clients (2,000 Reqs)`** *(hoặc `🚀 Spike 100 Reqs`)*
* **Hiện tượng quan sát trên Dashboard:**
  * **Header Badge:** Hiển thị rõ cấu hình: `Queue: 0/1000 | Workers: 16/16`.
  * **Biểu đồ số 3:** Đường Xanh (Active Conns) vọt lên 100–500, nhưng **Đường Đỏ (OS Threads) BỊ KHÓA CỨNG TUYỆT ĐỐI Ở 16 LUỒNG**.
  * **Độ trễ:** Tăng có kiểm soát (~210ms) do 16 worker chia ca nhau gắp task từ hàng đợi `ArrayBlockingQueue`.
  * **Log Console:** Các worker luân phiên tái sử dụng: `[Thread: WorkerPool-1]` đến `[Thread: WorkerPool-16]`.
* **💡 Điểm mấu chốt giải thích (Punchline):**
  > *"Bảo vệ tài nguyên hệ thống an toàn tuyệt đối. Luôn cố định 16 OS Threads, có cơ chế **Backpressure** (trả về HTTP 503 Service Unavailable khi hàng đợi 1,000 vượt ngưỡng)."*

---

### 4️⃣ MODE 5: Custom Thread Pool Server (Điểm sáng kỹ thuật - Tự viết từ đầu)

* **Lệnh khởi động:**
  ```powershell
  .\run_server.bat 5
  ```
* **Nút bấm Demo trên Dashboard:**
  Click nút: **`⚡ Burst 30 Reqs`** hoặc **`🔥 Benchmark 500 Clients`**
* **Hiện tượng quan sát trên Dashboard:**
  * **Header Badge:** Màu tím `Mode 5: Custom Thread Pool (Self-implemented Queue & Workers)`.
  * **Hiệu năng & Khóa luồng:** Tương đương Mode 3 (khóa cứng 16 luồng `CustomWorker`), độ trễ ~220ms.
  * **Log Console:** In ra tiền tố: `[Thread: CustomWorker-1]` đến `[Thread: CustomWorker-16]`.
* **💡 Điểm mấu chốt giải thích (Punchline):**
  > *"Toàn bộ cơ chế Producer-Consumer, mảng vòng tròn Circular Buffer O(1), con trỏ `head`/`tail` và đồng bộ `wait()`/`notifyAll()` được tự lập trình 100%, không sử dụng bất kỳ thư viện `java.util.concurrent` nào."*

---

### 5️⃣ MODE 4: Java 21 Virtual Threads Server (Project Loom - Đỉnh cao công nghệ)

* **Lệnh khởi động:**
  ```powershell
  .\run_server.bat 4
  ```
* **Nút bấm Demo trên Dashboard:**
  Click nút cam: **`🔥 Benchmark 500 Clients (2,000 Reqs)`** *(hoặc mở Terminal chạy `benchmark\run_benchmark.bat 4`)*
* **Hiện tượng quan sát trên Dashboard:**
  * **RPS (Throughput):** Đạt đỉnh cao nhất toàn hệ thống: **~1,145+ RPS** (gấp 8 lần Mode 3, gấp 110 lần Mode 1).
  * **Latency (Độ trễ):** Siêu thấp: **~70ms – 90ms** (P95 chỉ 95ms).
  * **Biểu đồ số 3 (KHOẢNH KHẮC ĐẮT GIÁ NHẤT):**
    * Đường Xanh (Conns) vọt lên đỉnh 500–1,000 kết nối.
    * **Đường Đỏ (OS Native Threads) NẰM NGANG PHẲNG LÌ Ở MỨC 15–16 CARRIER THREADS!**
  * **Log Console:** Định danh luồng ảo: `[✓] HTTP 200 trong 72ms [Thread: VirtualThread-1] [Loom Virtual]`.
* **💡 Điểm mấu chốt giải thích (Punchline):**
  > *"Mô hình **M:N Scheduling** của Java 21 Project Loom: Hàng nghìn Virtual Threads (chỉ vài trăm bytes ở User-space) được ánh xạ lên 16 Carrier Threads của OS. Khi gặp I/O Blocking (`delay`/truy vấn socket), Virtual Thread tự động unmount, nhường lõi CPU cho luồng khác xử lý."*

---

## 📊 3. BẢNG TRA CỨU NHANH KHI DEMO (CHEAT SHEET)

| Chế độ | Lệnh bật Server | Nút bấm thử nghiệm | Trạng thái OS Threads | Độ trễ (Avg Latency) | Throughput (RPS) |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **Mode 1** *(Iterative)* | `.\run_server.bat 1` | `⚡ Burst 30 Reqs` | Cố định 1 luồng chính | ~3,000 ms *(Rất chậm)* | ~9.8 RPS |
| **Mode 2** *(Thread/Conn)* | `.\run_server.bat 2` | `🚀 Spike 100 Reqs` | **Tăng vọt bám sát Conns** | ~140 ms *(Nhanh lúc đầu)* | ~285 RPS |
| **Mode 3** *(Worker Pool)* | `.\run_server.bat 3` | `🔥 Benchmark 500` | **Khóa cứng 16 luồng** | ~215 ms *(Ổn định)* | ~152 RPS |
| **Mode 5** *(Custom Pool)* | `.\run_server.bat 5` | `🔥 Benchmark 500` | **Khóa cứng 16 luồng** | ~224 ms *(Ổn định)* | ~148 RPS |
| **Mode 4** *(Virtual Threads)* | `.\run_server.bat 4` | `🔥 Benchmark 500` | **Phẳng lì 15-16 Carrier** | **~72 ms (Siêu tốc)** | **~1,145+ RPS** |

---

## 🎮 4. CÁC NÚT TEST KHÁC TRÊN DEMO CONTROL CENTER

Ngoài các nút bắn tải hàng loạt, bạn có thể click thử 3 nút đơn lẻ để giải thích hành vi xử lý I/O vs CPU:

* **🟢 `GET /api/hello (1 req - Pure I/O)`:** Phản hồi JSON tức thì (<1ms) kèm thông tin tên Thread và cờ `isVirtual`.
* **🟡 `GET /api/delay?ms=150 (1 req - I/O Bound)`:** Giả lập độ trễ I/O (Database / Network Call) bằng `Thread.sleep(150)`.
* **🔴 `GET /api/compute?n=32 (1 req - CPU Bound)`:** Giả lập tính toán nặng đệ quy Fibonacci(32), đẩy CPU lên cao để so sánh chi phí tính toán.
* **🧹 `Xóa Log`:** Làm sạch khung Console bên dưới để chuẩn bị cho lượt demo tiếp theo.

---

## 💡 5. MẸO CHUYỂN MODE NHANH & MƯỢT MÀ

1. **Tắt mode cũ:** Tại cửa sổ Server, bấm `Ctrl + C` (gõ `Y` nếu CMD hỏi xác nhận).
2. **Bật mode mới:** Bấm phím `↑` (mũi tên lên) và đổi số cuối: ví dụ `.\run_server.bat 4`.
3. **Không cần F5 trình duyệt:** Web Dashboard tự động kết nối lại (`auto-reconnect`) trong vòng 1-2 giây và cập nhật lại Huy hiệu Mode mới trên Header.
4. **Bấm nút `🧹 Xóa Log`** trên Dashboard trước mỗi lần test để người xem dễ nhìn thấy dòng log mới xuất hiện.

---

## 🔗 TÀI LIỆU LIÊN QUAN
* 📘 [Đặc tả Kiến trúc & Luồng hoạt động chi tiết 5 Mode](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/architecture-and-execution-flow.md)
* ⏱️ [Kịch bản thuyết trình Live Demo từng phút](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/demo-script.md)
* 🛡️ [Cẩm nang 7 câu hỏi phản biện chuyên sâu Q&A](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/qa-defense-guide.md)
* 📑 [Đề cương Slide báo cáo 18 Slides](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/docs/presentation-outline.md)
