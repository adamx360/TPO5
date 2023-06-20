/**
 * @author Śliwa Adam S25853
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer {

    private final Thread sThread;
    private final StringBuilder sLog;
    private final InetSocketAddress iSAddress;
    private final Map<SocketChannel, String> clients;
    private final Lock lock = new ReentrantLock();
    private ServerSocketChannel sSChannel;
    private Selector sel;

    public ChatServer(String host, int port) {
        iSAddress = new InetSocketAddress(host, port);
        sLog = new StringBuilder();
        clients = new HashMap<>();
        sThread = createServerThread();
    }

    private Thread createServerThread() {
        return new Thread(() -> {
            try {
                sel = Selector.open();
                sSChannel = ServerSocketChannel.open();
                sSChannel.bind(iSAddress);
                sSChannel.configureBlocking(false);
                sSChannel.register(sel, sSChannel.validOps(), null);
                while (!sThread.isInterrupted()) {
                    sel.select();
                    if (sThread.isInterrupted()) {
                        break;
                    }
                    Iterator<SelectionKey> it = sel.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey k = it.next();
                        it.remove();
                        if (k.isAcceptable()) {
                            SocketChannel clientSocket = sSChannel.accept();
                            clientSocket.configureBlocking(false);
                            clientSocket.register(sel, SelectionKey.OP_READ);
                        }
                        if (k.isReadable()) {
                            SocketChannel cSocket = (SocketChannel) k.channel();
                            int cap = 1024;
                            ByteBuffer buff = ByteBuffer.allocateDirect(cap);
                            StringBuilder clientRequest = new StringBuilder();
                            int rBytes = 0;
                            do {
                                try {
                                    lock.lock();
                                    rBytes = cSocket.read(buff);
                                    buff.flip();
                                    clientRequest.append(StandardCharsets.UTF_8.decode(buff));
                                    buff.clear();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    lock.unlock();
                                }
                            } while (rBytes != 0);
                            String[] splits = clientRequest.toString().split("#");
                            for (String split : splits) {
                                StringBuilder cRes = requestHandler(cSocket, split);
                                for (Map.Entry<SocketChannel, String> ent : clients.entrySet()) {
                                    ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(cRes.toString());
                                    ent.getKey().write(byteBuffer);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {

            }
        });
    }

    private StringBuilder requestHandler(SocketChannel cSocket, String string) throws IOException {
        StringBuilder res = new StringBuilder();
        if (string.startsWith("log in ")) {
            String username = string.substring(7);
            clients.put(cSocket, username);
            sLog.append(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"))).append(" ").append(username).append(" logged in").append("\n");
            res.append(username).append(" logged in").append("\n");
        } else if (string.equals("log out")) {
            String username = clients.get(cSocket);
            sLog.append(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"))).append(" ").append(username).append(" logged out").append("\n");
            res.append(username).append(" logged out").append("\n");
            ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(res.toString());
            cSocket.write(byteBuffer);
            clients.remove(cSocket);
        } else {
            String username = clients.get(cSocket);
            sLog.append(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS"))).append(" ").append(username).append(": ").append(string).append("\n");
            res.append(username).append(": ").append(string).append("\n");
        }
        return res;
    }

    public void startServer() {
        sThread.start();
        System.out.println("Server started\n");
    }

    public void stopServer() {
        try {
            lock.lock();
            sThread.interrupt();
            sel.close();
            sSChannel.close();
            System.out.println("Server stopped");
        } catch (IOException ignored) {

        } finally {
            lock.unlock();
        }
    }

    public String getServerLog() {
        return sLog.toString();
    }
}