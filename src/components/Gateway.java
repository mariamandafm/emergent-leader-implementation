package components;

import factory.HTTPNetworkFactory;
import factory.NetworkFactory;
import factory.TCPNetworkFactory;
import factory.UDPNetworkFactory;
import protocols.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Gateway {
    private final int PORT = 9000;
    private final Config config;
    private final Protocol protocol;
    private volatile boolean running = true;
    private MessageHandler handler;

    public Gateway(NetworkFactory factory, Config config) {
        this.config = config;
        this.protocol = factory.createProtocol(PORT);
        this.handler = factory.createGatewayMessageHandler(config);
        protocol.setHandler(handler);
    }

    public void start() {
        System.out.println("[Gateway] Iniciado na porta " + PORT);
        protocol.start();

        // Adiciona shutdown hook para encerramento gracioso
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Gateway] Encerrando...");
            stop();
        }));
    }

    public void stop() {
        running = false;
        protocol.stop();
        System.out.println("[Gateway] Encerrado com sucesso");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java components.Gateway <protocol> <seed_port>");
            System.exit(1);
        }

        try {
            String protocolType = args[0];
            int seedPort = Integer.parseInt(args[1]);

            //InetAddress seedAddress = InetAddress.getByName(seedHost);
            Config config = new Config(seedPort);
            List<Integer> allNodes = new ArrayList<>();
            allNodes.add(9001);

            config.setUpNodes(allNodes);

            NetworkFactory factory = switch (protocolType) {
                case "udp" -> new UDPNetworkFactory();
                case "tcp" -> new TCPNetworkFactory();
                case "http" -> new HTTPNetworkFactory();
                default -> null;
            }; // Use sua implementação real

            assert factory != null;
            Gateway gateway = new Gateway(factory, config);
            gateway.start();

            // Mantém o processo rodando
            while (gateway.running) {
                Thread.sleep(1000);
            }

        } catch (NumberFormatException e) {
            System.err.println("Erro: Porta do seed deve ser um número válido");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}