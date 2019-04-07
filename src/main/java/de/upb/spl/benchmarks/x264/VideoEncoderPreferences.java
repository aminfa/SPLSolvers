package de.upb.spl.benchmarks.x264;

import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.env.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;

import java.util.*;
import java.util.function.Function;

public class VideoEncoderPreferences {

    /**
     * Video length in seconds
     */
    public static final Map<String, Double> VIDEO_LENGTH;
    static {
        Map<String, Double> lengthEntries = new HashMap<>();
        lengthEntries.put("flower_garden", 12.);
        lengthEntries.put("grandma", 29.03);
        lengthEntries.put("stockholm", 4.);
        lengthEntries.put("touchdown_pass", 5.);
        lengthEntries.put("ducks_take_off", 10.);
        VIDEO_LENGTH = Collections.unmodifiableMap(lengthEntries);
    }

    /**
     * Base video quality runtimeThreshold for measurements by vmaf.
     */
    public static final Map<String, Double> VIDEO_QUALITY_THRESHOLD;
    static {
        Map<String, Double> qualityEntries = new HashMap<>();
        qualityEntries.put("flower_garden", 95.);
        qualityEntries.put("grandma", 100.);
        qualityEntries.put("stockholm", 95.);
        qualityEntries.put("touchdown_pass", 90.);
        qualityEntries.put("ducks_take_off", 75.);
        VIDEO_QUALITY_THRESHOLD =  Collections.unmodifiableMap(qualityEntries);
    }

    /**
     * Raw video file size in kBytes
     */
    public static final Map<String, Double> RAW_VIDEO_SIZE;
    static {
        Map<String, Double> sizeEntries = new HashMap<>();

        sizeEntries.put("flower_garden", 71284.);
        sizeEntries.put("grandma", 32304.);
        sizeEntries.put("stockholm", 648008.);
        sizeEntries.put("touchdown_pass", 911256.);
        sizeEntries.put("ducks_take_off", 675008.);

        RAW_VIDEO_SIZE = Collections.unmodifiableMap(sizeEntries);
    }

    private static abstract class VideoEncoderPreferenceInterpreter implements ReportInterpreter {

        protected final ReportInterpreter innerInterpreter;

        protected final boolean violatedConstraints;

        protected final String constrainedVariable;

        VideoEncoderPreferenceInterpreter(ReportInterpreter interpreter, String constrainedVariable, Function<Double, Boolean> violationArbiter) {
            this.innerInterpreter = interpreter;
            this.constrainedVariable = constrainedVariable;
            this.violatedConstraints = innerInterpreter
                    .rawResult(constrainedVariable)
                    .map(violationArbiter)
                    .orElse(true);
        }

        @Override
        public Optional<Double> readResult(String objective) {
            if(violatedConstraints) {
                return innerInterpreter.readResult(objective).map(result -> result + 1000000. + penalty());
            } else {
                return innerInterpreter.readResult(objective);
            }
        }

        @Override
        public boolean violatedConstraints() {
            return violatedConstraints || innerInterpreter.violatedConstraints();
        }

        abstract double penalty();

        @Override
        public Optional<Double> rawResult(String objective) {
            return innerInterpreter.rawResult(objective);
        }

    }

    public static class RuntimeThreshold extends BenchmarkEnvironmentDecoration {

        final List<String> objectives;

        final double runtimeThreshold;

        final static String RUNTIME_OBJECTIVE = VideoEncoderBlackBox.Objectives.run_time.name();

        public RuntimeThreshold(BenchmarkEnvironment env, double threshold) {
            super(env);
            objectives = new ArrayList<>(env.objectives());
            boolean removed = objectives.remove(RUNTIME_OBJECTIVE);
            if(!removed) {
                throw new IllegalArgumentException("Inner objectives doesn't include runtime: " + env.objectives());
            }
            if(objectives.isEmpty())
                throw new IllegalArgumentException("Objectives is empty. inner objectives: " + env.objectives());
            if(!VIDEO_LENGTH.containsKey(env.configuration().getVideoSourceFile())) {
                throw new IllegalArgumentException("Video name not recognized: " + env.configuration().getVideoSourceFile());
            }
            double videoLength = VIDEO_LENGTH.get(env.configuration().getVideoSourceFile());
            this.runtimeThreshold = threshold * videoLength;
        }

        @Override
        public ReportInterpreter interpreter(JobReport jobReport) {
            return new Interpreter(jobReport);
        }

        public String toString() {
            return "Runtime < " + Math.round(runtimeThreshold) + " | " + getBaseEnv().toString();
        }

