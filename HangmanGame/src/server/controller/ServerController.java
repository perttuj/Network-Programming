package server.controller;

import server.model.WordLogic;

/**
 *  ServerController used by classes in the server 'net' layer to communicate with the
 server side model
 * @author Perttu Jääskeläinen
 */
public class ServerController {
    private final WordLogic model;
    
    public ServerController () {
        this.model = new WordLogic();
    }
    /**
     * Generate a new, randomized word from the model
     * @return  a random word
     */
    public String getWord() {
        return model.getWord();
    }
    /**
     * Process a user guess in the model
     * @param guess the word/letter guessed by the user
     * @param word  the word to be guessed
     * @param hidden    the current progression of the hidden word
     * @return  the updated hidden word
     */
    public String processGuess(String guess, String word, String hidden) {
        return model.processGuess(guess, word, hidden);
    }
}
