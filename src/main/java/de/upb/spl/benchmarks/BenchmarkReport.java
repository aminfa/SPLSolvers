package de.upb.spl.benchmarks;

import java.util.Optional;

public interface BenchmarkReport {

	Optional<Double> readResult(String objective);

	default Optional<Double> rawResult(String objective) {
	    return readResult(objective);
    }

    JobReport getJobReport();

}
