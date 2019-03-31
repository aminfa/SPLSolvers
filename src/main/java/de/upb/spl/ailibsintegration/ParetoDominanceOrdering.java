package de.upb.spl.ailibsintegration;

public class ParetoDominanceOrdering extends FeatureSelectionOrdering {
    public ParetoDominanceOrdering(int violatedConstrains, double[] objectives) {
        super(violatedConstrains, objectives);
    }

    public ParetoDominanceOrdering(double[] objectivesAndViolatedConstraints) {
        super(objectivesAndViolatedConstraints);
    }


}
