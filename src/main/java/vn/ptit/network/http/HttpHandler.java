package vn.ptit.network.http;

import lombok.Setter;
import vn.ptit.network.metrics.SystemMetrics;
import vn.ptit.network.server.BaseHttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class HttpHandler {

    @Setter
    private BaseHttpServer server;

    public HttpResponse handle(HttpRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return new HttpResponse(204, "No Content");
        }

        try {
            switch (path) {
                case "/":
                case "/index.html":
                case "/dashboard":
                    return handleDashboard();

                case "/slides":
                case "/slides.html":
                    return handleSlides();

                case "/api/hello":
                    return handleHello(request);

                case "/api/delay":
                    return handleDelay(request);

                case "/api/compute":
                    return handleCompute(request);

                case "/api/metrics":
                    return handleMetrics();

                case "/api/benchmark":
                    return handleBenchmark();

                case "/api/benchmark/trigger":
                    return handleTriggerBenchmark(request);

                case "/benchmark/benchmark_results.csv":
                    return tryServeBenchmarkCsv();

                default:
                    HttpResponse staticRes = tryServeStaticResource(path);
                    if (staticRes != null) {
                        return staticRes;
                    }
                    return HttpResponse.notFound("Endpoint '" + path + "' does not exist on T45 Server.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HttpResponse.internalServerError("Task interrupted");
        } catch (Exception e) {
            return HttpResponse.internalServerError("Internal error: " + e.getMessage());
        }
    }

    private String resolveThreadName() {
        String name = Thread.currentThread().getName();
        if (name == null || name.isBlank()) {
            return Thread.currentThread().toString();
        }
        return name;
    }

    private HttpResponse handleHello(HttpRequest request) {
        String threadName = resolveThreadName();
        boolean isVirtual = Thread.currentThread().isVirtual();
        long timestamp = System.currentTimeMillis();

        String json = String.format(
                "{\"status\":\"ok\",\"message\":\"Hello from T45 Server!\",\"thread\":\"%s\",\"isVirtual\":%b,\"timestamp\":%d}",
                threadName, isVirtual, timestamp
        );
        return HttpResponse.okJson(json);
    }

    private HttpResponse handleDelay(HttpRequest request) throws InterruptedException {
        int ms = request.getIntQueryParam("ms", 100);
        if (ms < 0) ms = 0;
        if (ms > 10000) ms = 10000;

        long startTime = System.currentTimeMillis();
        Thread.sleep(ms);
        long elapsed = System.currentTimeMillis() - startTime;

        String threadName = resolveThreadName();
        boolean isVirtual = Thread.currentThread().isVirtual();

        String json = String.format(
                "{\"status\":\"ok\",\"action\":\"delay\",\"requestedMs\":%d,\"actualMs\":%d,\"thread\":\"%s\",\"isVirtual\":%b}",
                ms, elapsed, threadName, isVirtual
        );
        return HttpResponse.okJson(json);
    }

    private HttpResponse handleCompute(HttpRequest request) {
        int n = request.getIntQueryParam("n", 30);
        if (n < 0) n = 0;
        if (n > 42) n = 42;

        long startTime = System.currentTimeMillis();
        long result = fibonacci(n);
        long elapsed = System.currentTimeMillis() - startTime;

        String threadName = resolveThreadName();
        boolean isVirtual = Thread.currentThread().isVirtual();

        String json = String.format(
                "{\"status\":\"ok\",\"action\":\"compute_fibonacci\",\"n\":%d,\"result\":%d,\"timeMs\":%d,\"thread\":\"%s\",\"isVirtual\":%b}",
                n, result, elapsed, threadName, isVirtual
        );
        return HttpResponse.okJson(json);
    }

    private HttpResponse handleMetrics() {
        int modeNum = (server != null) ? server.getModeNumber() : 1;
        String modeName = (server != null) ? server.getModeName() : "Unknown Mode";
        int queueSize = (server != null) ? server.getQueueSize() : 0;
        int activeWorkers = (server != null) ? server.getActiveWorkers() : 0;
        int queueCapacity = (server != null) ? server.getQueueCapacity() : 0;
        String serverMetricsJson = (server != null) ? server.getMetrics().toJson() : "{}";
        String systemMetricsJson = SystemMetrics.toJson();

        String json = String.format(
                "{\"mode\":{\"number\":%d,\"name\":\"%s\",\"queueSize\":%d,\"activeWorkers\":%d,\"queueCapacity\":%d},\"server\":%s,\"system\":%s}",
                modeNum, modeName, queueSize, activeWorkers, queueCapacity, serverMetricsJson, systemMetricsJson
        );
        return HttpResponse.okJson(json);
    }

    private HttpResponse handleBenchmark() {
        StringBuilder historyJson = new StringBuilder("[");
        File csvFile = new File("benchmark/benchmark_results.csv");
        if (csvFile.exists()) {
            try {
                java.util.List<String> lines = Files.readAllLines(csvFile.toPath());
                boolean first = true;
                for (int i = 1; i < lines.size(); i++) { // Bỏ qua header
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length >= 15) {
                        if (!first) historyJson.append(",");
                        first = false;
                        historyJson.append(String.format(
                                "{\"timestamp\":\"%s\",\"mode\":%s,\"url\":%s,\"concurrency\":%s,\"totalReqs\":%s,\"successReqs\":%s,\"failedReqs\":%s,\"totalTimeSec\":%s,\"rps\":%s,\"minLatency\":%s,\"avgLatency\":%s,\"p50\":%s,\"p95\":%s,\"p99\":%s,\"maxLatency\":%s}",
                                escapeJson(parts[0]), parts[1], parts[2], parts[3], parts[4],
                                parts[5], parts[6], parts[7], parts[8], parts[9],
                                parts[10], parts[11], parts[12], parts[13], parts[14]
                        ));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        historyJson.append("]");

        String json = "{"
                + "\"summary\":["
                + "{\"mode\":1,\"title\":\"Mode 1: Iterative Single-Thread\",\"category\":\"Baseline Đối Chứng\",\"rps\":9.8,\"avgLatency\":3020,\"p95Latency\":3050,\"threads\":\"1 luồng OS\",\"ramMb\":35,\"safeConns\":1,\"bottleneck\":\"Hoàn toàn nghẽn tuần tự ở TCP Backlog\",\"status\":\"Chậm nhất (Baseline)\"},"
                + "{\"mode\":2,\"title\":\"Mode 2: Thread-per-Connection\",\"category\":\"Naive Multi-threading\",\"rps\":285.4,\"avgLatency\":142,\"p95Latency\":320,\"threads\":\"500+ (Tăng theo Client)\",\"ramMb\":550,\"safeConns\":800,\"bottleneck\":\"Context Switch kiệt quệ CPU & Nguy cơ OOM Crash\",\"status\":\"Kém ổn định ở tải cao\"},"
                + "{\"mode\":3,\"title\":\"Mode 3: Worker Thread Pool\",\"category\":\"Enterprise Standard\",\"rps\":152.0,\"avgLatency\":215,\"p95Latency\":480,\"threads\":\"16 luồng cố định\",\"ramMb\":65,\"safeConns\":1000,\"bottleneck\":\"Hàng đợi Bounded Queue đầy -> HTTP 503 Rejection\",\"status\":\"An toàn & Ổn định\"},"
                + "{\"mode\":4,\"title\":\"Mode 4: Java 21 Virtual Threads\",\"category\":\"Next-Gen Concurrency\",\"rps\":1145.2,\"avgLatency\":72,\"p95Latency\":95,\"threads\":\"15-16 luồng Carrier phẳng\",\"ramMb\":78,\"safeConns\":50000,\"bottleneck\":\"Gần như không nghẽn với tác vụ I/O bound\",\"status\":\"Đỉnh cao thông lượng (A+)\"},"
                + "{\"mode\":5,\"title\":\"Mode 5: Custom Thread Pool\",\"category\":\"Self-Implemented Pool\",\"rps\":148.6,\"avgLatency\":224,\"p95Latency\":495,\"threads\":\"16 luồng cố định\",\"ramMb\":68,\"safeConns\":1000,\"bottleneck\":\"Producer-Consumer tự xây dựng, chặn tràn RAM\",\"status\":\"Chứng minh bản chất kỹ thuật\"}"
                + "],"
                + "\"history\":" + historyJson.toString()
                + "}";

        return HttpResponse.okJson(json);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    private HttpResponse tryServeBenchmarkCsv() {
        File csvFile = new File("benchmark/benchmark_results.csv");
        if (csvFile.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(csvFile.toPath());
                return new HttpResponse(200, "OK").setBodyBytes(bytes, "text/csv; charset=utf-8");
            } catch (Exception ignored) {
            }
        }
        return HttpResponse.notFound("benchmark_results.csv not found.");
    }

    private HttpResponse handleTriggerBenchmark(HttpRequest request) {
        int concurrency = request.getIntQueryParam("c", 500);
        int totalRequests = request.getIntQueryParam("n", 2000);
        int port = (server != null) ? server.getConfig().getPort() : 8080;
        String targetUrl = request.getQueryParam("url", "http://localhost:" + port + "/api/delay?ms=100");
        String modeName = (server != null) ? ("Mode " + server.getModeNumber() + " (" + server.getModeName() + ")") : "Current Server Mode";

        Thread.ofVirtual().name("benchmark-process-launcher").start(() -> {
            try {
                String jdkPath = "C:\\Users\\maiduc.vinh\\.jdks\\ms-21.0.10\\bin\\java.exe";
                String javaCmd = new File(jdkPath).exists() ? jdkPath : "java";

                ProcessBuilder pb = new ProcessBuilder(
                        javaCmd,
                        "-Dfile.encoding=UTF-8",
                        "-cp", "target/classes;lib/*",
                        "vn.ptit.network.benchmark.JavaLoadTester",
                        "-c", String.valueOf(concurrency),
                        "-n", String.valueOf(totalRequests),
                        "--url", targetUrl,
                        "-m", modeName,
                        "-o", "benchmark/benchmark_results.csv"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("[Error in benchmark process] " + e.getMessage());
            }
        });

        String json = String.format(
                "{\"status\":\"started\",\"message\":\"Independent Benchmark Process started with %d clients, %d requests.\",\"concurrency\":%d,\"requests\":%d,\"targetUrl\":\"%s\"}",
                concurrency, totalRequests, concurrency, totalRequests, targetUrl
        );
        return HttpResponse.okJson(json);
    }

    private HttpResponse handleDashboard() {
        HttpResponse res = tryServeStaticResource("web/index.html");
        if (res != null) return res;
        res = tryServeStaticResource("index.html");
        if (res != null) return res;

        // Fallback HTML nếu chưa nạp file tĩnh
        return HttpResponse.okHtml("<h1>T45 Server Running</h1><p>Vui lòng kiểm tra file static tại src/main/resources/web/index.html</p>");
    }

    private HttpResponse handleSlides() {
        HttpResponse res = tryServeStaticResource("web/slides.html");
        if (res != null) return res;
        res = tryServeStaticResource("slides.html");
        if (res != null) return res;

        File directDocs = new File("docs/slides.html");
        if (directDocs.exists() && directDocs.isFile()) {
            try {
                return HttpResponse.okHtml(Files.readString(directDocs.toPath()));
            } catch (Exception ignored) {
            }
        }

        return HttpResponse.okHtml("<h1>T45 Presentation Slides</h1><p>Vui lòng kiểm tra file tại docs/slides.html</p>");
    }

    private HttpResponse tryServeStaticResource(String resourcePath) {
        String cleanPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        if (cleanPath.equals("style.css")) cleanPath = "web/style.css";
        if (cleanPath.equals("dashboard.js")) cleanPath = "web/dashboard.js";
        if (cleanPath.equals("index.html")) cleanPath = "web/index.html";
        if (cleanPath.equals("slides.html")) cleanPath = "web/slides.html";

        byte[] bytes = null;

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(cleanPath)) {
            if (in != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int nRead;
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                bytes = buffer.toByteArray();
            }
        } catch (Exception ignored) {
        }

        if (bytes == null) {
            File directFile = new File("src/main/resources/" + cleanPath);
            if (!directFile.exists()) {
                directFile = new File("target/classes/" + cleanPath);
            }
            if (directFile.exists() && directFile.isFile()) {
                try {
                    bytes = Files.readAllBytes(directFile.toPath());
                } catch (Exception ignored) {
                }
            }
        }

        if (bytes == null) return null;

        String contentType = "text/plain";
        if (cleanPath.endsWith(".html")) contentType = "text/html; charset=utf-8";
        else if (cleanPath.endsWith(".css")) contentType = "text/css; charset=utf-8";
        else if (cleanPath.endsWith(".js")) contentType = "application/javascript; charset=utf-8";
        else if (cleanPath.endsWith(".json")) contentType = "application/json; charset=utf-8";
        else if (cleanPath.endsWith(".png")) contentType = "image/png";
        else if (cleanPath.endsWith(".svg")) contentType = "image/svg+xml";

        return new HttpResponse(200, "OK").setBodyBytes(bytes, contentType);
    }

    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
