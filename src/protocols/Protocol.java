package protocols;

import components.Config;
import components.Membership;
import components.MembershipService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.function.Predicate;

public interface Protocol {

//    void sendMessage(String message, InetAddress address, int port) throws IOException;
//
//    String receiveMessage();

    void start();

    void send(String message, InetAddress address, int port) throws IOException;

    void setHandler(MessageHandler handler);

    void stop();

    String waitForMessage(Predicate<String> condition, int timeoutMs);

    void sendHeartbeats(Membership membership);
}
