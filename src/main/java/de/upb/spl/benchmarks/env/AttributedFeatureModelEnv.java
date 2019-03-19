package de.upb.spl.benchmarks.env;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.*;
import fm.FeatureTreeNode;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class AttributedFeatureModelEnv extends BenchmarkEnvironmentDecoration {
    private final static Logger logger = LoggerFactory.getLogger(AttributedFeatureModelEnv.class);

    private final StoredAttributesExecutor executor;


    public AttributedFeatureModelEnv(String resourceFolder, String splName) {
        this(new FileBenchmarkEnv(resourceFolder, splName));
    }

    public AttributedFeatureModelEnv(FileBenchmarkEnv env) {
        super(env);
        this.executor = new StoredAttributesExecutor(null, attributes());
    }

    @Override
    public Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill) {
        Optional<JobReport> loggedReport = bill.checkLog(selection);
        if(loggedReport.isPresent()) {
            return ConcurrentUtils.constantFuture(loggedReport.get());
        } else {
            JobReport job = toReport(selection);
            try {
                executor.executeJob(job);
                bill.logEvaluation(selection, job);
                return ConcurrentUtils.constantFuture(job);
            } catch (Exception e) {
                logger.warn("Couldn't create attribute values for assemble={}.", selection, e);
                throw new IllegalArgumentException("Attribute Feature model env.");
            }
        }
    }

    @Override
    public BenchmarkReport reader(JobReport jobReport) {
        return new AttributeValueReport(jobReport);
    }

    protected FileBenchmarkEnv getBaseEnv() {
        return (FileBenchmarkEnv) super.getBaseEnv();
    }

    public Map attributes() {
        return getBaseEnv().attributes();
    }

    public JobReport toReport(FeatureSelection selection) {
        JobReport report = new JobReport();
        report.setGroup("FixedAttributeValue");
        report.setConfiguration(toConfiguration(selection));
        report.setObjectives(objectives());
        return report;
    }

    private Map toConfiguration(FeatureSelection selection) {
        List<String> selectedFeatures = FMUtil.featureStream(model()).filter(selection::isSelected).map(FeatureTreeNode::getName).collect(Collectors.toList());
        return Collections.singletonMap("features", selectedFeatures);
    }

    class AttributeValueReport implements BenchmarkReport {

        private final JobReport report;

        AttributeValueReport(JobReport report) {
            this.report = report;
        }

        @Override
        public Optional<Double> readResult(String objective) {
            Optional<Double> rawResult = rawResult(objective);
            if(!rawResult.isPresent()){
                return rawResult;
            }
            Map<String, Boolean> attr = (Map<String, Boolean>) attributes().get(objective);
            if(attr!=null) {
                boolean toBeMinimized = attr.getOrDefault("minimized", true);
                if(!toBeMinimized){
                    return Optional.of(rawResult.get()*-1);
                }
            }
            return rawResult;
        }

        public Optional<Double> rawResult(String objective) {
            if(!report.getResults().isPresent()) {
                return Optional.empty();
            } else {
                Number result = (Number) report.getResults().get().get(objective);
                if(result == null) {
                    throw new IllegalArgumentException("Attribute " + objective + " cannot be found.");
                }
                return Optional.ofNullable(result.doubleValue());
            }
        }

        @Override
        public JobReport getJobReport() {
            return report;
        }
    }

}
