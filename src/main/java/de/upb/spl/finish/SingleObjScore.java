package de.upb.spl.finish;

import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class SingleObjScore extends Finisher {

    private int start;
    private int limit;
    private double min, max;

    private double result;

    public SingleObjScore(BenchmarkEnvironment env, int limit, double min, double max) {
        this(env, 0, limit, min, max);
    }

    public SingleObjScore(BenchmarkEnvironment env, int start, int end, double max, double min) {
        super(env);
        this.start = start;
        this.limit = end - start;
        this.min = min;
        this.max = max;
    }

    @Override
    public void run() {
        Optional<Double> best = StreamSupport.stream(env().currentTab().spliterator(), false)
                .skip(start)
                .limit(this.limit)
                .filter(entry -> !env().interpreter(entry.report()).violatedConstraints())
                .map(entry -> {
                    double[] objectives = BenchmarkHelper.extractEvaluation(
                            env(),
                            entry.report());
                    return objectives[0];
                })
                .min(Comparator.comparingDouble(d -> d));
        if(best.isPresent()) {
            if(best.get() >= max && best.get() <= min) {
                double b = best.get();
                result = (b - min)/ (max - min);
            }
        }
    }

    public double getResult() {
        return result;
    }



}
