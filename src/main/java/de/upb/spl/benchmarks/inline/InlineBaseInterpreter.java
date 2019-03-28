package de.upb.spl.benchmarks.inline;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.FMAttributes;
import de.upb.spl.benchmarks.env.FMXML;
import de.upb.spl.util.FileUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InlineBaseInterpreter extends BenchmarkEnvironmentDecoration {

    private static final List<String> OBJECTIVES = Collections.singletonList("runtime");

    public InlineBaseInterpreter() {
        this(
                new FMAttributes(
                        new FMXML(FileUtil.getPathOfResource("java-inline/feature-model.xml")),
                        new File(FileUtil.getPathOfResource("java-inline/feature-model.xml"))
                                .getParent(),
                        InlineBlackBox.SPL_NAME));
    }

    public InlineBaseInterpreter(BenchmarkEnvironment env) {
        super(env);
    }


    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        return new Interpreter(jobReport);
    }

    public static class Interpreter implements ReportInterpreter {
        private final JobReport report;

        Interpreter(JobReport report) {
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

        @Override
        public String group() {
            return InlineBlackBox.GROUP;
        }

    }


    public List<String> objectives() {
        return OBJECTIVES;
    }

}
