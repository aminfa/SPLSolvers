package de.upb.spl.finish;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ShutdownTest {

    @Test
    public void testShutdown() throws IOException, InterruptedException {
        Shutdown shutdown = new Shutdown();
        shutdown.shutdown();
    }
}