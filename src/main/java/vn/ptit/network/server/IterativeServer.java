package vn.ptit.network.server;

import vn.ptit.network.config.ServerConfig;
import vn.ptit.network.http.HttpHandler;

import java.net.Socket;

public class IterativeServer extends BaseHttpServer {

    public IterativeServer(ServerConfig config, HttpHandler handler) {
        super(config, handler);
    }

    @Override
    public String getModeName() {
        return "Iterative Single-Threaded Server (Baseline)";
    }

    @Override
    public int getModeNumber() {
        return 1;
    }

    @Override
    protected void dispatchClient(Socket socket) {
        // Xử lý trực tiếp trên luồng Acceptor hiện tại -> Gây block hoàn toàn cho client tiếp theo!
        processConnection(socket);
    }
}
