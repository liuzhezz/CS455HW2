package cs455.scaling.pool;



/**
 * Worker Thread
 */
public class Worker extends Thread{
    private final TaskQueue taskQueue;
    Worker(int id, TaskQueue taskQueue) {
        this.setName("Worker #" + id);
        this.taskQueue = taskQueue;
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                // get() will block until a new task is available in the taskQueue
                Task task = taskQueue.get();
                task.run();
            } catch (StopException e) {
                break;
            }
        }
    }


}