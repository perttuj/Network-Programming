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
public class GameServer {
    
    private int         PORT_NO         = 8080;     // default port number
    private final int   LINGER_TIME     = 10000;    // linger time when closing socket
    private final int   SOCKET_TIMEOUT  = 30000;    // time before timing out a connection
    private final Controller contr = new Controller();
    
    public static void main (String[] args) {
        GameServer server = new GameServer();
        server.parseArgs(args);
        server.serve();
    }
    
    
    private void serve() {
        try {
            ServerSocket server = new ServerSocket(PORT_NO);
            while (true) {
                Socket playerSocket = server.accept();
                startGame(playerSocket);
            }
        } catch (IOException e) {
            System.out.println("Error when creating server socket with port: " + PORT_NO);
        }    
    }
    
    String guess(String guess, String word, String hidden) {
        return contr.processGuess(guess, word, hidden);
    }
    
    String getWord() {
        return contr.getWord();
    }
    
    private void startGame(Socket player) throws SocketException {
        player.setSoLinger(true, LINGER_TIME);
        player.setSoTimeout(SOCKET_TIMEOUT);
        PlayerHandler handler = new PlayerHandler(this, player);
        Thread playerThread = new Thread(handler);
        playerThread.setPriority(Thread.MAX_PRIORITY);
        playerThread.run();
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
