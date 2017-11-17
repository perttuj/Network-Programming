package common;

/**
 *  Defines constants used by both client and server.
 * @author Perttu Jääskeläinen
 */
public class Constants {
    /**
     * Seperates the message type from the message body
     */
    public static final String DELIMETER = "##";
    /**
     *  The message type is specified in the first index
     */
    public static final int TYPE_INDEX = 0;
    /**
     * The message body is specified in the second index
     */
    public static final int MESSAGE_INDEX = 1;
}
