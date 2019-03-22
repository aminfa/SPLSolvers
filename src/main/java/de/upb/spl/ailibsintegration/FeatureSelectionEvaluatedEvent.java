package de.upb.spl.ailibsintegration;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.JobReport;
import jaicore.basic.algorithm.events.ASolutionCandidateFoundEvent;
import jaicore.basic.algorithm.events.ScoredSolutionCandidateFoundEvent;

public class FeatureSelectionEvaluatedEvent extends ASolutionCandidateFoundEvent<FeatureSelection> {

    private final int evaluationIndex;

    private JobReport report;

    public FeatureSelectionEvaluatedEvent(String algorithmId, FeatureSelection solutionCandidate, int evaluationIndex, JobReport report) {
        super(algorithmId, solutionCandidate);
        this.evaluationIndex = evaluationIndex;
        this.report = report;
    }


    public FeatureSelectionEvaluatedEvent(String algorithmId, int evaluationIndex, BenchmarkEntry entry) {
        this(algorithmId, entry.selection(), evaluationIndex, entry.report());
    }

    public void setReport(JobReport report) {
        this.report = report;
    }

    public JobReport getReport() {
        return report;
    }

    public int getEvaluationIndex() {
        return evaluationIndex;
    }

}
