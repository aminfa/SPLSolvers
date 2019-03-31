package de.upb.spl.ailibsintegration;

public class CountOrdering extends FeatureSelectionOrdering {
    public CountOrdering(int violatedConstrains, double[] objectives) {
        super(violatedConstrains, objectives);
    }

    public CountOrdering(double[] objectivesAndViolatedConstraints) {
        super(objectivesAndViolatedConstraints);
    }

    @Override
    public int compareTo(FeatureSelectionOrdering other) {
        if (!(other instanceof CountOrdering)) {
            return super.compareTo(other);
        } else {
            return this.compareTo((CountOrdering) other);
        }
    }

//    @Override
    public int compareTo(CountOrdering other) {
        int paretoEquality = super.compareTo(other);
        if(paretoEquality != 0) {
            return paretoEquality;
        }
        int superiorCount = 0;
        for (int i = 0; i < size(); i++) {
            double thisObj = objectives[i];
            double otherObj = other.objectives[i];
            int c = Double.compare(thisObj, otherObj);
            if(c > 0) {
                superiorCount++;
            } else if(c < 0) {
                superiorCount --;
            }
        }
        return superiorCount;
    }
}
