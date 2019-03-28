package de.upb.spl.benchmarks.drupal;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collector;
import java.util.stream.Collectors;


import static de.upb.spl.benchmarks.StoredAttributesExecutor.aggregator;

/**
 * This class contains the black box function that evaluates a feature selection based on the drupal feature attributes.
 */
public class DrupalBlackBox extends DrupalModel {

    private final static Logger logger = LoggerFactory.getLogger(DrupalBlackBox.class);

    public static final String GROUP = "drupal";

    @Override
    public Future<JobReport> run(FeatureSelection selection) {
        JobReport report = toReport(selection);
        evaluate(report);
        return ConcurrentUtils.constantFuture(report);
    }

    private JobReport toReport(FeatureSelection selection) {
        JobReport report = new JobReport();
        report.setGroup(GROUP);
        List<String> selectedModules = selection.stream().map(FMUtil::id).collect(Collectors.toList());
        selectedModules.sort(String::compareTo);
        String hash = DigestUtils.sha256Hex(JSONArray.toJSONString(selectedModules));
        JSONObject configuration = new JSONObject();
        configuration.put("selected_modules", selectedModules);
        configuration.put("modules_hash", hash);
        report.setConfiguration(configuration,"modules_hash");
        report.setObjectives(objectives());
        return report;
    }

    private void evaluate(JobReport report) {
        List<String> selectedModules = (List<String>) report.getConfiguration().get("selected_modules");
        List<Integer> selectedModuleIndices = selectedModules.stream()
                .map(this::getModuleIndex)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        Map<String, Double> evaluationResults = objectives().stream()
                .map(Objective::valueOf)
                .collect(Collectors.toMap(
                        Objective::name,
                        objective -> evaluate(objective, selectedModuleIndices)));
        report.setResults(evaluationResults);
    }

    private double evaluate(Objective objective, List<Integer> selectedModuleIndices) {
        if(objective == Objective.IntegrationFaults) {
            int integrationFaultCount = 0;
            for (int[] integrationFault : integrationFaults) {
                boolean integrationFaultExists = true;
                for (int i = 0; i < integrationFault.length; i++) {
                    int moduleIndex = integrationFault[i];
                    if(!selectedModuleIndices.contains(moduleIndex)) {
                        integrationFaultExists = false;
                        break;
                    }
                }
                if(integrationFaultExists) {
                    integrationFaultCount++;
                }
            }
            return integrationFaultCount;
        } else if(objective == Objective.ModuleCount) {
            return selectedModuleIndices.size();
        } else {
            return selectedModuleIndices.stream()
                    .map(attributes.get(objective)::get)
                    .map(Number::doubleValue)
                    .collect(getAttributeAggregator(objective));
        }
    }

    private Collector<Double, ?, Double> getAttributeAggregator(Objective objective) {
        switch (objective) {
            case CC:
                return aggregator("mean");
            case Size:
                return aggregator("15-percentile");
            case Changes:
                return aggregator("sum");
            case TestCases:
                return aggregator("mean");
            case Developers:
                return aggregator("mean");
            case Installations:
                return aggregator("mean");
            case TestAssertions:
                return aggregator("mean");
            case MinorFaults:
            case NormalFaults:
            case MajorFaults:
            case CriticalFaults:
                return aggregator("sum");

            default:
                throw new IllegalArgumentException("Objective " + objective.name() + " isn't a feature attribute.");
        }
    }

    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        return new DrupalAttributesInterpreter(jobReport);
    }

    private class DrupalAttributesInterpreter implements  ReportInterpreter {
        private final JobReport report;

        DrupalAttributesInterpreter(JobReport report) {
            this.report = report;
        }

        @Override
        public Optional<Double> readResult(String objective) {
            Optional<Double> raw = rawResult(objective);
            if(!raw.isPresent() || Double.isNaN(raw.get())) {
                return raw;
            }
            boolean maximize;
            switch (Objective.valueOf(objective)) {
                case Developers:
                case TestAssertions:
                case Installations:
                case ModuleCount:
                    maximize = true;
                    break;
                case Size:
                case CC:
                case Changes:
                case MinorFaults:
                case NormalFaults:
                case MajorFaults:
                case CriticalFaults:
                case IntegrationFaults:
                default:
                    maximize = false;
                    break;
            }
            if(maximize) {
                // flip result so minimizing it, will maximize the actual objective:
                return Optional.of(-1 * raw.get());
            }
            return raw;
        }

        @Override
        public Optional<Double> rawResult(String objective) {
            if(report.getResults().isPresent()) {
                Map<String, Double> results = report.getResults().get();
                Number result = results.get(objective);
                if(result == null) {
                    return Optional.empty();
                }
                else {
                    return Optional.of(result.doubleValue());
                }
            }
            return Optional.empty();
        }

        @Override
        public String group() {
            return GROUP;
        }

    }

}
