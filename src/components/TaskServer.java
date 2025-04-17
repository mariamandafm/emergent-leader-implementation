package components;


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

    public TaskServer() {
        this.protocol = new UDPProtocol(PORT, handler);
        this.handler = new UDPTaskMessageHandler();
        protocol.setHandler(handler);
    }

    public void start(){
        System.out.println("TaskServer iniciado na porta " + PORT);
        protocol.start();
    }
}
