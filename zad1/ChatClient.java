/**
 * @author Śliwa Adam S25853
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatClient {

    private final String cId;
    private final StringBuilder cView;
    private final InetSocketAddress iSAddress;
    private final Lock block = new ReentrantLock();
    private SocketChannel socketChannel;

    public ChatClient(String host, int port, String id) {
        this.iSAddress = new InetSocketAddress(host, port);
        this.cId = id;
        cView = new StringBuilder("=== " + cId + " chat view" + "\n");
    }

    private void run() {
        int cap = 1024;
        ByteBuffer buff = ByteBuffer.allocateDirect(cap);
        int bRead = 0;
        while (!receivingThread.isInterrupted()) {
            do {
                try {
                    block.lock();
                    bRead = socketChannel.read(buff);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    block.unlock();
                }
            } while (!receivingThread.isInterrupted() && bRead == 0);
            buff.flip();
            String res = StandardCharsets.UTF_8.decode(buff).toString();
            cView.append(res);
            buff.clear();
        }
    }

    public void login() {
        try {
            socketChannel = SocketChannel.open(iSAddress);
            socketChannel.configureBlocking(false);
            send("log in " + cId);
            receivingThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        send("log out" + "#");
        try {
            block.lock();
            receivingThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            block.unlock();
        }
    }

    public void send(String req) {
        try {
            Thread.sleep(30);
            socketChannel.write(StandardCharsets.UTF_8.encode(req + "#"));
            Thread.sleep(30);
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    public String getChatView() {
        return cView.toString();
    }

    private final Thread receivingThread = new Thread(this::run);


}