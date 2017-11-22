package client.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import common.Constants;
import common.ServerMessageTypes;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Class responsible for handling a server connection for a client
 * @author Perttu Jääskeläinen
 */
public class ServerConnection implements Runnable {
    private ByteBuffer serverReaderBuf = ByteBuffer.allocateDirect(4096); // replace with constant
    private String messageFromUser = null;
    private SocketChannel channel;
    private Selector selector;
    private Listener listener;
    private InetSocketAddress address;
    private volatile boolean writing = false;
    private volatile boolean connected;
    private volatile boolean timeToSend;
    
    public void registerHandler(ResponseHandler serverResponseHandler) {
        this.listener = new Listener(serverResponseHandler);
    }
    public void connect(String host, int port) {
        address = new InetSocketAddress(host, port);
        new Thread(this).start();
    }
    /**
     * Constructs the users guess into a server-type message.
     * Can fail if a command is already being processed, 
     * @param guess 
     */
    public void sendGuess(String guess) {
        StringJoiner joiner = new StringJoiner(Constants.DELIMETER);
        joiner.add(ServerMessageTypes.GUESS.toString());
        joiner.add(guess);
        System.out.println(joiner.toString());
        setMsg(joiner.toString());
    }
    public void newGame() {
        setMsg(ServerMessageTypes.NEWWORD.toString());
    }
    /**
     * Disconnect from the server, initiated by the user.
     */
    public void disconnect() {
        try {
            channel.close();
            channel = null;
            connected = false;
        } catch (IOException e) {
            listener.sendMsg("failed when closing channel - disconnected" + e.toString());
            channel = null;
            connected = false;
        }  
    }
    @Override
    public void run() {
        try {
            startConnection();
            openSelector();
            while (connected) {
                selector.select();
                if (timeToSend) {
                    channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    timeToSend = false;
                }
                Set<SelectionKey> set = selector.selectedKeys();
                for (SelectionKey k : set) {
                    set.remove(k);
                    if (!k.isValid()) {
                        System.out.println("not valid");
                        continue;
                    } 
                    if (k.isConnectable()) {
                        System.out.println("finishing connection");
                        finishConnection(k);
                    } else if (k.isReadable()) {
                        System.out.println("reading from serv");
                        readFromServer(k);
                    } else if (k.isWritable()) {
                        System.out.println("writing to serv");
                        writeToServer(k);
                    }
                }
            }
        } catch (IOException e) {
            listener.sendMsg("connection error - exiting\n" + e.toString());
            System.exit(0);
        }
    }
    /**
     * Start the connection, making it nonblocking and accepting new connections
     * @throws IOException  if binding or registering values fails
     */
    private void startConnection() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);
        connected = true;
    }
    /**
     * 
     * @param k
     * @throws IOException 
     */
    private void finishConnection(SelectionKey k) throws IOException {
        channel.finishConnect();
        listener.sendMsg("connected to: " + address);
    }
    /**
     * Initialize selector
     * @throws IOException  if initialization fails 
     */
    private void openSelector() throws IOException {
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);
    }
    /**
     * Reads from the server
     * @throws IOException  if reading from the channel fails
     */
    private void readFromServer(SelectionKey k) throws IOException {
        serverReaderBuf.clear();
        int bytesRead = channel.read(serverReaderBuf);
        if (bytesRead == -1) {
            throw new IOException("reading from server failed");
        }
        String msg = extractFromBuffer(serverReaderBuf);
        System.out.println("message received: " + msg);
        listener.formatAndSend(msg);
    }
    private void writeToServer(SelectionKey k) throws IOException {
        System.out.println(messageFromUser);
        channel.write(ByteBuffer.wrap(messageFromUser.getBytes()));
        messageFromUser = null;
        k.interestOps(SelectionKey.OP_READ);
        writing = false;
    }
    private String extractFromBuffer(ByteBuffer buf) {
        buf.flip();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return new String(bytes);
    }
    private void setMsg(String msg) {
        if (writing) {
            listener.sendMsg("already processing command - please wait for a response - " + messageFromUser);
            return;
        }   
        messageFromUser = msg;
        writing = true;
        timeToSend = true;
        System.out.println("waking up selector with: " + messageFromUser);
        selector.wakeup();
    }
    /**
     * Listens for callbacks from the server, which are printed to the user without 
     * going through the controller.
     */
    private class Listener {
        private final ResponseHandler handler;

        private Listener(ResponseHandler handler) {
            this.handler = handler;
        }
        private void sendMsg(String s) {
            handler.handleMsg(s);
        }
        private void formatAndSend(String s) {
            sendMsg(format(s));
        }
        /**
        * Extracts the message received (without type)
        * @param entireMsg the original format message from the server
        * @return  the message without the type included
        */
        private String format(String entireMsg) {
            String[] message = entireMsg.split(Constants.DELIMETER);
            if (message[0] == ServerMessageTypes.NEWWORD.toString()) {
                return "NEWWORD";
            }
            return message[1];
        }
    }
}