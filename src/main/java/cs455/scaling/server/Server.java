package cs455.scaling.server;

import cs455.scaling.pool.HashFunction;
import cs455.scaling.pool.ThreadPool;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;


public class Server{

    private int portNumber;
    private int threadPoolSize;
    private int batchSize;
    private int batchTime;
    private Selector serverSelector;
    private ServerInfoRecorder serverInfoRecorder;
    private ThreadPool threadPool;
    private HashFunction instance = new HashFunction();




    public Server(int portNumber, int threadPoolSize,  int batchSize, int batchTime){
        this.portNumber = portNumber;
        this.threadPoolSize = threadPoolSize;
        this.batchSize = batchSize;
        this.batchTime = batchTime;
        this.serverInfoRecorder = new ServerInfoRecorder();
        this.threadPool = new ThreadPool(threadPoolSize);
    }

    public void start(){
        serverInfoRecorder.start();
        try(ServerSocketChannel serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(portNumber));
            Selector selector = Selector.open()){
            serverSelector = selector;
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true){
                selector.select();
                for (Iterator it = selector.selectedKeys().iterator(); it.hasNext(); it.remove()) {
                    SelectionKey selectionKey = (SelectionKey) it.next();

                    if (selectionKey.isAcceptable()) {  //the server selector key
                        SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
                        if (socketChannel == null) continue;  //skip if no socket request connection
                        socketChannel.configureBlocking(false);
                        ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
                        SelectionKey socketChannelRead = socketChannel.register(selector, SelectionKey.OP_READ,byteBuffer);
                        serverInfoRecorder.clientPlusOne();
                        serverInfoRecorder.addSelectorKey(socketChannelRead);
                    }
                    else if(selectionKey.isReadable()){  //the client selector key
                        serverInfoRecorder.addClientMsg(selectionKey);
                        selectionKey.interestOps(selectionKey.interestOps() & (~SelectionKey.OP_READ));
                        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                        ByteBuffer byteBuffer = (ByteBuffer)selectionKey.attachment();

                        threadPool.submit(()->{
                            try {
                                socketChannel.read(byteBuffer);
                            } catch (IOException e) {
                                System.err.println("Fail to read from channel");
                                selectionKey.cancel();
                                e.printStackTrace();
                            }
                            boolean full = false;
                            if (!byteBuffer.hasRemaining()) {
                                full = true;
                                selectionKey.attach(ByteBuffer.allocate(8*1024));
                            }
                            selectionKey.interestOps(SelectionKey.OP_READ);
                            selector.wakeup();

                            if (full) {
                                threadPool.submit(() -> {
                                    String hashString = instance.SHA1FromBytes(byteBuffer.array());
                                    threadPool.submit(() -> {
                                        ByteBuffer ackBytes = ByteBuffer.allocate(hashString.length() + 1);
                                        ackBytes.put((byte)hashString.length());
                                        ackBytes.put(hashString.getBytes());
                                        ackBytes.flip();
                                        synchronized (socketChannel) {
                                            try {
                                                socketChannel.write(ackBytes);
                                            } catch (IOException e) {
                                                System.err.println(e.getMessage());
                                            }
                                        }
                                        serverInfoRecorder.msgProcessedPlusOne();
                                    });
                                });
                            }
                        });
                    }
                }
            }
        }
        catch (IOException ioe){
            System.err.println(ioe.getMessage());
        }
    }


    public String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] hash = digest.digest(data);
        BigInteger hashInt = new BigInteger(1, hash);
        return hashInt.toString(16);
    }





    public static void main(String[] args) {
        Server server = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        server.start();

    }



}
