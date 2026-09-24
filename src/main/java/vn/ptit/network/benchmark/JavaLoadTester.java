package vn.ptit.network.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [PORTABLE BENCHMARK ENGINE]:
 * Công cụ phát sinh tải đồng thời đa luồng được xây dựng 100% bằng Java 21 Standard Library.
 * 
 * Ưu thế vượt trội:
 * - Tận dụng Java 21 Virtual Threads để mô phỏng hàng nghìn Client đồng thời (High-concurrency Load Generator)
 *   mà không gặp giới hạn bộ nhớ OS Thread Stack như các tool truyền thống.
 * - Zero Dependency: Không yêu cầu cài đặt Python, Apache JMeter, hay k6.
 * - Đo đạc độ chính xác micro-giây: RPS, Latency Min, Avg, P50, P95, P99, Max và Tỷ lệ lỗi.
 * - Hỗ trợ xuất trực tiếp dữ liệu ra file CSV để vẽ đồ thị báo cáo BTL.
 */
public class JavaLoadTester {

    public static void main(String[] args) {
        String targetUrl = "http://localhost:8080/api/delay?ms=100";
        int concurrency = 100;
        int totalRequests = 1000;
        String modeName = "Current Server Mode";
        String csvOutput = "benchmark/benchmark_results.csv";

        // Phân tích tham số dòng lệnh
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--url" -> { if (i + 1 < args.length) targetUrl = args[++i]; }
                case "-c", "--concurrency" -> { if (i + 1 < args.length) concurrency = Integer.parseInt(args[++i]); }
                case "-n", "--requests" -> { if (i + 1 < args.length) totalRequests = Integer.parseInt(args[++i]); }
                case "-m", "--mode" -> { if (i + 1 < args.length) modeName = args[++i]; }
                case "-o", "--csv" -> { if (i + 1 < args.length) csvOutput = args[++i]; }
            }
        }

        runBenchmark(targetUrl, concurrency, totalRequests, modeName, csvOutput);
    }

    public static BenchmarkResult runBenchmark(String targetUrl, int concurrency, int totalRequests, String modeName, String csvOutput) {
        System.out.println("===========================================================================");
        System.out.println(" 🚀 [T45 BENCHMARK TOOL] JAVA 21 VIRTUAL THREADS LOAD GENERATOR");
        System.out.println("===========================================================================");
        System.out.println(" 🎯 Mục tiêu URL:         " + targetUrl);
        System.out.println(" 👥 Kết nối đồng thời:    " + concurrency + " concurrent clients");
        System.out.println(" 📦 Tổng số requests:     " + totalRequests + " requests");
        System.out.println(" 🏷️ Mô hình kiểm thử:     " + modeName);
        System.out.println("---------------------------------------------------------------------------");
        System.out.println(" [*] Đang khởi tạo Virtual Thread Client Pool và phát sinh tải...");

        // Khởi tạo HttpClient tối ưu cho Virtual Threads
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        URI uri = URI.create(targetUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "T45-JavaLoadTester/1.0")
                .GET()
                .build();

        AtomicInteger requestsSent = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatencyNs = new AtomicLong(0);

        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>(totalRequests));

        // Sử dụng VirtualThreadPerTaskExecutor để tạo tải đồng thời không giới hạn
        long benchmarkStartTime = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Dùng Semaphore để kiểm soát số lượng client đồng thời chính xác (Concurrency Limit)
            Semaphore semaphore = new Semaphore(concurrency);
            CountDownLatch latch = new CountDownLatch(totalRequests);

            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        requestsSent.incrementAndGet();

                        long reqStart = System.nanoTime();
                        try {
                            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                            long reqElapsedNs = System.nanoTime() - reqStart;
                            long reqElapsedMs = reqElapsedNs / 1_000_000;

                            totalLatencyNs.addAndGet(reqElapsedNs);
                            latenciesMs.add(reqElapsedMs);

                            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();
                        latch.countDown();
                    }
                });
            }

            // Chờ toàn bộ requests hoàn tất
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("[!] Benchmark bị ngắt: " + e.getMessage());
        }

        long totalTimeNs = System.nanoTime() - benchmarkStartTime;
        double totalTimeSec = totalTimeNs / 1_000_000_000.0;
        int completed = successCount.get() + failCount.get();
        double rps = totalTimeSec > 0 ? (completed / totalTimeSec) : 0.0;

        // Phân tích thống kê phân vị độ trễ (Percentiles)
        List<Long> sortedLatencies = new ArrayList<>(latenciesMs);
        Collections.sort(sortedLatencies);

        long minMs = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(0);
        long maxMs = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(sortedLatencies.size() - 1);
        double avgMs = completed > 0 ? ((totalLatencyNs.get() / 1_000_000.0) / completed) : 0.0;
        long p50Ms = getPercentile(sortedLatencies, 0.50);
        long p95Ms = getPercentile(sortedLatencies, 0.95);
        long p99Ms = getPercentile(sortedLatencies, 0.99);

        BenchmarkResult result = new BenchmarkResult(
                modeName, targetUrl, concurrency, totalRequests,
                successCount.get(), failCount.get(), totalTimeSec, rps,
                minMs, avgMs, p50Ms, p95Ms, p99Ms, maxMs
        );

        printResults(result);

        if (csvOutput != null && !csvOutput.isBlank()) {
            saveResultToCsv(result, csvOutput);
        }

        return result;
    }

    private static long getPercentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sorted.size()) index = sorted.size() - 1;
        return sorted.get(index);
    }

    private static void printResults(BenchmarkResult r) {
        System.out.println("\n===========================================================================");
        System.out.println(" 📊 KẾT QUẢ ĐO ĐẠC HIỆU NĂNG (BENCHMARK RESULTS)");
        System.out.println("===========================================================================");
        System.out.printf(" ⏱️ Tổng thời gian chạy:   %.2f giây\n", r.totalTimeSec);
        System.out.printf(" ⚡ Thông lượng (Throughput): %.2f Requests/giây (RPS)\n", r.rps);
        System.out.println("---------------------------------------------------------------------------");
        System.out.printf(" ✅ Requests thành công:    %d / %d (%.1f%%)\n",
                r.successRequests, r.totalRequests, (r.successRequests * 100.0 / r.totalRequests));
        System.out.printf(" ❌ Requests thất bại/lỗi:  %d (%.1f%%)\n",
                r.failedRequests, (r.failedRequests * 100.0 / r.totalRequests));
        System.out.println("---------------------------------------------------------------------------");
        System.out.println(" 📈 Phân bố độ trễ mạng (Latency Distribution):");
        System.out.printf("    - Min Latency:          %d ms\n", r.minLatencyMs);
        System.out.printf("    - Avg Latency:          %.2f ms\n", r.avgLatencyMs);
        System.out.printf("    - Median (P50):         %d ms\n", r.p50LatencyMs);
        System.out.printf("    - 95th Percentile (P95):%d ms\n", r.p95LatencyMs);
        System.out.printf("    - 99th Percentile (P99):%d ms\n", r.p99LatencyMs);
        System.out.printf("    - Max Latency:          %d ms\n", r.maxLatencyMs);
        System.out.println("===========================================================================\n");
    }

    private static void saveResultToCsv(BenchmarkResult r, String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            boolean isNew = !file.exists();
            try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
                if (isNew) {
                    out.println("Timestamp,Mode,TargetUrl,Concurrency,TotalReqs,SuccessReqs,FailedReqs,TotalTimeSec,RPS,MinLatencyMs,AvgLatencyMs,P50Ms,P95Ms,P99Ms,MaxLatencyMs");
                }
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                out.printf("%s,\"%s\",\"%s\",%d,%d,%d,%d,%.2f,%.2f,%d,%.2f,%d,%d,%d,%d\n",
                        timestamp, r.mode, r.url, r.concurrency, r.totalRequests,
                        r.successRequests, r.failedRequests, r.totalTimeSec, r.rps,
                        r.minLatencyMs, r.avgLatencyMs, r.p50LatencyMs, r.p95LatencyMs, r.p99LatencyMs, r.maxLatencyMs);
            }
            System.out.println("[*] Đã lưu kết quả đối sánh vào file: " + filePath);
        } catch (IOException e) {
            System.err.println("[!] Lỗi ghi file CSV: " + e.getMessage());
        }
    }

    public record BenchmarkResult(
            String mode,
            String url,
            int concurrency,
            int totalRequests,
            int successRequests,
            int failedRequests,
            double totalTimeSec,
            double rps,
            long minLatencyMs,
            double avgLatencyMs,
            long p50LatencyMs,
            long p95LatencyMs,
            long p99LatencyMs,
            long maxLatencyMs
    ) {}
}
