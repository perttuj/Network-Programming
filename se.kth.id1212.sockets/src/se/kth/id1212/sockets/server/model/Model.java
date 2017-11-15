/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.server.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import se.kth.id1212.sockets.common.Constants;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class Model {
    
    private final String WORDS_FILE = Constants.WORD_FILE_LOCATION;
    private ArrayList<String> words;
    private List<String> guesses;
    
    public Model () {
        indexWords(readFile(WORDS_FILE));
    }
    
    private BufferedReader readFile(String path) {
        try {
            return new BufferedReader(new FileReader(new File(path)));
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + path);
            System.exit(1);
        }
        return null;
    }
    private void indexWords(BufferedReader reader) {
        words = new ArrayList();
        try {
            String word;
            do {
                word = reader.readLine();
                words.add(word);
            } while (word != null);
        } catch (IOException ex) {
           ex.printStackTrace();
        }
    }
    
    public String getWord() {
        int index = (int) (Math.random() * words.size());
        return words.get(index);
    }
    
    private String replaceAll(char letter, String word, String current) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == letter) {
                sb.append(word.charAt(i));
            } else {
                sb.append(current.charAt(i));
            }
        }
        return sb.toString();
    }
    
    private String guessChar(String word, String guess, String hidden) {
        String newHidden = hidden;
        if (word.contains(guess)) {
            newHidden = replaceAll(guess.charAt(0), word, hidden);
        }
        return newHidden;
    }
    
    private String guessWord(String word, String guess, String hidden) {
        if (word.equals(guess)) 
                return word;
        else    return hidden;
    }
    
    public String processGuess(String guess, String word, String hidden) {
        if (guess.length() == 1) {
            return guessChar(word, guess, hidden);
        } else if (guess.length() == word.length()) {
            return guessWord(word, guess, hidden);
        } else {
            return "GUESS: " + guess;
        }
    }
}
