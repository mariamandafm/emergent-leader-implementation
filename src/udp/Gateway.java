package udp;

import protocols.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class Gateway {
    private final int PORT = 9000;
    private final Config config;
    private final Protocol protocol;
    private boolean running = true;

    private MessageHandler handler;

    // Guarda informações do cliente para enviar resposta depois
    private final ConcurrentHashMap<String, ClientInfo> clientMap = new ConcurrentHashMap<>();

    public Gateway(Config config) {
        this.config = config;
        this.protocol = new UDPProtocol(PORT, handler);
        this.handler = new GatewayMessageHandler(config);
        protocol.setHandler(handler);
    }

    public void start() {
        System.out.println("Gateway iniciado na porta " + PORT);
        protocol.start();
    }

    public void stop() {
        running = false;
        protocol.stop();
    }

    private String generateClientId(String base) {
        return String.valueOf(base.hashCode()); // Simples exemplo
    }

    private static class ClientInfo {
        public final Thread thread;
        public final InetAddress address;
        public final int port;

        public ClientInfo(Thread thread, String id) {
            this.thread = thread;
            this.address = null; // Você precisa preencher com o endereço original do cliente
            this.port = -1;
        }

        public ClientInfo(InetAddress address, int port) {
            this.thread = null;
            this.address = address;
            this.port = port;
        }
    }
}
