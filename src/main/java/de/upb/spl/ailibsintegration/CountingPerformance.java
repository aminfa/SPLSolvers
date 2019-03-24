package de.upb.spl.ailibsintegration;

public class CountingPerformance extends FeatureSelectionPerformance {
    public CountingPerformance(int violatedConstrains, double[] objectives) {
        super(violatedConstrains, objectives);
    }

    public CountingPerformance(double[] objectivesAndViolatedConstraints) {
        super(objectivesAndViolatedConstraints);
    }

    @Override
    public int compareTo(FeatureSelectionPerformance other) {
        if (!(other instanceof CountingPerformance)) {
            return super.compareTo(other);
        } else {
            return this.compareTo((CountingPerformance) other);
        }
    }

//    @Override
    public int compareTo(CountingPerformance other) {
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
