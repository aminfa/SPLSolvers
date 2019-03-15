package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkBill;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.hasco.FeatureSelectionEvaluatedEvent;
import de.upb.spl.hasco.FeatureSelectionPerformance;
import jaicore.basic.algorithm.AAlgorithm;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.events.*;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.Population;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.indicator.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.StreamSupport;

/**
 * Evolutionary Algorithm Reasoner.
 * Implementations need to provide AbstractEvolutionaryAlgorithm and this class provides the the bridge to the AILibs IAlgorithm.
 *
 */
public abstract class EAReasoner implements SPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(EAReasoner.class);

    private final String name;
    public EAReasoner(String name) {
        this.name = name;
    }

    public SPLEvoAlgorithm algorithm(BenchmarkEnvironment env) {
        return new SPLEvoAlgorithm(env);
    }

    public static Solution bestPerformer(Problem problem, Population population) {
        if(population.size() < 1) {
            /*
             * 0 solutions..
             */
            throw new IllegalArgumentException("Not a single solution was in the population.");
        } else if(population.size() == 1) {
            /*
             * A single solution
             */
            return population.get(0);
        }

        /*
         * Normalize the objectives and pick the candidate with the best mean performance:
         */
        Normalizer normalizer = new Normalizer(problem, population);
        Population normalizedPopulation = normalizer.normalize(population);
        final Mean mean = new Mean();
        /*
         * Calculate the mean performance of each condidate
         */
        final double[] populationPerformances = StreamSupport
                .stream(normalizedPopulation.spliterator(), false)
                .map(Solution::getObjectives)
                .mapToDouble(mean::evaluate).toArray();

        /*
         * Find the index of the best performer
         */
        int bestPerformerIndex = -1;
        double bestPerformance = Double.MAX_VALUE;
        for (int i = 0; i < populationPerformances.length; i++) {
            if(bestPerformance > populationPerformances[i]) {
                bestPerformerIndex = i;
                bestPerformance = populationPerformances[i];
            }
        }
        return population.get(bestPerformerIndex);
    }

    public AbstractEvolutionaryAlgorithm runAlgorithm(BenchmarkEnvironment env) {
        SPLEvoAlgorithm algorithm = algorithm(env);
        try {
            algorithm.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return algorithm.getEA();
    }

    /**
     * Helper method that runs the evo algorithm and returns the population.
     * @param env input
     * @return population of solutions
     */
    public Population runAndGetPopulation(BenchmarkEnvironment env) {
        return runAlgorithm(env).getPopulation();
    }

    @Override
    public List<FeatureSelection> search(BenchmarkEnvironment env) {
        Population population = runAlgorithm(env).getResult();
        List<FeatureSelection> selections = new ArrayList<>();
        for(Solution solution : population) {
            selections.add(assemble(env,solution));
        }
        return  selections;
    }

    @Override
    public String name() {
        return name;
    }

    public abstract AbstractEvolutionaryAlgorithm createEA(BenchmarkEnvironment env);

    /**
     * This IAlgorithm class wraps an evolutionary algorithm.
     */
    public final class SPLEvoAlgorithm extends AAlgorithm<BenchmarkEnvironment, FeatureSelection> {

        private int lastEvalCountLog = -1;
        private int nextEvalIndex = 0;
        private AbstractEvolutionaryAlgorithm ea;

        protected SPLEvoAlgorithm(BenchmarkEnvironment env) {
            super(env);
        }

        @Override
        public String getId() {
            return name;
        }

        @Override
        public AlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, TimeoutException, AlgorithmException {
            switch(getState()) {
                case created:
                    ea = createEA(getInput());
                    logger.info("Finished initializing {}", name());
                    activate();
                    return new AlgorithmInitializedEvent(getId());
                case active:
                    return step();
                default:
                    return new AlgorithmFinishedEvent(getId());
            }
        }

        /**
         * Advances the EA until a never-seen-before candidate has been evaluated.
         * When multiple new candidates are evaluated in one ea step, they are returned separately in each step invocation.
         * Returns candidates in the order they were found.
         * Ends the algorithm if the evaluation permits are reached.
         *
         * @return Candidate solution found by ea.
         * @throws AlgorithmException error when running ea algorithm
         */
        private ASolutionCandidateFoundEvent step() throws AlgorithmException {
            int evals = getInput().configuration().getEvaluationPermits();
            int currentEvals = getInput().bill(name()).getEvaluationsCount();
            if(nextEvalIndex > evals) {
                throw new AlgorithmException("Algorithm already finished.");
            }
            if(currentEvals > lastEvalCountLog) {
                lastEvalCountLog = currentEvals;
                logger.debug("{} performing evaluation {}/{}.", name(), currentEvals, evals);
            }
            while(nextEvalIndex == currentEvals) {
                try {
                    ea.step(); // EVO ALGORITHM GENERATION STEP
                } catch(Exception ex) {
                    throw new AlgorithmException(ex, "Error running while stepping ea in: " + name());
                }
                currentEvals = getInput().bill(name()).getEvaluationsCount();
            }
            BenchmarkBill.Log log = getInput().bill(name()).checkLog(nextEvalIndex);
            FeatureSelectionPerformance performance = new FeatureSelectionPerformance(0, SPLEvaluator.extractEvaluation(getInput(), log.report(), true));
            nextEvalIndex++;
            if(nextEvalIndex > evals) {
                terminate();
            }
            return new FeatureSelectionEvaluatedEvent(getId(), log.selection(), nextEvalIndex-1, performance);
        }

        public AbstractEvolutionaryAlgorithm getEA() {
            return ea;
        }

        @Override
        public FeatureSelection call() throws InterruptedException, AlgorithmExecutionCanceledException, TimeoutException, AlgorithmException {
            while (hasNext()) {
                next();
            }
            try{
                return assemble(getInput(), bestPerformer(getEA().getProblem(), getEA().getPopulation()));
            } catch (IllegalArgumentException ex) {
                throw new AlgorithmException(ex, "Error selecting best performer.");
            }
        }
    }
}
