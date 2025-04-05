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

    public void start() {
        try {
            serverSocket = new DatagramSocket(port);
        } catch (Exception e) {
            System.out.println("Could not start node");
        }
        this.membershipService = new MembershipService(port, serverSocket);
        if (membershipService.join(config.getSeedAddress(), config)){
            startService();
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

//        new Thread(() -> {
//            while (true){
//                if (config.getSeedAddress() == port) {
//                    System.out.println(membershipService.membership.liveMembers);
//                }
//            }
//
//        }).start();
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

                        return "accepted;" + newNodeAddress;
                    } else {
                        System.out.println("Apenas seed pode processar pedidos de join");
                    }
                default:
                    return "Erro: Operação inválida - " + operation;
            }
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    private void sendResponse(String response, InetAddress address, int port) {
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

//    public void stop() {
//        running = false;
//        if (serverSocket != null) {
//            serverSocket.close();
//        }
//        if (membershipService != null) {
//            membershipService.leave();
//        }
//    }
}