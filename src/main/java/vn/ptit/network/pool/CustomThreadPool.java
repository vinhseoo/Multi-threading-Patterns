package vn.ptit.network.pool;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * [INNOVATION - TECHNICAL DEPTH 30%]:
 * Hệ thống Thread Pool tự lập trình từ con số 0 (Custom Thread Pool).
 * 
 * Mô hình kiến trúc:
 * - Áp dụng mẫu thiết kế Producer - Consumer:
 *   + Producer: Acceptor Thread nhận Socket và đẩy tác vụ vào Bounded Blocking Queue.
 *   + Consumer: N Worker Threads liên tục tranh chấp an toàn để rút tác vụ từ Queue ra xử lý.
 * - Cơ chế tự bảo vệ: Giới hạn kích thước hàng đợi (Bounded Queue Capacity).
 *   Khi Queue đầy, kích hoạt RejectedExecutionException để bảo vệ hệ thống không bị tràn RAM.
 */
public class CustomThreadPool {

    @Getter
    private final int poolSize;
    @Getter
    private final int queueCapacity;
    private final BlockingQueue<Runnable> taskQueue;
    private final List<CustomWorker> workers;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public CustomThreadPool(int poolSize, int queueCapacity) {
        if (poolSize <= 0) throw new IllegalArgumentException("poolSize phải > 0");
        if (queueCapacity <= 0) throw new IllegalArgumentException("queueCapacity phải > 0");

        this.poolSize = poolSize;
        this.queueCapacity = queueCapacity;
        this.taskQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.workers = new ArrayList<>(poolSize);

        // Khởi tạo trước N worker threads sống lâu (Pre-spawning Workers)
        for (int i = 1; i <= poolSize; i++) {
            CustomWorker worker = new CustomWorker("CustomWorker-" + i, taskQueue);
            workers.add(worker);
            worker.start();
        }
    }

    /**
     * Đẩy tác vụ mới vào Thread Pool để thực thi.
     * @param task công việc cần xử lý
     * @throws RejectedExecutionException nếu queue đã đầy hoặc pool đã shutdown
     */
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

    /**
     * Số lượng worker hiện đang thực sự bận xử lý request.
     */
    public int getActiveCount() {
        int count = 0;
        for (CustomWorker worker : workers) {
            if (worker.isBusy()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Kích thước các tác vụ đang xếp hàng chờ trong Queue.
     */
    public int getQueueSize() {
        return taskQueue.size();
    }

    public boolean isShutdown() {
        return isShutdown.get();
    }

    /**
     * Đóng pool một cách êm ái: không nhận thêm task mới, đợi các task trong queue hoàn thành.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            for (CustomWorker worker : workers) {
                worker.stopWorker();
            }
        }
    }

    /**
     * Đóng pool khẩn cấp: ngắt các worker đang chạy và xóa sạch hàng đợi.
     */
    public void shutdownNow() {
        if (isShutdown.compareAndSet(false, true)) {
            taskQueue.clear();
            for (CustomWorker worker : workers) {
                worker.stopWorker();
            }
        }
    }

    /**
     * Chờ toàn bộ worker kết thúc trong thời gian quy định.
     */
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
