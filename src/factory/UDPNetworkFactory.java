package factory;

import components.Config;
import components.MembershipService;
import protocols.*;

public class UDPNetworkFactory implements NetworkFactory {
    @Override
    public Protocol createProtocol(int port) {
        return new UDPProtocol(port, null);
    }

    @Override
    public MessageHandler createMessageHandler(int port, MembershipService membershipService, Config config) {
        return new UDPMessageHandler(port, membershipService, config);
    }

    @Override
    public MessageHandler createGatewayMessageHandler(Config config) {
        return new UDPGatewayMessageHandler(config);
    }

    @Override
    public MessageHandler createTaskMessageHandler() {
        return new TaskMessageHandler();
    }
}