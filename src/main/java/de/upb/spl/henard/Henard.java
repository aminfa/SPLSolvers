package de.upb.spl.henard;

import de.upb.spl.benchmarks.BenchmarkEnvironment;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Henard {

	public final static String
				INIT_POPULATION_SIZE = "population_size",
				GA_GENERATIONS = "generations";

	private final static Logger logger = LoggerFactory.getLogger("Henard");
	private final BenchmarkEnvironment env;
	HenardProblemDefinition problem;
	public Henard(BenchmarkEnvironment environment) {
		this.env = Objects.requireNonNull(environment, "Provide benchmark environment! Was null.");
		problem = new HenardProblemDefinition(env);
	}


	public NondominatedPopulation run() {
		HenardProblemDefinition problem = new HenardProblemDefinition(env);

		Initialization initialization = new HenardInitialization(env);

		Algorithm algorithm = new HenardAlgorithm(problem, initialization);

		int generations = env.readParameter(GA_GENERATIONS);
		if(generations < 1) {
			throw new IllegalArgumentException("Generations must be > 0. Was : "  + generations);
		}
		while(algorithm.getNumberOfEvaluations() < generations) {
			algorithm.step();
		}
		return algorithm.getResult();
	}

}
