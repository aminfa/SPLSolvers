package de.upb.spl.finish;

import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class MinMaxValues extends Finisher {

    private double[] min;
    private double[] max;
    public MinMaxValues(BenchmarkEnvironment env) {
        super(env);
    }


    @Override
    public void run() {
        int objectiveSize = env().objectives().size();
        min = new double[objectiveSize];
        max = new double[objectiveSize];
        for (int i = 0; i < objectiveSize; i++) {
            min[i] = Double.MAX_VALUE;
            max[i] = Double.MIN_VALUE;
        }
        StreamSupport
                .stream(env().currentTab().spliterator(), false)
                .map(entry -> BenchmarkHelper.extractEvaluation(env(), entry.report()))
                .forEach(eval -> {
                    if (eval == null || eval.length < objectiveSize) {
                        throw new IllegalArgumentException("Evaluation was empty.");
                    }
                    for (int i = 0; i < objectiveSize; i++) {
                        if (min[i] > eval[i]) {
                            min[i] = eval[i];
                        }
                        if (max[i] < eval[i]) {
                            max[i] = eval[i];
                        }
                    }
                });


        for (int i = 0; i < objectiveSize; i++) {
            if(min[i] == Double.MAX_VALUE) {
                throw new IllegalArgumentException("Couldn't create determine the i'th min value.");
            }
            if(max[i] == Double.MIN_VALUE) {
                throw new IllegalArgumentException("Couldn't create determine the i'th max value.");
            }
        }


    }

    public double[] getMin() {
        return min;
    }

    public double[] getMax() {
        return max;
    }
}
