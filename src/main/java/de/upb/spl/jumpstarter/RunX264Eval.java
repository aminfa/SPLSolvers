package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.x264.VideoEncoderBaseInterpreter;
import de.upb.spl.finish.NBestSolutions;
import de.upb.spl.finish.ReasonerSolutionContribution;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RunX264Eval extends VisualSPLReasoner{
    private final static Logger logger = LoggerFactory.getLogger(RunX264Eval.class);

    private final double[] qualityThresholdDeltas = {0., 10., 20., 30., 35.};

    private final double[] sizeThresholds = {0.0075, 0.005, 0.0025, 0.00125, 0.001};

    private final double[] runtimeThresholds = {0.5, 1., 2., 4., 5.};

    @Env()
    public BenchmarkEnvironment videoEncodingEnv() {
        BenchmarkEnvironment env =  new VideoEncoderBaseInterpreter();
        env.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "60");
        env.configuration().setProperty("de.upb.spl.eval.solutionCount", "25");
        env.configuration().setProperty("de.upb.spl.benchmark.videoEncoding.RAWSourceFile", "stockholm");
        return env;
    }


    @Reasoner
    public List<? extends SPLReasoner> replayALL() {
        return ReasonerReplayer.loadReplays("replays/x264/", true);
    }

    @Finish(runOnExit = false)
    public List<Finish> calculateBestN() {
        List<Finish> finishers = new ArrayList<>();

        NBestSolutions solutions = new NBestSolutions(env());

        ReasonerSolutionContribution contribution =
                new ReasonerSolutionContribution(
                        bookkeeper(),
                        solutions);

        return finishers;

    }


    public static void main(String... args) {
        new RunX264Eval().setup(RunX264Eval.class);
    }

}
