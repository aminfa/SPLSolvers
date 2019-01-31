package de.upb.spl.guo11;

import de.upb.spl.benchmarks.BenchmarkEnvironment;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Guo11 {

	public final static String
				INIT_POPULATION_SIZE = "population_size",
				INIT_D = "GUO11_d",
				GA_GENERATIONS = "generations";

	private final static Logger logger = LoggerFactory.getLogger("GUO_11");
	private final BenchmarkEnvironment env;
	GuoProblemDefinition problem;
	public Guo11(BenchmarkEnvironment environment) {
		this.env = Objects.requireNonNull(environment, "Provide benchmark environment! Was null.");
		problem = new GuoProblemDefinition(env);
	}


	public NondominatedPopulation run() {
		GuoProblemDefinition problem = new GuoProblemDefinition(env);

		Initialization initialization = new GuoInitialization(env);

		Algorithm algorithm = new GuoAlgorithm(problem, initialization);

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
