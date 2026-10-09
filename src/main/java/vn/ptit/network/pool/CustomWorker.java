package vn.ptit.network.pool;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

public class CustomWorker extends Thread {

    private final CustomBlockingQueue<Runnable> taskQueue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    @Getter
    private volatile boolean busy = false;

    public CustomWorker(String name, CustomBlockingQueue<Runnable> taskQueue) {
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
