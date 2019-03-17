package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import jaicore.basic.algorithm.IAlgorithm;
import org.moeaframework.core.Solution;

import java.util.Collection;
import java.util.List;

public interface SPLReasoner {

    SPLReasonerAlgorithm algorithm(BenchmarkEnvironment env);

    Collection<FeatureSelection> search(BenchmarkEnvironment env);

	FeatureSelection assemble(BenchmarkEnvironment env, Solution solution);

	String name();

}
