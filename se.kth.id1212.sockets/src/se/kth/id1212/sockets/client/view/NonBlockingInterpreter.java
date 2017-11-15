package se.kth.id1212.sockets.client.view;

import java.io.IOException;
import java.util.Scanner;
import se.kth.id1212.sockets.client.controller.Controller;
import se.kth.id1212.sockets.client.net.OutputHandler;
import se.kth.id1212.sockets.common.MsgType;


public class NonBlockingInterpreter implements Runnable {
    private static final String PROMPT = "> ";
    private final ThreadSafeStdOut outMgr = new ThreadSafeStdOut();
    private final Scanner console = new Scanner(System.in);
    private boolean running = false;
    private Controller contr;

    public void start() {
        if (running) {
            return;
        }
        running = true;
        contr = new Controller();
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (running) {
            try {
                CommandLine line = new CommandLine(readLine());
                switch (line.getCmd()) {
                    case QUIT:
                        running = false;
                        contr.disconnect();
                        break;
                    case CONNECT:
                        contr.connect("127.0.0.1",
                                      8080,
                                      new ConsoleOutput());
                        break;
                    case GUESS:
                        contr.sendGuess(line.getCommand());
                        break;
                    case NEWWORD:
                        contr.newGame();
                        break;
                    default:
                        outMgr.println(usageMessage("INCORRECT INPUT, USAGE:"));
                        break;
                }
            } catch (IOException e) {
                outMgr.println("Failed");
            }
        }
    }
    
    private String usageMessage(String current) {
        StringBuilder sb = new StringBuilder(current);
        sb.append("\nCOMMANDS:\n");
        for (MsgType m : MsgType.values()) {
            sb.append(m + "\n");
        }
        return sb.toString();
    }
    
    
    private String readLine() {
        outMgr.print(PROMPT);
        return console.nextLine();
    }

    private class ConsoleOutput implements OutputHandler {
        @Override
        public void handleMsg(String msg) {
            outMgr.println((String) msg);
            outMgr.print(PROMPT);
        }
    }
}