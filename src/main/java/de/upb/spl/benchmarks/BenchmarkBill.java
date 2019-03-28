package de.upb.spl.benchmarks;

import de.upb.spl.FeatureSelection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BenchmarkBill implements Iterable<BenchmarkEntry> {

    private final String clientName;

    private final List<BenchmarkEntry> evalLogs = Collections.synchronizedList(new ArrayList<>());
    private final Map<FeatureSelection, JobReport> evalCache = new ConcurrentHashMap<>();

    public BenchmarkBill(String clientName) {
        this.clientName = clientName;
    }

    public int getEvaluationsCount() {
        return evalLogs.size();
    }

    public void logEvaluation(FeatureSelection selection, JobReport report) {
        boolean newLogEntry = false;
        synchronized (evalCache) {
            newLogEntry = !evalCache.containsKey(selection);
            if(newLogEntry) {
                evalCache.put(selection, report);
            }
        }
        if(newLogEntry) {
            BenchmarkEntry log = new BenchmarkEntry(selection, report);
            evalLogs.add(log);
        }
    }

    public Optional<JobReport> checkLog(FeatureSelection selection) {
        JobReport oldReport = evalCache.get(selection);
        return Optional.ofNullable(oldReport);
    }

    public BenchmarkEntry checkLog(int index) {
        if(index < 0 || index >= getEvaluationsCount()) {
            throw new ArrayIndexOutOfBoundsException("Array index " + index + " out of range: " + getEvaluationsCount());
        }
        return evalLogs.get(index);
    }

    public String getReasonerName() {
        return clientName;
    }

    @Override
    public Iterator<BenchmarkEntry> iterator() {
        return evalLogs.iterator();
    }

    @Override
    public Spliterator<BenchmarkEntry> spliterator() {
        return evalLogs.spliterator();
    }

}
