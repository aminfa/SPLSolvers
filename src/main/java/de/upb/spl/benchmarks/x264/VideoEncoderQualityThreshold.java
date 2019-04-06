package de.upb.spl.benchmarks.x264;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;

import java.util.*;

public class VideoEncoderQualityThreshold extends BenchmarkEnvironmentDecoration {


    public final static String fileSizeObjective = VideoEncoderBlackBox.Objectives.file_size.name();
    public final static String runtimeObjective = VideoEncoderBlackBox.Objectives.run_time.name();

    public List<String> objectives = Arrays.asList(fileSizeObjective, runtimeObjective);

    public static final Map<String, Double> VIDEO_QUALITY_THRESHOLD = new HashMap<>();

    static {
        VIDEO_QUALITY_THRESHOLD.put("grandma", 90.);
        VIDEO_QUALITY_THRESHOLD.put("stockholm", 60.);
        VIDEO_QUALITY_THRESHOLD.put("touchdown_pass", 55.);
        VIDEO_QUALITY_THRESHOLD.put("ducks_take_off", 50.);
    }

    private final double qualityThreshold;

    public VideoEncoderQualityThreshold(BenchmarkEnvironment env) {
        this(env, 0.);
    }

    public VideoEncoderQualityThreshold(BenchmarkEnvironment env, double thresholdDelta) {
        super(env);
        qualityThreshold = VIDEO_QUALITY_THRESHOLD.getOrDefault(env.configuration().getVideoSourceFile(), 90.) + thresholdDelta;
        objectives.retainAll(env.objectives());
        if(objectives.isEmpty()) {
            throw new RuntimeException("Resulting objectives is empty. Inner objectives: " + env.objectives());
        }
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

            @Override
            public String group() {
                return VideoEncoderBlackBox.GROUP;
            }
        };
    }

}
