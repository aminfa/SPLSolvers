package de.upb.spl;

import de.upb.spl.benchmarks.BenchmarkEnvironment;

import java.util.List;

public interface SPLReasoner {
	List<FeatureSelection> search(BenchmarkEnvironment env);
}
