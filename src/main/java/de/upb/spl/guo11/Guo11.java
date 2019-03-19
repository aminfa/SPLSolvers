package de.upb.spl.guo11;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkReport;
import de.upb.spl.benchmarks.env.BenchmarkHelper;
import de.upb.spl.reasoner.*;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import fm.FeatureTreeNode;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.*;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Guo11 extends EAReasoner {

	private final static Logger logger = LoggerFactory.getLogger("GUO_11");
    public final static String NAME = "GUO_11";

    public Guo11() {
        super(NAME);
    }

    @Override
    public AbstractEvolutionaryAlgorithm createEA(BenchmarkEnvironment env) {
        return new GuoAlgorithm(env, new Problem(env),  new Initialization(env));
    }

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return new Problem(env).assemble((BinaryVariable) solution.getVariable(0));
    }

    private class Problem extends BinaryStringProblem {
        final private BenchmarkEnvironment env;

        Problem(BenchmarkEnvironment env) {
            super(1, FMUtil.listFeatures(env.model()));
            this.env = env;
            PRNG.setRandom(env().generator());
        }

        @Override
        public void evaluate(Solution solution) {
            FeatureSelection selection = assemble((BinaryVariable) solution.getVariable(0));
            double[] evaluation = BenchmarkHelper.evaluateFeatureSelection(env, selection);
            double[] score = new double[1];
            /*
             * If a single objective fails (is NaN), return the worst score
             */
            for (int i = 0; i < evaluation.length; i++) {
                if (Double.isNaN(evaluation[i])) {
                    score[0] = Double.MAX_VALUE;
                    solution.setObjectives(score);
                    return;
                }
            }

            for (int i = 0; i < evaluation.length; i++) {
                score[0] += evaluation[i];
            }
            solution.setObjectives(score);
        }

        public BenchmarkEnvironment env() {
            return env;
        }
    }


    private class Initialization implements org.moeaframework.core.Initialization {

        final private BenchmarkEnvironment env;
        final private Double  d;  // [0, 1] parameter d determines expected number of selected features in every generated chromosome.
        final private Integer p,  // runAndGetPopulation size
                n; // number of features



        public Initialization(BenchmarkEnvironment environment) {
            SPLReasonerConfiguration configuration = environment.configuration();
            this.env = environment;
            this.n = FMUtil.countFeatures(environment.model());
            this.d = configuration.getGUOD();
            this.p = configuration.getGUOPopulationSize();
        }


        @Override
        public Solution[] initialize() {
            Random generator = env.generator();
            List<FeatureTreeNode> featureOrder = FMUtil.featureStream(env.model()).collect(Collectors.toList());
            List<Solution> generatedSolutions = new ArrayList<>();
            List<Future<BenchmarkReport>> reports =  new ArrayList<>();
            for (int i = 0; i < p; i++) {
                Solution solution = new Solution(1, 1);
                generatedSolutions.add(solution);
                BinaryVariable binaryString =  new BinaryVariable(FMUtil.countFeatures(env.model()));
                solution.setVariable(0, binaryString);
                for (int j = 0; j < n; j++) {
                    double rand = generator.nextDouble();
                    boolean gene = false;
                    if(rand < d) {
                        gene = true;
                    }
                    binaryString.set(j, gene);
                }
            }
            logger.info("Population generated.");
            return generatedSolutions.toArray(new Solution[0]);
        }
    }

}
