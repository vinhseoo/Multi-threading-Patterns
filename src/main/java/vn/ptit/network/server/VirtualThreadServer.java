package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadServer extends BaseHttpServer {

    private final ExecutorService virtualExecutor;

    public VirtualThreadServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
        // Khởi tạo Executor cấp phát Virtual Thread với tên định danh rõ ràng
        java.util.concurrent.ThreadFactory factory = Thread.ofVirtual().name("VirtualThread-", 1).factory();
        this.virtualExecutor = Executors.newThreadPerTaskExecutor(factory);
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
        System.out.println("[OK] Virtual Thread Executor đã dừng hoàn tất.");
    }
}
