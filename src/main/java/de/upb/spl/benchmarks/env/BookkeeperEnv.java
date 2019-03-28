package de.upb.spl.benchmarks.env;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.JobReport;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class BookkeeperEnv extends BenchmarkEnvironmentDecoration {

    private final static Logger logger = LoggerFactory.getLogger(BookkeeperEnv.class);
    private Map<String, BenchmarkBill> books = new HashMap<>();

    private final BenchmarkBill offBooksTab;

    public BookkeeperEnv(BenchmarkEnvironment env) {
        super(env);
        offBooksTab = new BenchmarkBill(null);
    }

    public BookkeeperEnv(BenchmarkEnvironment env, BenchmarkBill bill) {
        super(env);
        offBooksTab = bill;
    }

    public Collection<BenchmarkBill> bills() {
        return books.values();
    }

    @Override
    public BenchmarkBill currentTab() {
        return offBooksTab;
    }

    public Future<JobReport> run(FeatureSelection selection) {
        BenchmarkBill bill = currentTab();
        Optional<JobReport> loggedReport = bill.checkLog(selection);
        if(loggedReport.isPresent()) {
            return ConcurrentUtils.constantFuture(loggedReport.get());
        } else {
            Future<JobReport> future = super.run(selection);
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

    public synchronized BenchmarkBill bill(String clientName) {
        if(clientName == null) {
            return offBooksTab;
        }
        return books.computeIfAbsent(clientName, newClient-> {
            logger.info("Creating a new bill for {}.", newClient);
            return new BenchmarkBill(newClient);
        });
    }

    public synchronized void logEvaluation(FeatureSelection selection, JobReport report) {
        currentTab().logEvaluation(selection, report);
        BookkeeperEnv innerBook = ((BenchmarkEnvironmentDecoration)getBaseEnv()).getDecoration(BookkeeperEnv.class);
        if(innerBook != null) {
            innerBook.logEvaluation(selection, report);
        }
    }

    public BenchmarkEnvironment billedEnvironment(BenchmarkEnvironment base, String reasonerName) {
        return new BookkeeperEnv(base, bill(reasonerName));
    }

}
