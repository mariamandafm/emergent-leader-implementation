package factory;

import components.Config;
import components.MembershipService;
import protocols.MessageHandler;
import protocols.Protocol;

public interface NetworkFactory {
    Protocol createProtocol(int port);

    MessageHandler createMessageHandler(int port, MembershipService membershipService, Config config);

    MessageHandler createGatewayMessageHandler(Config config);

    MessageHandler createTaskMessageHandler();
}
