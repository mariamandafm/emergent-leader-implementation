package components;


import factory.NetworkFactory;
import protocols.*;

public class TaskServer {
    private final int PORT = 9005;
    private final TasksApp tasksApp = new TasksApp();
    private Protocol protocol;
    private MessageHandler handler;

//    public TaskServer(Protocol protocol, MessageHandler handler) {
//        this.protocol = protocol;
//        this.handler = handler;
//    }

    public TaskServer(NetworkFactory factory) {
        this.protocol = factory.createProtocol(PORT);
        this.handler = factory.createTaskMessageHandler();
        protocol.setHandler(handler);
    }

    public void start(){
        System.out.println("TaskServer iniciado na porta " + PORT);
        protocol.start();
    }
}
