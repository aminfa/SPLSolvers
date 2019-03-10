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

public class AttributedFeatureModelEnv extends AbstractBenchmarkEnv implements BenchmarkEnvironment {
    private final static Logger logger = LoggerFactory.getLogger(AttributedFeatureModelEnv.class);

    private final StoredAttributesExecutor executor;


    public AttributedFeatureModelEnv(String resourceFolder, String splName) {
        super(resourceFolder, splName);
        this.executor = new StoredAttributesExecutor(null, attributes());
    }


    @Override
    public Future<BenchmarkReport> run(FeatureSelection selection, String clientName) {
        JobReport job = toReport(selection);
        try {
            executor.executeJob(job);
            bill(clientName).logEvaluation();
        } catch (Exception e) {
            logger.warn("Couldn't create attribute values for assemble={}.", selection, e);
        }
        return ConcurrentUtils.constantFuture(new AttributeValueReport(job));
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

    static class AttributeValueReport implements BenchmarkReport {

        private final JobReport report;

        AttributeValueReport(JobReport report) {
            this.report = report;
        }

        @Override
        public Optional<Double> readResult(String objective) {
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
    }

}
