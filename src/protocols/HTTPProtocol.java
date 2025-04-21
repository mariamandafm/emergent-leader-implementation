package protocols;

import components.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class HTTPProtocol implements Protocol{
    private ServerSocket socket;
    private final int selfAddress;
    private boolean running = true;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private MessageHandler handler;

    public HTTPProtocol(int selfAddress) {
        this.selfAddress = selfAddress;
        try {
            socket = new ServerSocket(selfAddress, 600); // Servidor espera conexões na porta indicada
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override
    public void start() {
        new Thread(() -> {
            handler.setSocket(socket);
            System.out.println("[HTTPProtocol] Escutando na porta " + selfAddress);
            while(running) {
                try {
                    Socket connection = socket.accept(); // Bloqueia e fica esperando uma conexão
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    String receivedMessage = readHttpRequest(input);

                    PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
                    System.out.println("[" + selfAddress + "]: "+ receivedMessage);
                    messageQueue.offer(receivedMessage);
                    String response = handler.handle(receivedMessage);
                    if (response != null && !response.trim().isEmpty()) {
                        System.out.println("[" + selfAddress + "] Response: "+ response);
                        output.println(response);
                        connection.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

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

    @Override
    public void send(String message, InetAddress address, int port) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(message, ";");
        String request = tokenizer.nextToken();
        String params = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
        String version = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
        String httpMessage = createHttpMessage("GET", "/"+request + "?" + params+ "&"+version, null);

        try (Socket socket = new Socket(address, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(httpMessage);

            String receivedMessage = readHttpRequest(in);
            if (receivedMessage != null) {
                messageQueue.offer(receivedMessage);
                String response = handler.handle(receivedMessage);
                if (response != null && !response.trim().isEmpty()) {
                    System.out.println("[" + selfAddress + "]: " + response);
                }
            }
        }
    }

    @Override
    public void setHandler(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao encerrar TCPProtocol: " + e.getMessage());
        }
    }

    @Override
    public String waitForMessage(Predicate<String> condition, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null && condition.test(message)) {
                    return message;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private String createHttpMessage(String method, String route, String content){
        StringBuilder request = new StringBuilder();
        request.append(method.toUpperCase() + " " + route + " HTTP/1.1\r\n");
        request.append("\r\n");
        if (method.toUpperCase().equals("POST")){
            request.append(content + "\r\n");
        }
        return request.toString();
    }

    @Override
    public void sendHeartbeats(Config config) {
        new Thread(() -> {
            while (running) {
                // Se forr seed envia heartbeat para todos os nodes
                if (selfAddress == config.getSeedAddress()){
                    for (Integer nodePort : config.getUpNodes()) {
                        if (nodePort == selfAddress) continue;

                        try {
                            //String message = "heartbeat;" + selfAddress;
                            String message = createHttpMessage("GET", "/heartbeat?"+selfAddress, null);
                            try (Socket socket = new Socket(InetAddress.getByName("localhost"), nodePort);
                                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                                out.println(message);
                            }
                            //send(message, InetAddress.getByName("localhost"), nodePort);

                        } catch (Exception e) {
                            System.out.println("[Node " + selfAddress + "] Erro ao enviar heartbeat: " + e.getMessage());
                        }
                    }
                } else {
                    try {
                        String message = createHttpMessage("GET", "/heartbeat?"+selfAddress, null);
                        try (Socket socket = new Socket(InetAddress.getByName("localhost"), config.getSeedAddress());
                             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                            out.println(message);
                        }

                    } catch (Exception e) {
                        System.out.println("[Node " + selfAddress + "] Erro ao enviar heartbeat: " + e.getMessage());
                    }
                }

                try {
                    Thread.sleep(6000); // envia heartbeat a cada 3s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}