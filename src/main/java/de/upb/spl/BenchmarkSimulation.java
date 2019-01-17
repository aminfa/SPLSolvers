package de.upb.spl;

import fm.FeatureModel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class BenchmarkSimulation implements BenchmarkEnvironment {
	private final BiFunction<FeatureSelection, String, Double> simulation;

	public BenchmarkSimulation(BiFunction<FeatureSelection, String, Double> simulation) {
		this.simulation = simulation;
	}

	@Override
	public final Future<BenchmarkReport> run(FeatureSelection selection) {
		return new Future<BenchmarkReport>() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return true;
			}

			@Override
			public BenchmarkReport get() throws InterruptedException, ExecutionException {
				return new BenchmarkReport() {
					@Override
					public Optional<Double> readResult(String objective) {
						return Optional.ofNullable(simulation.apply(selection, objective));
					}

					@Override
					public boolean constraintsViolated() {
						return false;
					}

					@Override
					public double resourceSum() {
						return 1;
					}
				};
			}

			@Override
			public BenchmarkReport get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				return get();
			}
		};
	}


}
