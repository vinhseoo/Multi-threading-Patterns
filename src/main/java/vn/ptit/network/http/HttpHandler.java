package vn.ptit.network.http;

import lombok.Setter;
import vn.ptit.network.metrics.SystemMetrics;
import vn.ptit.network.server.BaseHttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Bộ định tuyến (Router) và xử lý logic nghiệp vụ cho các yêu cầu HTTP.
 * Hỗ trợ các kịch bản thực nghiệm: I/O thuần, giả lập I/O-bound (delay), CPU-bound (compute),
 * cung cấp API số liệu hiệu năng /api/metrics và Web Dashboard thời gian thực.
 */
public class HttpHandler {

    @Setter
    private BaseHttpServer server;

    public HttpResponse handle(HttpRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        // Xử lý tiền kiểm CORS (Preflight request)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return new HttpResponse(204, "No Content");
        }

        try {
            switch (path) {
                case "/":
                case "/index.html":
                case "/dashboard":
                    return handleDashboard();

                case "/api/hello":
                    return handleHello(request);

                case "/api/delay":
                    return handleDelay(request);

                case "/api/compute":
                    return handleCompute(request);

                case "/api/metrics":
                    return handleMetrics();

                default:
                    // Thử đọc static resource từ classpath hoặc file system
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

    private HttpResponse handleHello(HttpRequest request) {
        String threadName = Thread.currentThread().getName();
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

        String threadName = Thread.currentThread().getName();
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

        String threadName = Thread.currentThread().getName();
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
        String serverMetricsJson = (server != null) ? server.getMetrics().toJson() : "{}";
        String systemMetricsJson = SystemMetrics.toJson();

        String json = String.format(
                "{\"mode\":{\"number\":%d,\"name\":\"%s\"},\"server\":%s,\"system\":%s}",
                modeNum, modeName, serverMetricsJson, systemMetricsJson
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

    private HttpResponse tryServeStaticResource(String resourcePath) {
        String cleanPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        if (cleanPath.equals("style.css")) cleanPath = "web/style.css";
        if (cleanPath.equals("dashboard.js")) cleanPath = "web/dashboard.js";
        if (cleanPath.equals("index.html")) cleanPath = "web/index.html";

        byte[] bytes = null;

        // 1. Thử đọc từ ClassLoader
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
        } catch (Exception ignored) {}

        // 2. Thử đọc từ thư mục src/main/resources hoặc target/classes trực tiếp
        if (bytes == null) {
            File directFile = new File("src/main/resources/" + cleanPath);
            if (!directFile.exists()) {
                directFile = new File("target/classes/" + cleanPath);
            }
            if (directFile.exists() && directFile.isFile()) {
                try {
                    bytes = Files.readAllBytes(directFile.toPath());
                } catch (Exception ignored) {}
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

    /**
     * Thuật toán Fibonacci đệ quy thuần túy tạo tải CPU tính toán.
     */
    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
