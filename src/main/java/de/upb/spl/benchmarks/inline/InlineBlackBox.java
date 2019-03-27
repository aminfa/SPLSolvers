package de.upb.spl.benchmarks.inline;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.FMAttributes;
import de.upb.spl.benchmarks.env.FMXML;
import de.upb.spl.util.FileUtil;
import fm.FeatureTreeNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InlineBlackBox extends BenchmarkEnvironmentDecoration {

    private static final List<String> OBJECTIVES = Collections.singletonList("runtime");

    private static final String SPL_NAME = "java-inline";

    private final static Logger logger = LoggerFactory.getLogger(InlineBlackBox.class);

    private final BenchmarkAgent agent;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public InlineBlackBox(BenchmarkEnvironment env, BenchmarkAgent agent) {
        super(env);
        this.agent = agent;
    }



    public InlineBlackBox(BenchmarkAgent agent) {
        this(
                new FMAttributes(
                        new FMXML(FileUtil.getPathOfResource("java-inline/feature-model.xml")),
                        new File(FileUtil.getPathOfResource("java-inline/feature-model.xml"))
                                .getParent(),
                        SPL_NAME),
                agent);
    }

    @Override
    public Future<JobReport> run(FeatureSelection selection) {
        JobReport report = toReport(selection);
        Submission submission = new Submission(report);
        return executor.submit(submission);
    }

    private class Submission implements Callable<JobReport> {

        final JobReport report;

        Submission(JobReport report) {
            this.report = report;
        }

        @Override
        public JobReport call() throws Exception {
            agent.jobs().offerJob(report);
            agent.jobs().waitForResults(report);
            return report;
        }
    }


    private JobReport toReport(FeatureSelection selection) {
        JobReport report = new JobReport();
        report.setGroup(SPL_NAME);
        int FreqInlineSize = -1,
            InlineSmallCode = -1,
            MaxInlineLevel = -1,
            MaxInlineSize = -1,
            MaxRecursiveInlineLevel = -1,
            MinInliningThreshold = -1;

        for(FeatureTreeNode feature : selection) {
            String[] featureParameterTuple = FMUtil.id(feature).split("_");
            if (featureParameterTuple.length != 2) {
                continue;
            }
            String featureAbbreviation = featureParameterTuple[0];
            int parameter;
            try {
                parameter = Integer.parseInt(featureParameterTuple[1]);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("BUG in feature model:" +
                        " this feature has no numeric parameter: " +
                        feature.getID(), ex);
            }
            switch (featureAbbreviation) {
                case "FIS":
                    FreqInlineSize = parameter;
                    break;
                case "ISC":
                    InlineSmallCode = parameter;
                    break;
                case "MIL":
                    MaxInlineLevel = parameter;
                    break;
                case "MIS":
                    MaxInlineSize = parameter;
                    break;
                case "MRL":
                    MaxRecursiveInlineLevel = parameter;
                    break;
                case "MIT":
                    MinInliningThreshold = parameter;
                    break;
                default:
                    throw new RuntimeException("BUG in feature model:" +
                            " feature abbreviation is not known: " +
                            featureAbbreviation);
            }

        }

        InlineConfigurationSample sample = new InlineConfigurationSample();

        if(FreqInlineSize == -1) {
            throw new RuntimeException("BUG in feature model. Not all inline parameters were initialized.");
        } else
            sample.setFreqInlineSize(FreqInlineSize);

        if(InlineSmallCode == -1) {
            throw new RuntimeException("BUG in feature model. Not all inline parameters were initialized.");
        } else
            sample.setInlineSmallCode(InlineSmallCode);

        if(MaxInlineLevel == -1) {
            throw new RuntimeException("BUG in feature model. Not all inline parameters were initialized.");
        } else
            sample.setMaxInlineLevel(MaxInlineLevel);

        if(MaxInlineSize == -1) {
            throw new RuntimeException("BUG in feature model. Not all inline parameters were initialized.");
        } else
            sample.setMaxInlineSize(MaxInlineSize);

        if(MaxRecursiveInlineLevel == -1) {
            throw new RuntimeException("BUG in feature model. Not all inline parameters were initialized.");
        } else
            sample.setMaxRecursiveInlineLevel(MaxRecursiveInlineLevel);

        if(MinInliningThreshold == -1) {
            throw new RuntimeException("BUG in feature model. Not all inline parameters were initialized.");
        } else
            sample.setMinInliningThreshold(MinInliningThreshold);

        sample.setWarmups(configuration().getInlineBenchmarkWarmups());
        sample.setTarget(configuration().getInlineBenchmarkTarget());

        Map inlineConfiguration = sample.dumpMap();
        String hash = DigestUtils.sha256Hex(JSONObject.toJSONString(inlineConfiguration));
        JSONObject configuration = new JSONObject();
        configuration.put("inline_config", inlineConfiguration);
        configuration.put("config_hash", hash);
        report.setConfiguration(configuration,"config_hash");
        report.setObjectives(objectives());
        return report;
    }

    public List<String> objectives() {
        return OBJECTIVES;
    }

    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        return new InlineConfigurationReportInterpreter(jobReport);
    }

    public static class InlineConfigurationReportInterpreter implements ReportInterpreter {
        private final JobReport report;

        InlineConfigurationReportInterpreter(JobReport report) {
            this.report = report;
        }

        @Override
        public Optional<Double> readResult(String objective) {
            if(report.getResults().isPresent()) {
                Map results = report.getResults().get();
                if(results.containsKey(objective)){
                    return Optional.of(((Number) results.get(objective) ).doubleValue());
                }
            }
            return Optional.empty();
        }

    }

}
