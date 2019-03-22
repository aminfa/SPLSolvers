package de.upb.spl.benchmarks;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public final class BenchmarkHelper {
    private final static Logger logger = LoggerFactory.getLogger(BenchmarkHelper.class);

    public static double[] evaluateFeatureSelection(BenchmarkEnvironment env,
                                                    FeatureSelection selection) {
        if(!FMUtil.isValidSelection(env.model(), selection)) {
            return failedEvaluation(env);
        }
        JobReport report;
        try {
            report = env.run(selection).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Couldn't runAndGetPopulation benchmark for " + selection + ".", e);
            report = null;
        }

        if(report == null || env.violatesConstraints(report)) {
            return failedEvaluation(env);
        }
        return extractEvaluation(env, report);
    }

    public static double[] extractEvaluation(BenchmarkEnvironment env, JobReport report) {
        boolean raw = env.isRaw();
        double[] evaluation = new double[env.objectives().size()];
        for (int i = 0; i < evaluation.length; i++) {
            String objectiveName = env.objectives().get(i);
            ReportInterpreter reportReader = env.interpreter(report);
            if(raw) {
                evaluation[i] = reportReader.rawResult(objectiveName).orElse(Double.NaN);
            } else {
                evaluation[i] = reportReader.readResult(objectiveName).orElse(Double.NaN);
            }
        }
        return evaluation;
    }

    public static Solution toSolution(BinaryVariable featureSelectionBinaryString,
                                              double[] evaluation){
        Solution solution = new Solution(1, evaluation.length);
        solution.setVariable(0, featureSelectionBinaryString);
        solution.setObjectives(evaluation);
        return solution;
    }

    public static double[] failedEvaluation(BenchmarkEnvironment env) {
        double[] evaluation = new double[env.objectives().size()];
        for (int i = 0; i < evaluation.length; i++) {
            evaluation[i] = Double.NaN;
        }
        return evaluation;
    }
}
