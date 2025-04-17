package protocols;

import java.net.DatagramSocket;
import java.net.InetAddress;

public interface MessageHandler {
    String handle(String message);

    void setSocket(DatagramSocket socket);
}
