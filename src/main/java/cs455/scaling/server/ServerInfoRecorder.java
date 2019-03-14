package cs455.scaling.server;

import java.nio.channels.SelectionKey;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Set;

public class ServerInfoRecorder extends Thread {
    private volatile Integer activeClient;
    private volatile Integer msgProcessed;
    private  volatile Hashtable<SelectionKey, Integer> clientData;


    ServerInfoRecorder(){
        activeClient = 0;
        msgProcessed = 0;
        clientData = new Hashtable<>();
    }

    public int getActiveClient() {
        synchronized(activeClient){
            return activeClient;
        }

    }

    public void addSelectorKey(SelectionKey newClient){
        if (clientData.contains(newClient)){
            System.err.println("Client already register in server.");
        }
        else{
            clientData.put(newClient,0);
        }
    }

    public void addClientMsg(SelectionKey selectionKey){
        int origin = clientData.get(selectionKey);
        origin++;
        clientData.put(selectionKey,origin);
    }

    public int getMsgProcessed() {
        synchronized(msgProcessed){
            return msgProcessed;
        }
    }

    public void resetMsgProcessed(){
        synchronized(msgProcessed){
            msgProcessed = 0;
        }
    }

    public void resetClientData(){
        Set<SelectionKey> sk = clientData.keySet();
        for (SelectionKey tempSK : sk){
            clientData.put(tempSK,0);
        }
    }

    public void clientPlusOne(){
        synchronized (activeClient){
            activeClient++;
        }
    }

    public void msgProcessedPlusOne(){
        synchronized (msgProcessed){
            msgProcessed++;
        }
    }

    public int calMean(Hashtable<SelectionKey,Integer> clientData){
        Set<SelectionKey> sk = clientData.keySet();
        int totalMsgNumber = 0;
        for(SelectionKey tempSK : sk){
            totalMsgNumber += clientData.get(tempSK);
        }
        int result = (totalMsgNumber/20)/getActiveClient();
        return result;
    }

    public double calStdDev(Hashtable<SelectionKey,Integer> clientData){
        int mean = calMean(clientData);
        Set<SelectionKey> sk = clientData.keySet();
        int sum = 0;
        for (SelectionKey tempSK : sk){
            sum += Math.pow(((clientData.get(tempSK)/20)-mean),2);
        }
        return Math.sqrt(sum/getActiveClient());
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

            System.out.println(LocalTime.now() + " Server Throughput: " + getMsgProcessed()/20 + " messages/s, Active Client Connections: "
            + getActiveClient() + ", Mean Per-client Throughput: " + calMean(clientData) + " messages/s, Std. Dev. of Per-client Throughput: " +
                    calStdDev(clientData) + " messages/s.");
            resetClientData();
            resetMsgProcessed();


        }
    }
}
