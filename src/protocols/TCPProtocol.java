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
            socket = new ServerSocket(selfAddress, 600); // Servidor espera conexões na porta indicada
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override
    public void start() {
        new Thread(() -> {
            handler.setSocket(socket);
            System.out.println("[TCPProtocol] Escutando na porta " + selfAddress);
            while(running) {
                try {
                    Socket connection = socket.accept(); // Bloqueia e fica esperando uma conexão
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String receivedMessage = input.readLine();
                    PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
                    if (receivedMessage != null) {
                        messageQueue.offer(receivedMessage);
                        String response = handler.handle(receivedMessage);
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
            String receivedMessage = in.readLine();
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

    @Override
    public void sendHeartbeats(Config config) {
        new Thread(() -> {
            while (running) {
                // Se forr seed envia heartbeat para todos os nodes
                if (selfAddress == config.getSeedAddress()){
                    for (Integer nodePort : config.getUpNodes()) {
                        if (nodePort == selfAddress) continue;

                        try {
                            String message = "heartbeat;" + selfAddress;
                            send(message, InetAddress.getByName("localhost"), nodePort);

                        } catch (Exception e) {
                            System.out.println("[Node " + selfAddress + "] Erro ao enviar heartbeat: " + e.getMessage());
                        }
                    }
                } else {
                    try {
                        String message = "heartbeat;" + selfAddress;
                        byte[] data = message.getBytes();
                        send(message, InetAddress.getByName("localhost"), config.getSeedAddress());

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
