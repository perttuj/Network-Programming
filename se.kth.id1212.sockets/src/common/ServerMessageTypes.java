package common;
/**
 *  Enumenator types used by the server. It will only accept messages that
 * start with one of these types, and will only send responses with the 'RESPONSE'
 * type at the beginning
 * @author Perttu Jääskeläinen
 */
public enum ServerMessageTypes {
    /**
     * Response from server to user, received through listener
     */
    RESPONSE,
    /**
     * Entered when guessing a word, for examle: GUESS abc
     */
    GUESS,
    /**
     * Entered when the user wants to guess a new word
     */
    NEWWORD,
    /**
     * Entered to disconnect the client
     */
    DISCONNECT;
}
