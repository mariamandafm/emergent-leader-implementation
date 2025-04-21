package protocols;

import components.TasksApp;
import utils.HttpStatus;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.StringTokenizer;

public class HTTPTaskMessageHandler implements MessageHandler{
    private final TasksApp tasksApp = new TasksApp();

    @Override
    public String handle(String message) {
        return processMessage(message);
    }

    private String processMessage(String message) {
        try {
            String[] lines = message.split("\n");
            String messageType = lines[0];
            StringTokenizer tokenizer = new StringTokenizer((messageType));
            String httpMethod = tokenizer.nextToken();
            String httpRoute = tokenizer.nextToken();

            String[] actions = httpRoute.split("\\?");
            String operation = actions[0];
            String params = actions.length > 1 ? actions[1] : "";

            switch (operation) {
                case "/add":
                    if (!httpMethod.equals("POST")){
                        return createHttpResponse(405,"Métodos suportados: POST.");
                    }
                    String data = lines[lines.length-1];
                    if (data.isEmpty()) {
                        return createHttpResponse(400,"Nenhuma tarefa especificada");
                    }
                    tasksApp.addTask(data);
                    return createHttpResponse(200,"Tarefa adicionada: " + data);
                case "/read":
                    if (!httpMethod.equals("GET")){
                        return createHttpResponse(405,"Métodos suportados: GET.");
                    }
                    return createHttpResponse(200, tasksApp.getTasks());
                default:
                    return createHttpResponse(400,"Operação inválida");
            }
        } catch (Exception e) {
            return createHttpResponse(400, e.getMessage());
            //return "[TaskServer] Erro: " + e.getMessage();
        }
    }

    private String createHttpResponse(int statusCode, String content){
        HttpStatus status = HttpStatus.fromCode(statusCode);
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.0 " + status.toString() + "\r\n");
        response.append("Server: WebServer\r\n");
        response.append("Content-Type: text/html\r\n");

        if (content != null){
            response.append("Content-Length: ").append(content.length()).append("\r\n");
            response.append("\r\n");
            response.append(content);
        }
        return response.toString();
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