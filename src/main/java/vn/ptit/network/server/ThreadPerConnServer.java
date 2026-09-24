package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [MODE 2]: Thread-per-Connection Server (Mỗi kết nối tạo một OS Platform Thread).
 * 
 * Đặc điểm kiến trúc:
 * - Khi Acceptor Thread nhận được một Socket từ client, nó lập tức tạo một luồng hệ điều hành mới
 *   (OS Native Thread) để ủy thác toàn bộ việc đọc/xử lý/ghi cho luồng đó:
 *       new Thread(() -> processConnection(socket)).start();
 * - Luồng chính quay trở lại gọi accept() ngay lập tức mà không bị block.
 * 
 * Điểm mạnh:
 * - Đơn giản, giải quyết triệt để vấn đề nghẽn tuần tự của Mode 1.
 * 
 * Tử huyệt kỹ thuật (Technical Pitfalls) dùng để phản biện:
 * 1. Chi phí bộ nhớ (Memory Overhead): Mỗi OS Thread trong JVM ngốn mặc định 1MB Stack (-Xss1m).
 *    Với 2,000 kết nối đồng thời -> mất 2GB RAM chỉ để cấp phát Thread Stack!
 * 2. Chi phí chuyển ngữ cảnh (Context Switching Overhead): Khi số luồng vượt xa số CPU Cores,
 *    CPU tiêu tốn phần lớn chu kỳ tính toán chỉ để lưu/phục hồi thanh ghi, TCB và xả CPU Cache.
 * 3. Nguy cơ sập: Khi tải tăng đột biến, hệ thống ném ngoại lệ:
 *    java.lang.OutOfMemoryError: unable to create new native thread -> Crash tiến trình.
 */
public class ThreadPerConnServer extends BaseHttpServer {

    private final AtomicLong threadCounter = new AtomicLong(0);

    public ThreadPerConnServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
    }

    @Override
    public String getModeName() {
        return "Thread-per-Connection Server (Naive Multi-threading)";
    }

    @Override
    public int getModeNumber() {
        return 2;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        long id = threadCounter.incrementAndGet();
        // Khởi tạo một OS Platform Thread cho mỗi kết nối client vào
        Thread clientThread = new Thread(() -> processConnection(socket), "ThreadPerConn-" + id);
        
        // Thiết lập làm Daemon Thread hoặc User Thread (mặc định User thread để đảm bảo xử lý xong request)
        clientThread.start();
    }
}
