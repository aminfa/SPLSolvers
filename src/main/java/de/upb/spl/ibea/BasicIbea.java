package de.upb.spl.ibea;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.reasoner.BinaryStringProblem;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.reasoner.SPLEvaluator;
import org.moeaframework.Analyzer;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.algorithm.IBEA;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.fitness.AdditiveEpsilonIndicatorFitnessEvaluator;
import org.moeaframework.core.fitness.HypervolumeFitnessEvaluator;
import org.moeaframework.core.fitness.IndicatorFitnessEvaluator;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.core.operator.binary.HUX;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Function;

public class BasicIbea extends EAReasoner {

    private final static Logger logger = LoggerFactory.getLogger(BasicIbea.class);

    public static String NAME = "BASIC_IBEA";

    public BasicIbea() {
        super(NAME);
    }

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return new Problem(env).assemble((BinaryVariable) solution.getVariable(0));
    }

    @Override
    public AbstractEvolutionaryAlgorithm createAlgorithm(BenchmarkEnvironment env) {

        Problem problem = new Problem(env);

        int populationSize = env.configuration().getBasicIbeaPopulationSize();
        String indicator = env.configuration().getBasicIbeaIndicator();
        IndicatorFitnessEvaluator fitnessEvaluator = null;
        Initialization initialization = new RandomInitialization(problem, populationSize);
//        Variation variation = OperatorFactory.getInstance().getVariation((String)null, new Properties(), problem);
        CompoundVariation variation = new CompoundVariation();
        variation.appendOperator(new HUX(env.configuration().getBasicIbeaSinglePointCrossoverProbability()));
        variation.appendOperator(new BitFlip(env.configuration().getBasicIbeaBitFlipProbability()));
        if ("hypervolume".equals(indicator)) {
            fitnessEvaluator = new HypervolumeFitnessEvaluator(problem);
        } else {
            if (!"epsilon".equals(indicator)) {
                throw new IllegalArgumentException("invalid indicator: " + indicator);
            }
            fitnessEvaluator = new AdditiveEpsilonIndicatorFitnessEvaluator(problem);
        }
        Population population = new Population();
        return new IBEA(problem, null, initialization, variation, fitnessEvaluator);
    }



    private static class Problem extends BinaryStringProblem {

        private BenchmarkEnvironment env;

        public Problem(BenchmarkEnvironment env) {
            super(1 + env.objectives().size(), FMUtil.listFeatures(env.model()));
            this.env = env;
        }

        @Override
        public void evaluate(Solution solution) {
            solution.setObjectives(evaluateAndCountViolatedConstraints(env,
                    assemble((BinaryVariable) solution.getVariable(0)),
                    NAME));
        }
    }


    public static double[] evaluateAndCountViolatedConstraints(BenchmarkEnvironment env,
                                                           FeatureSelection selection,
                                                           String clientName) {
        double [] objectives = new double[env.objectives().size() + 1];
        int violations = env.sat().violatedConstraints(selection);
        objectives[objectives.length-1] =violations;
        double [] evaluations;
        if(violations > 0) {
            evaluations = SPLEvaluator.failedEvaluation(env);
        } else {
            evaluations = SPLEvaluator.evaluateFeatureSelection(env, selection, clientName,false);
        }
        for (int i = 0; i < evaluations.length; i++) {
            objectives[i] = evaluations[i];
        }
        return objectives;
    }
}
