package components;

import factory.HTTPNetworkFactory;
import factory.NetworkFactory;
import factory.TCPNetworkFactory;
import factory.UDPNetworkFactory;
import protocols.*;

public class TaskServer {
    private final int PORT = 9005;
    private final TasksApp tasksApp = new TasksApp();
    private Protocol protocol;
    private MessageHandler handler;
    private volatile boolean running = true;

    public TaskServer(NetworkFactory factory) {
        this.protocol = factory.createProtocol(PORT);
        this.handler = factory.createTaskMessageHandler();
        protocol.setHandler(handler);
    }

    public void start() {
        System.out.println("[TaskServer] Iniciado na porta " + PORT);
        protocol.start();

        // Adiciona shutdown hook para encerramento gracioso
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[TaskServer] Encerrando...");
            stop();
        }));

        // Mantém o processo ativo
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[TaskServer] Interrompido");
                stop();
            }
        }
    }

    public void stop() {
        running = false;
        if (protocol != null) {
            protocol.stop();
        }
        System.out.println("[TaskServer] Encerrado com sucesso");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java components.TaskServer <protocol>");
            System.exit(1);
        }
        try {
            String protocolType = args[0];

            NetworkFactory factory = switch (protocolType) {
                case "udp" -> new UDPNetworkFactory();
                case "tcp" -> new TCPNetworkFactory();
                case "http" -> new HTTPNetworkFactory();
                default -> null;
            };
            TaskServer server = new TaskServer(factory);
            server.start();
        } catch (Exception e) {
            System.err.println("[TaskServer] Erro ao iniciar: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}