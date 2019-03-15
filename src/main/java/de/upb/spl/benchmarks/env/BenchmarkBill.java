package de.upb.spl.benchmarks.env;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkReport;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BenchmarkBill {

    private final String clientName;

    private final List<Log> evalLogs = Collections.synchronizedList(new ArrayList<>());
    private final Map<FeatureSelection, BenchmarkReport> evalCache = new ConcurrentHashMap<>();

    public BenchmarkBill(String clientName) {
        this.clientName = clientName;
    }

    public int getEvaluationsCount() {
        return evalLogs.size();
    }

    public void logEvaluation(FeatureSelection selection, BenchmarkReport report) {
        Log log = new Log(selection, report);
        boolean newLogEntry;
        synchronized (evalCache) {
            newLogEntry = evalCache.containsKey(selection);
            if(!newLogEntry) {
                evalCache.put(selection, report);
            }
        }
        if(!newLogEntry) {
            evalLogs.add(log);
        }
    }

    public Optional<BenchmarkReport> checkLog(FeatureSelection selection) {
        BenchmarkReport oldReport = evalCache.get(selection);
        return Optional.ofNullable(oldReport);
    }

    public Log checkLog(int index) {
        if(index < 0 || index >= getEvaluationsCount()) {
            throw new ArrayIndexOutOfBoundsException("Array index " + index + " out of range: " + getEvaluationsCount());
        }
        return evalLogs.get(index);
    }

    public String getClientName() {
        return clientName;
    }

    public class Log {
        private final FeatureSelection selection;
        private final BenchmarkReport report;

        public Log(FeatureSelection selection, BenchmarkReport report) {
            this.selection = selection;
            this.report = report;
        }

        public FeatureSelection selection() {
            return selection;
        }

        public BenchmarkReport report() {
            return report;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Log)) return false;

            Log log = (Log) o;

            if (!selection.equals(log.selection)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = selection.hashCode();
            return result;
        }
    }
}
