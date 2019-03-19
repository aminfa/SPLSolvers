package de.upb.spl.benchmarks.env;

public class RawResults extends BenchmarkEnvironmentDecoration {


    private final boolean raw;

    public RawResults(BenchmarkEnvironment env, boolean raw) {
        super(env);
        this.raw = raw;
    }

    public RawResults(BenchmarkEnvironment env) {
        this(env, true);
    }

    public boolean isRaw() {
        return  raw;
    }

}
