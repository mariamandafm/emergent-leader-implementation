package protocols;

import components.Config;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class HTTPGatewayMessageHandler implements MessageHandler {
    private final Config config;

    private ServerSocket socket;

    public HTTPGatewayMessageHandler(Config config) {
        this.config = config;
    }

    @Override
    public String handle(String message) {
        if (message == null || message.trim().isEmpty()) {
            System.out.println("[Gateway] Mensagem vazia recebida. Ignorando.");
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(message, ";");
        if (!tokenizer.hasMoreTokens()) return "ERROR: Mensagem malformada";

        String operation = tokenizer.nextToken();

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
            return "ERROR: Nenhum node disponível";
        }

        try {
            Random rand = new Random();
            int nodePort = allNodes.get(rand.nextInt(allNodes.size()));
            InetAddress nodeAddress = InetAddress.getByName("localhost");

            try (Socket clientSocket = new Socket(nodeAddress, nodePort)) {
                clientSocket.setSoTimeout(5000);

                OutputStream output = clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(originalMessage);

                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                return readHttpRequest(input);
            }
        } catch (SocketTimeoutException e) {
            return "ERROR: Timeout ao aguardar resposta do node";
        } catch (IOException e) {
            return "ERROR: Falha ao encaminhar requisição - " + e.getMessage();
        }
    }

    private String readHttpRequest(BufferedReader input) throws IOException {
        StringBuilder request = new StringBuilder();
        int contentLenght = 0;
        String line;
        while ((line = input.readLine()) != null){
            if (line.trim().isEmpty()) break;
            if (line.toLowerCase().startsWith("content-length")) {
                contentLenght = Integer.parseInt(line.split(":")[1].trim());
            }
            request.append(line).append("\n");
        }

        request.append("\r\n");
        char[] body = new char[contentLenght];
        if (contentLenght > 0) {
            input.read(body, 0, contentLenght);
            request.append(body);
        }
        return request.toString();
    }
}
