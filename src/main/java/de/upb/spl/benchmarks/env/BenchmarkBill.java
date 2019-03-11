package de.upb.spl.benchmarks.env;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkReport;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkBill {

    private final String clientName;

    private final AtomicInteger evaluations;

    private final Map<FeatureSelection, BenchmarkReport> log = new ConcurrentHashMap<>();

    public BenchmarkBill(String clientName) {
        this.clientName = clientName;
        this.evaluations = new AtomicInteger(0);
    }

    public int getEvaluationsCount() {
        return evaluations.get();
    }

    public void logEvaluation(FeatureSelection selection, BenchmarkReport report) {
        BenchmarkReport oldReport = log.get(selection);
        if(oldReport == null || oldReport != report) {
            synchronized (log) {
                log.put(selection, report);
            }
            evaluations.incrementAndGet();
        }
    }

    public Optional<BenchmarkReport> checkLog(FeatureSelection selection) {
        BenchmarkReport oldReport = log.get(selection);
        return Optional.ofNullable(oldReport);
    }

    public String getClientName() {
        return clientName;
    }

    public void reset() {
        evaluations.set(0);
    }
}
