package udp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class TasksApp {
    private HashMap<String, String> tasks = new HashMap<>();
    public void addTask(String taskDescription) {
        try {
            FileWriter myWriter = new FileWriter("tasks.txt");
            myWriter.write(taskDescription + "\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("Erro ao abrir arquivo.");
        }
        tasks.put(taskDescription, "incomplete");

        System.out.println("Tarefa adicionada");
    }

    public HashMap<String, String> getTasks() {
        System.out.println(tasks);
        try {
            BufferedReader buffer = new BufferedReader(new FileReader("tasks.txt"));
            StringBuilder sb = new StringBuilder();
            String line = buffer.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = buffer.readLine();
            }
            String tasksFromFile = sb.toString();
            buffer.close();
        } catch (IOException e) {
            System.out.println("Erro ao abrir arquivo.");
        }
        return tasks;
    }
}
