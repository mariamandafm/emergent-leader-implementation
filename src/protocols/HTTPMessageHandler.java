package protocols;

import components.Config;
import components.MembershipService;
import utils.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.StringTokenizer;

public class HTTPMessageHandler implements MessageHandler{
    private int port;

    private final MembershipService membershipService;

    private ServerSocket socket;

    private final Config config;

    public HTTPMessageHandler(int port, MembershipService membershipService, Config config) {
        this.port = port;
        this.config = config;
        this.membershipService = membershipService;
    }

    public void setSocket(DatagramSocket socket) {
        //this.socket = socket;
    }

    @Override
    public void setSocket(ServerSocket socket) {
        this.socket = socket;
    }

    @Override
    public String handle(String message) {
        if (message == null || message.trim().isEmpty()) {
            System.out.println(port + " Recebida mensagem vazia. Ignorando.");
            return null;  // Retorno null indica que não deve enviar resposta
        }


        String response = processMessage(message);

        // Se for uma mensagem de heartbeat, não responda
        if (message.startsWith("heartbeat;")) {
            return null;
        }

        return response;
    }


    private String processMessage(String message) {
        try {
            String[] lines = message.split("\n");
            String messageType = lines[0].trim(); // ex: "GET /add?x=1 HTTP/1.1" ou "HTTP/1.1 200 OK"
            StringTokenizer tokenizer = new StringTokenizer(messageType);

            String operation = "";
            String version = "";
            String params = "";
            String data = "";

            if (isHttpResponse(messageType)) {
                String statusCode = messageType.split(" ")[1];  // "200"

                if (Objects.equals(statusCode, "202")){
                    data = lines[lines.length-1];
                    operation = "accepted";
                    String [] infos = data.split(";");
                    params = infos[0];
                    version = infos[1];

                }
                System.out.println("Recendo response http: "+data);
            } else {
                // Processar como request
                String httpMethod = tokenizer.nextToken();
                String httpRoute = tokenizer.nextToken();

                String[] actions = httpRoute.split("\\?", 2);
                operation = actions[0];
                params = actions.length > 1 ? actions[1] : "";

                System.out.println("Requisição HTTP: método=" + httpMethod + " operação=" + operation + " params=" + params);
            }

            switch (operation) {
                case "/add":
                case "/read":
                    return forwardToTaskServer(message);
                case "/join_request":
                    // se for seed
                    if (port == config.getSeedAddress()){
                        int newNodeAddress = Integer.parseInt(params.substring(0, params.length() - 1));
                        System.out.println("Processando join request de: " + newNodeAddress);
                        membershipService.handleNewJoin(newNodeAddress, config);
                        config.setUpNodes(membershipService.membership.getUpNodesAddress());
                        String updatedMembership = membershipService.membership.getSerializedMembership();
                        return createHttpResponse(202, updatedMembership);
                    } else {
                        return createHttpResponse(402,"Apenas seed pode processar pedidos de join");
                    }
                case "/membership_update":
                    System.out.println("[ " + port + " ] Recebendo atualização de join ");
                    String[] newMembership = params.split("&");
                    membershipService.membership.deserializeMembershipAndUpdate(newMembership[0], newMembership[1]);
                    return createHttpResponse(200, "ack");
                case "accepted":
                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
                    return createHttpResponse(200, "ack");
                case "/heartbeat":
                    int senderPort = Integer.parseInt(params);
                    membershipService.receiveHeartbeat(senderPort);
                    return createHttpResponse(200, null);
                default:
                    return createHttpResponse(400, "Operação inválida - " + operation);
            }
        } catch (Exception e) {
            return createHttpResponse(400, e.getMessage());
        }
    }

    private String forwardToTaskServer(String message) {
        try (Socket clientSocket = new Socket("localhost", 9005)) {
            clientSocket.setSoTimeout(5000); // timeout de resposta

            // Enviar a mensagem
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
            output.println(message);

            // Receber a resposta
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String reply = readHttpRequest(input);

            if (reply == null) {
                return createHttpResponse(500,"TaskServer fechou conexão sem responder");
            }
            return reply;

        } catch (SocketTimeoutException e) {
            return createHttpResponse(500,"TaskServer não respondeu (timeout)");
        } catch (IOException e) {
            return createHttpResponse(500,"Erro ao comunicar com TaskServer: " + e.getMessage());
        }
    }

    private boolean isHttpResponse(String messageTypeLine) {
        return messageTypeLine.startsWith("HTTP/");
    }

    private String createHttpResponse(int statusCode, String content){
        HttpStatus status = HttpStatus.fromCode(statusCode);
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.0 " + status.toString() + "\r\n");
        response.append("Server: WebServer\r\n");
        response.append("Content-Type: text/html\r\n");

        if (content != null){
            response.append("Content-Length: ").append(content.length()).append("\r\n");
            response.append("\r\n");
            response.append(content);
        }
        return response.toString();
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

    public String extractHttpResponseBody(String httpResponse) {
        // Divide os headers do corpo usando a quebra dupla padrão (\r\n\r\n ou \n\n)
        String[] parts = httpResponse.split("\\r?\\n\\r?\\n", 2);

        if (parts.length < 2) {
            return ""; // Nenhum corpo presente
        }

        String headers = parts[0];
        String body = parts[1];

        // Verifica se há Content-Length
        int contentLength = -1;
        for (String headerLine : headers.split("\\r?\\n")) {
            if (headerLine.toLowerCase().startsWith("content-length")) {
                try {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                } catch (NumberFormatException e) {
                    contentLength = -1;
                }
                break;
            }
        }

        // Se o Content-Length for válido e o corpo tiver o tamanho esperado, retorna o corpo correto
        if (contentLength >= 0 && body.length() >= contentLength) {
            return body.substring(0, contentLength);
        }

        // Caso contrário, retorna tudo o que tem como corpo
        return body;
    }

}
