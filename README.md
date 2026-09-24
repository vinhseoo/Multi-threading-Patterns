# ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING
**Môn học:** Lập Trình Mạng (PTIT)  
**Giảng viên:** TS. Đặng Ngọc Hùng (hungdn@ptit.edu.vn)  
**Sinh viên thực hiện:** 1 thành viên (Solo Project)  
**Ngôn ngữ:** Java 21 LTS (`java.net`, `java.util.concurrent`, Virtual Threads Loom)

---

## 📌 Tổng Quan Dự Án
Dự án nghiên cứu, cài đặt và đánh giá thực nghiệm **4 mô hình luồng (Multi-threading Patterns)** trong lập trình ứng dụng mạng:
1. **Mode 1: Iterative Single-Threaded Server** (Xử lý tuần tự - Baseline)
2. **Mode 2: Thread-per-Connection Server** (Mỗi kết nối 1 luồng OS - Naive Multi-threading)
3. **Mode 3: Worker Thread Pool Server** (Hàng đợi Bounded Blocking Queue + N Workers - Enterprise Standard)
4. **Mode 4: Virtual Thread Server** (Java 21 Project Loom - Modern High-Concurrency)

Kèm theo:
- **Real-time Web Dashboard** (`/dashboard`): Giám sát trực quan trạng thái luồng, CPU, RAM, RPS và Latency.
- **Load Testing Tool** (`benchmark/load_tester.py`): Phát sinh tải đồng thời để kiểm chứng hiệu năng và phơi bày các điểm nghẽn.
- **Kế hoạch chi tiết & Tài liệu thuyết trình 15-20 phút** trong file [KE_HOACH_DE_TAI_T45.md](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/KE_HOACH_DE_TAI_T45.md).
- **Nhật ký theo dõi tiến độ & Checklist từng Phase** trong file [PROJECT_PHASES_LOG.md](file:///c:/Users/maiduc.vinh/OneDrive%20-%20VietCredit/Desktop/NetworkProgramming/PROJECT_PHASES_LOG.md).

---

## 🚀 Môi Trường & Khởi Chạy
- **Java yêu cầu:** Java 21 LTS (Đã có sẵn tại `C:\Users\maiduc.vinh\.jdks\ms-21.0.10`).
- **Thư viện bên ngoài:** 0 (Dùng 100% Java Standard Library `java.net`, `java.util.concurrent`, `java.lang.management` - không lo lỗi dependency).

