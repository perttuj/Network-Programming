/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.server.controller;

import se.kth.id1212.sockets.server.model.Model;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class Controller {
    
    private final Model model;
    
    public Controller () {
        this.model = new Model();
    }
    
    public String processGuess(String guess) {
        return model.processGuess(guess);
    }
}
