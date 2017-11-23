package client.net;
/**
 * Interface for the listener which is handling responses from the server
 */
public interface ResponseHandler {
    /**
     * Handles the message received from the server
     * @param message The message from the server.
     */
    public void handleMsg(String message);
}