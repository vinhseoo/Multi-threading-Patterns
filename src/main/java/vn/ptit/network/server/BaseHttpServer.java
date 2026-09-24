package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;
import vn.ptit.network.http.HttpRequest;
import vn.ptit.network.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Lớp cơ sở trừu tượng quản lý ServerSocket và vòng đời dịch vụ mạng.
 * Cung cấp khung chuẩn hóa để các mô hình luồng (Mode 1, 2, 3, 4) kế thừa và hiện thực hóa
 * chiến lược điều phối luồng (dispatching strategy) riêng biệt.
 */
public abstract class BaseHttpServer {
    protected final ServerConfig config;
    protected final HttpHandler handler;
    protected volatile boolean running = false;
    protected ServerSocket serverSocket;

    public BaseHttpServer(ServerConfig config, HttpHandler handler) {
        this.config = config != null ? config : new ServerConfig();
        this.handler = handler != null ? handler : new HttpHandler();
    }

    /**
     * Tên mô hình luồng để hiển thị trên console và dashboard.
     */
    public abstract String getModeName();

    /**
     * Số thứ tự mô hình (1, 2, 3, 4).
     */
    public abstract int getModeNumber();

    /**
     * Chiến lược điều phối xử lý kết nối client của từng mô hình luồng.
     * @param socket kết nối vừa được accept() từ mạng
     */
    protected abstract void dispatchClient(Socket socket);

    /**
     * Khởi động ServerSocket và bắt đầu vòng lặp tiếp nhận kết nối (Accept Loop).
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getPort(), config.getBacklog());
        running = true;

        System.out.println("=================================================================");
        System.out.println(" 🌐 [T45 SERVER STARTED]");
        System.out.println(" 📌 Mô hình luồng: Mode " + getModeNumber() + " - " + getModeName());
        System.out.println(" 📍 Địa chỉ lắng nghe: http://localhost:" + config.getPort());
        System.out.println(" ⚙️ TCP Backlog: " + config.getBacklog() + " | Core/Max Pool: " +
                config.getCorePoolSize() + "/" + config.getMaxPoolSize());
        System.out.println("=================================================================");

        // Vòng lặp Accept Loop chạy trên luồng gọi start()
        try {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    dispatchClient(clientSocket);
                } catch (SocketException e) {
                    if (!running) {
                        // Server đang chủ động shutdown
                        break;
                    }
                    System.err.println("[SocketException in Accept Loop] " + e.getMessage());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[Accept Error] " + e.getMessage());
                    }
                }
            }
        } finally {
            stop();
        }
    }

    /**
     * Dừng máy chủ và giải phóng socket mạng.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        System.out.println("\n[*] Đang dừng máy chủ [Mode " + getModeNumber() + " - " + getModeName() + "]...");

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("[Error closing server socket] " + e.getMessage());
            }
        }
        System.out.println("[✓] Máy chủ đã dừng an toàn.");
    }

    /**
     * Xử lý đọc request từ socket, gọi handler và ghi response về client.
     * Hàm dùng chung cho các luồng xử lý của mọi mô hình.
     */
    protected void processConnection(Socket socket) {
        try {
            socket.setSoTimeout(config.getSocketTimeoutMs());
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                HttpRequest request = HttpRequest.parse(in);
                HttpResponse response = handler.handle(request);
                response.writeTo(out);

            } catch (SocketTimeoutException e) {
                // Client mở kết nối nhưng không gửi dữ liệu quá thời hạn timeout
            } catch (IOException e) {
                // Lỗi mạng hoặc client ngắt kết nối đột ngột
            }
        } catch (Exception e) {
            System.err.println("[Process Error] " + e.getMessage());
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    public boolean isRunning() {
        return running;
    }

    public ServerConfig getConfig() {
        return config;
    }
}
