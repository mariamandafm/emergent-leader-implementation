package protocols;

import components.Config;
import components.MembershipService;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class TCPMessageHandler implements MessageHandler{
    private int port;

    private final MembershipService membershipService;

    private ServerSocket socket;

    private final Config config;

    public TCPMessageHandler(int port, MembershipService membershipService, Config config) {
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
            StringTokenizer tokenizer = new StringTokenizer(message, ";");
            String operation = tokenizer.nextToken();
            String params = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

            String version = "";

            switch (operation) {
                case "add":
                case "read":
                    return forwardToTaskServer(message);
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
                    break;
                case "membership_update":
                    System.out.println("[ " + port + " ] Recebendo atualização de join ");
                    version = tokenizer.nextToken();
                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
                    return "ack;";
                case "accepted":
                    version = tokenizer.nextToken();
                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
                    return "ack;";
                case "heartbeat":
                    int senderPort = Integer.parseInt(params);
                    membershipService.receiveHeartbeat(senderPort);
                    return "";
                default:
                    System.out.println(port+ "Erro: Operação inválida - " + operation);
                    return "";
            }
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
        return "";
    }

    private String forwardToTaskServer(String message) {
        try (Socket clientSocket = new Socket("localhost", 9005)) {
            clientSocket.setSoTimeout(2000); // timeout de resposta

            // Enviar a mensagem
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
            output.println(message);

            // Receber a resposta
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String reply = input.readLine();

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
}
