package vn.ptit.network.server;

import lombok.Getter;
import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;
import vn.ptit.network.http.HttpResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [MODE 3]: Worker Thread Pool Server (Chuẩn mực doanh nghiệp - Enterprise Standard).
 * 
 * Đặc điểm kiến trúc:
 * - Khởi tạo sẵn một số lượng Worker Threads cố định (Core Pool Size: 16 luồng).
 * - Sử dụng hàng đợi có giới hạn (Bounded ArrayBlockingQueue, sức chứa 1,000 tasks).
 * - Mô hình Producer - Consumer:
 *   + Acceptor Thread (Producer): Lắng nghe kết nối từ mạng và đẩy Socket vào Queue.
 *   + Worker Threads (Consumers): Nhận task từ Queue và xử lý độc lập.
 * - Cơ chế Backpressure & Rejection Policy (Chính sách từ chối):
 *   + Khi hàng đợi đầy (Queue Full): Server kích hoạt RejectedExecutionHandler để tự bảo vệ.
 *   + Phản hồi ngay mã HTTP 503 Service Unavailable kèm header 'Retry-After: 2'
 *     thay vì để tràn bộ nhớ (Out-Of-Memory) hay làm crash tiến trình.
 */
public class WorkerThreadPoolServer extends BaseHttpServer {

    @Getter
    private final ThreadPoolExecutor threadPool;
    private final AtomicInteger threadSequence = new AtomicInteger(1);

    public WorkerThreadPoolServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);

        // 1. Khởi tạo Bounded Blocking Queue
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(config.getQueueCapacity());

        // 2. Custom Thread Factory đặt tên luồng gợi nhớ
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "WorkerPool-" + threadSequence.getAndIncrement());
            t.setDaemon(false);
            return t;
        };

        // 3. Custom RejectedExecutionHandler: Trả về HTTP 503 thay vì vứt bỏ kết nối
        RejectedExecutionHandler rejectionHandler = (task, executor) -> {
            if (task instanceof SocketHandlerRunnable handlerTask) {
                sendServiceUnavailable(handlerTask.getSocket());
            }
        };

        // 4. Khởi tạo ThreadPoolExecutor chuẩn của Java Concurrency
        this.threadPool = new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                60L, TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectionHandler
        );
    }

    @Override
    public String getModeName() {
        return "Worker Thread Pool Server (Enterprise Standard)";
    }

    @Override
    public int getModeNumber() {
        return 3;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        try {
            threadPool.execute(new SocketHandlerRunnable(socket));
        } catch (RejectedExecutionException e) {
            sendServiceUnavailable(socket);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        System.out.println("[*] Đang giải phóng Worker Thread Pool...");
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[✓] Worker Thread Pool đã dừng hoàn tất.");
    }

    /**
     * Gửi phản hồi HTTP 503 khi server bị quá tải (Queue đầy).
     */
    private void sendServiceUnavailable(Socket socket) {
        try {
            HttpResponse response = HttpResponse.serviceUnavailable(
                    "Server is busy. Worker queue is full (" + config.getQueueCapacity() + " tasks). Please retry later."
            );
            response.writeTo(socket.getOutputStream());
        } catch (IOException ignored) {
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Tác vụ thực thi kết nối socket trên luồng Worker.
     */
    private class SocketHandlerRunnable implements Runnable {
        private final Socket socket;

        public SocketHandlerRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            processConnection(socket);
        }

        public Socket getSocket() {
            return socket;
        }
    }
}
