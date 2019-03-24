package de.upb.spl.benchmarks.env;

import java.util.Random;

public class EnvSeed extends BenchmarkEnvironmentDecoration{

    private final Random generator;

    public EnvSeed(BenchmarkEnvironment env) {
        super(env);
        generator = new Random(-1);
    }

    public EnvSeed(BenchmarkEnvironment env, long seed) {
        super(env);
        generator = new Random(seed);
    }

    @Override
    public Random generator() {
        return generator;
    }
}
