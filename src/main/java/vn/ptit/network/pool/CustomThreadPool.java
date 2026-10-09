package vn.ptit.network.pool;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomThreadPool {

    @Getter
    private final int poolSize;
    @Getter
    private final int queueCapacity;
    private final CustomBlockingQueue<Runnable> taskQueue;
    private final List<CustomWorker> workers;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public CustomThreadPool(int poolSize, int queueCapacity) {
        if (poolSize <= 0) throw new IllegalArgumentException("poolSize phải > 0");
        if (queueCapacity <= 0) throw new IllegalArgumentException("queueCapacity phải > 0");

        this.poolSize = poolSize;
        this.queueCapacity = queueCapacity;
        this.taskQueue = new CustomBlockingQueue<>(queueCapacity);
        this.workers = new ArrayList<>(poolSize);

        // Khởi tạo trước N worker threads sống lâu (Pre-spawning Workers)
        for (int i = 1; i <= poolSize; i++) {
            CustomWorker worker = new CustomWorker("CustomWorker-" + i, taskQueue);
            workers.add(worker);
            worker.start();
        }
    }

    public void execute(Runnable task) {
        if (task == null) throw new NullPointerException("Task cannot be null");
        if (isShutdown.get()) {
            throw new RejectedExecutionException("CustomThreadPool đã shutdown, từ chối nhận task mới.");
        }

        // offer() trả về false nếu hàng đợi đã đầy mà không làm nghẽn Producer vô hạn
        boolean accepted = taskQueue.offer(task);
        if (!accepted) {
            throw new RejectedExecutionException("CustomThreadPool Bounded Queue đầy (" +
                    taskQueue.size() + "/" + queueCapacity + ")! Kích hoạt Rejection Policy.");
        }
    }

    public int getActiveCount() {
        int count = 0;
        for (CustomWorker worker : workers) {
            if (worker.isBusy()) {
                count++;
            }
        }
        return count;
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public boolean isShutdown() {
        return isShutdown.get();
    }

    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            for (CustomWorker worker : workers) {
                worker.stopWorker();
            }
        }
    }

    public void shutdownNow() {
        if (isShutdown.compareAndSet(false, true)) {
            taskQueue.clear();
            for (CustomWorker worker : workers) {
                worker.stopWorker();
            }
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for (CustomWorker worker : workers) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return false;
            worker.join(remaining);
            if (worker.isAlive()) return false;
        }
        return true;
    }
}
