package de.upb.spl.benchmarks.env;

import com.google.common.util.concurrent.Futures;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class Bookkeeper extends BenchmarkEnvironmentDecoration {

    private final static Logger logger = LoggerFactory.getLogger(Bookkeeper.class);
    private Map<String, BenchmarkBill> books = new HashMap<>();

    private final BenchmarkBill offBooksTab = new BenchmarkBill(null) {
        public void logEvaluation(FeatureSelection selection, ReportInterpreter report) {
            // log nothing.
        }
    };

    public Bookkeeper(BenchmarkEnvironment env) {
        super(env);
    }

    @Override
    public BenchmarkBill currentTab() {
        return offBooksTab;
    }

    public synchronized BenchmarkBill bill(String clientName) {
        if(clientName == null) {
            return offBooksTab;
        }
        return books.computeIfAbsent(clientName, newClient-> {
            logger.info("Creating a new bill for {}.", newClient);
            return new BenchmarkBill(newClient);
        });
    }

    public static class Bill extends BenchmarkEnvironmentDecoration {

        private final BenchmarkBill bill;

        public Bill(BenchmarkEnvironment env, BenchmarkBill bill) {
            super(env);
            this.bill = bill;
        }


        public BenchmarkBill currentTab() {
            return bill;
        }

        @Override
        public Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill) {
            Optional<JobReport> loggedReport = bill.checkLog(selection);
            if(loggedReport.isPresent()) {
                return ConcurrentUtils.constantFuture(loggedReport.get());
            } else {
                Future<JobReport> future = super.run(selection, bill);
                return new Future<JobReport>() {
                    boolean billed = false;

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
                        billReport(report);
                        return report;
                    }

                    synchronized void billReport(JobReport report) {
                        if (billed) {
                            return;
                        }
                        currentTab().logEvaluation(selection, report);
                        billed = true;
                    }

                    @Override
                    public JobReport get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                        JobReport report = future.get(timeout, unit);
                        billReport(report);
                        return report;
                    }
                };
            }
        }

    }

}
