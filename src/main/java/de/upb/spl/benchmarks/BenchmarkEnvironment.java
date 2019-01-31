package de.upb.spl.benchmarks;

import de.upb.spl.FeatureSelection;
import fm.FeatureModel;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

public interface BenchmarkEnvironment {

	FeatureModel model();

	List<String> objectives();

	Future<BenchmarkReport> run(FeatureSelection selection);

	Random generator();

	<T> T readParameter(String parameterName);
}
