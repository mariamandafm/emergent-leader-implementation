package protocols;

import components.Config;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

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

        StringTokenizer tokenizer = new StringTokenizer(message);

        // Processar como request
        String httpMethod = tokenizer.nextToken();
        String httpRoute = tokenizer.nextToken();

        String[] actions = httpRoute.split("\\?", 2);
        String operation = actions[0];
        String params = actions.length > 1 ? actions[1] : "";

        if (operation.equals("/membership_update")) {
            config.setUpNodes(extractPorts(params));
            System.out.println("ok");
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
            System.out.println(allNodes);
            return "ERROR: Nenhum node disponível aaaa";
        }

        try {
            Random rand = new Random();
            int nodePort = allNodes.get(rand.nextInt(allNodes.size()));
            InetAddress nodeAddress = InetAddress.getByName("localhost");

            System.out.printf("[Gateway] Encaminhando requisição %s para %s%n",
                    operation, nodePort);

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

    private static List<Integer> extractPorts(String nodesUpdate) {
        if (nodesUpdate == null || nodesUpdate.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Remove a parte após & se existir
            String portsPart = nodesUpdate.split("&")[0];

            return Arrays.stream(portsPart.split(","))
                    .map(entry -> entry.split(":")[0])  // Pega a parte antes do :
                    .map(String::trim)
                    .filter(portStr -> !portStr.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Erro ao processar string de nodes: " + nodesUpdate);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
