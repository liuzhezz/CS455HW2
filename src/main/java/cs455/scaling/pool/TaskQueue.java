package cs455.scaling.pool;

import java.util.LinkedList;

public class TaskQueue {

    private final LinkedList<Task> tasks = new LinkedList<>();

    public void add(Task task) {
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                tasks.add(task);
                tasks.notifyAll();
            } else {
                tasks.add(task);
            }
        }
    }

    public Task get() {
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                try {
                    tasks.wait();
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }
            return tasks.remove();
        }
    }
}
