package de.upb.spl.reasoner;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.BenchmarkReport;
import fm.FeatureTreeNode;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

public final class SPLEvaluator {
    private final static Logger logger = LoggerFactory.getLogger(SPLEvaluator.class);

    public static double[] evaluateBinaryString(BenchmarkEnvironment env,
                                                BinaryVariable featureSelectionBinaryString,
                                                List<FeatureTreeNode> featureOrder, String client, boolean raw) {
        FeatureSelection selection = FMUtil.selectFromPredicate(featureOrder, featureSelectionBinaryString::get);
        return evaluateFeatureSelection(env, selection, client, raw);
    }

    public static double[] evaluateFeatureSelection(BenchmarkEnvironment env,
                                                    FeatureSelection selection, String client, boolean raw) {
        if(!FMUtil.isValidSelection(env.model(), selection)) {
            return failedEvaluation(env);
        }
        BenchmarkReport report;
        try {
            report = env.run(selection, client).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Couldn't run benchmark for " + selection + ".", e);
            report = null;
        }

        if(report == null || env.violatesConstraints(report)) {
            return failedEvaluation(env);
        }
        double[] evaluation = new double[env.objectives().size()];
        for (int i = 0; i < evaluation.length; i++) {
            String objectiveName = env.objectives().get(i);
            if(raw) {
                evaluation[i] = report.rawResult(objectiveName).orElse(Double.NaN);
            } else {
                evaluation[i] = report.readResult(objectiveName).orElse(Double.NaN);
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
