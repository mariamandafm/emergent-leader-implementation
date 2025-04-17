package imd.ufrn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;

class TestClient {

    public TestClient() {
        System.out.println("UDP Client Started");
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress inetAddress = InetAddress.getByName("localhost");
            byte[] sendMessage;
            byte[] receivemessage = new byte[1024];
            String message = "read;";
            sendMessage = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, 9000);
            clientSocket.send(sendPacket);

            DatagramPacket receivepacket = new DatagramPacket(receivemessage, receivemessage.length);
            clientSocket.receive(receivepacket);
            message = new String(receivepacket.getData());
            System.out.println(message);

            message = "add;Limpar Carro";
            sendMessage = message.getBytes();
            sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, 9000);
            clientSocket.send(sendPacket);

            Arrays.fill(receivemessage, (byte)0);
            clientSocket.receive(receivepacket);
            message = new String(receivepacket.getData());
            System.out.println(message);
            clientSocket.close();

            message = "add;Correr";
            sendMessage = message.getBytes();
            sendPacket = new DatagramPacket(
                    sendMessage, sendMessage.length,
                    inetAddress, 9000);
            clientSocket.send(sendPacket);

            //Receber resposta do Servidor - Saldo obtido
            Arrays.fill(receivemessage, (byte)0);
            clientSocket.receive(receivepacket);
            message = new String(receivepacket.getData());
            System.out.println(message);
            clientSocket.close();



        } catch (IOException ex) {
        }
        System.out.println("UDP Client Terminating ");
    }

    public static void main(String args[]) {
        new TestClient();
    }
}