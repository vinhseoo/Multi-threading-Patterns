package vn.ptit.network.metrics;

import lombok.Getter;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;


@Getter
public class ServerMetrics {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    private static final int LATENCY_SAMPLE_SIZE = 1000;
    private final ConcurrentLinkedQueue<Long> recentLatencies = new ConcurrentLinkedQueue<>();

    private volatile long lastRpsCheckTime = System.currentTimeMillis();
    private volatile long lastRpsRequestCount = 0;
    private volatile double currentRps = 0.0;

    private final long serverStartTime = System.currentTimeMillis();

    public void recordConnectionOpen() {
        activeConnections.incrementAndGet();
    }

    public void recordRequestCompleted(long latencyMs, boolean success) {
        activeConnections.decrementAndGet();
        totalRequests.increment();

        if (success) {
            successfulRequests.increment();
        } else {
            failedRequests.increment();
        }

        totalLatencyMs.add(latencyMs);

        long currentMin;
        do {
            currentMin = minLatencyMs.get();
            if (latencyMs >= currentMin) break;
        } while (!minLatencyMs.compareAndSet(currentMin, latencyMs));

        long currentMax;
        do {
            currentMax = maxLatencyMs.get();
            if (latencyMs <= currentMax) break;
        } while (!maxLatencyMs.compareAndSet(currentMax, latencyMs));

        recentLatencies.offer(latencyMs);
        if (recentLatencies.size() > LATENCY_SAMPLE_SIZE) {
            recentLatencies.poll();
        }
    }

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
