package udp;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TasksApp {
    private List<String> tasks = new ArrayList<>();
    private static final String FILENAME = "tasks.txt";

    public TasksApp() {
        loadTasksFromFile();
    }

    public String addTask(String taskDescription) {
        try (FileWriter fw = new FileWriter(FILENAME, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(taskDescription);

            tasks.add(taskDescription);

        } catch (IOException e) {
            System.out.println("Erro ao adicionar tarefa no arquivo: " + e.getMessage());
        }
        return "Tarefa adicionada: " + taskDescription;
    }

    public String getTasks() {
        loadTasksFromFile();

        if (tasks.isEmpty()) {
            return "Nenhuma tarefa cadastrada.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("LISTA DE TAREFAS\n");
        sb.append("================\n");

        for (int i = 0; i < tasks.size(); i++) {
            sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
        }

        return sb.toString();
    }

    private void loadTasksFromFile() {
        tasks.clear();

        File file = new File(FILENAME);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !tasks.contains(line)) {
                    tasks.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao carregar tarefas: " + e.getMessage());
        }
    }
}
