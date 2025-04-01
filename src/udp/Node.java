package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.StringTokenizer;

public class Node {
    // ..
    private final Config config;
    MembershipService membershipService;
    private int port;
    //private TasksApp tasksApp;

    public Node(Config config, int port) {
        this.config = config;
        this.port = port;
    }

    public void start() {
        this.membershipService = new MembershipService(port);
        membershipService.join(config.getSeedAddress());

        startService();
    }

    public void startService() {
        new Thread(() -> {
            TasksApp tasksApp = new TasksApp();
            System.out.println("Node iniciando na porta " + port);
            //this.membershipService = new MembershipService(getListenAddress());
            //membershipService.join(config.getSeedAddress());
            try {
                //membershipService.join(config.getSeedAddress());
                DatagramSocket serverSocket = new DatagramSocket(port);
                String operationMsg;
                while(true){
                    byte[] receiveMessage = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
                    serverSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("[Node " + port + "]" + "Received from client: " + message+ "\nFrom: " + receivePacket.getAddress());
                    operationMsg = message;

                    StringTokenizer tokenizer = new StringTokenizer(message, ";");
                    String operation = null;
                    String params = null;
                    while (tokenizer.hasMoreElements()){
                        operation = tokenizer.nextToken();
                        params = tokenizer.nextToken();
                    }

                    switch (operation) {
                        case "add":
                            tasksApp.addTask(params);
                            break;
                        case "read":
                            tasksApp.getTasks();
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Node terminando");
            }
        }).start();

    }


//    public static void main(String[] args) {
//
//        if (args.length < 1) {
//            System.out.println("Uso: java Node <porta>");
//            return;
//        }
//        int port = Integer.parseInt(args[0]);
//        new Node(port).start();
//    }
}