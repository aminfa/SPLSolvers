package de.upb.spl.benchmarks;

import java.util.Optional;

public interface ReportInterpreter {

	Optional<Double> readResult(String objective);

	default Optional<Double> rawResult(String objective) {
	    return readResult(objective);
    }

    String group();
}
