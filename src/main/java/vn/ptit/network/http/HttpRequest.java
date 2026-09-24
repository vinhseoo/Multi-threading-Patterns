package vn.ptit.network.http;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp phân tích cú pháp (parser) gói tin HTTP/1.1 thủ công từ Socket InputStream.
 * Sử dụng Lombok (@Getter, @ToString, @AllArgsConstructor) để tinh giản mã nguồn tối đa.
 */
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpRequest {
    private final String method;
    private final String rawUri;
    private final String path;
    private final String protocol;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final String body;

    /**
     * Phân tích một HTTP Request từ InputStream của Socket.
     */
    public static HttpRequest parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            throw new IOException("Empty or closed HTTP request stream");
        }

        String[] parts = requestLine.trim().split("\\s+");
        if (parts.length < 3) {
            throw new IOException("Malformed HTTP request line: " + requestLine);
        }

        String method = parts[0].toUpperCase();
        String rawUri = parts[1];
        String protocol = parts[2];

        // Tách Path và Query String
        String path = rawUri;
        Map<String, String> queryParams = new HashMap<>();
        int questionMarkIdx = rawUri.indexOf('?');
        if (questionMarkIdx != -1) {
            path = rawUri.substring(0, questionMarkIdx);
            String queryString = rawUri.substring(questionMarkIdx + 1);
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                if (pair.isEmpty()) continue;
                int eqIdx = pair.indexOf('=');
                if (eqIdx != -1) {
                    String key = URLDecoder.decode(pair.substring(0, eqIdx), StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(pair.substring(eqIdx + 1), StandardCharsets.UTF_8);
                    queryParams.put(key, val);
                } else {
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    queryParams.put(key, "");
                }
            }
        }

        // Đọc HTTP Headers
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonIdx = headerLine.indexOf(':');
            if (colonIdx != -1) {
                String key = headerLine.substring(0, colonIdx).trim().toLowerCase();
                String value = headerLine.substring(colonIdx + 1).trim();
                headers.put(key, value);
            }
        }

        // Đọc Body nếu có Content-Length
        StringBuilder bodyBuilder = new StringBuilder();
        String contentLengthStr = headers.get("content-length");
        if (contentLengthStr != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthStr);
                if (contentLength > 0) {
                    char[] buffer = new char[Math.min(contentLength, 8192)];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int read = reader.read(buffer, 0, Math.min(buffer.length, contentLength - totalRead));
                        if (read == -1) break;
                        bodyBuilder.append(buffer, 0, read);
                        totalRead += read;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        return new HttpRequest(
                method, rawUri, path, protocol,
                Collections.unmodifiableMap(queryParams),
                Collections.unmodifiableMap(headers),
                bodyBuilder.toString()
        );
    }

    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    public String getQueryParam(String key, String defaultValue) {
        return queryParams.getOrDefault(key, defaultValue);
    }

    public int getIntQueryParam(String key, int defaultValue) {
        String val = queryParams.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }
}
