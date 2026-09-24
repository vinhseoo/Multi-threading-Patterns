package vn.ptit.network.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * [SYSTEM & HARDWARE METRICS COLLECTOR]:
 * Thu thập các chỉ số phần cứng và tài nguyên JVM trực tiếp qua JMX (Java Management Extensions).
 * 
 * Các chỉ số đo đạc then chốt:
 * - CPU Load: % CPU của toàn hệ thống và % CPU của riêng tiến trình JVM Java.
 * - RAM Memory: Dung lượng bộ nhớ Heap (đang dùng / tối đa) và Non-Heap (Metaspace / Code cache).
 * - OS Threads Count: Số lượng luồng hệ điều hành thực tế (Live Threads & Peak Threads).
 *   Đây là thước đo then chốt chứng minh sự khác biệt:
 *   + Mode 2 (Thread-per-conn): Live Threads tăng vọt bằng số kết nối đồng thời.
 *   + Mode 3 (Thread pool): Live Threads phẳng lì ở mức 16-32 luồng.
 *   + Mode 4 (Virtual threads): Live Threads vẫn giữ nguyên ~20 luồng dù có 10,000 clients!
 */
public class SystemMetrics {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static com.sun.management.OperatingSystemMXBean osBean;

    static {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                osBean = sunBean;
            }
        } catch (Throwable ignored) {}
    }

    /**
     * % CPU sử dụng bởi tiến trình Java hiện tại (0.0 - 100.0).
     */
    public static double getProcessCpuLoad() {
        if (osBean != null) {
            double load = osBean.getProcessCpuLoad();
            if (load >= 0) {
                return Math.round(load * 1000.0) / 10.0;
            }
        }
        return 0.0;
    }

    /**
     * % CPU sử dụng của toàn bộ máy chủ (0.0 - 100.0).
     */
    public static double getSystemCpuLoad() {
        if (osBean != null) {
            double load = osBean.getCpuLoad();
            if (load >= 0) {
                return Math.round(load * 1000.0) / 10.0;
            }
        }
        return 0.0;
    }

    /**
     * Dung lượng RAM Heap JVM đang sử dụng (MB).
     */
    public static double getHeapUsedMb() {
        long bytes = memoryBean.getHeapMemoryUsage().getUsed();
        return Math.round((bytes / (1024.0 * 1024.0)) * 10.0) / 10.0;
    }

    /**
     * Dung lượng RAM Heap tối đa được cấp phát (MB).
     */
    public static double getHeapMaxMb() {
        long bytes = memoryBean.getHeapMemoryUsage().getMax();
        return Math.round((bytes / (1024.0 * 1024.0)) * 10.0) / 10.0;
    }

    /**
     * Số lượng OS Platform Threads đang còn sống trong JVM.
     */
    public static int getLiveThreadCount() {
        return threadBean.getThreadCount();
    }

    /**
     * Số lượng luồng đỉnh cao nhất từng đạt tới kể từ khi khởi động server.
     */
    public static int getPeakThreadCount() {
        return threadBean.getPeakThreadCount();
    }

    /**
     * Tổng số luồng hệ điều hành đã được khởi tạo.
     */
    public static long getTotalStartedThreadCount() {
        return threadBean.getTotalStartedThreadCount();
    }

    /**
     * Số nhân CPU vật lý/logic sẵn có của máy tính.
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Xuất chuỗi JSON nhanh cho số liệu hệ thống.
     */
    public static String toJson() {
        return String.format(
                "{\"cpuProcessPercent\":%.1f,\"cpuSystemPercent\":%.1f," +
                "\"heapUsedMb\":%.1f,\"heapMaxMb\":%.1f," +
                "\"liveThreadCount\":%d,\"peakThreadCount\":%d," +
                "\"totalStartedThreadCount\":%d,\"availableProcessors\":%d}",
                getProcessCpuLoad(), getSystemCpuLoad(),
                getHeapUsedMb(), getHeapMaxMb(),
                getLiveThreadCount(), getPeakThreadCount(),
                getTotalStartedThreadCount(), getAvailableProcessors()
        );
    }
}
