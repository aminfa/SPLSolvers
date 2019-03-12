package de.upb.spl.henard;

import de.upb.spl.FMSatUtil;
import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.ibea.CompoundVariation;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.reasoner.SPLEvaluator;
import de.upb.spl.sayyad.Sayyad;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.algorithm.IBEA;
import org.moeaframework.core.*;
import org.moeaframework.core.fitness.AdditiveEpsilonIndicatorFitnessEvaluator;
import org.moeaframework.core.fitness.HypervolumeFitnessEvaluator;
import org.moeaframework.core.fitness.IndicatorFitnessEvaluator;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.core.operator.binary.HUX;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.problem.AbstractProblem;
import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Henard extends EAReasoner {

    private final static Logger logger = LoggerFactory.getLogger(Henard.class);

    public static String NAME = "HENARD";

    public Henard() {
        super(NAME);
    }

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return new Problem(env).assemble((BinaryVariable) solution.getVariable(0));
    }

    @Override
    public AbstractEvolutionaryAlgorithm createAlgorithm(BenchmarkEnvironment env) {
        Problem problem = new Problem(env);
        int populationSize = env.configuration().getHenardPopulationSize();
        String indicator = env.configuration().getBasicIbeaIndicator();
        IndicatorFitnessEvaluator fitnessEvaluator = null;
        Initialization initialization = new HenardInit(env, problem, populationSize);

        CompoundVariation variation = new CompoundVariation();
        variation.appendOperator(new HUX(env.configuration().getBasicIbeaSinglePointCrossoverProbability()));
        variation.appendOperator(new BitFlip(env.configuration().getBasicIbeaBitFlipProbability()));
        variation.appendOperator(new SmartMutation(problem.literalsOrder, env.sat(), problem::assemble, env.configuration().getHenardSmartMutationProbability()));
        variation.appendOperator(new SmartReplacement(problem.literalsOrder, env.sat(), env.configuration().getHenardSmartReplacementProbability()));

        if ("hypervolume".equals(indicator)) {
            fitnessEvaluator = new HypervolumeFitnessEvaluator(problem);
        } else if ("epsilon".equals(indicator)) {
            fitnessEvaluator = new AdditiveEpsilonIndicatorFitnessEvaluator(problem);
        } else {
            throw new IllegalArgumentException("invalid indicator: " + indicator);
        }

        return new IBEA(problem, null, initialization, variation, fitnessEvaluator);
    }

    private static class HenardInit implements Initialization {
        protected final BenchmarkEnvironment env;
        protected final Problem problem;
        protected final int populationSize;

        public HenardInit(BenchmarkEnvironment env, Problem problem, int populationSize) {
            this.env = env;
            this.problem = problem;
            this.populationSize = populationSize;
        }

        public Solution[] initialize() {
            Solution[] initialPopulation = new Solution[this.populationSize];

            for(int i = 0; i < this.populationSize; ++i) {
                Solution solution = this.problem.newSolution();
                if(i<env.richSeeds().size() && i < env.configuration().getHernardSeedCount()) {
                    VecInt seed = env.richSeeds().get(0);
                    VecInt literalOrder = problem.literalsOrder;
                    BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
                    Sayyad.binarizeSeed(variable, literalOrder, seed);
//                    logger.info("Injecting seed: {}\n into initial population: {}", seed.toString(), variable.toString());
                } else {
                    for (int j = 0; j < solution.getNumberOfVariables(); ++j) {
                        solution.getVariable(j).randomize();
                    }
                }

                initialPopulation[i] = solution;
            }

            return initialPopulation;
        }

    }

    private static class Problem extends AbstractProblem {

        private BenchmarkEnvironment env;
        private VecInt literalsOrder;
        public Problem(BenchmarkEnvironment env) {
            super(1, 1 + env.objectives().size());
            this.env = env;
            this.literalsOrder = FMSatUtil.nonUnitLiteralOrder(env.sat());
        }

        @Override
        public void evaluate(Solution solution) {
            solution.setObjectives(BasicIbea.evaluateAndCountViolatedConstraints(env,
                    assemble((BinaryVariable) solution.getVariable(0)),
                    NAME));
        }

        FeatureSelection assemble(BinaryVariable variable) {
            VecInt model = new VecInt();
            for (int i = 0; i < variable.getNumberOfBits(); i++) {
                int literal = literalsOrder.get(i);
                if(variable.get(i)) {
                    model.push(literal);
                }
            }
            FMSatUtil.unitLiterals(env.sat()).copyTo(model);
            return env.sat().toSelection(env.model(), model.toArray());
        }

        @Override
        public Solution newSolution() {
            Solution solution = new Solution(1, this.getNumberOfObjectives());
            solution.setVariable(0, new BinaryVariable(literalsOrder.size()));
            return solution;
        }
    }
}