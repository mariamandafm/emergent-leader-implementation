package protocols;

import components.TasksApp;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.StringTokenizer;

public class TaskMessageHandler implements MessageHandler{
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
                        return "[TaskServer] Erro: Nenhuma tarefa especificada";
                    }
                    tasksApp.addTask(params);
                    return "[TaskServer] Tarefa adicionada: " + params;

                case "read":
                    return tasksApp.getTasks();
                default:
                    System.out.println("[TaskServer] Erro: Operação inválida - " + operation);
                    return "";
            }
        } catch (Exception e) {
            return "[TaskServer] Erro: " + e.getMessage();
        }
    }

    @Override
    public void setSocket(DatagramSocket socket) {
        // ...
    }

    @Override
    public void setSocket(ServerSocket socket) {
        //
    }
}
