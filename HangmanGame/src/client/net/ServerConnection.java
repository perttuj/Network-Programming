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
 *
 * @author Perttu Jääskeläinen
 */
public class ServerConnection implements Runnable {

    private ByteBuffer      serverReaderBuf = ByteBuffer.allocateDirect(Constants.MAX_MESSAGE_SIZE);
    private SocketChannel   channel;
    private Selector        selector;
    private InetSocketAddress   address;
    private ResponseHandler     handler;
    
    private volatile boolean writing = false;
    private volatile boolean connected;
    private String messageFromUser = null;
    

    /**
     * Register an output handler, where responses from the server are sent
     *
     * @param serverResponseHandler the handler which responses are sent to
     */
    public void registerHandler(ResponseHandler serverResponseHandler) {
        this.handler = serverResponseHandler;
    }

    /**
     * Connect to the specified host and port, creating a new thread for
     * handling the connection
     *
     * @param host the IP to connect to
     * @param port the port to connect to
     */
    public void connect(String host, int port) {
        address = new InetSocketAddress(host, port);
        new Thread(this).start();
    }

    private void notConnected() {
        handler.handleMsg("not connected");
    }

    /**
     * Transforms the users guess into an acceptable server-type message. Can
     * fail if a command is already being processed, returning without changing
     * anything.
     *
     * @param guess the user guess to be transformed
     */
    public void sendGuess(String guess) {
        StringJoiner joiner = new StringJoiner(Constants.DELIMETER);
        joiner.add(ServerMessageTypes.GUESS.toString());
        joiner.add(guess);
        setMsg(joiner.toString());
    }

    /**
     * Transforms a 'newgame' command into an acceptable server-type message Can
     * fail if a command is already being processed, returning without changing
     * anything
     */
    public void newGame() {
        setMsg(ServerMessageTypes.NEWWORD.toString());
    }

    /**
     * Disconnect from the server, initiated by the user.
     */
    public void disconnect() {
        try {
            if (!connected) {
                notConnected();
                return;
            } else {
                channel.close();
                handler.handleMsg("closed connection");
            }
        } catch (IOException e) {
            handler.handleMsg("error when closing channel: " + e.toString());
        } finally {
            channel = null;
            connected = false;
        }
    }

    @Override
    public void run() {
        try {
            startConnection();
            while (connected) {
                selector.select();
                Set<SelectionKey> set = selector.selectedKeys();
                for (SelectionKey k : set) {
                    set.remove(k);
                    if (!k.isValid()) {
                        continue;
                    }
                    if (k.isConnectable()) {
                        finishConnection(k);
                    } else if (k.isReadable()) {
                        readFromServer();
                    } else if (k.isWritable()) {
                        writeToServer(k);
                    }
                }
            }
        } catch (IOException e) {
            disconnect();
            handler.handleMsg("connection error - exiting\n" + e.toString());
        }
    }

    /**
     * Start the connection, making it nonblocking and accepting new connections
     *
     * @throws IOException if binding or registering values fails
     */
    private void startConnection() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);
        connected = true;
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    /**
     * Finishes the specified connection, committing all registered values and
     * configurations, then wait for a confirmation from the server
     *
     * @param k the SelectionKey for the connection
     * @throws IOException if finishConnect() fails
     */
    private void finishConnection(SelectionKey k) throws IOException {
        channel.finishConnect();
        k.interestOps(SelectionKey.OP_READ);
    }

    /**
     * Reads from the server into bytebuffer 'serverReaderBuf'
     *
     * @throws IOException if reading from the channel fails
     */
    private void readFromServer() throws IOException {
        serverReaderBuf.clear();
        int bytesRead = channel.read(serverReaderBuf);
        if (bytesRead == -1) {
            throw new IOException("reading from server failed");
        }
        String msg = extractFromBuffer(serverReaderBuf);
        formatAndSend(msg);
    }

    /**
     * Writes a ByteBuffer to the server, containing the user message
     *
     * @param k the key to change interest operation for
     * @throws IOException if writing to the channel fails
     */
    private void writeToServer(SelectionKey k) throws IOException {
        channel.write(ByteBuffer.wrap(messageFromUser.getBytes()));
        messageFromUser = null;
        k.interestOps(SelectionKey.OP_READ);
        writing = false;
    }

    /**
     * Extracts a String from the bytebuffer object
     *
     * @param buf bytebuffer object to read
     * @return string read from bytebuffer
     */
    private String extractFromBuffer(ByteBuffer buf) {
        buf.flip();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return new String(bytes);
    }

    /**
     * Sets the current user message to the message specified. If a message
     * already exists, return, saying that only one command may be processed at
     * a time
     *
     * @param msg the message to be sent to the server
     */
    private void setMsg(String msg) {
        if (!connected) {
            notConnected();
            return;
        }
        if (writing) {
            handler.handleMsg("already processing command - please wait for a response - ");
            return;
        }
        writing = true;
        messageFromUser = msg;
        channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    /**
     * Formats the string received from an acceptable server-type to an user
     * friendly message
     *
     * @param msg the message to format and send
     */
    private void formatAndSend(String msg) {
        String formatted = format(msg);
        handler.handleMsg(formatted);
    }

    /**
     * Extracts the message received (without type)
     *
     * @param entireMsg the original format message from the server
     * @return the message without the type included
     */
    private String format(String entireMsg) {
        String[] message = entireMsg.split(Constants.DELIMETER);
        if (ServerMessageTypes.valueOf(message[Constants.TYPE_INDEX]) == ServerMessageTypes.NEWWORD) {
            return "NEWWORD";
        }
        return message[Constants.MESSAGE_INDEX];
    }
}
