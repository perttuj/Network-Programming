package se.kth.id1212.sockets.client.view;

/**
 * Defines all commands that can be performed by a user of the hangman game.
 */
public enum Command {
    /**
     * Connect to the server, first param is the IP and the second is the port number
     */
    CONNECT,
    /**
     * Specified when the user wants to make a guess for a letter or the entire word
     */
    GUESS,
    /**
     * Specified to start a new game
     */
    NEWWORD,
    /**
     * Quit the chat application.
     */
    QUIT,
    /**
     * Specified when the input is in incorrect format
     */
    INCORRECT_FORMAT,
}
