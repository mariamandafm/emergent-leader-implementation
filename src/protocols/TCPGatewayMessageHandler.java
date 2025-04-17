package protocols;

import components.Config;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class TCPGatewayMessageHandler implements MessageHandler {
    private final Config config;

    private ServerSocket socket;

    public TCPGatewayMessageHandler(Config config) {
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
        //
    }

    @Override
    public void setSocket(ServerSocket socket) {
        //this.socket = socket;
    }

    private String forwardToNodeAndGetResponse(String operation, String originalMessage) {
        List<Integer> allNodes = config.getUpNodes();

        if (allNodes.isEmpty()) {
            return "Erro: Nenhum node disponível";
        }

        try {
            Random rand = new Random();
            int nodePort = allNodes.get(rand.nextInt(allNodes.size()));
            InetAddress nodeAddress = InetAddress.getByName("localhost");

            // Abre conexão TCP com o node
            try (Socket clientSocket = new Socket(nodeAddress, nodePort)) {
                clientSocket.setSoTimeout(5000); // Timeout de 5 segundos

                // Envia mensagem
                OutputStream output = clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(originalMessage);

                // Recebe resposta
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                return reader.readLine(); // espera uma única linha como resposta
            }
        } catch (SocketTimeoutException e) {
            return "Erro: Timeout ao aguardar resposta do node";
        } catch (IOException e) {
            return "Erro: Falha ao encaminhar requisição - " + e.getMessage();
        }
    }
}
