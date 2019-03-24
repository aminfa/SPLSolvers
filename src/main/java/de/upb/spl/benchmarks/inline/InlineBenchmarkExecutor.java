package de.upb.spl.benchmarks.inline;

import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.JobApplication;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class InlineBenchmarkExecutor implements Runnable {

    public static final String GROUP = "java-inline";

    private final static Logger logger = LoggerFactory.getLogger(InlineBenchmarkExecutor.class);
    private static final AtomicInteger GLOBAL_ID_COUNT = new AtomicInteger(0);

    private final Thread executor;
    private final String id;
    private final BenchmarkAgent agent;

    public InlineBenchmarkExecutor(BenchmarkAgent agent){
        id = String.format("java-inline-executor-%02d", GLOBAL_ID_COUNT.getAndIncrement());
        this.agent = agent;
        executor = new Thread(this);
        executor.setDaemon(true);
        executor.setName(id);
        executor.setPriority(Thread.MIN_PRIORITY);
        executor.start();
    }

    public String getExecutorId(){
        return id;
    }

    public void run() {
        logger.info("`{}` started.", getExecutorId());
        while(true) {
            try{
                executeJob();
            }catch(Exception ex) {
                logger.error("`{}` unexpected error while executing job:\n ", getExecutorId(),  ex);
            }
        }
    }


    private JobApplication getJobApplication () {
        JobApplication application = new JobApplication(id, GROUP, Collections.EMPTY_LIST);
        return application;
    }

    private void executeJob() throws InterruptedException {
        logger.info("Asking agent for job");
        JobReport report = agent.jobs().waitForJob(getJobApplication());
        try {
            executeJob(report);
        } catch (Exception ex) {
            logger.error("{} error while executing job with id {}: ", id, report.getJobId(), ex);
            report.setResultsIfNull();
        } finally {
            agent.jobs().update(report);
        }
    }

    private void executeJob(JobReport report) {
        Map<String, Integer> inlineConfig = (Map<String, Integer>) report.getConfiguration().get("inline_config");
        InlineConfigurationSample sample = new InlineConfigurationSample();
        sample.loadMap(inlineConfig);
        double runtime = sample.score();
        Map<String, Double> results = new HashMap<>();
        results.put("runtime", runtime);
        report.setResults(results);
    }
}
