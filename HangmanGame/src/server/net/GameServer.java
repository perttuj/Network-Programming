package server.net;

import common.Constants;
import common.ServerMessageTypes;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import server.controller.ServerController;
/**
 *  Server for handling new connections for new players
 * @author Perttu Jääskeläinen
 */
public class GameServer {
    
    private int         PORT_NO         = 8080;         // default port number
    private final int   LINGER_TIME     = 30000;        // linger time when closing socket
    //private final int   SOCKET_TIMEOUT  = 1800000;      // time before timing out a connection, unused in socket channels
    private Selector selector;
    private ServerSocketChannel server;
    protected final ServerController contr = new ServerController();
    
    public static void main (String[] args) {
        GameServer server = new GameServer();
        server.parseArgs(args);
        server.serve();
    }
    protected void writable(SocketChannel playerChannel) {
        System.out.println("enabling writing");
        playerChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }
    /**
     * Initialize selector
     * @throws IOException  if initialization fails 
     */
    private void openSelector() throws IOException {
        selector = Selector.open();
    }
    protected void print(String s) {
        System.out.println(s);
    }
    /**
     * Start the server, making it nonblocking and accepting new connections
     * @throws IOException  if binding or registering values fails
     */
    private void startServer() throws IOException {
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(PORT_NO));
        server.register(selector, SelectionKey.OP_ACCEPT);
    }
    /**
     * Accept a new connection and bind a Player Object to it
     * for handling input and output for the created socket
     * @param k the key which is to be accepted
     * @throws IOException  if getting the channel or opening a SocketChannel fails
     */
    private void acceptKey(SelectionKey k) throws IOException   {
        System.out.println("accepting");
        ServerSocketChannel serverChannel = (ServerSocketChannel) k.channel();
        SocketChannel clientChannel = serverChannel.accept();
        PlayerHandler handler = new PlayerHandler(clientChannel, this);
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, handler);
        clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER_TIME);
    }
    private void readFrom(SelectionKey k) throws IOException {
        PlayerHandler player = (PlayerHandler) k.attachment();
        System.out.println("reading");
        try {
            player.read();
        } catch (IOException e) {
            player.disconnect();
            k.cancel();
        }  
    }
    private void sendTo(SelectionKey k) {
        PlayerHandler player = (PlayerHandler) k.attachment();
        System.out.println("writing");
        try {
            player.sendAll();
        } catch (IOException e) {
            
        }
        k.interestOps(SelectionKey.OP_READ);
    }
    /**
     * The main thread spends its lifetime here, accepting new connections and
     * handling them
     */
    private void serve() {
        try {
            openSelector();
            startServer();
            while (true) {
                System.out.println("sleeping b4 select");
                selector.select();
                System.out.println("waking up after select");
                Set<SelectionKey> set = selector.selectedKeys();
                for (SelectionKey k : set) {
                    set.remove(k);
                    if (!k.isValid()) {
                        continue;
                    }
                    if (k.isAcceptable()) {
                        acceptKey(k);
                    } else if (k.isReadable()) {
                        readFrom(k);
                    } else if (k.isWritable()) {
                        sendTo(k);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Server error");
            e.printStackTrace();
            System.exit(0);
        }    
    }
    /**
     * Used to parse arguments received when compiling the server - if a port number is not specified,
     * use the default port number defined in this class
     * @param args  arguments received when compiling the server
     */
    public void parseArgs(String[] args) {
        if (args.length > 0) {
            try {
                PORT_NO = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Error when parsing Portnumber, using default value: " + PORT_NO);
            }
        }
    }
}
