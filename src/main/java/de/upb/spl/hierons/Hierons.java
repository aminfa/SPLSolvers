package de.upb.spl.hierons;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.henard.SmartMutation;
import de.upb.spl.henard.SmartReplacement;
import de.upb.spl.ibea.CompoundVariation;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.sayyad.Sayyad;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.algorithm.IBEA;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Solution;
import org.moeaframework.core.fitness.AdditiveEpsilonIndicatorFitnessEvaluator;
import org.moeaframework.core.fitness.HypervolumeFitnessEvaluator;
import org.moeaframework.core.fitness.IndicatorFitnessEvaluator;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.core.operator.binary.HUX;
import org.moeaframework.core.variable.BinaryVariable;
import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hierons extends EAReasoner {
    private final static Logger logger = LoggerFactory.getLogger(Hierons.class);

    public static String NAME = "Hierons";

    public Hierons() {
        super(NAME);
    }

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return new NovelRepresentation(env, NAME).getProblem().assemble((BinaryVariable) solution.getVariable(0));
    }

    @Override
    public AbstractEvolutionaryAlgorithm createEA(BenchmarkEnvironment env) {
        NovelRepresentation representation = new NovelRepresentation(env, NAME);
        NovelRepresentation.Problem problem = representation.getProblem();
        int populationSize = env.configuration().getHieronsPopulationSize();
        String indicator = env.configuration().getBasicIbeaIndicator();
        IndicatorFitnessEvaluator fitnessEvaluator = null;
        Initialization initialization = new HieronsInit(env, representation, populationSize);

        CompoundVariation variation = new CompoundVariation();
        variation.appendOperator(new HUX(env.configuration().getBasicIbeaSinglePointCrossoverProbability()));
        variation.appendOperator(new BitFlip(env.configuration().getBasicIbeaBitFlipProbability()));
        variation.appendOperator(new SmartMutation(representation.literalOrder(), env.sat(), problem::assemble, env.configuration().getHieronsSmartMutationProbability()));
        variation.appendOperator(new SmartReplacement(representation.literalOrder(), env.sat(), env.configuration().getHieronsSmartReplacementProbability()));

        if ("hypervolume".equals(indicator)) {
            fitnessEvaluator = new HypervolumeFitnessEvaluator(problem);
        } else if ("epsilon".equals(indicator)) {
            fitnessEvaluator = new AdditiveEpsilonIndicatorFitnessEvaluator(problem);
        } else {
            throw new IllegalArgumentException("invalid indicator: " + indicator);
        }

        return new IBEA(problem, null, initialization, variation, fitnessEvaluator);
    }

    private static class HieronsInit implements Initialization {
        protected final BenchmarkEnvironment env;
        protected final NovelRepresentation representation;
        protected final int populationSize;

        public HieronsInit(BenchmarkEnvironment env, NovelRepresentation representation, int populationSize) {
            this.env = env;
            this.representation = representation;
            this.populationSize = populationSize;
        }

        public Solution[] initialize() {
            Solution[] initialPopulation = new Solution[this.populationSize];

            for (int i = 0; i < this.populationSize; ++i) {
                Solution solution = this.representation.getProblem().newSolution();
                if(i<env.richSeeds().size() && i < env.configuration().getHieronsSeedCount()) {
                    VecInt seed = env.richSeeds().get(i);
                    VecInt literalOrder = representation.literalOrder();
                    BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
                    Sayyad.binarizeSeed(variable, literalOrder, seed);
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
}
