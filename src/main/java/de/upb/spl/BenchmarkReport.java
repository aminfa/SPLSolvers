package de.upb.spl;

import java.util.Optional;

public interface BenchmarkReport {

	Optional<Double> readResult(String objective);

	boolean constraintsViolated();

	double resourceSum();
}
