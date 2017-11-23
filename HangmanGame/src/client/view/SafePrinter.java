package client.view;

/**
 * Class for handling thread safe output (all methods are synchronized) used by
 * UserInterpreter and ServerConnection threads, which handles server responses
 */
class SafePrinter {

    synchronized void print(String output) {
        System.out.print(output);
    }

    synchronized void println(String output) {
        System.out.println(output);
    }
}
