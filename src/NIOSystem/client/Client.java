package NIOSystem.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by tjy on 2015/11/18.
 */
public class Client implements Runnable{

    private static int idleCounter = 0;
    private Selector selector;
    private SocketChannel socketChannel;
    private ByteBuffer temp = ByteBuffer.allocate(1024);

    public static void main(String[] args) throws IOException {
        Client client =  new Client();
        new Thread(client).start();
    }

    public Client() throws IOException {
        this.selector = selector.open();

        socketChannel = SocketChannel.open();

        boolean isConnected = socketChannel.connect(new InetSocketAddress("localhost",7878));
        socketChannel.configureBlocking(false);
        SelectionKey key =  socketChannel.register(selector, SelectionKey.OP_READ);

        if(isConnected){
            this.sendFirstMsg();
        } else {
            key.interestOps(SelectionKey.OP_CONNECT);
        }


    }

    public void sendFirstMsg() throws IOException {
        String msg = "Hello NIO.";
        socketChannel.write(ByteBuffer.wrap(msg.getBytes(Charset.forName("UTF-8"))));
    }


    public void run(){
        while(true){
            try{
                int num = selector.select(1000);
                if(num == 0){
                    idleCounter ++;
                    if(idleCounter > 10){
                        try{
                            this.sendFirstMsg();
                        }
                        catch(ClosedChannelException e){
                            e.printStackTrace();
                            this.socketChannel.close();
                            return;
                        }
                    }

                }
                else{
                    idleCounter = 0;
                }

                Set<SelectionKey> keys = this.selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while(it.hasNext()){
                    SelectionKey key = it.next();
                    it.remove();
                    if(key.isConnectable()){
                        SocketChannel sc = (SocketChannel) key.channel();
                        if(sc.isConnectionPending()){
                            sc.finishConnect();
                        }

                        this.sendFirstMsg();
                    }

                    if(key.isReadable()){
                        SocketChannel sc = (SocketChannel) key.channel();
                        this.temp = ByteBuffer.allocate(1024);
                        int count = sc.read(temp);
                        if(count < 0){
                            sc.close();
                            continue;
                        }

                        temp.flip();
                        String msg = Charset.forName("UTF-8").decode(temp).toString();
                        System.out.println("client received ["+msg+"] from server address:" + sc.getRemoteAddress());

                        Thread.sleep(1000);
                        sc.write(ByteBuffer.wrap(msg.getBytes(Charset.forName("UTF-8"))));
                        temp.clear();
                    }
                }

            }
            catch(IOException e){
                e.printStackTrace();
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
