package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * [MODE 4]: Modern High-Concurrency — Java 21 Virtual Threads (Project Loom).
 * 
 * Đỉnh cao công nghệ luồng hiện đại của Java 21:
 * - Thay vì tạo OS Platform Thread nặng nề (chiếm 1MB Stack bộ nhớ OS), Java 21 đưa vào
 *   Virtual Threads (Luồng ảo) hoạt động ở không gian người dùng (User-space Thread) do JVM quản lý.
 * - Mô hình ánh xạ M:N: Hàng trăm nghìn Virtual Threads được ánh xạ linh hoạt lên một số ít
 *   Carrier Threads (OS Threads, thường bằng số nhân CPU).
 * - Cơ chế Mount / Unmount kỳ diệu:
 *   Khi một Virtual Thread thực hiện thao tác I/O mạng bị chặn (như đọc Socket InputStream hoặc Thread.sleep()),
 *   JVM tự động unmount (tháo gỡ) Virtual Thread đó khỏi Carrier Thread và gán Virtual Thread khác vào chạy tiếp.
 *   Khi Socket có tín hiệu dữ liệu sẵn sàng, Virtual Thread được mount (gắn) trở lại để tiếp tục.
 * - Hiệu quả thực tế:
 *   + Cho phép xử lý 10,000+ đến 100,000+ kết nối đồng thời trên một chiếc laptop thông thường.
 *   + Bộ nhớ tiêu thụ chỉ vài KB mỗi luồng (thay vì 1MB như Platform Thread).
 *   + Lập trình phong cách tuần tự (Blocking I/O) dễ đọc, dễ debug nhưng hiệu năng tiệm cận Non-blocking / Reactive.
 */
public class VirtualThreadServer extends BaseHttpServer {

    private final ExecutorService virtualExecutor;

    public VirtualThreadServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
        // Khởi tạo Executor cấp phát Virtual Thread cho mỗi tác vụ kết nối
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public String getModeName() {
        return "Java 21 Virtual Threads (Project Loom)";
    }

    @Override
    public int getModeNumber() {
        return 4;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        // Mỗi kết nối socket được ủy thác cho một Virtual Thread siêu nhẹ
        virtualExecutor.submit(() -> processConnection(socket));
    }

    @Override
    public synchronized void stop() {
        super.stop();
        System.out.println("[*] Đang đóng Virtual Thread Executor...");
        virtualExecutor.shutdown();
        try {
            if (!virtualExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                virtualExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[✓] Virtual Thread Executor đã dừng hoàn tất.");
    }
}
