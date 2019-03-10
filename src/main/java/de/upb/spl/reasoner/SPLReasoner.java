package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import org.moeaframework.core.Solution;

import java.util.List;

public interface SPLReasoner {

	List<Solution> search(BenchmarkEnvironment env);

	FeatureSelection assemble(BenchmarkEnvironment env, Solution solution);

	String name();

}
