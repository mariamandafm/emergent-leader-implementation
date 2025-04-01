package imd.ufrn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

class TestClient {

    public TestClient() {
        System.out.println("UDP Client Started");
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress inetAddress = InetAddress.getByName("localhost");
            byte[] sendMessage;
            String message = "add;Limpar Carro";
            sendMessage = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, 9001);
            clientSocket.send(sendPacket);

            message = "add;Correr";
            sendMessage = message.getBytes();
            sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, 9001);
            clientSocket.send(sendPacket);

            message = "read;";
            sendMessage = message.getBytes();
            sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, 9001);
            clientSocket.send(sendPacket);

            clientSocket.close();


        } catch (IOException ex) {
        }
        System.out.println("UDP Client Terminating ");
    }

    public static void main(String args[]) {
        new TestClient();
    }
}