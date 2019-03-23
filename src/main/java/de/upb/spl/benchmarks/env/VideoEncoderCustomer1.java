package de.upb.spl.benchmarks.env;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;

import java.util.*;

public class VideoEncoderCustomer1 extends BenchmarkEnvironmentDecoration {


    String fileSizeObjective = VideoEncoderBlackBox.Objectives.file_size.name();
    List<String> objectives = (List<String>) Collections.singletonList(fileSizeObjective);

    public static final Map<String, Double> VIDEO_QUALITY_THRESHOLD = new HashMap<>();

    static {
        VIDEO_QUALITY_THRESHOLD.put("grandma", 90.);
    }

    private final double qualityThreshold;

    public VideoEncoderCustomer1(BenchmarkEnvironment env) {
        this(env, 0.);
    }

    public VideoEncoderCustomer1(BenchmarkEnvironment env, double thresholdDelta) {
        super(env);
        qualityThreshold = VIDEO_QUALITY_THRESHOLD.getOrDefault(env.configuration().getVideoSourceFile(), 90.) + thresholdDelta;
        assert env.objectives().contains(VideoEncoderBlackBox.Objectives.file_size.name());
        assert env.objectives().contains(VideoEncoderBlackBox.Objectives.subjective_quality.name());
    }

    public List<String> objectives() {
        return objectives;
    }

    public ReportInterpreter interpreter(JobReport report) {
        ReportInterpreter baseInterpreter = super.interpreter(report);
        return new ReportInterpreter() {
            @Override
            public Optional<Double> readResult(String objective) {
                Optional<Double> quality = baseInterpreter.rawResult(VideoEncoderBlackBox.Objectives.subjective_quality.name());
                if(!quality.isPresent()) {
                    return Optional.empty();
                } else {
                    if(quality.get() < qualityThreshold) {
                        return Optional.empty();
                    } else {
                        return baseInterpreter.readResult(fileSizeObjective);
                    }
                }

            }
        };
    }

}
