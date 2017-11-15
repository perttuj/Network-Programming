package se.kth.id1212.sockets.client.view;

import static se.kth.id1212.sockets.common.Constants.*;

/**
 *  Wrapper class for CommandLine input
 * @author Perttu Jääskeläinen
 */
public class CommandLine {
    
    private String comm;
    private Command cmd;
    private String message;
    
    public CommandLine (String command) {
        extract(command);
    }
    
    public String getCommand() {
        return this.comm;
    }
    public Command getCmd() {
        return this.cmd;
    }
    
    public String getMsg() {
        return this.message;
    }
    private void extract(String command) {
        this.comm = command;
        extractCmd(command);
        extractMsg(command);
    }
    private void extractCmd(String command) {
        String[] split = command.split("\\s+");
        try {
            this.cmd = Command.valueOf(split[COMMAND_INDEX].toUpperCase());
        } catch (IllegalArgumentException e) {
            this.cmd = Command.INCORRECT_FORMAT;
        }     
    }
    private void extractMsg(String command) {
        String[] commands = command.split("\\s+");
        try {
            this.message = commands[MESSAGE_INDEX];
        } catch (IndexOutOfBoundsException e) {
            this.message = command;
        }
    }
}
