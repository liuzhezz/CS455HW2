package cs455.scaling.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
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
    private Selector selector;

    public Client(String serverHost, int serverPort, int msgRate){
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.msgRate = msgRate;
        msgHashs = new LinkedList<>();
        recorder = new ClientInfoRecorder();

        try{
            this.socketChannel = SocketChannel.open();
            this.selector = Selector.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(this.serverHost,this.serverPort));
        }
        catch (IOException ioe){
            System.err.println(ioe.getMessage());
        }
    }


    /**
     * Start all thread, sender, receiver, recorder
     */
    public void start() throws IOException{
        System.out.println("Client started!");
        recorder.start();
        while (true) {
            try {
                this.selector.select();
            }
            catch (IOException ioe){
                System.err.println(ioe.getMessage());
            }
            Iterator<SelectionKey> selectionKeyIterator = this.selector.selectedKeys().iterator();
            while (selectionKeyIterator.hasNext()) {
                SelectionKey selectionKey = selectionKeyIterator.next();
                selectionKeyIterator.remove();
                if(selectionKey.isConnectable()){
                    this.connectFunction(selectionKey);
                }
                else if (selectionKey.isReadable()) {
                    this.readFunction(selectionKey);
                }
            }
        }
    }

    public void connectFunction(SelectionKey selectionKey)throws IOException{
        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
        socketChannel.finishConnect();
        //sender thread
        sender = new Thread(()->{
            while (true){

                byte[] data = msgGenerator();
                String hash = SHA1FromBytes(data);
                this.msgHashs.add(hash);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                try{
                    //write to channel
                    socketChannel.write(buffer);
                }
                catch (IOException ioe){
                    System.err.println(ioe.getMessage());
                }

                selectionKey.interestOps(SelectionKey.OP_READ);
                recorder.sentPlusOne();
                selectionKey.selector().wakeup();

                try {
                    Thread.sleep(1000/msgRate);
                }
                catch (InterruptedException ie){
                    System.err.println(ie.getMessage());
                }

            }
        });
        sender.start();
    }

    public void readFunction(SelectionKey selectionKey){
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(8*1024);

        try {
            socketChannel.read(byteBuffer);
            byte[] data = byteBuffer.array();
            String string = new String(data);
            int len = Integer.parseInt(string.substring(0,2));
            String hashes = string.substring(2,2+len);
            recorder.receivePlusOne();
            if(msgHashs.contains(hashes)){
                msgHashs.remove(hashes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        selectionKey.interestOps(SelectionKey.OP_WRITE);
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
