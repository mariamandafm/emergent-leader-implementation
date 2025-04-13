package protocols;

import java.net.InetAddress;

public interface MessageHandler {
    String handle(String message);
}
