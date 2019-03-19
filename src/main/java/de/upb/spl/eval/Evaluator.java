package de.upb.spl.eval;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;

public abstract class Evaluator implements Runnable {

    private BenchmarkEnvironment env;

    public Evaluator(BenchmarkEnvironment env) {
        this.env = env;
    }

    public BenchmarkEnvironment env() {
        return env;
    }

    public void setEnv(BenchmarkEnvironment env) {
        this.env = env;
    }

}
