package sockets.server.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  WordLogic for handling everything to do with generating words, guessing words or letters
 and reading from the word file
 * @author Perttu Jääskeläinen
 */
public class WordLogic {
    private final String WORDS_FILE = "resources/words.txt";
    private ArrayList<String> words;
    
    public WordLogic () {
        indexWords(readFile(WORDS_FILE));
    }
    /**
     * Attempts to read the specified file and return a buffered reader for it
     * @param path  the path where the file is to be found
     * @return      the buffered reader
     */
    private BufferedReader readFile(String path) {
        try {
            return new BufferedReader(new FileReader(new File(path)));
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + path);
            System.exit(1);
        }
        return null;
    }
    /**
     * Index all the words read from the BufferedReader into an arrayList
     * @param reader the BufferedReader to read from
     */
    private void indexWords(BufferedReader reader) {
        words = new ArrayList();
        try {
            String word = reader.readLine();
            while (word != null) {
                words.add(word.toLowerCase());
                word = reader.readLine();
            }
        } catch (IOException ex) {
           ex.printStackTrace();
        }
    }
    /**
     * Generate a random index number and return the word at the index
     * @return a randomized word from the list of words found in the defined WordFile path
     */
    public String getWord() {
        int index = (int) (Math.random() * words.size());
        return words.get(index);
    }
    /**
     * Replaces dashes in the hidden word with the guessed letter
     * @param letter    the letter to be inserted into the hidden word
     * @param word      the word to be guessed
     * @param current   the current hidden word
     * @return          the updated hidden word
     */
    private String replaceAll(char letter, String word, String current) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c == letter) {
                sb.append(c);
            } else {
                sb.append(current.charAt(i));
            }
        }
        return sb.toString();
    }
    /**
     * Checks if the word to be guessed contains the guessed char. If so,
     * replace all dashes from the hidden word where the letter is found in
     * the word to be guessed.
     * @param word      the word to be guessed by the user
     * @param guess     the guessed character
     * @param hidden    the current hidden word
     * @return          the updated hidden word
     */
    private String guessChar(String word, String guess, String hidden) {
        String newHidden = hidden;
        if (word.contains(guess)) {
            newHidden = replaceAll(guess.charAt(0), word, hidden);
        }
        return newHidden;
    }
    /**
     * Checks if the users guess of the complete word is correct
     * @param word      the word to be guessed
     * @param guess     the users guess
     * @param hidden    the current hidden word
     * @return          the hidden word or the correctly guessed word
     */
    private String guessWord(String word, String guess, String hidden) {
        if (word.equals(guess)) 
                return word;
        /*else*/return hidden;
    }
    /**
     * Called by controller to process a user guess
     * @param guess     the users guess
     * @param word      the word to be guessed
     * @param hidden    the current hidden word
     * @return          the updated hidden word
     */
    public String processGuess(String guess, String word, String hidden) {
        String guess1 = guess.toLowerCase();
        if (guess.length() == 1) {
            return guessChar(word, guess1, hidden);
        } else if (guess.length() == word.length()) {
            return guessWord(word, guess1, hidden);
        } else {
            return null;
        } 
    }
}
