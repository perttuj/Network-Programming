package se.kth.id1212.sockets.common;

/**
 *
 * @author Perttu Jääskeläinen
 */
public enum MsgType {
    /**
     * Entered when guessing a word, for examle: GUESS abc
     */
    GUESS,
    /**
     * Entered to disconnect the client
     */
    DISCONNECT;
}
