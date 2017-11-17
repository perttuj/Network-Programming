package sockets.common;

/**
 *  Defines constants used by both client and server.
 * @author Perttu Jääskeläinen
 */
public class Constants {
    /**
     * Separates a message type specifier from the message body.
     */
    public static final String DELIMETER = "##";
    /**
     * The message type specifier is the first token in a message.
     */
    public static final int TYPE_INDEX = 0;
    /**
     * The message body is the second token in a message.
     */
    public static final int MESSAGE_INDEX = 1;
}
