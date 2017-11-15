/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import se.kth.id1212.sockets.common.Constants;
import se.kth.id1212.sockets.common.MessageException;
import se.kth.id1212.sockets.common.MsgType;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class PlayerHandler implements Runnable {
    
    private final Socket playerSocket;
    private final GameServer server;
    private String currentWord;
    private String hiddenWord;
    private int currentScore;
    private int tries;
    private volatile boolean connected;
    private boolean playing;
    
    public PlayerHandler (GameServer server, Socket player) {
        this.playerSocket = player; 
        this.server = server;
        this.connected = true;
        this.currentScore = 0;
        this.currentWord = server.getWord();
        this.tries = currentWord.length();
    }
    
    private void disconnect() {
        try {
            playerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }
    
    private boolean processResponse(String newHidden) {
        if (newHidden.equals(hiddenWord) || newHidden.equals("Invalid guess")) {
            return false;
        } else  {
            return true;
        }
    }
    
    private boolean completedWord() {
        return currentWord.equals(hiddenWord);
    }
    
    private String gameDone() {
        currentScore++;
        playing = false;
        StringBuilder sb = new StringBuilder();
        sb.append("Congratulations, you completed the word: " + currentWord);
        sb.append("\nYour new score is: " + currentScore);
        sb.append("\nWrite 'NEWWORD' to play again");
        return sb.toString();
    }
    
    private String gameOver() {
        currentScore--;
        playing = false;
        StringBuilder sb = new StringBuilder();
        sb.append("Game over. The correct word was: " + currentWord);
        sb.append("\nYour new score is: " + currentScore);
        sb.append("\nWrite 'NEWWORD' to play again");
        return sb.toString();
    }

    private String succesfulGuess() {
        if (completedWord()) {
            return gameDone();
        }
        String response = ("Guess succesful, current word: " + hiddenWord);
        return response;
    }
    
    private String unsuccesfulGuess() {
        String response = ("Guess unsuccesful, tries remaining: " + tries);
        return response;
    }
    private String invalidGuess() {
        String response = "Invalid guess, either guess a letter or the entire word";
        return response;
    }
    
    private void newGame() {
        currentWord = server.getWord();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentWord.length(); i++) {
            sb.append("-");
        }
        hiddenWord = sb.toString();
        tries = currentWord.length();
        currentScore--;
    }

    @Override
    public void run() {
        try {
            boolean flush = true;
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(playerSocket.getOutputStream(), flush); 
            while (connected) {
                Message msg = new Message(clientReader.readLine());
                switch (msg.type) {
                    case NEWWORD:
                        if (!playing) {
                            playing = true;
                            newGame();
                        } else {
                            clientWriter.println("Already playing. Start new game anyway? YES/NO (Score will be decremented if a new game is started)");
                            String line = clientReader.readLine().toUpperCase();
                            if (line.contains("YES")) {
                                newGame();
                            } else {
                                clientWriter.println("Continuing");
                            }
                        }
                        break;
                    case DISCONNECT:
                        clientWriter.println("Disconnecting..");
                        disconnect();
                        break;
                    case GUESS:
                        if (!playing) {
                            clientWriter.println("Currently not playing. Write 'NEWWORD' to start a new game");
                            break;
                        }
                        String newHidden = server.guess(msg.body, currentWord, hiddenWord);
                        boolean succesful = processResponse(newHidden);
                        if (succesful) {
                            hiddenWord = newHidden;
                            clientWriter.println(succesfulGuess());
                        } else {
                            if (newHidden.equals("Invalid guess")) {
                                clientWriter.println(invalidGuess());
                            } else {
                                tries--;
                                if (tries == 0) {
                                    clientWriter.println(gameOver());
                                } else {
                                    clientWriter.println(unsuccesfulGuess());
                                }
                            }
                        }
                        break;
                    default:
                        throw new MessageException("Error when parsing message: " + msg.fullMsg);
                }
            }
        } catch (IOException e) {
            disconnect();
            System.out.println("Disconnecting..");
        }
    }
    
    private static class Message {
        private MsgType type;
        private String body;
        private String fullMsg;

        private Message(String receivedString) {
            parse(receivedString);
            this.fullMsg = receivedString;
        }

        private void parse(String strToParse) {
            try {
                String[] msgTokens = strToParse.split(Constants.MSG_DELIMETER);
                type = MsgType.valueOf(msgTokens[Constants.MSG_TYPE_INDEX].toUpperCase());
                if (msgTokens.length > 1) {
                    body = msgTokens[Constants.MSG_BODY_INDEX];
                }
            } catch (Throwable throwable) {
                throw new MessageException(throwable);
            }
        }
    }
}
