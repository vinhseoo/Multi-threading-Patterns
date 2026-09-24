package vn.ptit.network.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Cấu hình tham số hoạt động cho hệ thống máy chủ mạng T45.
 * Được tinh giản tối đa nhờ Lombok annotations (@Data, @Builder, @Accessors(chain = true)).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ServerConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_BACKLOG = 1024;
    public static final int DEFAULT_CORE_POOL_SIZE = 16;
    public static final int DEFAULT_MAX_POOL_SIZE = 32;
    public static final int DEFAULT_QUEUE_CAPACITY = 1000;
    public static final int DEFAULT_SOCKET_TIMEOUT_MS = 15000;

    @Builder.Default
    private int port = DEFAULT_PORT;

    @Builder.Default
    private int backlog = DEFAULT_BACKLOG;

    @Builder.Default
    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;

    @Builder.Default
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

    @Builder.Default
    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

    @Builder.Default
    private int socketTimeoutMs = DEFAULT_SOCKET_TIMEOUT_MS;

    public ServerConfig(int port) {
        this();
        this.port = port;
    }
}
