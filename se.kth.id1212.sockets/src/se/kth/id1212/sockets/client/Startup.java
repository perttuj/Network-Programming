/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class Startup {
    
    private static final int TIMEOUT_HALF_HOUR = 180000;
    private static final int TIMEOUT_HALF_MINUTE = 30000;
    private Socket socket;
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private boolean connected;
    
    public static void main (String[] args) {
        Startup s = new Startup();
        try {
        s.connect();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 8080));
        socket.setSoTimeout(TIMEOUT_HALF_HOUR);
        connected = true;
        boolean autoFlush = true;
        toServer = new PrintWriter(socket.getOutputStream(), autoFlush);
        fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        toServer.println("hello");
        
    }
}
