package server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import server.controller.ServerController;

/**
 * Server for handling new connections for new players
 *
 * @author Perttu Jääskeläinen
 */
public class GameServer {

    private int PORT_NO = 8080;         // default port number
    private final int LINGER_TIME = 30000;        // linger time when closing socket
    private Selector selector;
    private ServerSocketChannel server;
    protected final ServerController contr = new ServerController();

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.parseArgs(args);
        server.serve();
    }

    /**
     * Called by a worker thread in PlayerHandler to specify that the object the
     * thread is running in has data to send
     *
     * @param playerChannel the channel that is ready to be sent data
     */
    protected void writable(SocketChannel playerChannel) {
        playerChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    /**
     * Start the server, making it nonblocking and accepting new connections
     *
     * @throws IOException if binding or registering values fails
     */
    private void startServer() throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(PORT_NO));
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Accept a new connection and bind a PlayerHandler object to it, for
     * handling input and output for the created SocketChannel
     *
     * @param k the key which is to be accepted
     * @throws IOException if getting the channel or opening a SocketChannel
     * fails
     */
    private void acceptKey(SelectionKey k) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) k.channel();
        SocketChannel clientChannel = serverChannel.accept();
        PlayerHandler handler = new PlayerHandler(clientChannel, this);
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_WRITE, handler);
        clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER_TIME);
        handler.connected();
    }

    /**
     * Reads from a channel which belongs to the specified SelectionKey
     *
     * @param k the key which channel is to be read
     * @throws IOException if reading from the channel fails
     */
    private void readFrom(SelectionKey k) throws IOException {
        PlayerHandler player = (PlayerHandler) k.attachment();
        try {
            player.read();
        } catch (IOException e) {
            player.disconnect();
            k.cancel();
        }
    }

    /**
     * Writes to the specified SelectionKey's SocketChannel, through the
     * PlayerHandler object
     *
     * @param k the SelecionKey which channel is to be written to
     * @throws IOException if writing to the channel fails
     */
    private void sendTo(SelectionKey k) throws IOException {
        PlayerHandler player = (PlayerHandler) k.attachment();
        try {
            player.sendAll();
            k.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            player.disconnect();
            k.cancel();
        }
    }

    /**
     * The main thread spends its lifetime here, accepting new connections and
     * forwarding the inputs to their respective attached PlayerHandler objects
     */
    private void serve() {
        try {
            startServer();
            while (true) {
                selector.select();
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
     * Used to parse arguments received when compiling the server - if a port
     * number is not specified, use the default port number defined in this
     * class
     *
     * @param args arguments received when compiling the server
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
