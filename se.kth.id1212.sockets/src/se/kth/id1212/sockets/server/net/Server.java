/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import se.kth.id1212.sockets.server.controller.Controller;


/**
 *
 * @author Perttu Jääskeläinen
 */
public class Server {
    
    private int         PORT_NO         = 8080;     // default port number
    private final int   LINGER_TIME     = 10000;    // linger time when closing socket
    private final int   SOCKET_TIMEOUT  = 30000;    // time before timing out a connection
    private final Controller contr = new Controller();
    
    public static void main (String[] args) {
        Server server = new Server();
        server.parseArgs(args);
        server.serve();
    }
    
    
    private void serve() {
        try {
            ServerSocket server = new ServerSocket(PORT_NO);
            while (true) {
                Socket client = server.accept();
                startHandler(client);
            }
        } catch (IOException e) {
            System.out.println("Error when creating server socket with port: " + PORT_NO);
        }    
    }
    
    public String guess(String guess) {
        return contr.processGuess(guess);
    }
    
    private void startHandler(Socket client) throws SocketException {
        client.setSoLinger(true, LINGER_TIME);
        client.setSoTimeout(SOCKET_TIMEOUT);
        ClientHandler handler = new ClientHandler(this, client);
        Thread clientThread = new Thread(handler);
        clientThread.setPriority(Thread.MAX_PRIORITY);
        clientThread.run();
    }
    
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
