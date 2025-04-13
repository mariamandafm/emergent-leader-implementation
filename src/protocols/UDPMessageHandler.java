package protocols;

import udp.Config;
import udp.MembershipService;
import udp.TasksApp;

import java.net.InetAddress;
import java.util.StringTokenizer;

public class UDPMessageHandler implements MessageHandler{
    private int port;
    private final TasksApp tasksApp;

    private final MembershipService membershipService;

    private final Config config;

    public UDPMessageHandler(int port, MembershipService membershipService, TasksApp tasksApp, Config config) {
        this.port = port;
        this.tasksApp = tasksApp;
        this.config = config;
        this.membershipService = membershipService;
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
                    break;
                case "membership_update":
                    System.out.println("[ " + port + " ] Recebendo artualização de join #######");
                    version = tokenizer.nextToken();
                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
                    System.out.println(membershipService.membership.getLiveMembers());
                    return "ack;";
                case "accepted":
                    version = tokenizer.nextToken();
                    System.out.println(membershipService.membership.getSerializedMembership());
                    System.out.println("Atualizando membership do node");
                    membershipService.membership.deserializeMembershipAndUpdate(params, version);
                    System.out.println(membershipService.membership.getSerializedMembership());
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
}
