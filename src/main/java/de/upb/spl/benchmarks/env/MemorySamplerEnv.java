package de.upb.spl.benchmarks.env;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.JobReport;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MemorySamplerEnv extends BenchmarkEnvironmentDecoration {
    public MemorySamplerEnv(BenchmarkEnvironment env) {
        super(env);
    }

    @Override
    public Future<JobReport> run(FeatureSelection selection) {
        Future<JobReport> future = super.run(selection);
        /*
         * Measure the memory when the report is accessed.
         */
        return new Future<JobReport>() {
            /**
             * This flag guards against doing multiple measurements.
             */
            boolean measured = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public JobReport get() throws InterruptedException, ExecutionException {
                JobReport report = future.get();
                measureMemory(report);
                return report;
            }

            synchronized void measureMemory(JobReport report) {
                if (measured) {
                    return;
                }
                Runtime runtime = Runtime.getRuntime();
                // Calculate the used memory
                long memoryInBytes = runtime.totalMemory() - runtime.freeMemory();
                int memoryInMegaBytes =(int) (memoryInBytes >> 20);
                report.setMemory(memoryInMegaBytes);
                measured = true;
            }

            @Override
            public JobReport get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                JobReport report = future.get(timeout, unit);
                measureMemory(report);
                return report;
            }
        };
    }
}
