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
    private String word;
    private int score = 0;
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
            String word = reader.readLine();
            while (word != null) {
                words.add(word);
                word = reader.readLine();
            }
            newWord();
        } catch (IOException ex) {
           ex.printStackTrace();
        }
    }
    
    private void newWord() {
        int index = (int) (Math.random() * words.size());
        word = words.get(index);
    }
    
    private String guessChar(String s) {
        if (word.contains(s)) {
            
        }
        return null;
    }
    
    private String guessWord(String s) {
        return null;
    }
    
    public String processGuess(String guess) {
        
        return "hello";
        /*if (guess.length() == 1) {
            return guessChar(guess);
        } else if (guess.length() == word.length()) {
            return guessWord(guess);
        } else {
            
        }
        return null;*/
    }
}
