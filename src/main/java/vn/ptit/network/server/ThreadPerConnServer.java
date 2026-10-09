package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPerConnServer extends BaseHttpServer {

    private final AtomicLong threadCounter = new AtomicLong(0);

    public ThreadPerConnServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
    }

    @Override
    public String getModeName() {
        return "Thread-per-Connection Server (Naive Multi-threading)";
    }

    @Override
    public int getModeNumber() {
        return 2;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        long id = threadCounter.incrementAndGet();
        // Khởi tạo một OS Platform Thread cho mỗi kết nối client vào
        Thread clientThread = new Thread(() -> processConnection(socket), "ThreadPerConn-" + id);
        
        // Thiết lập làm Daemon Thread hoặc User Thread (mặc định User thread để đảm bảo xử lý xong request)
        clientThread.start();
    }
}
