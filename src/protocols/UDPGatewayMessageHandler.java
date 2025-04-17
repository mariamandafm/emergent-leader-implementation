package protocols;
import components.Config;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class UDPGatewayMessageHandler implements MessageHandler {
    private final Config config;

    private DatagramSocket socket;

    public UDPGatewayMessageHandler(Config config) {
        this.config = config;
    }

    @Override
    public String handle(String message) {
        if (message == null || message.trim().isEmpty()) {
            System.out.println("[Gateway] Mensagem vazia recebida. Ignorando.");
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(message, ";");
        if (!tokenizer.hasMoreTokens()) return "Erro: Mensagem malformada";

        String operation = tokenizer.nextToken();

        // Você pode ignorar certos tipos de mensagens se quiser, como heartbeat:
        if (operation.equals("heartbeat")) {
            return null;
        }

        return forwardToNodeAndGetResponse(operation, message);
    }

    @Override
    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void setSocket(ServerSocket socket) {
        //
    }

    private String forwardToNodeAndGetResponse(String operation, String originalMessage) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress nodeAddress = InetAddress.getByName("localhost");
            List<Integer> allNodes = config.getUpNodes();

            if (allNodes.isEmpty()) {
                return "Erro: Nenhum node disponível";
            }

            Random rand = new Random();
            int nodePort = allNodes.get(rand.nextInt(allNodes.size()));

            //System.out.printf("[Gateway] Encaminhando requisição %s para %s%n",
            //        operation, nodePort);

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
        } catch (IOException e) {
            return "Erro: Falha ao encaminhar requisição - " + e.getMessage();
        }
    }
}
