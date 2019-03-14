package cs455.scaling.pool;

import java.nio.channels.SelectionKey;
import java.util.*;

public class WorkList {

    private volatile int workNumber;
    private Hashtable<SelectionKey, List<byte[]>> work;



    public WorkList(){
        this.workNumber = 0;
        this.work = new Hashtable<>();
    }

    public int getWorkNumber() {
        return workNumber;
    }

    public void addWork(SelectionKey sk, byte[] msg){
        if(work.contains(sk)){
            work.get(sk).add(msg);
        }
        else{
            ArrayList<byte[]> temp = new ArrayList<>();
            temp.add(msg);
            work.put(sk,temp);
        }
        workNumber++;
    }
}
