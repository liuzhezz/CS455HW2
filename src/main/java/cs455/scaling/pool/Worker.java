package cs455.scaling.pool;

import java.util.LinkedList;

public class Worker extends Thread {

    private Queue workerQueue;
    private Queue taskQueue;
    private Task task;
    private int id;

    public Worker(Queue queue, Queue taskQueue, int id) {
        this.workerQueue = queue;
        this.taskQueue = taskQueue;
        this.id = id;
    }

    public void run() {
        synchronized(this){
            while(true){
                try{
                    this.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    System.out.println("failed to notify");
                }
/*
                try{
                    for (int i0 = 0; i0 < taskQueue.size(); i0++){
                        Task tempTask = (Task) taskQueue.dequeue();
                        tempTask.func();
                        System.out.println("worker is working");
                    }
                }
                catch (InterruptedException ie){
                    System.err.println(ie.getMessage());
                }
*/
                task.func();
                try {
                    this.workerQueue.enqueue(this);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void assignTask(Task task){
        this.task = task;
        /*
        try {
            taskQueue.enqueue(task);
        }
        catch (InterruptedException ie){
            System.err.println(ie.getMessage());
        }
*/
    }
}