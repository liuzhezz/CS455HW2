package cs455.scaling.pool;

public class ThreadPool extends Thread{

    private Queue workerQueue;
    private Queue taskQueue;
    private Queue flushTaskQueue;
    private boolean isStopped;
    private int maxNoOfWorkers;
    private int batchSize;
    private int batchTime;

    public ThreadPool(int threadPoolSize, int batchSize, int batchTime) {
        this.maxNoOfWorkers = threadPoolSize;
        this.batchSize = batchSize;
        this.batchTime = batchTime;
        workerQueue = new Queue(threadPoolSize);
        taskQueue = new Queue();
        flushTaskQueue = new Queue(batchSize);
        isStopped = false;
    }

    public void initiate(){
        for(int i = 0; i < maxNoOfWorkers; i++){
            if(this.isStopped)throw
                    new IllegalStateException("ThreadPool is stopped");
            try {
                Worker worker = new Worker(workerQueue, flushTaskQueue, i);
                Thread thread = new Thread(worker);
                thread.setName("Worker" + String.valueOf(i));
                thread.start();
                this.workerQueue.enqueue(worker);

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void run(){
        long tStart = System.currentTimeMillis();
        long tEnd;
        long tDelta;
        while(true){
            try{

                //one worker with a flushTaskQueue, decided by the batch size of batch time.
                Worker worker = (Worker) workerQueue.dequeue();
                /*
                if(taskQueue.size()>=batchSize){
                    //if batch size reach
                    for (int i0 = 0; i0 < batchSize; i0++){
                        Task newTask = (Task)taskQueue.dequeue();
                        worker.assignTask(newTask);
                    }
                    tStart = System.currentTimeMillis();
                }
                else if (taskQueue.size()<batchSize&&taskQueue.size()!=0){
                    //not enough task, exam the time elapsed
                    tEnd = System.currentTimeMillis();
                    //time elapsed -> tDelta
                    tDelta = tEnd-tStart;
                    if (tDelta>(batchTime*1000)) {
                        //reach batch time, flush all task in queue
                        for (int i0 = 0; i0 < taskQueue.size(); i0++) {
                            Task newTask = (Task) taskQueue.dequeue();
                            worker.assignTask(newTask);
                        }
                        tStart = System.currentTimeMillis();
                    }
                    else {
                        //does not reach, sleep a mount and flush all
                        Thread.sleep((batchTime*1000)-tDelta);
                        for (int i0 = 0; i0 < taskQueue.size(); i0++) {
                            Task newTask = (Task) taskQueue.dequeue();
                            worker.assignTask(newTask);
                        }
                        tStart = System.currentTimeMillis();
                    }
                }
                else {
                    workerQueue.enqueue(worker);
                }
*/
                Task task = (Task)taskQueue.dequeue();
                worker.assignTask(task);
                synchronized(worker){
                    worker.notify();
                }
            }catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error in retrieving task!");
            }
        }
    }

    public void addTask(Task work){
        try {
            taskQueue.enqueue(work);
        } catch (InterruptedException ex) {
            System.out.println("Error in adding task!");
        }
    }

    public static void main(String args[]){
        ThreadPool tp = new ThreadPool(10,10,10);
        tp.initiate();
    }
}

