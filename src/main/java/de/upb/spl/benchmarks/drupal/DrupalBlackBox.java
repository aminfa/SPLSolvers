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
public class DrupalBlackBox extends DrupalFiles {

    private final static Logger logger = LoggerFactory.getLogger(DrupalBlackBox.class);

    @Override
    public Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill) {
        JobReport report = toReport(selection, bill.getClientName());
        evaluate(report);
        return ConcurrentUtils.constantFuture(report);
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


    private Collector<Double, ?, Double> getAttributeAggregator(Objective objective) {
        switch (objective) {
            case CC:
                return aggregator("mean");
            case Size:
                return aggregator("sum");
            case Changes:
                return aggregator("sum");
            case TestCases:
                return aggregator("20-percentile");
            case Developers:
                return aggregator("5-percentile");
            case Installations:
                return aggregator("min");
            case TestAssertions:
                return aggregator("20-percentile");
            case MinorFaults:
            case NormalFaults:
            case MajorFaults:
            case CriticalFaults:
                return aggregator("sum");

            default:
                throw new IllegalArgumentException("Objective " + objective.name() + " isn't a feature attribute.");
        }
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
        } else if(objective == Objective.FeatureCount) {
            return selectedModuleIndices.size();
        } else {
            return selectedModuleIndices.stream()
                    .map(attributes.get(objective)::get)
                    .map(Number::doubleValue)
                    .collect(getAttributeAggregator(objective));
        }
    }

    private JobReport toReport(FeatureSelection selection, String clientName) {
        JobReport report = new JobReport();
        report.setGroup("drupal");
        report.setClient(clientName);
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
                case Size:
                case CC:
                case Developers:
                case Changes:
                case MinorFaults:
                case NormalFaults:
                case MajorFaults:
                case CriticalFaults:
                case IntegrationFaults:
                default:
                    maximize = false;
                    break;
                case TestAssertions:
                case Installations:
                case FeatureCount:
                    maximize = true;
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

    }

}
