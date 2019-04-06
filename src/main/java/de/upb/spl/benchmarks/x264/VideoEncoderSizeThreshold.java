package de.upb.spl.benchmarks.x264;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;

import java.util.*;

public class VideoEncoderSizeThreshold extends BenchmarkEnvironmentDecoration {


    String qualityObjective = VideoEncoderBlackBox.Objectives.subjective_quality.name();
    List<String> objectives = (List<String>) Collections.singletonList(qualityObjective);

    public static final Map<String, Double> RAW_VIDEO_SIZE = new HashMap<>();

    static {
        RAW_VIDEO_SIZE.put("flower_garden", 71284.);
        RAW_VIDEO_SIZE.put("grandma", 32304.);
        RAW_VIDEO_SIZE.put("stockholm", 648008.);
        RAW_VIDEO_SIZE.put("touchdown_pass", 911256.);
        RAW_VIDEO_SIZE.put("ducks_take_off", 675008.);
    }
    private final double rawSize;
    private double compressionThreshold = 0.005;

    public VideoEncoderSizeThreshold(BenchmarkEnvironment env) {
        super(env);
        rawSize = RAW_VIDEO_SIZE.getOrDefault(env.configuration().getVideoSourceFile(), Double.MAX_VALUE);
        assert env.objectives().contains(VideoEncoderBlackBox.Objectives.file_size.name());
        assert env.objectives().contains(VideoEncoderBlackBox.Objectives.subjective_quality.name());
    }

    public VideoEncoderSizeThreshold(BenchmarkEnvironment env, double compressionThreshold) {
        this(env);
        this.compressionThreshold = compressionThreshold;
    }

    public List<String> objectives() {
        return objectives;
    }

    public ReportInterpreter interpreter(JobReport report) {
        ReportInterpreter baseInterpreter = super.interpreter(report);
        return new ReportInterpreter() {
            @Override
            public Optional<Double> readResult(String objective) {
                Optional<Double> fileSize = baseInterpreter.rawResult(VideoEncoderBlackBox.Objectives.file_size.name());
                if(!fileSize.isPresent()) {
                    return Optional.empty();
                } else {
                    double compressionRatio = fileSize.get()/rawSize;
                    if(compressionRatio > compressionThreshold) {
                        return Optional.empty();
                    } else {
                        return baseInterpreter.readResult(qualityObjective);
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
