package vn.ptit.network.server;

import lombok.Getter;
import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;
import vn.ptit.network.http.HttpResponse;
import vn.ptit.network.pool.CustomThreadPool;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class CustomThreadPoolServer extends BaseHttpServer {

    @Getter
    private final CustomThreadPool customThreadPool;

    public CustomThreadPoolServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
        this.customThreadPool = new CustomThreadPool(config.getCorePoolSize(), config.getQueueCapacity());
    }

    @Override
    public String getModeName() {
        return "Custom Thread Pool Server (Self-implemented Pool)";
    }

    @Override
    public int getModeNumber() {
        return 5; // Chế độ đặc biệt đối chứng với Mode 3 chuẩn
    }

    @Override
    public int getQueueSize() {
        return customThreadPool != null ? customThreadPool.getQueueSize() : 0;
    }

    @Override
    public int getActiveWorkers() {
        return customThreadPool != null ? customThreadPool.getActiveCount() : 0;
    }

    @Override
    public int getQueueCapacity() {
        return config != null ? config.getQueueCapacity() : 1000;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        try {
            customThreadPool.execute(() -> processConnection(socket));
        } catch (RejectedExecutionException e) {
            sendServiceUnavailable(socket);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        System.out.println("[*] Đang giải phóng Custom Thread Pool...");
        customThreadPool.shutdown();
        try {
            if (!customThreadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                customThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            customThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[OK] Custom Thread Pool đã dừng hoàn tất.");
    }

    private void sendServiceUnavailable(Socket socket) {
        try {
            HttpResponse response = HttpResponse.serviceUnavailable(
                    "Server is busy. Custom queue is full (" + config.getQueueCapacity() + " tasks). Please retry later."
            );
            response.writeTo(socket.getOutputStream());
        } catch (IOException ignored) {
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
