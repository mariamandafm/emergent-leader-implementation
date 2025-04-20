package factory;

import components.Config;
import components.MembershipService;
import protocols.*;

public class HTTPNetworkFactory implements NetworkFactory {
    @Override
    public Protocol createProtocol(int port) {
        return new HTTPProtocol(port);
    }

    @Override
    public MessageHandler createMessageHandler(int port, MembershipService membershipService, Config config) {
        return new HTTPMessageHandler(port, membershipService, config);
    }

    @Override
    public MessageHandler createGatewayMessageHandler(Config config) {
        return new HTTPGatewayMessageHandler(config);
    }

    @Override
    public MessageHandler createTaskMessageHandler() {
        return new HTTPTaskMessageHandler();
    }
}
