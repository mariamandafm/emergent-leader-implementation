package factory;

import components.Config;
import components.MembershipService;
import protocols.*;

public class TCPNetworkFactory implements NetworkFactory {
    @Override
    public Protocol createProtocol(int port) {
        return new TCPProtocol(port);
    }

    @Override
    public MessageHandler createMessageHandler(int port, MembershipService membershipService, Config config) {
        return new TCPMessageHandler(port, membershipService, config);
    }

    @Override
    public MessageHandler createGatewayMessageHandler(Config config) {
        return new TCPGatewayMessageHandler(config);
    }
}
