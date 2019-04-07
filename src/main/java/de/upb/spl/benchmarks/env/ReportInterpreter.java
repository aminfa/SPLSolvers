package de.upb.spl.benchmarks.env;

import java.util.Optional;

public interface ReportInterpreter {

	Optional<Double> readResult(String objective);

	default Optional<Double> rawResult(String objective) {
	    return readResult(objective);
    }

    default boolean violatedConstraints() {
	    return false;
    }

}
