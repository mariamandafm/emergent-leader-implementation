package components;

import factory.NetworkFactory;
import protocols.*;

import java.net.InetAddress;
import java.net.SocketException;

public class Node {
    private final Config config;
    private final int port;
    private MembershipService membershipService;
    private volatile boolean running = true;
    //private DatagramSocket serverSocket;
    private final TasksApp tasksApp = new TasksApp();
    private Protocol protocol;

    private MessageHandler handler;

    private NetworkFactory factory;

    public Node(NetworkFactory factory, Config config, int port) {
        this.config = config;
        this.port = port;
        this.factory = factory;
    }

    public int getPort() {
        return port;
    }

    public void start() throws SocketException {
        this.protocol = factory.createProtocol(port);
        this.membershipService = new MembershipService(port, protocol);
        //this.handler = new TCPMessageHandler(port, membershipService, config);
        this.handler = factory.createMessageHandler(port, membershipService, config);
        protocol.setHandler(handler);

        if (membershipService.join(config.getSeedAddress(), config)){
            System.out.println(port + " Iniciando protocolo UDP");
            protocol.start();
            //protocol.sendHeartbeats(config);
        } else {
            System.out.println("Não foi possivel se juntar ao cluster");
        }
    }

    public void stop() {
        running = false;
        protocol.stop();
        membershipService.stopFailureDetector();

        System.out.println("[Node " + port + "] Encerrado.");
    }
}