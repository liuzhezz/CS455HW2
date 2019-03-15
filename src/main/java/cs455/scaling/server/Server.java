package cs455.scaling.server;


import cs455.scaling.pool.ReadTask;
import cs455.scaling.pool.Task;
import cs455.scaling.pool.ThreadPool;
import cs455.scaling.pool.WriteTask;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.UUID;


public class Server{

    private int portNumber;
    private int threadPoolSize;
    private int batchSize;
    private int batchTime;

    private ThreadPool threadPool;
    private ServerInfoRecorder serverInfoRecorder;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;




    public Server(int portNumber, int threadPoolSize, int batchSize, int batchTime){
        this.portNumber = portNumber;
        this.threadPoolSize = threadPoolSize;
        this.batchSize = batchSize;
        this.batchTime = batchTime;
    }

    public void initiate() throws IOException, NoSuchAlgorithmException{
        //initiate threadPool and start the thread
        threadPool = new ThreadPool(this.threadPoolSize,this.batchSize,this.batchTime);
        threadPool.initiate();
        threadPool.start();

        //initiate serverInfoRecorder and start the thread
        this.serverInfoRecorder = new ServerInfoRecorder();
        serverInfoRecorder.start();

        this.selector = Selector.open();

        //initiate serverSocketChannel
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        start();
    }

    public void start() throws IOException,NoSuchAlgorithmException{
        System.out.println("Server started!");
        while(true){
            this.selector.select();
            Iterator<SelectionKey> keyIterator = this.selector.selectedKeys().iterator();
            while (keyIterator.hasNext()){
                SelectionKey selectionKey = keyIterator.next();
                if(selectionKey.isAcceptable()){
                    String uniqueID = UUID.randomUUID().toString();
                    selectionKey.attach(uniqueID);
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    serverInfoRecorder.clientPlusOne();
                    serverInfoRecorder.addClientMsg(selectionKey);

                }
                else if(selectionKey.isReadable()){
                    selectionKey.interestOps(selectionKey.interestOps()&(~SelectionKey.OP_READ));
                    Task readTask = new ReadTask(selectionKey, this);
                    threadPool.addTask(readTask);
                }
                else if(selectionKey.isWritable()){
                    selectionKey.interestOps(selectionKey.interestOps()&(~SelectionKey.OP_WRITE));
                    Task newTask = new WriteTask(this, selectionKey);
                    threadPool.addTask(newTask);
                }
                keyIterator.remove();
            }
        }
    }

    public void readFunction(SelectionKey selectionKey) throws IOException, NoSuchAlgorithmException{
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(8*1024);
        ;
        try {
            while (byteBuffer.hasRemaining()) {
                socketChannel.read(byteBuffer);
                byte[] data = byteBuffer.array();
                String hash = SHA1FromBytes(data);
                selectionKey.attach(hash);
                serverInfoRecorder.addClientMsg(selectionKey);
            }
        }
        catch (IOException ioe){
            System.err.println(ioe.getMessage());
        }

        try {
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
        catch (CancelledKeyException cke){
            cke.printStackTrace();
        }

        selectionKey.selector().wakeup();
    }

    public void writeFunction(SelectionKey selectionKey) throws IOException {

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        String hash = (String)selectionKey.attachment();
        int hashLength = hash.length();
        String hashWithLength = String.valueOf(hashLength) + hash;
        ByteBuffer byteBuffer = ByteBuffer.wrap(hashWithLength.getBytes());
        serverInfoRecorder.msgProcessedPlusOne();
        int lengthOfWrote = socketChannel.write(byteBuffer);

        if(lengthOfWrote < hash.length()){
            Task writeTask = new WriteTask(this,selectionKey);
            threadPool.addTask(writeTask);
            return;
        }
        try{
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
        catch (CancelledKeyException cke){
            cke.printStackTrace();
        }
        selectionKey.selector().wakeup();
    }

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
        Server server = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        try{
            server.initiate();
        }
        catch (IOException ioe){
            System.err.println(ioe.getMessage());
        }
        catch (NoSuchAlgorithmException nsae){
            System.err.println(nsae.getMessage());
        }

    }



}
