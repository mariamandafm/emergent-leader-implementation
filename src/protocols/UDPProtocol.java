package protocols;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class UDPProtocol implements Protocol {
    private DatagramSocket socket;
    private boolean running = true;
    private MessageHandler handler;
    private final int selfAddress;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public UDPProtocol(int selfAddress, MessageHandler handler) {
        this.selfAddress = selfAddress;
        try {

            socket = new DatagramSocket(selfAddress);
            socket.setSoTimeout(1000);

        } catch (Exception e) {
            throw new RuntimeException("Erro iniciando UDP socket", e);
        }
    }

    @Override
    public void start() {
        new Thread(() -> {
            handler.setSocket(socket);
            System.out.println("[UDPProtocol] Escutando na porta " + selfAddress);
            while (running) {
                try {
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);

                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    //System.out.println("[UDPProtocol " + selfAddress + "] Mensagem recebida: " + receivedMessage);

                    // Envia a mensagem pra fila
                    messageQueue.offer(receivedMessage);

                    // Processa também, se quiser manter lógica atual
                    String response = handler.handle(receivedMessage);
                    if (response != null && !response.trim().isEmpty()) {
                        byte[] responseBytes = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseBytes, responseBytes.length,
                                receivePacket.getAddress(), receivePacket.getPort());
                        socket.send(responsePacket);
                        //System.out.println("[UDPProtocol " + selfAddress + "] Resposta enviada: " + response);
                    }
                } catch (IOException e) {
//                    if (running) {
//                        System.err.println("Erro em UDPProtocol: " + e.getMessage());
//                    }
                }
            }
            socket.close();
        }).start();
    }


    @Override
    public void send(String message, InetAddress address, int port) throws IOException {
        byte[] sendBytes = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);
        socket.send(sendPacket);
    }

    @Override
    public void setHandler(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void stop() {
        running = false;
        if (socket != null) socket.close();
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
