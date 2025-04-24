package components;

import factory.HTTPNetworkFactory;
import factory.NetworkFactory;
import factory.TCPNetworkFactory;
import factory.UDPNetworkFactory;
import protocols.*;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Node {
    private final Config config;
    private final int port;
    private MembershipService membershipService;
    private volatile boolean running = true;
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
        this.handler = new TCPMessageHandler(port, membershipService, config);
        this.handler = factory.createMessageHandler(port, membershipService, config);
        protocol.setHandler(handler);

        if (membershipService.join(config.getSeedAddress(), config)) {
            protocol.start();
            protocol.sendHeartbeats(config);
        } else {
            System.out.println("Não foi possível se juntar ao cluster");
        }
    }

    public void stop() {
        running = false;
        protocol.stop();
        membershipService.stopFailureDetector();
        System.out.println("[Node " + port + "] Encerrado.");
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Uso: java components.Node <protocol> <seed_host> <seed_port>");
            System.exit(1);
        }

        try {
            String protocolType = args[0];
            int port = Integer.parseInt(args[1]);
            int seedPort = Integer.parseInt(args[2]);

            Config config = new Config(seedPort);

            NetworkFactory factory = switch (protocolType) {
                case "udp" -> new UDPNetworkFactory();
                case "tcp" -> new TCPNetworkFactory();
                case "http" -> new HTTPNetworkFactory();
                default -> null;
            };

            Node node = new Node(factory, config, port);
            node.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Encerrando nó...");
                node.stop();
            }));

        } catch (NumberFormatException e) {
            System.err.println("Porta deve ser um número válido");
            System.exit(1);
        } catch (SocketException e) {
            System.err.println("Erro ao iniciar o nó: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}