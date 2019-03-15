package cs455.scaling.pool;

import java.util.LinkedList;

public class Queue {

    private LinkedList<Object> queue;
    private int limit;

    public Queue(int limit) {
        this.limit = limit;
        queue = new LinkedList<Object>();
    }

    public Queue() {
        queue = new LinkedList<Object>();
    }

    public synchronized void enqueue(Object item)
            throws InterruptedException {
        if (this.queue.size() == 0) {
            this.notifyAll();
        }
        this.queue.add(item);
        //return this.queue.get(this.queue.size()-1);
    }

    public synchronized Object dequeue()
            throws InterruptedException {
        while (this.queue.size() == 0) {
            this.wait();
        }
        return this.queue.remove(0);
    }

    public LinkedList<Object> getQueue() {
        return queue;
    }

    public synchronized int size(){
        return this.queue.size();
    }
}
