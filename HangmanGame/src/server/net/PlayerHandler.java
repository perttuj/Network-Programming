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
import java.util.concurrent.ForkJoinPool;

/**
 * Class for handling all player actions and responses for playing the hangman
 * game
 *
 * @author Perttu Jääskeläinen
 */
public class PlayerHandler implements Runnable {

    private final GameServer server;

    // connection info
    private final SocketChannel playerChannel;
    private ByteBuffer reader = ByteBuffer.allocateDirect(Constants.MAX_MESSAGE_SIZE);
    private Queue<String> responses = new ArrayDeque<>();

    // game info
    private int currentScore;
    private int tries;
    private boolean playing;
    private volatile String message = null;
    private String currentWord;
    private String hiddenWord;
    private List<String> guesses;

    public PlayerHandler(SocketChannel channel, GameServer server) {
        this.playerChannel = channel;
        this.server = server;
        this.currentScore = 0;
    }

    /**
     * Reads all info written to the queue of strings in responses, gathering
     * them in a stringbuilder and sending to the user in a bytebuffer
     *
     * @throws IOException if writing to the user channel fails
     */
    public void sendAll() throws IOException {
        synchronized (responses) {
            if (!responses.isEmpty()) {
                StringBuilder sb = new StringBuilder(ServerMessageTypes.RESPONSE.toString() + Constants.DELIMETER);
                for (String b : responses) {
                    sb.append("\n" + b);
                }
                ByteBuffer msg = ByteBuffer.wrap(sb.toString().getBytes());
                playerChannel.write(msg);
            }
            responses.clear();
        }
    }

    /**
     * Reads all bytes from the userchannel into a ByteBuffer, then starts a
     * thread that handles the message received
     *
     * @throws IOException if reading from the channel fails
     */
    protected void read() throws IOException {
        reader.clear();
        int bytesRead = playerChannel.read(reader);
        if (bytesRead == -1) {
            throw new IOException("Failed to read from player channel");
        }
        message = extractFromBuffer(reader);
        ForkJoinPool.commonPool().execute(this);
    }

    /**
     * Adds the string to the queue of responses to be sent to the user
     *
     * @param s the string to add to the queue
     */
    protected void add(String s) {
        responses.add(s);
    }

    /**
     * Adds a response string to be sent to the user, specifying that the
     * connection was made
     */
    protected void connected() {
        String s = "succesfully connected - type 'NEWWORD' to start a game";
        add(s);
        writable();
    }

    /**
     * Disconnects the user channel
     *
     * @throws IOException if disconnecting the channel fails
     */
    protected void disconnect() throws IOException {
        playerChannel.close();
    }

    /**
     * Extracts a String from the specified ByteBuffer
     *
     * @param buf Buffer to be extracted from
     * @return String extracted from buffer
     */
    private String extractFromBuffer(ByteBuffer buf) {
        buf.flip();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return new String(bytes);
    }

    /**
     * returns true if the user has guessed all letters correctly
     *
     * @return true or false depending on if the user is done
     */
    private boolean completedWord() {
        return currentWord.equals(hiddenWord);
    }

    /**
     * Message when successfully guessing a word
     *
     * @return the game done screen text
     */
    private String gameDone() {
        currentScore++;
        playing = false;
        String response = "Congratulations, you completed the word: " + currentWord + " with " + tries + " tries remaining.\n"
                + "Your new score is: " + currentScore + ". Write 'NEWWORD' to play again";
        return response;
    }

    /**
     * Message when running out of tries when guessing the word
     *
     * @return the game over screen text
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
     *
     * @return a proper message for a successful guess
     */
    private String succesfulGuess() {
        if (completedWord()) {
            return gameDone();
        }
        String response = ("Guess succesful! Current word: " + hiddenWord + ", tries remaining: " + tries);
        return response;
    }

    /**
     * String when a guess is unsuccessful (incorrect guess, not to be confused
     * with invalid guess)
     *
     * @return response for an unsuccessful guess
     */
    private String incorrectGuess() {
        String response = ("Guess unsuccesful! Current word: " + hiddenWord + ", tries remaining: " + tries);
        return response;
    }

    /**
     * String when a guess is invalid
     *
     * @return response for an invalid guess
     */
    private String invalidGuess() {
        String response = "Invalid guess, either guess a letter or the entire word - only characters allowed";
        return response;
    }

    /**
     * checks if the response from the model was successful (correct guess) or
     * unsuccessful (incorrect guess)
     *
     * @param newHidden the updated hidden word received from the model
     * @return true or false, (correct or incorrect guess)
     */
    private boolean processResponse(String newHidden) {
        if (newHidden.equals(hiddenWord)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Prints out user information
     */
    private String getInfo() {
        String response = "Current word is " + currentWord.length() + " characters. You have " + tries + " guesses remaining";
        return response;
    }

    /**
     * Processes a user guess, saving it in a list of past guesses to assure
     * non-multiple same letter/word guesses.
     *
     * @param guess the guessed letter/word by the user
     * @return a reply depending on the result of the guess
     */
    private String guess(String guess) {
        if (guesses.contains(guess)) {
            return "You already made the same guess, try a new letter or word!";
        }
        String newHidden = server.contr.processGuess(guess, currentWord, hiddenWord);
        if (newHidden == null) {
            return invalidGuess();
        }
        guesses.add(guess);
        boolean succesful = processResponse(newHidden);
        if (succesful) {
            hiddenWord = newHidden;
            return succesfulGuess();
        } else {
            tries--;
            if (tries == 0) {
                return gameOver();
            } else {
                return incorrectGuess();
            }
        }
    }

    /**
     * Tells the server that this PlayerHandler channel has writable data, if
     * such data exists
     */
    private void writable() {
        if (!responses.isEmpty()) {
            server.writable(playerChannel);
        }
    }

    /**
     * Processes a guess made by the user, adding appropriate messages
     *
     * @param msg the message received from the user
     */
    private void guess(Message msg) {
        if (!playing) {
            add("Currently not playing. Write 'NEWWORD' to start a new game");
        } else if (msg.body == null) {
            add("error when parsing msg body, please try again");
        } else {
            add(guess(msg.body));
        }
    }

    /**
     * Initiates a new game, generating a new word from the server and replacing
     * existing values with initial values. Hidden word is replaced with dashes
     * (-) equal to the word to be guessed length, currentWord is replaced with
     * the new word and tries is replaced the length of the new word (word of
     * length 5 has 5 guesses)
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
     * Called by the run() method to generate specific user replies, depending
     * on if the user is currently playing or not
     */
    private void newword() {
        if (!playing) {
            String s = "starting new game";
            add(s);
            playing = true;
            newGame();
            add(getInfo());
        } else {
            String s = "Already playing, finish the current word";
            add(s);
        }
    }

    /**
     * Returns a message if the specified type is illegal
     *
     * @param msg the message received
     * @param desc a descriptor for the illegal type message
     */
    private void illegalType(Message msg, String desc) {
        String s = desc == null ? "" : desc;
        add("Illegal type " + msg.type + s);
        writable();
    }

    /**
     * Main method run by the users serverside 'PlayerHandler' thread. The
     * thread executes and handles the request specified by the user, adding new
     * responses if needed.
     */
    @Override
    public void run() {
        try {
            Message msg = new Message(message);
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
            writable();
        } catch (IOException e) {
            System.out.println("Disconnecting.." + e);
        }
    }

    /**
     * Class for handling different parts of a message, this includes: Message
     * type, message body and the original fullMsg
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
