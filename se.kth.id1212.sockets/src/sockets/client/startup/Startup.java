package sockets.client.startup;

import sockets.client.view.UserInterpreter;
/**
 * class for starting a clientside view of the hangman game
 * @author Perttu Jääskeläinen
 */
public class Startup {
    public static void main(String[] args) {
        new UserInterpreter().start();        
    }
}
