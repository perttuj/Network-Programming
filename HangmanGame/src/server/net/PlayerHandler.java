package server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import server.controller.ServerController;
import common.Constants;
import common.ServerMessageTypes;
/**
 *  Class for handling all player actions and responses
 * for playing the hangman game
 * @author Perttu Jääskeläinen
 */
public class PlayerHandler implements Runnable {
    
    private final Socket playerSocket;
    private final ServerController contr;
    private String currentWord;
    private String hiddenWord;
    private int currentScore;
    private int tries;
    private volatile boolean connected;
    private boolean playing;
    private List<String> guesses;
    
    public PlayerHandler (ServerController controller, Socket player) {
        this.playerSocket = player; 
        this.contr = controller;
        this.connected = true;
        this.currentScore = 0;
    }
    /**
     * Closes the user socket, ending the life of the running 
     * 'PlayerHandler' thread and closing the socket.
     */
    private void disconnect() {
        try {
            playerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }
    /**
     * returns true if the user has guessed all letters correctly
     * @return  true or false depending on if the user is done
     */
    private boolean completedWord() {
        return currentWord.equals(hiddenWord);
    }
    /**
     * Message when succesfully guessing a word
     * @return  the gamedone screen text
     */
    private String gameDone() {
        currentScore++;
        playing = false;
        String response = "Congratulations, you completed the word: " + currentWord + " with " + tries + " tries remaining. "
                + "Your new score is: " + currentScore + ". Write 'NEWWORD' to play again";
        return response;
    }
    /**
     * Message when running out of tries when guessing the word
     * @return      the gameover screen text
     */
    private String gameOver() {
        currentScore--;
        playing = false;
        String response = "Game over. The correct word was: " + currentWord + ", your new score is: " + currentScore + ". Write 'NEWWORD' to play again";
        return response;
    }
    /**
     * Generates a response for a correct guess, if the word is now complete,
     * generate a gameDone message.
     * @return      a proper message for a succesful guess
     */
    private String succesfulGuess() {
        if (completedWord()) {
            return gameDone();
        }
        String response = ("Guess succesful! Current word: " + hiddenWord + ", tries remaining: " + tries);
        return response;
    }
    /**
     * String when a guess is unsuccesful (incorrect guess, not to be confused with invalid guess)
     * @return      response for an unsuccesful guess
     */
    private String unsuccesfulGuess() {
        String response = ("Guess unsuccesful! Current word: " + hiddenWord + ", tries remaining: " + tries);
        return response;
    }
    /**
     * String when a guess is invalid
     * @return  response for an invalid guess
     */
    private String invalidGuess() {
        String response = "Invalid guess, either guess a letter or the entire word";
        return response;
    }
    /**
     * Initiates a new game, generating a new word from the server
     * and replacing existing values with initial values.
     * Hidden word is replaced with dashes (-) equal to the word to be guessed's length,
     * currentWord is replaced with the new word and tries is replaced the length of 
     * the new word (word of length 5 has 5 guesses)
     */
    private void newGame() {
        currentWord = contr.getWord();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentWord.length(); i++) {
            sb.append("-");
        }
        hiddenWord = sb.toString();
        tries = currentWord.length();
        guesses = new ArrayList<String>();
    }
    /**
     * checks if the response from the model was succesful (correct guess)
     * or unsuccesful (incorrect guess)
     * @param newHidden     the updated hidden word received from the model
     * @return              true or false, (correct or incorrect guess)
     */
    private boolean processResponse(String newHidden) {
        if (newHidden.equals(hiddenWord)) {
            return false;
        } else  {
            return true;
        }
    }
    /**
     * Checks if the given string only contains characters
     * @param s the string to check 
     * @return true if only characters are found, else false
     */
    private boolean isLetter(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Prints out user information
     */
    private String getInfo() {
        String response = "Current word is " + currentWord.length() + " characters. You have " + tries + " guesses remaining";
        return response;
    }
    /**
     * Processes a user guess, saving it in a list of past guesses to assure non-multiple 
     * same letter/word guesses.
     * @param guess     the guessed letter/word by the user
     * @return          a reply depending on the result of the guess
     */
    private String guess(String guess) {
        if (!isLetter(guess)) {
            return "Incorrect format, please only use letters when guessing";
        }
        if (guesses.contains(guess)) {
            return "You already made the same guess, try a new letter or word!";
        }
        guesses.add(guess);
        String newHidden = contr.processGuess(guess, currentWord, hiddenWord);
        if (newHidden == null) {
            return invalidGuess();
        }
        boolean succesful = processResponse(newHidden);
        if (succesful) {
            hiddenWord = newHidden;
            return succesfulGuess();
        } else {
            tries--;
            if (tries == 0) {
                return gameOver();
            } else {
                return unsuccesfulGuess();
            }
        }
    }
    /**
     * Creates a new ClientMessenger object for handling input and output streams from the user
     * @param client    The client socket to read and write from/to
     * @param flush     If autoflush is to be used when writing to the user
     * @return          a new ClientMessenger object
     * @throws IOException  if reading from the clientSocket's input or output stream is unsuccesful
     */
    private ClientMessenger newMessenger(Socket client, boolean flush) throws IOException {
        try {
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(client.getOutputStream(), flush); 
            return new ClientMessenger(clientReader, clientWriter, connected);
        } catch (IOException e) {
            throw new IOException("Error when creating output and inputstreams: " + e);
        }
    }
    /**
     * Main method run by the users serverside 'PlayerHandler' thread.
     * The thread stays in the while loop until a disconnect is initiated,
     * after which it simply exits.
     */
    @Override
    public void run() {
        try {
            boolean autoFlush = true;
            ClientMessenger client = newMessenger(playerSocket, autoFlush);
            while (connected) {
                Message msg = new Message(client.readLine());
                switch (msg.type) {
                    case NEWWORD:
                        if (!playing) {
                            client.respond("Starting new game");
                            playing = true;
                            newGame();
                            client.respond(getInfo());
                        } else {
                            client.respond("Already playing. Start a new game anyway? YES/NO (Score will be decremented if a new game is started)");
                            String line = client.readLine().toUpperCase();
                            if (line.contains("YES")) {
                                client.respond("Starting new game");
                                newGame();
                            } else {
                                client.respond("Continuing");
                                client.respond(getInfo());
                            }
                        }
                        break;
                    case DISCONNECT:
                        disconnect();
                        break;
                    case GUESS:
                        if (!playing) {
                            client.respond("Currently not playing. Write 'NEWWORD' to start a new game");
                            break;
                        }
                        if (msg.body == null) {
                            client.respond("error when parsing msg body, please try again");
                        } else {
                            client.respond(guess(msg.body));
                        }
                        break;
                    case RESPONSE:
                        client.respond("Illegal type - should only be used by the server for responses");
                        break;
                    default:
                        throw new IllegalArgumentException("Error when parsing message: " + msg.fullMsg);
                }
            }
            client.disconnected();
        } catch (IOException e) {
            disconnect();
            System.out.println("Disconnecting..");
        }
    }
    /**
     * Class used by the PlayerHandler to message and read from the user.
     * The specified BufferedReader and PrintWriter need to be pre-defined 
     * from the user socket when creating a new ClientMessenger.
     */
    private static class ClientMessenger {
        
        private BufferedReader clientReader;
        private PrintWriter clientWriter;
        private volatile boolean connected;
        
        private ClientMessenger(BufferedReader reader, PrintWriter writer, boolean connected) {
            clientReader = reader;
            clientWriter = writer;
            this.connected = connected;
        }
        /**
         * Send a response to the user in correct format
         * @param message   The message to be sent
         */
        private void respond(String message) {
            if (connected) {
                clientWriter.println(ServerMessageTypes.RESPONSE.toString() + Constants.DELIMETER + message);
            }
        }
        /**
         * Read a line from the user socket
         * @return  the line read from the user
         * @throws IOException  if the socket is closed while waiting for a read
         */
        private String readLine() throws IOException {
            if (!connected)
                return null;
            return clientReader.readLine();
        }
        /**
         * Change the boolean to false when disconnected, 
         * preventing further reads and writes
         */
        private void disconnected() {
            connected = false;
        }
    }
    /**
     * Class for handling different parts of a message, this includes:
     * Message type, message body and the origianl fullMsg
     */
    private static class Message {
        private ServerMessageTypes type;
        private String body;
        private String fullMsg;

        private Message(String fullMessage) {
            parse(fullMessage);
            this.fullMsg = fullMessage;
        }

        private void parse(String fullMessage) {
            try {
                String[] message = fullMessage.split(Constants.DELIMETER);
                type = ServerMessageTypes.valueOf(message[Constants.TYPE_INDEX].toUpperCase());
                if (message.length > Constants.MESSAGE_INDEX) {
                    body = message[Constants.MESSAGE_INDEX].toLowerCase();
                } else {
                    body = null;
                }
            } catch (Throwable throwable) {
                throw new IllegalArgumentException("Error when parsing message " + throwable);
            }
        }
    }
}
