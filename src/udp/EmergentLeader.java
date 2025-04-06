package udp;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class EmergentLeader {
    public static void main(String[] args) throws InterruptedException {
        Config config = new Config(9001);

        Gateway gateway = new Gateway(config);
        gateway.start();

        Map<Integer, Node> nodes = new HashMap<>();

        Node node1 = new Node(config, 9001);
        node1.start();
        nodes.put(9001, node1);
        //config.addNode(node1);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Digite comandos para gerenciar nodes:");
        System.out.println("start <port> | stop <port> | list | exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            String[] parts = input.split(" ");
            String command = parts[0];

            switch (command) {
                case "start":
                    if (parts.length != 2) {
                        System.out.println("Uso: start <port>");
                        break;
                    }
                    int startPort = Integer.parseInt(parts[1]);
                    if (nodes.containsKey(startPort)) {
                        System.out.println("Node já está rodando.");
                    } else {
                        Node node = new Node(config, startPort);
                        node.start();

                        // TODO: Modificar isso, pois o seed node que deve deifinir a lista de nodes ativos
                        nodes.put(startPort, node);
                        System.out.println("Node " + startPort + " iniciado.");
                    }
                    break;

                case "stop":
                    if (parts.length != 2) {
                        System.out.println("Uso: stop <port>");
                        break;
                    }
                    int stopPort = Integer.parseInt(parts[1]);
                    Node nodeToStop = nodes.remove(stopPort);
                    if (nodeToStop != null) {
                        nodeToStop.stop();  // Você precisa implementar isso
                        System.out.println("Node " + stopPort + " parado.");
                    } else {
                        System.out.println("Node " + stopPort + " não está rodando.");
                    }
                    break;

                case "list":
                    System.out.println("Nodes ativos:");
                    for (Integer port : nodes.keySet()) {
                        System.out.println("- Node " + port);
                    }
                    break;

                case "exit":
                    System.out.println("Encerrando todos os nodes...");
                    for (Node node : nodes.values()) {
                        node.stop();
                    }
                    return;

                default:
                    System.out.println("Comando inválido.");
            }
        }
    }
}
