package sockets.client.view;
/**
 * Defines all commands that can be performed by a user of the hangman game.
 * Also includes an description which is printed out when starting the program
 * as well as when 'HELP' is entered
 */
public enum Command {
    /**
     * Connect to the server, first param is the IP and the second is the port number
     */
    CONNECT("Connect to the server. "
            + "USAGE: 'CONNECT <IP> <PORT>' for specified IP & Port OR 'CONNECT', which will use the default IP and Port"),
    /**
     * Specified when the user wants to make a guess for a letter or the entire word
     */
    GUESS("Send a guess to the server. " 
            + "USAGE: 'GUESS <LETTER/WORD>'"),
    /**
     * Specified to start a new game
     */
    NEWWORD("Start the game/request new word from server. " 
            + "USAGE: 'NEWWORD')"),
    /**
     * Quit the chat application.
     */
    DISCONNECT("Disconnect from the server. "
            + "USAGE: 'DISCONNECT'"),
    /**
     * Entered to print out all usage commands in this enum class
     */
    HELP("For a list of commands and usages"),
    /**
     * Entered when the user wants to exit the program
     */
    QUIT("Quit the program"),
    /**
     * Specified when the input is in incorrect format
     */
    INCORRECT_FORMAT("Specified when no correct format is entered");
    // Stores a descriptor for each enumenator
    private final String descriptor;
    
    private Command (String descriptor) {
        this.descriptor = descriptor;
    }
    public String getDescription() {
        return this.descriptor;
    }
}
