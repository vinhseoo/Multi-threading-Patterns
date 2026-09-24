package vn.ptit.network.http;

import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lớp xây dựng (builder) và gửi phản hồi HTTP/1.1 chuẩn về client.
 * Tận dụng Lombok @Getter cho các thuộc tính trạng thái.
 */
@Getter
public class HttpResponse {
    private final int statusCode;
    private final String statusText;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] bodyBytes = new byte[0];

    public HttpResponse(int statusCode, String statusText) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        // Các header mặc định
        setHeader("Server", "T45-MultiThreaded-Server/1.0 (PTIT)");
        setHeader("Connection", "close");
        setHeader("Access-Control-Allow-Origin", "*");
        setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    public HttpResponse setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpResponse setBody(String body, String contentType) {
        this.bodyBytes = (body != null) ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        setHeader("Content-Type", contentType + "; charset=utf-8");
        setHeader("Content-Length", String.valueOf(this.bodyBytes.length));
        return this;
    }

    public HttpResponse setBodyBytes(byte[] bytes, String contentType) {
        this.bodyBytes = (bytes != null) ? bytes : new byte[0];
        setHeader("Content-Type", contentType);
        setHeader("Content-Length", String.valueOf(this.bodyBytes.length));
        return this;
    }

    /**
     * Ghi toàn bộ gói tin HTTP xuống OutputStream của Socket.
     */
    public void writeTo(OutputStream out) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        if (!headers.containsKey("Content-Length")) {
            headers.put("Content-Length", String.valueOf(bodyBytes.length));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headerBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        headerBuilder.append("\r\n");
        out.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
        if (bodyBytes.length > 0) {
            out.write(bodyBytes);
        }
        out.flush();
    }

    // ================= STATIC FACTORIES =================

    public static HttpResponse okJson(String json) {
        return new HttpResponse(200, "OK").setBody(json, "application/json");
    }

    public static HttpResponse okHtml(String html) {
        return new HttpResponse(200, "OK").setBody(html, "text/html");
    }

    public static HttpResponse okText(String text) {
        return new HttpResponse(200, "OK").setBody(text, "text/plain");
    }

    public static HttpResponse notFound(String message) {
        String json = "{\"error\": 404, \"message\": \"" + escapeJson(message) + "\"}";
        return new HttpResponse(404, "Not Found").setBody(json, "application/json");
    }

    public static HttpResponse badRequest(String message) {
        String json = "{\"error\": 400, \"message\": \"" + escapeJson(message) + "\"}";
        return new HttpResponse(400, "Bad Request").setBody(json, "application/json");
    }

    public static HttpResponse serviceUnavailable(String message) {
        String json = "{\"error\": 503, \"message\": \"" + escapeJson(message) + "\"}";
        HttpResponse res = new HttpResponse(503, "Service Unavailable").setBody(json, "application/json");
        res.setHeader("Retry-After", "2");
        return res;
    }

    public static HttpResponse internalServerError(String message) {
        String json = "{\"error\": 500, \"message\": \"" + escapeJson(message) + "\"}";
        return new HttpResponse(500, "Internal Server Error").setBody(json, "application/json");
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
