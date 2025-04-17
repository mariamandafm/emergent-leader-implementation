package protocols;

import components.TasksApp;

import java.net.DatagramSocket;
import java.util.StringTokenizer;

public class UDPTaskMessageHandler implements MessageHandler{
    private final TasksApp tasksApp = new TasksApp();

    @Override
    public String handle(String message) {
        return processMessage(message);
    }

    private String processMessage(String message) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(message, ";");
            String operation = tokenizer.nextToken();
            String params = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

            String version = "";

            switch (operation) {
                case "add":
                    if (params.isEmpty()) {
                        return "Erro: Nenhuma tarefa especificada";
                    }
                    tasksApp.addTask(params);
                    return "Tarefa adicionada: " + params;

                case "read":
                    return tasksApp.getTasks();
                default:
                    System.out.println("Erro: Operação inválida - " + operation);
                    return "";
            }
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    @Override
    public void setSocket(DatagramSocket socket) {
        // ...
    }
}
