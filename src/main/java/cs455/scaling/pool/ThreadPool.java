package cs455.scaling.pool;

public class ThreadPool implements AutoCloseable{
    private final int numOfThreads;
    private final TaskQueue taskQueue;

    public ThreadPool(int numOfThreads) {
        this.numOfThreads = numOfThreads;
        this.taskQueue = new TaskQueue();
        for (int i = 0; i < numOfThreads; i++) {
            Worker worker = new Worker(i, taskQueue);
        }
    }

    public void submit(Task task) {
        taskQueue.add(task);
    }

    @Override
    public void close() throws Exception {
        for (int i = 0; i < numOfThreads; i++) {
            submit(() -> {
                throw new StopException();
            });
        }
    }
}

