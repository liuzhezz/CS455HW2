package cs455.scaling.server;

import java.nio.channels.SelectionKey;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerInfoRecorder extends Thread {
    private volatile Integer activeClient;
    private volatile Integer msgProcessed;
    private  volatile ConcurrentHashMap<String, Integer> clientData;


    ServerInfoRecorder(){
        activeClient = 0;
        msgProcessed = 0;
        clientData = new ConcurrentHashMap<>();
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
            clientData.put("",0);
        }
    }

    public void addClientMsg(SelectionKey selectionKey){
        if (!clientData.contains(selectionKey.attachment())){
            clientData.put((String) selectionKey.attachment(),0);
        }
        else {
            int origin = clientData.get(selectionKey.attachment());
            origin++;
            clientData.put((String) selectionKey.attachment(), origin);
        }
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
        Set<String> sk = clientData.keySet();
        for (String uid : sk){
            clientData.put(uid,0);
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

    public int calMean(ConcurrentHashMap<String,Integer> clientData){
        Set<String> sk;
        synchronized (clientData) {
            sk = clientData.keySet();
        }
        int totalMsgNumber = 0;
        for(String tempSK : sk){
            totalMsgNumber += clientData.get(tempSK);
        }

        if (getActiveClient()==0)
            return 0;
        int result = (totalMsgNumber/20)/getActiveClient();
        return result;
    }

    public double calStdDev(ConcurrentHashMap<String,Integer> clientData){

        int mean = calMean(clientData);
        Set<String> sk;
        synchronized (clientData) {
             sk = clientData.keySet();
        }
        int sum = 0;
        for (String tempSK : sk){
            sum += Math.pow(((clientData.get(tempSK)/20)-mean),2);
        }

        if (getActiveClient()==0)
            return 0;
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
            int mean = calMean(clientData);
            double stdDev = calStdDev(clientData);
            System.out.println("["+LocalTime.now()+"]" + " Server Throughput: " + getMsgProcessed()/20 + " messages/s, Active Client Connections: "
            + getActiveClient() + ", Mean Per-client Throughput: " + mean + " messages/s, Std. Dev. of Per-client Throughput: " +
                    stdDev + " messages/s.");
            //System.out.println(clientData);
            resetClientData();
            resetMsgProcessed();

        }
    }
}
