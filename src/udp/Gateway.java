package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import java.io.IOException;
import java.net.*;
import java.util.StringTokenizer;

public class Gateway {
    private final int PORT = 9000;
    private final Config config;
    private DatagramSocket serverSocket;
    private boolean running = true;

    public Gateway(Config config) {
        this.config = config;
    }

    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new DatagramSocket(PORT);
                System.out.println("Gateway iniciado na porta " + PORT);

                while (running) {
                    System.out.println("Up nodes gat: " + config.getUpNodes());
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    serverSocket.receive(receivePacket);

                    InetAddress clientAddress = receivePacket.getAddress();
                    int clientPort = receivePacket.getPort();
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    System.out.println("[Gateway] Mensagem recebida do cliente: " + message);

                    StringTokenizer tokenizer = new StringTokenizer(message, ";");
                    String operation = tokenizer.nextToken();

                    String nodeResponse = forwardToNodeAndGetResponse(operation, message);

                    byte[] responseBuffer = nodeResponse.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseBuffer, responseBuffer.length, clientAddress, clientPort);
                    serverSocket.send(responsePacket);

                    System.out.println("[Gateway] Resposta enviada ao cliente: " + nodeResponse);
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                    System.out.println("Erro no gateway");
                }
            } finally {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            }
        }).start();
    }

    private String forwardToNodeAndGetResponse(String operation, String originalMessage) throws IOException {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress nodeAddress = InetAddress.getByName("localhost");
            List<Integer> allNodes = config.getUpNodes();

            if (allNodes.isEmpty()) {
                return "Erro: Nenhum node disponível";
            }

            Random rand = new Random();
            int nodePort = allNodes.get(rand.nextInt(allNodes.size()));

            System.out.printf("[Gateway] Encaminhando requisição %s para %s%n",
                    operation, nodePort);

            byte[] sendBuffer = originalMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendBuffer, sendBuffer.length, nodeAddress, nodePort);
            clientSocket.send(sendPacket);

            clientSocket.setSoTimeout(5000); // 5 segundos de timeout
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            try {
                clientSocket.receive(receivePacket);
                return new String(receivePacket.getData(), 0, receivePacket.getLength());
            } catch (SocketTimeoutException e) {
                return "Erro: Timeout ao aguardar resposta do node";
            }
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}