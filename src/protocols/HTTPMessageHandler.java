package protocols;

import components.Config;
import components.MembershipService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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
//            StringTokenizer tokenizer = new StringTokenizer(message, ";");
//            String operation = tokenizer.nextToken();
//            String params = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
            String[] lines = message.split("\n");
            String messageType = lines[0];
            StringTokenizer tokenizer = new StringTokenizer((messageType));
            String httpMethod = tokenizer.nextToken();
            String httpRoute = tokenizer.nextToken();

            String[] actions = httpRoute.split("\\?");
            String operation = actions[0];
            String params = actions.length > 1 ? actions[1] : "";

            switch (operation) {
                case "/add":
                case "/read":
                    return forwardToTaskServer(message);
                case "/join-request":
                    // se for seed
                    if (port == config.getSeedAddress()){
                        int newNodeAddress = Integer.parseInt(params);
                        System.out.println("Processando join request de: " + newNodeAddress);
                        membershipService.handleNewJoin(newNodeAddress, config);
                        config.setUpNodes(membershipService.membership.getUpNodesAddress());
                        String updatedMembership = membershipService.membership.getSerializedMembership();
                        return "/accepted" + updatedMembership;
                    } else {
                        System.out.println("Apenas seed pode processar pedidos de join");
                    }
                    break;
//                case "/membership_update":
//                    System.out.println("[ " + port + " ] Recebendo atualização de join ");
//                    version = tokenizer.nextToken();
//                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
//                    return "ack;";
//                case "/accepted":
//                    version = tokenizer.nextToken();
//                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
//                    return "ack;";
                case "/heartbeat":
                    int senderPort = Integer.parseInt(params);
                    membershipService.receiveHeartbeat(senderPort);
                    return "";
                default:
                    System.out.println(port+ "ERROR: Operação inválida - " + operation);
                    return "";
            }
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
        return "";
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
                return "Erro: TaskServer fechou conexão sem responder";
            }
            //String reply = new String(responseBuffer, 0, read);
            //String reply = "Resposta do servidor task";
            return reply;

        } catch (SocketTimeoutException e) {
            return "Erro: TaskServer não respondeu (timeout)";
        } catch (IOException e) {
            return "Erro ao comunicar com TaskServer: " + e.getMessage();
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
