package de.upb.spl.hasco;

import java.util.Arrays;

public class FeatureSelectionPerformance implements Comparable<FeatureSelectionPerformance> {

    private final int violatedConstraints;
    private final double[] objectives;

    public FeatureSelectionPerformance(int violatedConstrains, double[] objectives) {
        this.violatedConstraints = violatedConstrains;
        this.objectives = objectives;
    }

    public FeatureSelectionPerformance(double[] objectivesAndViolatedConstraints) {
        this.objectives = new double[objectivesAndViolatedConstraints.length -1];
        for (int i = 0; i < objectivesAndViolatedConstraints.length-1; i++) {
            objectives[i] = objectivesAndViolatedConstraints[i];
        }

        this.violatedConstraints = (int) objectivesAndViolatedConstraints[objectivesAndViolatedConstraints.length-1];
    }

    int size () {
        return objectives.length;
    }


    public int violatedConstraints() {
        return violatedConstraints;
    }

    public double[] objectives() {
        return objectives;
    }

    @Override
    public int compareTo(FeatureSelectionPerformance other) {
        int c = Integer.compare(violatedConstraints, other.violatedConstraints);
        if(c == 0) {
            boolean negative = false;
            boolean positive = false;
            for (int i = 0; !(negative && positive) && i < size(); i++) {
                double thisObj = objectives[i];
                double otherObj = other.objectives[i];
                c = Double.compare(thisObj, otherObj);
                if(c == 0) {
                    negative = true;
                    positive = true;
                } else if(c < 0) {
                    negative = true;
                } else {
                    positive = true;
                }
            }
            if(positive && negative) {
                return 0;
            } else if(negative){
                return -1;
            } else {
                return 1;
            }
        } else {
            return c;
        }
    }

    public String toString() {
        if(violatedConstraints > 0)
            return "# Violated constraints: " + violatedConstraints;
        else {
            return Arrays.toString(objectives);
        }
    }
}
