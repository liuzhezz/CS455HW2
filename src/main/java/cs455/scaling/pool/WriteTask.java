package cs455.scaling.pool;

import cs455.scaling.server.Server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class WriteTask implements Task{
    private SelectionKey key;
    private Server server;

    public WriteTask(Server server, SelectionKey key){
        this.server = server;
        this.key = key;
    }

    public void func(){
        try{
            server.writeFunction(key);
        } catch (IOException ie){
            ie.printStackTrace();
            System.out.println("IOException");
        }
    }
}