        class Interpreter extends VideoEncoderPreferenceInterpreter {

            double penalty = 0.;

            Interpreter(JobReport jobReport) {
                super(getBaseEnv().interpreter(jobReport),
                        RUNTIME_OBJECTIVE,
                        runtime ->runtime > runtimeThreshold);

                if(violatedConstraints) {
                    penalty = innerInterpreter.rawResult(RUNTIME_OBJECTIVE).orElse(100.);
                }

            }

            @Override
            double penalty() {
                return penalty;
            }

        }

        public List<String> objectives() {
            return objectives;
        }
    }



    public static class SizeThreshold extends BenchmarkEnvironmentDecoration {

        final List<String> objectives;

        final double sizeThreshold;

        final static String SIZE_OBJECTIVE = VideoEncoderBlackBox.Objectives.file_size.name();

        public SizeThreshold(BenchmarkEnvironment env, double threshold) {
            super(env);
            objectives = new ArrayList<>(env.objectives());
            boolean removed = objectives.remove(SIZE_OBJECTIVE);
            if(!removed) {
                throw new IllegalArgumentException("Inner objectives doesn't include size: " + env.objectives());
            }
            if(objectives.isEmpty())
                throw new IllegalArgumentException("Objectives is empty. inner objectives: " + env.objectives());

            if(!RAW_VIDEO_SIZE.containsKey(env.configuration().getVideoSourceFile())) {
                throw new IllegalArgumentException("Video name not recognized: " + env.configuration().getVideoSourceFile());
            }
            double rawSize = RAW_VIDEO_SIZE.get(env.configuration().getVideoSourceFile());
            this.sizeThreshold = threshold * rawSize;
        }

        public String toString() {
            return "Size < " + Math.round(sizeThreshold) + " | " + getBaseEnv().toString();
        }

        @Override
        public ReportInterpreter interpreter(JobReport jobReport) {
            return new Interpreter(jobReport);
        }

        class Interpreter extends VideoEncoderPreferenceInterpreter {

            double penalty = 0.;

            Interpreter(JobReport jobReport) {
                super(getBaseEnv().interpreter(jobReport),
                        SIZE_OBJECTIVE,
                        size -> size > sizeThreshold);

                if(violatedConstraints) {
                    penalty = innerInterpreter.rawResult(SIZE_OBJECTIVE).orElse(10000.);
                }

            }

            @Override
            double penalty() {
                return penalty;
            }

        }

        public List<String> objectives() {
            return objectives;
        }
    }



    public static class QualityThreshold extends BenchmarkEnvironmentDecoration {

        final List<String> objectives;

        final double qualityThreshold;

        final static String QUALITY_OBJECTIVE = VideoEncoderBlackBox.Objectives.subjective_quality.name();

        public QualityThreshold(BenchmarkEnvironment env, double thresholdDelta) {
            super(env);
            objectives = new ArrayList<>(env.objectives());
            boolean removed = objectives.remove(QUALITY_OBJECTIVE);
            if(!removed) {
                throw new IllegalArgumentException("Inner objectives doesn't include quality: " + env.objectives());
            }
            if(objectives.isEmpty())
                throw new IllegalArgumentException("Objectives is empty. inner objectives: " + env.objectives());

            if(!VIDEO_QUALITY_THRESHOLD.containsKey(env.configuration().getVideoSourceFile())) {
                throw new IllegalArgumentException("Video name not recognized: " + env.configuration().getVideoSourceFile());
            }
            double baseQuality = VIDEO_QUALITY_THRESHOLD.get(env.configuration().getVideoSourceFile());
            this.qualityThreshold = baseQuality + thresholdDelta;
        }


        public String toString() {
            return "Quality > " + qualityThreshold + " | " + getBaseEnv().toString();
        }


        @Override
        public ReportInterpreter interpreter(JobReport jobReport) {
            return new Interpreter(jobReport);
        }

        class Interpreter extends VideoEncoderPreferenceInterpreter {

            double penalty = 0.;
            Interpreter(JobReport jobReport) {
                super(getBaseEnv().interpreter(jobReport),
                        QUALITY_OBJECTIVE,
                        quality -> quality < qualityThreshold);
                if(violatedConstraints) {
                    penalty = innerInterpreter.rawResult(QUALITY_OBJECTIVE).map(quality -> -quality).orElse(100.);
                }
            }

            @Override
            double penalty() {
                return penalty;
            }
        }




        public List<String> objectives() {
            return objectives;
        }
    }
}
