package sockets.client.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import sockets.client.net.ServerConnection;
import sockets.client.net.ResponseHandler;

/**
 * ClientController used by all users for communicating with the server
 * All communication done with the server are done by seperate threads,
 * through the use of the 'CompletableFuture' class
 * @author Perttu Jääskeläinen
 */
public class ClientController {
    // reference to the specific serverConnection 
    private final ServerConnection serverConnection = new ServerConnection();
    /**
     * Connect to the specified host-IP, portnr 'port' and create a handler
     * 'outputHandler' reference for the handler thread on the server side 
     * @param host          IP- address to connect to
     * @param port          portnumber to use
     * @param response      reference to a 'ResponseHandler' connected to the user
     */
    public void connect(String ip, int port, ResponseHandler response) {
        CompletableFuture.runAsync(() -> {
            try {
                serverConnection.connect(ip, port, response);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }).thenRun(() -> response.handleMsg("Succesfully connected to IP:" + ip + " PORT:" + port));
    }
    /**
     * Disconnect from server
     */
    public void disconnect() {
        CompletableFuture.runAsync(() -> {
            try {  
                serverConnection.disconnect();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
    /**
     * Start a new game (generate a new word)
     */
    public void newGame() {
        CompletableFuture.runAsync(() -> serverConnection.newGame());
    }
    /**
     * Send a guess to the server
     * @param command 
     */
    public void sendGuess(String command) {
        CompletableFuture.runAsync(() -> serverConnection.sendGuess(command));
    }
}
