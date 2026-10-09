package vn.ptit.network.server;

import lombok.Getter;
import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;
import vn.ptit.network.http.HttpRequest;
import vn.ptit.network.http.HttpResponse;
import vn.ptit.network.metrics.ServerMetrics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public abstract class BaseHttpServer {
    @Getter
    protected final ServerConfig config;
    @Getter
    protected final HttpHandler handler;
    @Getter
    protected final ServerMetrics metrics;
    protected volatile boolean running = false;
    protected ServerSocket serverSocket;

    public BaseHttpServer(ServerConfig config, HttpHandler handler) {
        this.config = config != null ? config : new ServerConfig();
        this.handler = handler != null ? handler : new HttpHandler();
        this.metrics = new ServerMetrics();
        this.handler.setServer(this);
    }

    public abstract String getModeName();

    public abstract int getModeNumber();

    protected abstract void dispatchClient(Socket socket);

    public int getQueueSize() {
        return 0;
    }

    public int getActiveWorkers() {
        return 0;
    }

    public int getQueueCapacity() {
        return config != null ? config.getQueueCapacity() : 0;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getPort(), config.getBacklog());
        running = true;

        System.out.println("=================================================================");
        System.out.println(" [+] [T45 SERVER STARTED]");
        System.out.println("  * Mô hình luồng:    Mode " + getModeNumber() + " - " + getModeName());
        System.out.println("  * Địa chỉ lắng nghe: http://localhost:" + config.getPort());
        System.out.println("  * Web Dashboard:     http://localhost:" + config.getPort() + "/dashboard");
        System.out.println("  * Metrics API:        http://localhost:" + config.getPort() + "/api/metrics");
        System.out.println("  * TCP Backlog: " + config.getBacklog() + " | Core/Max Pool: " +
                config.getCorePoolSize() + "/" + config.getMaxPoolSize());
        System.out.println("=================================================================");

        try {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    dispatchClient(clientSocket);
                } catch (SocketException e) {
                    if (!running) {
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
        System.out.println("[OK] Máy chủ đã dừng an toàn.");
    }

    protected void processConnection(Socket socket) {
        metrics.recordConnectionOpen();
        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            socket.setSoTimeout(config.getSocketTimeoutMs());
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                HttpRequest request = HttpRequest.parse(in);
                HttpResponse response = handler.handle(request);
                response.writeTo(out);

                success = (response.getStatusCode() < 400);

            } catch (SocketTimeoutException e) {
                // Client timeout
            } catch (IOException e) {
                // Client disconnect
            }
        } catch (Exception e) {
            System.err.println("[Process Error] " + e.getMessage());
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            metrics.recordRequestCompleted(latency, success);

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
}
