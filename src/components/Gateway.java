package components;

import factory.NetworkFactory;
import protocols.*;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class Gateway {
    private final int PORT = 9000;
    private final Config config;
    private final Protocol protocol;
    private boolean running = true;
    private MessageHandler handler;

    public Gateway(NetworkFactory factory, Config config) {
        this.config = config;
        this.protocol = factory.createProtocol(PORT);
        this.handler = factory.createGatewayMessageHandler(config);
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
}
