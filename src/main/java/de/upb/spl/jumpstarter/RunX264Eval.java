package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.x264.VideoEncoderBaseInterpreter;
import de.upb.spl.benchmarks.x264.VideoEncoderCustomer1;
import de.upb.spl.benchmarks.x264.VideoEncoderCustomer2;
import de.upb.spl.finish.Finisher;
import de.upb.spl.finish.NBestSolutions;
import de.upb.spl.finish.ReasonerSolutionContribution;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@GUI(enabled = false)
public class RunX264Eval extends VisualSPLReasoner{
    private final static Logger logger = LoggerFactory.getLogger(RunX264Eval.class);



    @Env()
    public BenchmarkEnvironment videoEncodingEnv() {
        BenchmarkEnvironment env =  new VideoEncoderBaseInterpreter();
        env.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "60");
        env.configuration().setProperty("de.upb.spl.eval.solutionCount", "10");
        env.configuration().setProperty("de.upb.spl.benchmark.videoEncoding.RAWSourceFile", "ducks_take_off");
        return env;
    }


    @Reasoner
    public List<SPLReasoner> replayALL() {
        return ReasonerReplayer.loadReplays(env(), "replays/x264/", true);
    }

    @Finish(runOnExit = false)
    public Finisher[] calculateBestN() {
        NBestSolutions solutions = new NBestSolutions(env());
        ReasonerSolutionContribution contribution =
                new ReasonerSolutionContribution(
                        bookkeeper(),
                        solutions);
        Finisher[] finishers = {
                solutions,
                contribution
        };
        return finishers;
    }

    @Finish(runOnExit = false)
    public Finisher[] calculateBestNC1() {
        NBestSolutions solutions = new NBestSolutions(new VideoEncoderCustomer1(env()));
        ReasonerSolutionContribution contribution =
                new ReasonerSolutionContribution(
                        bookkeeper(),
                        solutions);
        Finisher[] finishers = {
                solutions,
                contribution
        };
        return finishers;
    }

    @Finish(runOnExit = false)
    public Finisher[] calculateBestNC2() {
        NBestSolutions solutions = new NBestSolutions(new VideoEncoderCustomer2(env()));
        ReasonerSolutionContribution contribution =
                new ReasonerSolutionContribution(
                        bookkeeper(),
                        solutions);
        Finisher[] finishers = {
                solutions,
                contribution
        };
        return finishers;
    }


    @GUI(order = -1)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

    @GUI
    public IGUIPlugin paretor() {
        return new ParetoFront(env());
    }

    @GUI
    public IGUIPlugin timelineCustomer1() {
        return new ReasonerPerformanceTimeline(new VideoEncoderCustomer1(env()));
    }
    @GUI
    public IGUIPlugin timelineCustomer2() {
        return new ReasonerPerformanceTimeline(new VideoEncoderCustomer2(env()));
    }

    public static void main(String... args) {
        new RunX264Eval().setup(RunX264Eval.class);
    }

}
