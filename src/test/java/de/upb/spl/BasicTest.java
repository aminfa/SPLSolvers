package de.upb.spl;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.lang.reflect.*;

public class BasicTest {

    public static void main(String []args) {
//        DebugSignalHandler.listenTo("USR1");
        DebugSignalHandler.listenTo("USR2");

        while (true) {
            try {
                Thread.sleep(1000);
            }
            catch(InterruptedException e) {
            }
        }
    }
}

class DebugSignalHandler implements SignalHandler
{
    public static void listenTo(String name) {
        Signal signal = new Signal(name);
        Signal.handle(signal, new DebugSignalHandler());
    }

    public void handle(Signal signal) {
        System.out.println("Signal: " + signal);
        if (signal.toString().trim().equals("SIGTERM")) {
            System.out.println("SIGTERM raised, terminating...");
            System.exit(1);
        }
    }
}