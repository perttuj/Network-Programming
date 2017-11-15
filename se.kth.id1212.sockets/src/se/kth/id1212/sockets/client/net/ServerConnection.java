package se.kth.id1212.sockets.client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import se.kth.id1212.sockets.common.Constants;
import se.kth.id1212.sockets.common.MessageException;
import se.kth.id1212.sockets.common.MsgType;

public class ServerConnection {
    private static final int TIMEOUT_HALF_HOUR = 1800000;
    private static final int TIMEOUT_HALF_MINUTE = 30000;
    private Socket socket;
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private volatile boolean connected;


    public void connect(String host, int port, OutputHandler broadcastHandler) throws
            IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), TIMEOUT_HALF_MINUTE);
        socket.setSoTimeout(TIMEOUT_HALF_HOUR);
        connected = true;
        boolean autoFlush = true;
        toServer = new PrintWriter(socket.getOutputStream(), autoFlush);
        fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new Thread(new Listener(broadcastHandler)).start();
    }

    public void disconnect() throws IOException {
        sendCommand(MsgType.DISCONNECT.toString());
        socket.close();
        socket = null;
        connected = false;
    }
    
    public void sendGuess(String guess) {
        sendCommand(guess);
    }

    private void sendCommand(String command) {
        toServer.println(command);
    }
    
    public void newGame() {
        sendCommand(MsgType.NEWWORD.toString());
    }

    private class Listener implements Runnable {
        private final OutputHandler outputHandler;

        private Listener(OutputHandler outputHandler) {
            this.outputHandler = outputHandler;
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    outputHandler.handleMsg(extractMsg(fromServer.readLine()));
                }
            } catch (Throwable connectionFailure) {
                if (connected) {
                    outputHandler.handleMsg("Lost connection.");
                }
            }
        }

        private String extractMsg(String entireMsg) {/*
            String[] message = entireMsg.split(Constants.MSG_DELIMETER);
            if (MsgType.valueOf(message[Constants.MSG_TYPE_INDEX].toUpperCase()) == MsgType.RESPONSE/* ensure type is correct, TODO  ) {
                throw new MessageException("Received corrupt message: " + entireMsg);
            }*/
            return entireMsg;
        }
    }
}