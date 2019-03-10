package de.upb.spl.benchmarks;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

public abstract class BenchmarkSimulation implements BenchmarkEnvironment {
	private final BiFunction<FeatureSelection, String, Double> simulation;

	public BenchmarkSimulation(BiFunction<FeatureSelection, String, Double> simulation) {
	    this.simulation = simulation;
	}

	@Override
	public final Future<BenchmarkReport> run(FeatureSelection selection, String clientName) {
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
			    bill(clientName).logEvaluation();
				return objective -> Optional.ofNullable(simulation.apply(selection, objective));
			}

			@Override
			public BenchmarkReport get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				return get();
			}
		};
	}


}
