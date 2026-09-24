package vn.ptit.network.config;

/**
 * Cấu hình tham số hoạt động cho hệ thống máy chủ mạng T45.
 * Được thiết kế linh hoạt, cho phép tinh chỉnh số lượng luồng, kích thước hàng đợi,
 * độ sâu TCP Backlog và timeout mạng.
 */
public class ServerConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_BACKLOG = 1024;
    public static final int DEFAULT_CORE_POOL_SIZE = 16;
    public static final int DEFAULT_MAX_POOL_SIZE = 32;
    public static final int DEFAULT_QUEUE_CAPACITY = 1000;
    public static final int DEFAULT_SOCKET_TIMEOUT_MS = 15000;

    private int port = DEFAULT_PORT;
    private int backlog = DEFAULT_BACKLOG;
    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
    private int socketTimeoutMs = DEFAULT_SOCKET_TIMEOUT_MS;

    public ServerConfig() {}

    public ServerConfig(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public ServerConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public int getBacklog() {
        return backlog;
    }

    public ServerConfig setBacklog(int backlog) {
        this.backlog = backlog;
        return this;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public ServerConfig setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public ServerConfig setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public ServerConfig setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public ServerConfig setSocketTimeoutMs(int socketTimeoutMs) {
        this.socketTimeoutMs = socketTimeoutMs;
        return this;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                ", backlog=" + backlog +
                ", corePoolSize=" + corePoolSize +
                ", maxPoolSize=" + maxPoolSize +
                ", queueCapacity=" + queueCapacity +
                ", socketTimeoutMs=" + socketTimeoutMs +
                '}';
    }
}
