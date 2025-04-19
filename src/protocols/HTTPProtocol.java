package protocols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

                    StringBuilder receivedMessage = new StringBuilder();
                    int contentLenght = 0;
                    String messageBody = "";
                    //String receivedMessage = input.readLine();
                    String line;
                    while ((line = input.readLine()) != null){
                        if (line.trim().isEmpty()) break;
                        if (line.toLowerCase().startsWith("content-length")) {
                            contentLenght = Integer.parseInt(line.split(":")[1].trim());
                        }
                        receivedMessage.append(line).append("\n");
                    }

                    if (contentLenght > 0) {
                        char[] body = new char[contentLenght];
                        input.read(body, 0, contentLenght);
                        messageBody = new String(body);
                    }
                    System.out.println("Body: " + messageBody);
                    PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
                    System.out.println("[" + selfAddress + "]: "+ receivedMessage.toString());
                    if (receivedMessage != null) {
                        messageQueue.offer(receivedMessage.toString());
                        String response = handler.handle(receivedMessage.toString());
                        if (response != null && !response.trim().isEmpty()) {
                            output.println(response);
                            connection.close();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public void send(String message, InetAddress address, int port) throws IOException {
        try (Socket socket = new Socket(address, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(message);
            // Opcional: ler resposta
//            String response = in.readLine();
//            if (response != null) {
//                messageQueue.offer(response);
//            }
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
}