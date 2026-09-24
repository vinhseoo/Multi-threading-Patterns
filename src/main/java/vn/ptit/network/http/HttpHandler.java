package vn.ptit.network.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Bộ định tuyến (Router) và xử lý logic nghiệp vụ cho các yêu cầu HTTP.
 * Hỗ trợ các kịch bản thực nghiệm: I/O thuần, giả lập I/O-bound (delay), CPU-bound (compute).
 */
public class HttpHandler {

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
                    return handleRoot();

                case "/api/hello":
                    return handleHello(request);

                case "/api/delay":
                    return handleDelay(request);

                case "/api/compute":
                    return handleCompute(request);

                case "/dashboard":
                    return handleDashboard();

                default:
                    // Thử đọc static resource từ classpath nếu có
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

    private HttpResponse handleRoot() {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head><meta charset=\"UTF-8\"><title>T45 Network Server - PTIT</title>\n" +
                "<style>body{font-family:system-ui,sans-serif;background:#0f172a;color:#f8fafc;padding:40px;line-height:1.6}\n" +
                ".card{max-width:700px;margin:0 auto;background:#1e293b;border-radius:12px;padding:32px;box-shadow:0 10px 25px rgba(0,0,0,0.5);border:1px solid #334155}\n" +
                "h1{color:#38bdf8;margin-top:0;}a{color:#38bdf8;text-decoration:none}a:hover{text-decoration:underline}\n" +
                ".badge{background:#0284c7;color:#fff;padding:4px 10px;border-radius:6px;font-size:14px;font-weight:bold}\n" +
                "</style></head>\n" +
                "<body><div class=\"card\">\n" +
                "<h1>🚀 Đề Tài T45: Multi-Threading Patterns</h1>\n" +
                "<p><span class=\"badge\">PTIT Network Programming</span> Giảng viên: <b>TS. Đặng Ngọc Hùng</b></p>\n" +
                "<p>Máy chủ mạng đa luồng đã khởi chạy thành công!</p>\n" +
                "<h3>Các Endpoint kiểm thử có sẵn:</h3>\n" +
                "<ul>\n" +
                "  <li><a href=\"/api/hello\"><code>GET /api/hello</code></a> - Kiểm tra thông lượng socket thuần túy</li>\n" +
                "  <li><a href=\"/api/delay?ms=150\"><code>GET /api/delay?ms=150</code></a> - Giả lập I/O-bound (database/microservice)</li>\n" +
                "  <li><a href=\"/api/compute?n=32\"><code>GET /api/compute?n=32</code></a> - Giả lập CPU-bound (Fibonacci đệ quy)</li>\n" +
                "  <li><a href=\"/dashboard\"><code>GET /dashboard</code></a> - Real-time Web Dashboard (Phase 3)</li>\n" +
                "</ul>\n" +
                "</div></body></html>";
        return HttpResponse.okHtml(html);
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
        // Khống chế thời gian sleep an toàn tối đa 10 giây
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
        // Khống chế Fibonacci n <= 42 để tránh tràn stack/treo server quá lâu
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

    private HttpResponse handleDashboard() {
        HttpResponse res = tryServeStaticResource("/web/index.html");
        if (res != null) return res;
        return handleRoot();
    }

    private HttpResponse tryServeStaticResource(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=utf-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=utf-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=utf-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=utf-8";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            return new HttpResponse(200, "OK").setBodyBytes(bytes, contentType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Thuật toán Fibonacci đệ quy thuần túy tạo tải CPU tính toán.
     */
    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
