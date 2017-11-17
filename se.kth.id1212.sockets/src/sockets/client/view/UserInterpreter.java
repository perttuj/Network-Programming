package sockets.client.view;

import java.io.IOException;
import java.util.Scanner;
import sockets.client.controller.ClientController;
import sockets.client.net.ResponseHandler;

/**
 * Class for handling all userside communication when playing
 * the Hangman Game
 * @author Perttu Jääskeläinen
 */
public class UserInterpreter implements Runnable {
    private static final String PROMPT = ">> ";
    private final SafePrinter safePrinter = new SafePrinter();
    private final Scanner console = new Scanner(System.in);
    private final int PORT_INDEX = 2;
    private final int IP_INDEX = 1;
    private String ip = "127.0.0.1";
    private int port = 8080;
    private boolean running = false;
    private boolean connected;
    private ClientController contr;

    /**
     * The start method for the class, initiated from the Startup.java class.
     * Creates reference to controller and starts the thread that will handle all
     * user input
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        contr = new ClientController();
        new Thread(this).start();
    }
    /**
     * Disconnects the user from the specified IP and port but does not
     * stop the program from running.
     */
    private void disconnect() {
        connected = false;
        safePrinter.println("Disconnecting");
        contr.disconnect();
    }
    /**
     * Quits running the program entirely
     */
    private void quit() {
        running = false;
        if (connected) {
            contr.disconnect();   
        }
        safePrinter.println("Quitting");
    }
    /**
     * returns the max number of two integers
     * @param a first integer
     * @param b second integer
     * @return  the largest integer
     */
    private int max(int a, int b) {
        if (a < b)
            return b;
        return a;
    }
    /**
     * Connects a user to the specified IP and portnumber. If no port/IP is specified,
     * uses the default values
     * @param IPport    the message from the user, which should include the IP and portnumber
     */
    private void connect(String IPport) {
        String[] s = IPport.split(" ");
        if (s.length <= max(PORT_INDEX, IP_INDEX)) {
            safePrinter.println("Incorrect format when specifying IP and port, using default values for IP: " + ip + ", PORT: " + port);
        } else {
            try {
                port = Integer.parseInt(s[PORT_INDEX]);
                ip = s[IP_INDEX];
                safePrinter.println("Using specified IP: " + ip + " and PORT: " + port);
            } catch (NumberFormatException e) {
                safePrinter.println("error when parsing port number, using default values");
            }
        }
        safePrinter.println("Connecting..");
        contr.connect(ip, port, new ConsoleOutput());
        connected = true;
    }
    /**
     * Prints out a message informing the user that it is not connected
     */
    private void notConnected() {
        safePrinter.println("Not connected");
    }
    /**
     * Starts a new game, generating a new word for the user
     */
    private void startGame() {
        contr.newGame();
    }
    /**
     * Sends a guess to the server
     * @param guess the guess to be sent
     */
    private void sendGuess(String guess) {
        contr.sendGuess(guess);
    }
    /**
     * Main method for reading user input. Each row is saved as a new 'CommandLine'
     * object, which splits the input into a command and a body.
     */
    @Override
    public void run() {
        safePrinter.println(usageMessage("Welcome!"));
        while (running) {
            try {
                CommandLine line = new CommandLine(readLine());
                switch (line.getCommand()) {
                    case DISCONNECT:
                        if (connected)
                            disconnect();
                        else 
                            safePrinter.println("not yet connected");
                        break;
                    case CONNECT:
                        if (!connected)
                            connect(line.getMessage());
                        else 
                            safePrinter.println("already connected");
                        break;
                    case GUESS:
                        if (connected) {
                            safePrinter.println(line.getBody());
                            sendGuess(line.getBody());
                        } else {
                            notConnected();
                        }
                        break;
                    case NEWWORD:
                        if (connected) {
                            startGame();
                        } else {
                            notConnected();
                        }
                        break;
                    case HELP:
                        safePrinter.println(usageMessage(""));
                        break;
                    case QUIT:
                        quit();
                        break;
                    default:
                        safePrinter.println("INCORRECT INPUT, write HELP for commands");
                        break;
                }
            } catch (Exception e) {
                safePrinter.println("Error when reading from console");
            }
        }
    }
    /**
     * Returns the default usage message for an user. This includes all 
     * commands and their descriptors
     * @param current   If an initial message is wanted, append the commands to this message
     * @return          a constructed usage message
     */
    private String usageMessage(String current) {
        StringBuilder sb = new StringBuilder(current);
        sb.append("\nCOMMANDS:\n");
        for (Command c : Command.values()) {
            sb.append(c + " - " + c.getDescription() + "\n");
        }
        sb.append("\nNOTE: Seperate commands from their respective arguments using spaces");
        return sb.toString();
    }
    
    /**
     * Prompts the user and reads the next line received from the console
     * @return 
     */
    private String readLine() {
        safePrinter.print(PROMPT);
        return console.nextLine();
    }
    /**
     * Used by Listener to print messages received from the server to the user
     */
    private class ConsoleOutput implements ResponseHandler {
        @Override
        public void handleMsg(String msg) {
            safePrinter.println(msg);
            safePrinter.print(PROMPT);
        }
    }
}