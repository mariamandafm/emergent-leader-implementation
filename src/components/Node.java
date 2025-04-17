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
            sendHeartbeats();
        } else {
            System.out.println("Não foi possivel se juntar ao cluster");
        }
    }

    private void sendHeartbeats() {
        new Thread(() -> {
            while (running) {
                // Se forr seed envia heartbeat para todos os nodes
                if (port == config.getSeedAddress()){
                    for (Integer nodePort : config.getUpNodes()) {
                        if (nodePort == port) continue;

                        try {
                            String message = "heartbeat;" + port;
                            protocol.send(message, InetAddress.getByName("localhost"), nodePort);

                        } catch (Exception e) {
                            System.out.println("[Node " + port + "] Erro ao enviar heartbeat: " + e.getMessage());
                        }
                    }
                } else {
                    try {
                        String message = "heartbeat;" + port;
                        byte[] data = message.getBytes();
                        protocol.send(message, InetAddress.getByName("localhost"), config.getSeedAddress());

                    } catch (Exception e) {
                        System.out.println("[Node " + port + "] Erro ao enviar heartbeat: " + e.getMessage());
                    }
                }


                try {
                    Thread.sleep(6000); // envia heartbeat a cada 3s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        protocol.stop();
        membershipService.stopFailureDetector();

        System.out.println("[Node " + port + "] Encerrado.");
    }
}