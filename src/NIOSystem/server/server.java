package NIOSystem.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by tjy on 2015/11/18.
 */
public class server {
    public SelectorLoop connectionBell;
    public SelectorLoop readBell;
    public boolean isReadBellRunning=false;

    public static void main(String [] args) throws IOException {
        new server().start();
    }

    public void start() throws IOException{
        connectionBell = new SelectorLoop();

        readBell = new SelectorLoop();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        ServerSocket socket =  ssc.socket();
        socket.bind(new InetSocketAddress("localhost",7878));

        ssc.register(connectionBell.getSelector(), SelectionKey.OP_ACCEPT);
        new Thread(connectionBell).start();
    }

    public class SelectorLoop implements Runnable{
        private Selector selector;
        private ByteBuffer temp = ByteBuffer.allocate(1024);

        public SelectorLoop() throws IOException{
            this.selector = Selector.open();
        }

        public Selector getSelector(){
            return this.selector;
        }

        public void run(){
            while(true){
                try{
                    this.selector.select();

                    Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while(keyIterator.hasNext()){
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();
                        this.dispatch(key);
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

        public void dispatch(SelectionKey key) throws IOException,InterruptedException{
            if(key.isAcceptable()){
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();

                SocketChannel sc = ssc.accept();

                sc.configureBlocking(false);
                sc.register(readBell.getSelector(), SelectionKey.OP_READ);

                synchronized (server.this){
                    if(!server.this.isReadBellRunning){
                        server.this.isReadBellRunning = true;
                        new Thread(readBell).start();
                    }
                }
            }
            else if(key.isReadable()){
                SocketChannel sc = (SocketChannel) key.channel();

                int count = sc.read(temp);
                if(count < 0){
                    key.cancel();
                    sc.close();
                    return;
                }

                temp.flip();
                String msg = Charset.forName("UTF-8").decode(temp).toString();
                System.out.println("Server received [" + msg + "] from client address: " + sc.getRemoteAddress());

                Thread.sleep(1000);

                sc.write(ByteBuffer.wrap(msg.getBytes(Charset.forName("UTF-8"))));

                temp.clear();
            }

        }


    }



    }

