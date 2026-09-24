package vn.ptit.network;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;
import vn.ptit.network.server.*;

import java.io.IOException;
import java.util.Scanner;

/**
 * Điểm khởi động chính của ứng dụng máy chủ mạng T45.
 * Hỗ trợ chuyển đổi runtime linh hoạt giữa 4 mô hình luồng đối đầu + 1 mô hình Custom Thread Pool:
 *   Mode 1: Iterative Single-Threaded Server (Baseline đối chứng)
 *   Mode 2: Thread-per-Connection Server (Mỗi kết nối 1 luồng OS)
 *   Mode 3: Worker Thread Pool Server (Chuẩn doanh nghiệp với Bounded Queue + Rejection 503)
 *   Mode 4: Java 21 Virtual Threads (Project Loom hiện đại)
 *   Mode 5: Custom Thread Pool Server (Tự lập trình từ đầu để lấy trọn điểm Technical Depth)
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        int mode = 4; // Mặc định Mode 4 (Virtual Threads Loom)
        int port = ServerConfig.DEFAULT_PORT;

        if (args.length > 0) {
            try {
                mode = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[!] Tham số mode không hợp lệ. Mặc định chạy Mode 4.");
                mode = 4;
            }
        } else if (System.console() != null) {
            mode = promptUserMode();
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        ServerConfig config = ServerConfig.builder()
                .port(port)
                .backlog(1024)
                .corePoolSize(16)
                .maxPoolSize(32)
                .queueCapacity(1000)
                .socketTimeoutMs(15000)
                .build();

        HttpHandler handler = new HttpHandler();

        BaseHttpServer server = switch (mode) {
            case 1 -> new IterativeServer(config, handler);
            case 2 -> new ThreadPerConnServer(config, handler);
            case 3 -> new WorkerThreadPoolServer(config, handler);
            case 4 -> new VirtualThreadServer(config, handler);
            case 5 -> new CustomThreadPoolServer(config, handler);
            default -> {
                System.out.println("[!] Mode không hợp lệ (" + mode + "). Mặc định khởi chạy Mode 4.");
                yield new VirtualThreadServer(config, handler);
            }
        };

        // Đăng ký JVM Shutdown Hook để giải phóng socket và thread pool an toàn
        final BaseHttpServer runningServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[!] Nhận tín hiệu ngắt từ hệ thống (Ctrl+C / SIGINT)...");
            runningServer.stop();
        }, "Shutdown-Hook"));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[FATAL ERROR] Không thể khởi động máy chủ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int promptUserMode() {
        System.out.println("Vui lòng chọn mô hình luồng muốn khởi chạy:");
        System.out.println("  [1] Mode 1: Iterative Single-Threaded Server (Xử lý tuần tự - Baseline)");
        System.out.println("  [2] Mode 2: Thread-per-Connection Server (Mỗi kết nối 1 OS luồng)");
        System.out.println("  [3] Mode 3: Worker Thread Pool Server (Fixed Pool 16 + Bounded Queue 1000)");
        System.out.println("  [4] Mode 4: Virtual Thread Server (Java 21 Project Loom) [Khuyên Dùng]");
        System.out.println("  [5] Mode 5: Custom Thread Pool Server (Tự cài đặt Blocking Queue & Workers)");
        System.out.print("👉 Nhập lựa chọn (1-5, bấm Enter để chọn mặc định [4]): ");

        try {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                int selected = Integer.parseInt(line);
                if (selected >= 1 && selected <= 5) return selected;
            }
        } catch (Exception ignored) {}
        return 4;
    }

    private static void printBanner() {
        System.out.println("===========================================================================");
        System.out.println("  _____  _______ _____ _______   _   _ ______ _______          ______  _____  _  __");
        System.out.println(" |  __ \\|__   __|_   _|__   __| | \\ | |  ____|__   __|        |  ____|/ ____|| |/ /");
        System.out.println(" | |__) |  | |    | |    | |    |  \\| | |__     | |           | |__  | |     | ' / ");
        System.out.println(" |  ___/   | |    | |    | |    | . ` |  __|    | |           |  __| | |     |  <  ");
        System.out.println(" | |       | |   _| |_   | |    | |\\  | |____   | |           | |____| |____ | . \\ ");
        System.out.println(" |_|       |_|  |_____|  |_|    |_| \\_|______|  |_|           |______|\\_____||_|\\_\\");
        System.out.println("===========================================================================");
        System.out.println("  ĐỀ TÀI T45: MULTI-THREADING PATTERNS IN NETWORK PROGRAMMING");
        System.out.println("  Học viện Công nghệ Bưu chính Viễn thông (PTIT) - Giảng viên: TS. Đặng Ngọc Hùng");
        System.out.println("  Sinh viên thực hiện: Solo Project (Java 21 LTS - Loom)");
        System.out.println("===========================================================================\n");
    }
}
