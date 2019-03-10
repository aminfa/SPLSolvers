package de.upb.spl.henard;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.PositiveLiteralSelectionStrategy;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class Henard implements SPLReasoner {

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
		int permits = env.configuration().getEvaluationPermits();
		while(algorithm.getNumberOfEvaluations() < permits) {
			algorithm.step();
		}
		return algorithm.getResult();
	}



	@Override
	public List<Solution> search(BenchmarkEnvironment env) {
		return null;
	}

    @Override
    public FeatureSelection assemble(BenchmarkEnvironment env, Solution solution) {
        return null;
    }

    @Override
	public String name() {
		return "Henard";
	}
}
