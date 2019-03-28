package de.upb.spl.benchmarks.drupal;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.upb.spl.benchmarks.drupal.DrupalBlackBox.GROUP;
import static de.upb.spl.benchmarks.drupal.DrupalModel.Objective.ModuleCount;

public class DrupalWeightedFaults extends BenchmarkEnvironmentDecoration {

    public DrupalWeightedFaults(BenchmarkEnvironment env) {
        super(env);
    }

    private List<String> objectives = Arrays.asList("FaultRate");//, ModuleCount.name());

    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        return new Interpreter(super.interpreter(jobReport));
    }

    private static class Interpreter implements ReportInterpreter {
        final ReportInterpreter base;

        private Interpreter(ReportInterpreter base) {
            this.base = base;
        }

        @Override
        public Optional<Double> readResult(String objective) {
            if(objective.equals("FaultRate")) {
                return rawResult(objective);
            } else if(objective.equals(ModuleCount.name())) {
                return rawResult(objective).map(count -> -count);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Optional<Double> rawResult(String objective) {
            if(objective.equals("FaultRate")) {
                double faultRate = 0;
                Optional<Double> minorFaults = base.rawResult(DrupalModel.Objective.MinorFaults.name());
                Optional<Double> normalFaults = base.rawResult(DrupalModel.Objective.NormalFaults.name());
                Optional<Double> majorFaults = base.rawResult(DrupalModel.Objective.MajorFaults.name());
                Optional<Double> criticalFaults = base.rawResult(DrupalModel.Objective.CriticalFaults.name());
                Optional<Double> integrationFaults = base.rawResult(DrupalModel.Objective.IntegrationFaults.name());
                if (!minorFaults.isPresent() || !normalFaults.isPresent() || !majorFaults.isPresent() || !criticalFaults.isPresent() || !integrationFaults.isPresent()) {
                    return Optional.empty();
                }
                faultRate += minorFaults.get() * 0.0;
                faultRate += normalFaults.get() * 0.0;
                faultRate += majorFaults.get() * 0.;
                faultRate += criticalFaults.get();
                faultRate += integrationFaults.get() * 1;

                return Optional.of(faultRate);
            } else if(objective.equals(ModuleCount.name())) {
                return base.rawResult(ModuleCount.name());
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String group() {
            return GROUP;
        }
    }

    public List<String> objectives(){
        return objectives;
    }


}
