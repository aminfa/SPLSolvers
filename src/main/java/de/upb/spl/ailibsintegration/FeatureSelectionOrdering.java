package de.upb.spl.ailibsintegration;

import java.util.Arrays;

public class FeatureSelectionOrdering implements Comparable<FeatureSelectionOrdering> {

    protected final int violatedConstraints;
    protected final double[] objectives;

    public FeatureSelectionOrdering(int violatedConstrains, double[] objectives) {
        this.violatedConstraints = violatedConstrains;
        this.objectives = objectives;
    }

    /**
     * Constructor for array of objectives mixed with number of violated constraints.
     * The supplied array should come from: BasicIbea::evaluateAndCountViolatedConstraints
     * The last element is the amount of violated constraints.
     *
     * @param objectivesAndViolatedConstraints objective values and amount of violated constraints.
     */
    public FeatureSelectionOrdering(double[] objectivesAndViolatedConstraints) {
        int objectiveSize = objectivesAndViolatedConstraints.length -1;
        this.objectives = new double[objectiveSize];
        for (int i = 0; i < objectiveSize; i++) {
            objectives[i] = objectivesAndViolatedConstraints[i];
        }

        this.violatedConstraints = (int) objectivesAndViolatedConstraints[objectiveSize]; // last element is violated constraints.
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

    public boolean hasEmptyResults() {
        for (int i = 0; i < objectives.length; i++) {
            if(Double.isNaN(objectives[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implements the compareTo method of Comparable.
     * This method will first sort feature selections based on the amount of constraint violations.
     * If two feature selections have equal amount of violated constraints, they will be sorted based on pareto optimality of the remaining objectives.
     *
     * @param other object to be compared against.
     * @return
    a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object
     */
    @Override
    public int compareTo(FeatureSelectionOrdering other) {
        int c = Integer.compare(violatedConstraints, other.violatedConstraints);

        boolean hasEmptyResults = this.hasEmptyResults();
        boolean otherHashEmptyResults = other.hasEmptyResults();

        if(hasEmptyResults || otherHashEmptyResults)
            if(otherHashEmptyResults)
                if(hasEmptyResults)
                    return 0;
                else
                    return -1;
            else
                return 1;


        if(c == 0) {
            boolean negative = false;
            boolean positive = false;
            for (int i = 0; !(negative && positive) && i < size(); i++) {
                double thisObj = objectives[i];
                double otherObj = other.objectives[i];
                c = Double.compare(thisObj, otherObj);
                if(c == 0) {
                } else if(c < 0) {
                    negative = true;
                } else {
                    positive = true;
                }
            }
            if(positive == negative) {
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
            return "[ #Violated-constraints: " + violatedConstraints + " ]";
        else {
            return Arrays.toString(objectives);
        }
    }
}
