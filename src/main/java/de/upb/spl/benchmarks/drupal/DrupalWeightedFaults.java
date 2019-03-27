package de.upb.spl.benchmarks.drupal;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DrupalWeightedFaults extends BenchmarkEnvironmentDecoration {

    public DrupalWeightedFaults(BenchmarkEnvironment env) {
        super(env);
    }

    private List<String> objectives = Collections.singletonList("FaultRate");

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
            return rawResult(objective).map(result -> result * -1);
        }

        @Override
        public Optional<Double> rawResult(String objective) {
            double faultRate = 0;
            Optional<Double> minorFaults = base.rawResult(DrupalModel.Objective.MinorFaults.name());
            Optional<Double> normalFaults = base.rawResult(DrupalModel.Objective.NormalFaults.name());
            Optional<Double> majorFaults = base.rawResult(DrupalModel.Objective.MajorFaults.name());
            Optional<Double> criticalFaults = base.rawResult(DrupalModel.Objective.CriticalFaults.name());
            Optional<Double> integrationFaults = base.rawResult(DrupalModel.Objective.IntegrationFaults.name());
            if (!minorFaults.isPresent() || !normalFaults.isPresent() || !majorFaults.isPresent() || !criticalFaults.isPresent() || !integrationFaults.isPresent()) {
                return Optional.empty();
            }
            faultRate += minorFaults.get() * 0.125;
            faultRate += normalFaults.get() * 0.25;
            faultRate += majorFaults.get() * 0.5;
            faultRate += criticalFaults.get();
            faultRate += integrationFaults.get() * 2;
            return Optional.of(faultRate);
        }
    }

    public List<String> objectives(){
        return objectives;
    }


}
