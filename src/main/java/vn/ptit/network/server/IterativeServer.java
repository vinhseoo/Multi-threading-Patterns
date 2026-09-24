package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;

import java.net.Socket;

/**
 * [MODE 1]: Iterative Single-Threaded Server (Mô hình xử lý tuần tự cơ bản).
 * 
 * Đặc điểm kiến trúc:
 * - Chỉ có DUY NHẤT một luồng chính (Main Thread) thực hiện cả việc accept() lẫn xử lý I/O / tính toán.
 * - Luồng chính trực tiếp gọi `processConnection(socket)` ngay trong vòng lặp tiếp nhận.
 * - Hạn chế nghiêm trọng: Khi server đang bận xử lý một client (ví dụ đang bị block ở I/O hoặc tính toán lâu),
 *   mọi kết nối từ các client khác đều bị chặn và phải chờ đợi trong hàng đợi TCP Backlog của Hệ điều hành.
 * - Vai trò trong đề tài: Mốc đối chứng chuẩn (Baseline) để chứng minh sự cần thiết của lập trình đa luồng.
 */
public class IterativeServer extends BaseHttpServer {

    public IterativeServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
    }

    @Override
    public String getModeName() {
        return "Iterative Single-Threaded Server (Baseline)";
    }

    @Override
    public int getModeNumber() {
        return 1;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        // Xử lý trực tiếp trên luồng Acceptor hiện tại -> Gây block hoàn toàn cho client tiếp theo!
        processConnection(socket);
    }
}
