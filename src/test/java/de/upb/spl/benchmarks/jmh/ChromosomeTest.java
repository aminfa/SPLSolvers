package de.upb.spl.benchmarks.jmh;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChromosomeTest {

    @Test
    public void score() {
        Chromosome chromosome = new Chromosome();
        System.out.println(chromosome.toString() + "\n" + chromosome.score());

    }
}