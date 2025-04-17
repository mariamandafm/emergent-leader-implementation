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

public class TCPProtocol implements Protocol{
    private ServerSocket socket;
    private final int selfAddress;
    private boolean running = true;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private MessageHandler handler;

    public TCPProtocol(int selfAddress) {
        this.selfAddress = selfAddress;
        try {
            socket = new ServerSocket(selfAddress, 300);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override
    public void start() {
        new Thread(() -> {
            handler.setSocket(socket);
            while(running) {
                System.out.println("[TCPProtocol] Escutando na porta " + selfAddress);
                try {
                    Socket connection = socket.accept();
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String receivedMessage = input.readLine();
                    PrintWriter output = new PrintWriter(connection.getOutputStream(), true);

                    if (receivedMessage != null) {
                        messageQueue.offer(receivedMessage);
                        String response = handler.handle(receivedMessage);
                        if (response != null && !response.trim().isEmpty()) {
                            output.println(response);
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
