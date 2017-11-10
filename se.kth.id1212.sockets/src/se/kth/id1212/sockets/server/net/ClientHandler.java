/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.server.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import se.kth.id1212.sockets.common.Constants;
import se.kth.id1212.sockets.common.MessageException;
import se.kth.id1212.sockets.common.MsgType;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class ClientHandler implements Runnable {
    
    private final Socket clientSocket;
    private final Server server;
    private volatile boolean connected;
    
    public ClientHandler (Server server, Socket client) {
        this.clientSocket = client; 
        this.server = server;
        this.connected = true;
    }
    
    private void disconnect() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }

    @Override
    public void run() {
        try {
            boolean autoFlush = true;
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), autoFlush); 
            while (connected) {
                Message msg = new Message(clientReader.readLine());
                switch (msg.msgType) {
                    case DISCONNECT:
                        clientWriter.println("Disconnecting..");
                        disconnect();
                        break;
                    case GUESS:
                        clientWriter.println(server.guess(msg.msgBody));
                    default:
                        throw new MessageException("Error when parsing message: " + msg.receivedString);
                }
            }
        } catch (IOException e) {
            disconnect();
            e.printStackTrace();
        }
    }
    
    private static class Message {
        private MsgType msgType;
        private String msgBody;
        private String receivedString;

        private Message(String receivedString) {
            parse(receivedString);
            this.receivedString = receivedString;
        }

        private void parse(String strToParse) {
            try {
                String[] msgTokens = strToParse.split(Constants.MSG_DELIMETER);
                msgType = MsgType.valueOf(msgTokens[Constants.MSG_TYPE_INDEX].toUpperCase());
                if (hasBody(msgTokens)) {
                    msgBody = msgTokens[Constants.MSG_BODY_INDEX];
                }
            } catch (Throwable throwable) {
                throw new MessageException(throwable);
            }
        }
        
        private boolean hasBody(String[] msgTokens) {
            return msgTokens.length > 1;
        }
    }
}
