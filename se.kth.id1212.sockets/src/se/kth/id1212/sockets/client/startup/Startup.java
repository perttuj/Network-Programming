/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.id1212.sockets.client.startup;

import se.kth.id1212.sockets.client.view.NonBlockingInterpreter;

/**
 *
 * @author Perttu Jääskeläinen
 */
public class Startup {
    public static void main(String[] args) {
        new NonBlockingInterpreter().start();        
    }
}
