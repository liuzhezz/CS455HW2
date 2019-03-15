package cs455.scaling.pool;

import cs455.scaling.server.Server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.security.NoSuchAlgorithmException;

public class ReadTask implements Task {
    private SelectionKey key;
    private Server server;

    public ReadTask(SelectionKey key, Server server){
        this.server = server;
        this.key = key;
    }

    public void func(){
        try{
            server.readFunction(key);
        } catch (IOException ie){
            ie.printStackTrace();
            System.out.println("IOException");
        } catch (NoSuchAlgorithmException ae){
            ae.printStackTrace();
            System.out.println("NoSuchAlgorithmException");
        }
    }

}
