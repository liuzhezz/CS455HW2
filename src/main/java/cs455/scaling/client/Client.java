package cs455.scaling.client;


import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;


public class Client {

    private LinkedList<String> msgHashs;
    private String serverHost;
    private int serverPort;
    private int msgRate;
    private ClientInfoRecorder recorder;
    private Thread sender;
    private Thread receiver;
    private SocketChannel socketChannel;

    public Client(String serverHost, int serverPort, int msgRate){
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.msgRate = msgRate;
        msgHashs = new LinkedList<>();
        recorder = new ClientInfoRecorder();

        //connect to server
        InetSocketAddress isa = new InetSocketAddress(serverHost,serverPort);
        try {
            socketChannel = SocketChannel.open(isa);
        }
        catch (IOException ioe){
            System.err.println(ioe.getMessage());
        }

        //sender thread
        sender = new Thread(()->{
            while (socketChannel!=null){  //connection alive
                //get a msg
                byte[] msg = msgGenerator();
                //get the hash value of msg
                String hashValue = SHA1FromBytes(msg);
                //store hash value into LinkedList
                msgHashs.add(hashValue);
                //put in buffer
                ByteBuffer buffer = ByteBuffer.wrap(msg);
                try{
                    //write to channel
                    socketChannel.write(buffer);
                }
                catch (IOException ioe){
                    System.err.println(ioe.getMessage());
                }
                //sent msg plus one
                recorder.sentPlusOne();

                try{
                    Thread.sleep(1000/msgRate);
                }
                catch (InterruptedException ie){
                    System.err.println(ie.getMessage());
                }
            }
        });

        receiver = new Thread(()->{
            ByteBuffer tempBuffer = ByteBuffer.allocate(8192);

            while (socketChannel != null){
                try{
                    socketChannel.read(tempBuffer);
                }
                catch (IOException ioe){
                    System.err.println(ioe.getMessage());
                }
                //change to read mode
                tempBuffer.flip();
                //read the first byte which is the string length
                int length = tempBuffer.get();

                while(tempBuffer.remaining()>=length){
                    //create a byte array with length of hash value
                    byte[] hashFromServer = new byte[length];
                    //fill  in byte array
                    tempBuffer.get(hashFromServer);
                    //convert the byte array to String
                    String reveivedHash = new String(hashFromServer);
                    //find in msgHashs and remove the record, add one to the received  recorder
                    if(msgHashs.contains(reveivedHash)){
                        recorder.receivePlusOne();
                        msgHashs.remove(reveivedHash);
                    }
                    else {
                        System.err.println("Client get a wrong acknowledge (hash value) from Server");
                    }
                    //exam whether there is another hash value
                    if(tempBuffer.hasRemaining()) {
                        length = tempBuffer.get();
                    }
                }

            }
        });
    }

    /**Generate a 8kb byte array randomly and return it.
     *
     * @return msg
     */
    public byte[] msgGenerator(){
        byte[] msg = new byte[8192];  //8192 indicate 8KB
        Random random = new Random();
        random.nextBytes(msg);  //filling info randomly
        return msg;
    }

    /**
     *
     * @param data: byte array info.
     * @return hash in string
     */
    public String SHA1FromBytes(byte[] data) {
        MessageDigest digest = null;
        try{
            digest = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException nsae){
            nsae.printStackTrace();;
        }
        byte[] hash = digest.digest(data);
        BigInteger hashInt = new BigInteger(1, hash);
        return hashInt.toString(16);
    }

    /**
     * Start all thread, sender, receiver, recorder
     */
    public void start(){
        sender.start();
        receiver.start();
        recorder.start();
    }


    public static void main(String[] args) {
        try{
            Client client = new Client(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));
            client.start();
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
}
