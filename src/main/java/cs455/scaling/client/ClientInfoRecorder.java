package cs455.scaling.client;

import java.time.LocalTime;

public class ClientInfoRecorder extends Thread{
    private volatile int pkgSent;
    private volatile int pkgReceive;

    ClientInfoRecorder(){
        pkgReceive = 0;
        pkgSent = 0;
    }

    public void sentPlusOne(){
        pkgSent++;
    }

    public void receivePlusOne(){
        pkgReceive++;
    }

    @Override
    public void run(){
        while (true){
            try{
                Thread.sleep(20*1000);
            }
            catch (InterruptedException ie){
                System.err.println(ie.getMessage());
            }

            System.out.println("[" + LocalTime.now() + "]" + " Total Sent Count: " + pkgSent + ", Total Received Count: " + pkgReceive);
        }
    }
}
