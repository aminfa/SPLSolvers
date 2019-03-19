package de.upb.spl.sayyad;

import de.upb.spl.FMSatUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.ibea.CompoundVariation;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.algorithm.IBEA;
import org.moeaframework.core.*;
import org.moeaframework.core.fitness.AdditiveEpsilonIndicatorFitnessEvaluator;
import org.moeaframework.core.fitness.HypervolumeFitnessEvaluator;
import org.moeaframework.core.fitness.IndicatorFitnessEvaluator;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.core.operator.binary.HUX;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.problem.AbstractProblem;
import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sayyad  extends EAReasoner {

    private final static Logger logger = LoggerFactory.getLogger(Sayyad.class);

    public static String NAME = "SAYYAD";

    public Sayyad() {
        super(NAME);
    }

    public static BinaryVariable binarizeSeed(VecInt literalOrder, VecInt seed) {
        BinaryVariable var = new BinaryVariable(literalOrder.size());
        binarizeSeed(var, literalOrder, seed);
        return var;
    }

    public static void binarizeSeed(BinaryVariable variable, VecInt literalOrder, VecInt seed) {
        for (int j = 0; j < literalOrder.size(); j++) {
            int literal = literalOrder.get(j);
            variable.set(j, seed.get(literal-1) > 0);
        }
    }

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return new Problem(env).assemble((BinaryVariable) solution.getVariable(0));
    }

    @Override
    public AbstractEvolutionaryAlgorithm createEA(BenchmarkEnvironment env) {
        Problem problem = new Problem(env);
        int populationSize = env.configuration().getSayyadPopulationSize();
        String indicator = env.configuration().getBasicIbeaIndicator();
        IndicatorFitnessEvaluator fitnessEvaluator = null;
        Initialization initialization = new SayyadInit(env, problem, populationSize);

        CompoundVariation variation = new CompoundVariation();
        variation.appendOperator(new HUX(env.configuration().getBasicIbeaSinglePointCrossoverProbability()));
        variation.appendOperator(new BitFlip(env.configuration().getBasicIbeaBitFlipProbability()));

        if ("hypervolume".equals(indicator)) {
            fitnessEvaluator = new HypervolumeFitnessEvaluator(problem);
        } else if ("epsilon".equals(indicator)) {
            fitnessEvaluator = new AdditiveEpsilonIndicatorFitnessEvaluator(problem);
        } else {
            throw new IllegalArgumentException("invalid indicator: " + indicator);
        }

        return new IBEA(problem, null, initialization, variation, fitnessEvaluator);
    }

    private static class SayyadInit implements Initialization {
        protected final BenchmarkEnvironment env;
        protected final Problem problem;
        protected final int populationSize;

        public SayyadInit(BenchmarkEnvironment env, Problem problem, int populationSize) {
            this.env = env;
            this.problem = problem;
            this.populationSize = populationSize;
        }

        public Solution[] initialize() {
            Solution[] initialPopulation = new Solution[this.populationSize];

            for(int i = 0; i < this.populationSize; ++i) {
                Solution solution = this.problem.newSolution();
                if(i<env.richSeeds().size() && i < env.configuration().getSayyadSeedCount()) {
                    VecInt seed = env.richSeeds().get(i);
                    VecInt literalOrder = problem.literalsOrder;
                    BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
                    binarizeSeed(variable, literalOrder, seed);
//                    logger.info("Injecting seed: {}\n into initial runAndGetPopulation: {}", seed.toString(), variable.toString());
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