package udp;

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
    private DatagramSocket serverSocket;
    private final TasksApp tasksApp = new TasksApp();

    public Node(Config config, int port) {
        this.config = config;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        try {
            serverSocket = new DatagramSocket(port);
        } catch (Exception e) {
            System.out.println("Could not start node");
        }
        this.membershipService = new MembershipService(port, serverSocket);
        if (membershipService.join(config.getSeedAddress(), config)){
            startService();
            sendHeartbeats();
        } else {
            System.out.println("Não foi possivel se juntar ao cluster");
        }
    }

    public void startService() {
        new Thread(() -> {
            try {
                System.out.println("[Node " + port + "] Serviço iniciado");

                while (running) {
                    System.out.println("[" + port + "] Up nodes node: " + config.getUpNodes());
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    serverSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    System.out.println("[Node " + port + "] Mensagem recebida: " + message);

                    // Evitar tratar ack como requisição
                    if (message.trim().startsWith("ack")) {
                        System.out.println("[Node " + port + "] ACK recebido como resposta. Ignorado.");
                        continue;
                    }

                    String response = processMessage(message);
                    sendResponse(response, receivePacket.getAddress(), receivePacket.getPort());
                }
            } catch (SocketException e) {
                if (running) {
                    System.out.println("[Node " + port + "] Erro no socket: " + e.getMessage());
                }
            } catch (IOException e) {
                System.out.println("[Node " + port + "] Erro de IO: " + e.getMessage());
            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                System.out.println("[Node " + port + "] Serviço encerrado");
            }
        }).start();
    }

    private String processMessage(String message) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(message, ";");
            String operation = tokenizer.nextToken();
            String params = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

            switch (operation) {
                case "add":
                    if (params.isEmpty()) {
                        return "Erro: Nenhuma tarefa especificada";
                    }
                    tasksApp.addTask(params);
                    return "Tarefa adicionada: " + params;

                case "read":
                    return tasksApp.getTasks();
                case "join_request":
                    // se for seed
                    if (port == config.getSeedAddress()){
                        int newNodeAddress = Integer.parseInt(params);
                        System.out.println("Processando join request de: " + newNodeAddress);
                        membershipService.handleNewJoin(newNodeAddress, config);
                        config.setUpNodes(membershipService.membership.getUpNodesAddress());
                        String updatedMembership = membershipService.membership.getSerializedMembership();
                        return "accepted;" + updatedMembership;
                    } else {
                        System.out.println("Apenas seed pode processar pedidos de join");
                    }
                case "membership_update":
                    System.out.println("[ " + port + " ] Recebendo artualização de join #######");
                    String version = tokenizer.nextToken();
                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
                    System.out.println(membershipService.membership.getLiveMembers());
                    return "ack;";
                case "heartbeat":
                    int senderPort = Integer.parseInt(params);
                    membershipService.receiveHeartbeat(senderPort);
                    return "";
                default:
                    return "Erro: Operação inválida - " + operation;
            }
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    private void sendResponse(String response, InetAddress address, int port) {
        if (response == null || response.trim().isEmpty()) return; // <-- ignora respostas vazias

        try {
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length, address, port);
            serverSocket.send(responsePacket);
            System.out.println("[Node " + this.port + "] Resposta enviada: " + response);
        } catch (IOException e) {
            System.out.println("[Node " + this.port + "] Erro ao enviar resposta: " + e.getMessage());
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
                            byte[] data = message.getBytes();
                            DatagramPacket packet = new DatagramPacket(
                                    data, data.length, InetAddress.getByName("localhost"), nodePort);
                            serverSocket.send(packet);

                        } catch (Exception e) {
                            System.out.println("[Node " + port + "] Erro ao enviar heartbeat: " + e.getMessage());
                        }
                    }
                } else {
                    try {
                        String message = "heartbeat;" + port;
                        byte[] data = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(
                                data, data.length, InetAddress.getByName("localhost"), config.getSeedAddress());
                        serverSocket.send(packet);

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
        if (serverSocket != null) {
            serverSocket.close();
        }
        membershipService.stopFailureDetector();

        System.out.println("[Node " + port + "] Encerrado.");
    }
}