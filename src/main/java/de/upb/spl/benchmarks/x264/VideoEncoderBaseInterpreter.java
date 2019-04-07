package de.upb.spl.benchmarks.x264;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.env.ReportInterpreter;
import de.upb.spl.benchmarks.env.*;
import de.upb.spl.util.FileUtil;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class VideoEncoderBaseInterpreter extends BenchmarkEnvironmentDecoration {


    public VideoEncoderBaseInterpreter() {
            this(new ConfiguredEnv(new FMAttributes(
                            new FMXML(FileUtil.getPathOfResource("x264/feature-model.xml")),
                            new File(FileUtil.getPathOfResource("x264/feature-model.xml")).getParent(),
                            VideoEncoderBlackBox.SPL_NAME)));
    }

    public VideoEncoderBaseInterpreter(BenchmarkEnvironment env) {
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
            Optional<Double> raw = rawResult(objective);
            if(!raw.isPresent()) {
                return raw;
            }
            if(objective.equals(VideoEncoderBlackBox.Objectives.run_time.name())) {
                return raw.map(runtime -> (double) Math.round(runtime * 100d) / 100d);
            }
            if(objective.equals(VideoEncoderBlackBox.Objectives.subjective_quality.name())) {
                return raw.map(quality -> -1. * Math.round(quality));
            }
            return raw;
        }

        @Override
        public Optional<Double> rawResult(String objective) {
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
