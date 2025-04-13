package udp;

import protocols.MessageHandler;
import protocols.Protocol;
import protocols.UDPMessageHandler;
import protocols.UDPProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.StringTokenizer;

public class Node {
    private final Config config;
    private final int port;
    private MembershipService membershipService;
    private volatile boolean running = true;
    //private DatagramSocket serverSocket;
    private final TasksApp tasksApp = new TasksApp();
    private Protocol protocol;

    private MessageHandler handler;

    public Node(Config config, int port) {
        this.config = config;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws SocketException {
//        try {
//            serverSocket = new DatagramSocket(port);
//        } catch (Exception e) {
//            System.out.println("Could not start node");
//        }
        this.protocol = new UDPProtocol(port, handler);
        this.membershipService = new MembershipService(port, protocol);
        this.handler = new UDPMessageHandler(port, membershipService, tasksApp, config);
        protocol.setHandler(handler);



        if (membershipService.join(config.getSeedAddress(), config)){
            //startService();
            System.out.println(port + " Iniciando protocolo UDP");
            protocol.start();
            sendHeartbeats();
            System.out.println(membershipService.membership.getLiveMembers());
        } else {
            System.out.println("Não foi possivel se juntar ao cluster");
        }
    }

    private void sendHeartbeats() {
        System.out.println("Heartbeat");
        new Thread(() -> {
            while (running) {
                // Se forr seed envia heartbeat para todos os nodes
                if (port == config.getSeedAddress()){
                    for (Integer nodePort : config.getUpNodes()) {
                        if (nodePort == port) continue;

                        try {
                            System.out.println("Seed enviando heartbeat");
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
//
//
    public void stop() {
        running = false;
        protocol.stop();
        membershipService.stopFailureDetector();

        System.out.println("[Node " + port + "] Encerrado.");
    }
}