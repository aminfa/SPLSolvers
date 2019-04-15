package de.upb.spl.benchmarks.env;

public class SeededEnv extends BenchmarkEnvironmentDecoration {

    private final long seed;

    public SeededEnv(BenchmarkEnvironment env, long seed) {
        super(env);
        this.seed = seed;
    }

    @Override
    public Long seed() {
        return seed;
    }
}
