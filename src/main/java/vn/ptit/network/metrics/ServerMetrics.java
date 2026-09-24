package vn.ptit.network.metrics;

import lombok.Getter;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * [LOCK-FREE METRICS ENGINE]:
 * Bộ thu thập số liệu hiệu năng máy chủ tốc độ cao.
 * 
 * Kỹ thuật cốt lõi bảo vệ hiệu năng (Core Concurrency Primitives):
 * 1. LongAdder & AtomicLong:
 *    - Tuyệt đối không dùng từ khóa 'synchronized' hay 'ReentrantLock' để tránh hiện tượng
 *      Lock Contention (thắt cổ chai) khi hàng nghìn luồng cùng ghi nhận số liệu.
 *    - LongAdder sử dụng kỹ thuật phân tán cell bộ nhớ (Striped64) trên các CPU Cache lines,
 *      triệt tiêu tối đa xung đột phần cứng.
 * 2. Cửa sổ trượt (Sliding Window):
 *    - Đo lường chính xác Throughput thời gian thực (Requests Per Second - RPS).
 *    - Lưu trữ lịch sử độ trễ gần nhất để tính toán phân vị Latency P50, P95.
 */
@Getter
public class ServerMetrics {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    // Lưu trữ mẫu độ trễ gần nhất (Circular buffer / Concurrent Queue)
    private static final int LATENCY_SAMPLE_SIZE = 1000;
    private final ConcurrentLinkedQueue<Long> recentLatencies = new ConcurrentLinkedQueue<>();

    // Đo RPS theo cửa sổ 1 giây
    private volatile long lastRpsCheckTime = System.currentTimeMillis();
    private volatile long lastRpsRequestCount = 0;
    private volatile double currentRps = 0.0;

    private final long serverStartTime = System.currentTimeMillis();

    /**
     * Ghi nhận khi một kết nối client mới bắt đầu được xử lý.
     */
    public void recordConnectionOpen() {
        activeConnections.incrementAndGet();
    }

    /**
     * Ghi nhận khi một request hoàn tất.
     * @param latencyMs thời gian xử lý tính bằng mili-giây
     * @param success kết quả thành công (HTTP 2xx, 3xx) hay thất bại (4xx, 5xx, Exception)
     */
    public void recordRequestCompleted(long latencyMs, boolean success) {
        activeConnections.decrementAndGet();
        totalRequests.increment();

        if (success) {
            successfulRequests.increment();
        } else {
            failedRequests.increment();
        }

        totalLatencyMs.add(latencyMs);

        // Cập nhật Min Latency bằng vòng lặp CAS Lock-free
        long currentMin;
        do {
            currentMin = minLatencyMs.get();
            if (latencyMs >= currentMin) break;
        } while (!minLatencyMs.compareAndSet(currentMin, latencyMs));

        // Cập nhật Max Latency bằng vòng lặp CAS Lock-free
        long currentMax;
        do {
            currentMax = maxLatencyMs.get();
            if (latencyMs <= currentMax) break;
        } while (!maxLatencyMs.compareAndSet(currentMax, latencyMs));

        // Thêm vào danh sách mẫu độ trễ gần nhất
        recentLatencies.offer(latencyMs);
        if (recentLatencies.size() > LATENCY_SAMPLE_SIZE) {
            recentLatencies.poll();
        }
    }

    /**
     * Tính toán RPS tức thời (Requests Per Second) dựa trên delta thời gian thực.
     */
    public synchronized double getRps() {
        long now = System.currentTimeMillis();
        long timeDelta = now - lastRpsCheckTime;

        if (timeDelta >= 500) { // Cập nhật mỗi 500ms
            long total = totalRequests.sum();
            long reqDelta = total - lastRpsRequestCount;

            currentRps = (reqDelta * 1000.0) / timeDelta;
            lastRpsCheckTime = now;
            lastRpsRequestCount = total;
        }
        return Math.round(currentRps * 10.0) / 10.0;
    }

    /**
     * Tính toán độ trễ trung bình (Average Latency) tính bằng ms.
     */
    public double getAvgLatencyMs() {
        long total = totalRequests.sum();
        if (total == 0) return 0.0;
        return Math.round(((double) totalLatencyMs.sum() / total) * 100.0) / 100.0;
    }

    public long getMinLatencyMs() {
        long min = minLatencyMs.get();
        return (min == Long.MAX_VALUE) ? 0 : min;
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs.get();
    }

    /**
     * Tính toán phân vị độ trễ P95 (95th percentile latency).
     */
    public double getP95LatencyMs() {
        Object[] samples = recentLatencies.toArray();
        if (samples.length == 0) return 0.0;

        java.util.Arrays.sort(samples);
        int index = (int) Math.ceil(0.95 * samples.length) - 1;
        if (index < 0) index = 0;
        if (index >= samples.length) index = samples.length - 1;

        return ((Long) samples[index]).doubleValue();
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - serverStartTime) / 1000;
    }

    /**
     * Xuất chuỗi JSON nhanh cho số liệu của máy chủ.
     */
    public String toJson() {
        return String.format(
                "{\"totalRequests\":%d,\"successfulRequests\":%d,\"failedRequests\":%d," +
                "\"activeConnections\":%d,\"rps\":%.1f,\"uptimeSeconds\":%d," +
                "\"latency\":{\"avgMs\":%.2f,\"minMs\":%d,\"maxMs\":%d,\"p95Ms\":%.2f}}",
                totalRequests.sum(), successfulRequests.sum(), failedRequests.sum(),
                activeConnections.get(), getRps(), getUptimeSeconds(),
                getAvgLatencyMs(), getMinLatencyMs(), getMaxLatencyMs(), getP95LatencyMs()
        );
    }
}
