package protocols;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;

public interface MessageHandler {
    String handle(String message);

    void setSocket(DatagramSocket socket);

    void setSocket(ServerSocket socket);
}
