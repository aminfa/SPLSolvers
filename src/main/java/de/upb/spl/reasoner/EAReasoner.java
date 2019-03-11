package de.upb.spl.reasoner;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class EAReasoner implements SPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(EAReasoner.class);

    private final String name;
    public EAReasoner(String name) {
        this.name = name;
    }


    public AbstractEvolutionaryAlgorithm runAlgorithm(BenchmarkEnvironment env) {
        AbstractEvolutionaryAlgorithm algorithm = createAlgorithm(env);
        logger.info("Finished initializing {}", name());


        int evals = env.configuration().getEvaluationPermits();
        int currentEvaluations = env.bill(name()).getEvaluationsCount();
        while (currentEvaluations <= evals) {
            logger.debug("{} performing evaluation {}/{}.", name(), currentEvaluations, evals);
            try{
                algorithm.step();
            } catch (Exception ex) {
                logger.error("Error in {}: ", name(), ex);
                break;
            }
            currentEvaluations = env.bill(name()).getEvaluationsCount();
        }
        logger.info("Finished {}.", name());
        return algorithm;
    }

    public Population run(BenchmarkEnvironment env) {
        return runAlgorithm(env).getPopulation();
    }

    @Override
    public List<Solution> search(BenchmarkEnvironment env) {
        Population population = runAlgorithm(env).getResult();
        List<Solution> selections = new ArrayList<>();
        for(Solution solution : population) {
            selections.add(solution);
        }
        return selections;
    }

    @Override
    public String name() {
        return name;
    }


    public abstract AbstractEvolutionaryAlgorithm createAlgorithm(BenchmarkEnvironment env);


}
