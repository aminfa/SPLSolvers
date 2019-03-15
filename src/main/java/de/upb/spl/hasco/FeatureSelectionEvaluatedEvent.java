package de.upb.spl.hasco;

import de.upb.spl.FeatureSelection;
import jaicore.basic.algorithm.events.ASolutionCandidateFoundEvent;
import jaicore.basic.algorithm.events.ScoredSolutionCandidateFoundEvent;

public class FeatureSelectionEvaluatedEvent extends ASolutionCandidateFoundEvent<FeatureSelection> implements ScoredSolutionCandidateFoundEvent<FeatureSelection, FeatureSelectionPerformance> {
    private final int evaluationIndex;
    private FeatureSelectionPerformance performance;
    public FeatureSelectionEvaluatedEvent(String algorithmId, FeatureSelection solutionCandidate, int evaluationIndex, FeatureSelectionPerformance performance) {
        super(algorithmId, solutionCandidate);
        this.evaluationIndex = evaluationIndex;
        this.performance = performance;
    }

    public void setPerformance(FeatureSelectionPerformance performance) {
        this.performance = performance;
    }

    @Override
    public FeatureSelectionPerformance getScore() {
        return performance;
    }

    public int getEvaluationIndex() {
        return evaluationIndex;
    }
}
