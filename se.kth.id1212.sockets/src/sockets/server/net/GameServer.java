package sockets.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import sockets.server.controller.ServerController;
/**
 *  Server for handling new connections for new players
 * @author Perttu Jääskeläinen
 */
public class GameServer {
    
    private int         PORT_NO         = 8080;         // default port number
    private final int   LINGER_TIME     = 30000;        // linger time when closing socket
    private final int   SOCKET_TIMEOUT  = 1800000;      // time before timing out a connection
    private final ServerController contr = new ServerController();
    
    public static void main (String[] args) {
        GameServer server = new GameServer();
        server.parseArgs(args);
        server.serve();
    }
    /**
     * The main thread spends its lifetime here, accepting new connections and 
     * assigning them a seperate PlayerHandler thread
     */
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
    /**
     * Method to handle creation of a new thread for a user with a reference to the controller,
     * to avoid redundant instances of opening and indexing the same wordfile for each user
     * @param player    the playersocket the thread will communicate with
     * @throws SocketException  if assigning values to the socket linger and/or timeout fails
     */
    private void startGame(Socket player) throws SocketException  {
        player.setSoLinger(true, LINGER_TIME);
        player.setSoTimeout(SOCKET_TIMEOUT);
        PlayerHandler handler = new PlayerHandler(contr, player);
        Thread playerThread = new Thread(handler);
        playerThread.setPriority(Thread.MAX_PRIORITY);
        playerThread.run();
    }
    /**
     * Used to parse arguments received when compiling the server - if a port number is not specified,
     * use the default portnumber defined in this class
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
