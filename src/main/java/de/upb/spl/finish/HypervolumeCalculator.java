package de.upb.spl.finish;

import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.indicator.Hypervolume;
import org.moeaframework.problem.AbstractProblem;

import java.util.stream.StreamSupport;

public class HypervolumeCalculator extends Finisher {

    private int limit;
    private double[] min, max;
    private int objectiveCount;
    private DummyProblem problem;

    private double result;
    public HypervolumeCalculator(BenchmarkEnvironment env, int limit, double[] min, double[] max) {
        super(env);
        this.limit = limit;
        this.min = min;
        this.max = max;
        objectiveCount = Math.min(min.length, max.length);
        this.problem = new DummyProblem();
    }

    @Override
    public void run() {
        Hypervolume helper = new Hypervolume(problem, min, max);
        NondominatedPopulation population = new NondominatedPopulation();
        StreamSupport.stream(env().currentTab().spliterator(), false)
                .limit(this.limit)
                .forEach(entry -> {
                    double[] objectives = BenchmarkHelper.extractEvaluation(
                            env(),
                            entry.report());
                    Solution solution = new Solution(objectives);
                    population.add(solution);
                });
        result = helper.evaluate(population);
    }

    public double getResult() {
        return result;
    }

    class DummyProblem extends AbstractProblem {

        public DummyProblem() {
            super(0, objectiveCount);
        }

        @Override
        public void evaluate(Solution solution) {
            throw new RuntimeException();
        }

        @Override
        public Solution newSolution() {
            throw new RuntimeException();
        }
    }
}
