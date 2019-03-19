package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
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


    public abstract FeatureSelection assemble(BenchmarkEnvironment env, Solution solution);


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

//    @Override
    public List<FeatureSelection> search(BenchmarkEnvironment env) {
        Population population = runAlgorithm(env).getResult();
        List<FeatureSelection> selections = new ArrayList<>();
        for(Solution solution : population) {
            selections.add(assemble(env,solution));
        }
        return selections;
    }

    @Override
    public String name() {
        return name;
    }

    public abstract AbstractEvolutionaryAlgorithm createEA(BenchmarkEnvironment env);

    /**
     * This IAlgorithm class wraps an evolutionary algorithm.
     */
    public final class SPLEvoAlgorithm extends SPLReasonerAlgorithm {

        private AbstractEvolutionaryAlgorithm ea;

        protected SPLEvoAlgorithm(BenchmarkEnvironment env) {
            super(env, name);
        }

        public AbstractEvolutionaryAlgorithm getEA() {
            return ea;
        }

        @Override
        protected void init() {
            ea = createEA(getInput());
            logger.info("Finished initializing {}", name());
            activate();
        }

        /**
         * Steps a generation in the evolutionary algorithm.
         */
        @Override
        protected void proceed() {
            ea.step(); // EVO ALGORITHM GENERATION STEP
        }

        @Override
        protected FeatureSelection best() {
            return assemble(getInput(), bestPerformer(getEA().getProblem(), getEA().getPopulation()));
        }
    }
}
