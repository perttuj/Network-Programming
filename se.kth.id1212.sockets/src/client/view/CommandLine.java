package client.view;

import static common.Constants.*;

/**
 *  Wrapper class for console inputs
 * @author Perttu Jääskeläinen
 */
public class CommandLine {
    
    private final String DELIMETER = " ";
    private String message; // the entire message
    private Command cmd;    // the commad in the message
    private String body;    // the body of the message
    
    public CommandLine (String command) {
        extract(command);
    }
    
    public String getMessage() {
        return this.message;
    }
    public Command getCommand() {
        return this.cmd;
    }
    public String getBody() {
        return this.body;
    }
    private void extract(String message) {
        this.message = message;
        extractCmd(message);
        extractBody(message);
    }
    private void extractCmd(String command) {
        String[] split = command.split(DELIMETER);
        try {
            this.cmd = Command.valueOf(split[TYPE_INDEX].toUpperCase());
        } catch (IllegalArgumentException e) {
            this.cmd = Command.INCORRECT_FORMAT;
        }     
    }
    private void extractBody(String command) {
        String[] commands = command.split(DELIMETER);
        try {
            this.body = commands[MESSAGE_INDEX];
        } catch (IndexOutOfBoundsException e) {
            this.body = null;
        }
    }
}
