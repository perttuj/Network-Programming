package client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import common.Constants;
import common.ServerMessageTypes;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Class responsible for handling a server connection for a client
 * @author Perttu Jääskeläinen
 */
public class ServerConnection implements Runnable {
    private static final int TIMEOUT_USER_SOCKET = 1800000;   // User socket timeout time
    private static final int TIMEOUT_SERVER_SOCKET = 30000;   // Timeout for server socket
    private SocketChannel socket;
    private Selector selector;
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private volatile boolean connected;

    public void connect(String host, int port, ResponseHandler serverResponseHandler) throws IOException {
        socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.bind(new InetSocketAddress(host, port));
        socket.register(selector, SelectionKey.OP_CONNECT);
        new Thread(this).start();
    }
    @Override
    public void run() {
        
    }
    /**
     * Disconnect from the server, initiated by the user.
     * @throws IOException If the socket.close() method fails
     */
    public void disconnect() throws IOException {
        sendCommand(ServerMessageTypes.DISCONNECT.toString());
        socket.close();
        socket = null;
        connected = false;
    }
    /**
     * Structures the guess into the proper format before sending to the server
     * Example:
     *  'A'
     * is structured into 'GUESS##A', where '##' is the (example) message delimeter
     * @param guess the letter or word to be guessed and calculated by the server
     */
    public void sendGuess(String guess) {
        sendCommand(ServerMessageTypes.GUESS + Constants.DELIMETER + guess);
    }
    /**
     * Structures a 'NEWGAME' command into proper type used by the server
     * Sever types are found in Constants.ServerMessageTypes
     */
    public void newGame() {
        sendCommand(ServerMessageTypes.NEWWORD.toString());
    }
    /**
     * Send a structured command to the server, which includes a type 'ServerMessageTypes' for the server, 
     * followed by a message delimeter defined in constants and the body of the ServerMessageType command.
     * Example: 'GUESS##A' 
     * For type 'GUESS' for the letter 'A', split by the (exampe) delimeter '##'
     * @param command the structured command to be sent to the server
     */
    private void sendCommand(String command) {
        if (connected) {
            toServer.println(command);
        }
    }
    /**
     * Listens for callbacks from the server, which are printed to the user without 
     * going through the controller.
     */
    private class Listener implements Runnable {
        private final ResponseHandler handler;

        private Listener(ResponseHandler handler) {
            this.handler = handler;
        }
        /**
         * Keep listening for messages, if received print to user
         * If connection lost, also notify the user
         */
        @Override
        public void run() {
            try {
                for (;;) {
                    handler.handleMsg(formatMsg(fromServer.readLine()));
                }
            } catch (Throwable connectionFailure) {
                if (connected) {
                    handler.handleMsg("Lost connection.");
                }
            }
        }
        /**
         * Extracts the message received (without type)
         * @param entireMsg the original format message from the server
         * @return  the message without the type included
         */
        private String formatMsg(String entireMsg) {
            String[] message = entireMsg.split(Constants.DELIMETER);
            if (ServerMessageTypes.valueOf(message[Constants.TYPE_INDEX]) == ServerMessageTypes.RESPONSE) {
                return message[Constants.MESSAGE_INDEX];
            } else if (ServerMessageTypes.valueOf(message[Constants.TYPE_INDEX]) == ServerMessageTypes.DISCONNECT) {
                return "DISCONNECTED FROM SERVER";
            } else {
                return message[Constants.MESSAGE_INDEX];
            }
        }
    }
}