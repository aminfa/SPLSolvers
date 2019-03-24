package de.upb.spl.finish;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;

public abstract class Finisher implements Runnable {

    private BenchmarkEnvironment env;

    public Finisher(BenchmarkEnvironment env) {
        this.env = env;
    }

    public BenchmarkEnvironment env() {
        return env;
    }

    public void setEnv(BenchmarkEnvironment env) {
        this.env = env;
    }

}
