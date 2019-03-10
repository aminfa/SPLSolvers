package de.upb.spl.benchmarks.env;

import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkBill {

    private final String clientName;

    private final AtomicInteger evaluations;

    public BenchmarkBill(String clientName) {
        this.clientName = clientName;
        this.evaluations = new AtomicInteger(0);
    }

    public int getEvaluationsCount() {
        return evaluations.get();
    }

    public void logEvaluation() {
        evaluations.incrementAndGet();
    }

    public String getClientName() {
        return clientName;
    }

    public void reset() {
        evaluations.set(0);
    }
}
