package de.upb.spl;

import org.junit.Assert;
import org.junit.Test;

public class TestBasics {

    @Test
    public void testSeedCompression() {
        long seed = 0xffffffffL;
        seed = seed << 2;
        seed = (seed & 0xffffffffL);
        Assert.assertTrue(seed < 0xffffffffL);
        Assert.assertEquals((int) 0xfffffffcL, (int) seed);

        seed = 0x7abbaabb7cacacacL;
        long seedUpper = seed >> 32;
        long seedLower = seed & 0xffffffffL;
        long seedCompressed = seedLower ^ seedUpper;
        Assert.assertEquals((int)0x6170617L, (int) seedCompressed);
    }


}
