package de.upb.spl.ailibsintegration;

public class ParetoPerformance extends FeatureSelectionPerformance {
    public ParetoPerformance(int violatedConstrains, double[] objectives) {
        super(violatedConstrains, objectives);
    }

    public ParetoPerformance(double[] objectivesAndViolatedConstraints) {
        super(objectivesAndViolatedConstraints);
    }


}
