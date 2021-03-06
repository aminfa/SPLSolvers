package de.upb.spl.finish;

import de.upb.spl.util.Streams;

import java.io.IOException;

public class Shutdown implements Runnable {

    @Override
    public void run() {
        try {
            shutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() throws RuntimeException, IOException, InterruptedException {
        String shutdownCommand;
        String operatingSystem = System.getProperty("os.name");

        if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
            shutdownCommand = "sudo shutdown +10";
        }
        else if ("Windows".equals(operatingSystem)) {
            shutdownCommand = "shutdown.exe -s -t 10";
        }
        else {
            throw new RuntimeException("Unsupported operating system.");
        }

        Process p = Runtime.getRuntime().exec(shutdownCommand);
        int exitCode = p.waitFor();
        if(exitCode == 0) {
            String input = Streams.InReadString(p.getInputStream());
            input += "\n" + Streams.InReadString(p.getErrorStream());
            System.out.println("Shutdown command: " + input);
        } else {
            String error = Streams.InReadString(p.getErrorStream());
            throw new RuntimeException("Error running Shutdown command:\n" + error);
        }
    }

    @Override
    public String toString() {
        return "Shutdown in 3 minutes";
    }
}
