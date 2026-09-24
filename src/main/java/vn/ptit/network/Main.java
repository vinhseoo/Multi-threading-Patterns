package vn.ptit.network;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;
import vn.ptit.network.server.BaseHttpServer;
import vn.ptit.network.server.IterativeServer;

import java.io.IOException;

/**
 * Điểm khởi động chính của ứng dụng máy chủ mạng T45.
 * Hỗ trợ chọn chế độ hoạt động (Mode 1, 2, 3, 4) qua tham số dòng lệnh hoặc mặc định.
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        int mode = 1; // Mặc định Mode 1 cho Phase 1
        int port = ServerConfig.DEFAULT_PORT;

        if (args.length > 0) {
            try {
                mode = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[!] Tham số mode không hợp lệ. Sử dụng mode mặc định: 1");
            }
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        ServerConfig config = new ServerConfig(port);
        HttpHandler handler = new HttpHandler();

        BaseHttpServer server;
        switch (mode) {
            case 1:
                server = new IterativeServer(config, handler);
                break;
            default:
                System.out.println("[!] Mode " + mode + " sẽ được hoàn thiện trong Phase tiếp theo.");
                System.out.println("[*] Tự động chuyển về Mode 1: Iterative Single-Threaded Server.");
                server = new IterativeServer(config, handler);
                break;
        }

        // Đăng ký Shutdown Hook để đóng socket sạch sẽ khi nhấn Ctrl + C
        final BaseHttpServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[!] Nhận tín hiệu tắt từ hệ thống (Ctrl+C / SIGINT)...");
            finalServer.stop();
        }, "Shutdown-Hook"));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[FATAL ERROR] Không thể khởi động máy chủ: " + e.getMessage());
            e.printStackTrace();
        }
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
        System.out.println("  Sinh viên thực hiện: Solo Project (Java 21 LTS)");
        System.out.println("===========================================================================\n");
    }
}
