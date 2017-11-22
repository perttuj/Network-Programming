package server.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import common.Constants;
import common.ServerMessageTypes;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
/**
 *  Class for handling all player actions and responses
 * for playing the hangman game
 * @author Perttu Jääskeläinen
 */
public class PlayerHandler implements Runnable {
    
    private final GameServer server;
    
    // connection info
    private final SocketChannel playerChannel;
    private ByteBuffer reader = ByteBuffer.allocateDirect(4096); // replace value with constant
    private Queue<String> responses = new ArrayDeque<>();
    private volatile boolean writing;
    
    // game info
    private int currentScore;
    private int tries;
    private boolean playing; 
    private volatile String message = null;
    private String currentWord;
    private String hiddenWord;
    private List<String> guesses;
    
    public PlayerHandler (SocketChannel channel, GameServer server) {
        this.playerChannel = channel; 
        this.server = server;
        this.writing = false;
        this.currentScore = 0;
    }
    public void sendAll() throws IOException {
        synchronized (responses) {
            if (!responses.isEmpty()) {
                StringBuilder sb = new StringBuilder(ServerMessageTypes.RESPONSE.toString() + Constants.DELIMETER);
                for (String b : responses) {
                    sb.append(b + " ");
                }
                server.print("writing : " + sb.toString());
                ByteBuffer msg = ByteBuffer.wrap(sb.toString().getBytes());
                playerChannel.write(msg);
            }
            responses.clear();
        }
        writing = false;
    }
    protected void read() throws IOException {
        reader.clear();
        int bytesRead = playerChannel.read(reader);
        if (bytesRead == -1) {
            throw new IOException("Failed to read from player channel");
        }
        message = extractFromBuffer(reader);
        server.print("read msg: " + message);
        ForkJoinPool.commonPool().execute(this);
    }
    protected void add(String s) {
        responses.add(s);
    }
    protected void disconnect() throws IOException {
        playerChannel.close();
    }
    private String extractFromBuffer(ByteBuffer buf) {
        buf.flip();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return new String(bytes);
    }
    /**
     * returns true if the user has guessed all letters correctly
     * @return  true or false depending on if the user is done
     */
    private boolean completedWord() {
        return currentWord.equals(hiddenWord);
    }
    /**
     * Message when successfully guessing a word
     * @return  the game done screen text
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
     * @return      the game over screen text
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
     * @return      a proper message for a successful guess
     */
    private String succesfulGuess() {
        if (completedWord()) {
            return gameDone();
        }
        String response = ("Guess succesful! Current word: " + hiddenWord + ", tries remaining: " + tries);
        return response;
    }
    /**
     * String when a guess is unsuccessful (incorrect guess, not to be confused with invalid guess)
     * @return      response for an unsuccessful guess
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
     * Hidden word is replaced with dashes (-) equal to the word to be guessed length,
     * currentWord is replaced with the new word and tries is replaced the length of 
     * the new word (word of length 5 has 5 guesses)
     */
    private void newGame() {
        currentWord = server.contr.getWord();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentWord.length(); i++) {
            sb.append("-");
        }
        hiddenWord = sb.toString();
        tries = currentWord.length();
        guesses = new ArrayList<String>();
    }
    /**
     * checks if the response from the model was successful (correct guess)
     * or unsuccessful (incorrect guess)
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
        String newHidden = server.contr.processGuess(guess, currentWord, hiddenWord);
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
    private void writable() {
        server.writable(playerChannel);
        writing = true;
    }
    private void guess(Message msg) {
        if (!playing) {
            add("Currently not playing. Write 'NEWWORD' to start a new game");
        } else if (msg.body == null) {
            add("error when parsing msg body, please try again");
        } else {
            add(guess(msg.body));
        }
        server.print("guessing done: " + responses.toString());
        writable();
    }
    private void newword() {
        if (!playing) {
            String s = "starting new game";
            add(s);
            playing = true;
            newGame();
            add(getInfo());
        } else {
            String s = "Already playing. To start a new game, type 'STOP'";
            add(s);
        }
        writable();
    }
    private void illegalType(Message msg, String desc) {
        String s = desc == null ? "" : desc;
        add("Illegal type " + msg.type + s);
        writable();
    }
    /**
     * Main method run by the users serverside 'PlayerHandler' thread.
     * The thread stays in the while loop until a disconnect is initiated,
     * after which it simply exits.
     */
    @Override
    public void run() {
        try {
            Message msg = new Message(message);
            server.print("handler msg type, body: " + msg.type + ", " + msg.body);
            message = null;
                switch (msg.type) {
                    case NEWWORD:
                        newword();
                        break;
                    case DISCONNECT:
                        disconnect();
                        break;
                    case GUESS:
                        guess(msg);
                        break;
                    case RESPONSE:
                        illegalType(msg, "should only be used by the server for responses");
                        break;
                    default:
                        throw new IllegalArgumentException("Error when parsing message: " + msg.fullMsg);
                }
        } catch (IOException e) {
            System.out.println("Disconnecting.." + e);
        }
    }
    /**
     * Class for handling different parts of a message, this includes:
     * Message type, message body and the original fullMsg
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
                    body = message[Constants.MESSAGE_INDEX].toLowerCase().trim();
                } else {
                    body = null;
                }
            } catch (Throwable throwable) {
                throw new IllegalArgumentException("Error when parsing message " + throwable);
            }
        }
    }
}
