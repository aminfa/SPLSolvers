package de.upb.spl.benchmarks;

import de.upb.spl.FeatureSelection;

import java.util.Map;

public class BenchmarkEntry implements Map.Entry<FeatureSelection, JobReport> {
    private final FeatureSelection selection;
    private JobReport report;

    public BenchmarkEntry(FeatureSelection selection, JobReport report) {
        this.selection = selection;
        this.report = report;
    }

    public FeatureSelection selection() {
        return selection;
    }

    public JobReport report() {
        return report;
    }

    @Override
    public FeatureSelection getKey() {
        return selection;
    }

    @Override
    public JobReport getValue() {
        return report;
    }

    @Override
    public JobReport setValue(JobReport value) {
        JobReport oldReport = report;
        report = value;
        return oldReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BenchmarkEntry)) return false;

        BenchmarkEntry log = (BenchmarkEntry) o;

        if (!selection.equals(log.selection)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = selection.hashCode();
        return result;
    }
}
