package vn.ptit.network.pool;

import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Luồng thợ (Worker Thread) tự cài đặt từ con số 0.
 * Liên tục chạy vòng lặp lấy công việc (Runnable) từ hàng đợi dùng chung và thực thi.
 * 
 * Minh họa nguyên lý luồng sống lâu (Long-lived Thread) giúp tránh chi phí khởi tạo
 * và hủy luồng liên tục của hệ điều hành.
 */
public class CustomWorker extends Thread {

    private final BlockingQueue<Runnable> taskQueue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    @Getter
    private volatile boolean busy = false;

    public CustomWorker(String name, BlockingQueue<Runnable> taskQueue) {
        super(name);
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        while (running.get() || !taskQueue.isEmpty()) {
            try {
                // Chờ và lấy task từ hàng đợi (chặn nếu hàng đợi rỗng - Blocking)
                Runnable task = taskQueue.take();
                busy = true;
                try {
                    task.run();
                } catch (Throwable t) {
                    System.err.println("[" + getName() + "] Lỗi thực thi task: " + t.getMessage());
                } finally {
                    busy = false;
                }
            } catch (InterruptedException e) {
                if (!running.get()) {
                    break; // Ngắt để dừng worker khi shutdown
                }
            }
        }
    }

    public void stopWorker() {
        running.set(false);
        this.interrupt();
    }
}
