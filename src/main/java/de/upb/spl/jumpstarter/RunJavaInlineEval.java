package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.benchmarks.env.ConfiguredEnv;
import de.upb.spl.benchmarks.inline.InlineBaseInterpreter;
import de.upb.spl.finish.*;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.graphvisualizer.plugin.IGUIPlugin;

import java.util.List;

public class RunJavaInlineEval extends VisualSPLReasoner{

    @Env(parallel = false)
    public BenchmarkEnvironment videoEncodingEnv() {
        BenchmarkEnvironment env = new ConfiguredEnv(new BookkeeperEnv(new InlineBaseInterpreter()));
        env.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "100");
        env.configuration().setProperty("de.upb.spl.eval.solutionCount", "20");
        return env;
    }

    @Reasoner
    public List<? extends SPLReasoner> replayALL() {
        return ReasonerReplayer.loadReplays("replays/inline/", false);
    }

    @Finish(runOnExit = false)
    public Finisher[] evalSolutions() {
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


    @GUI(order = -1)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

    @GUI
    public IGUIPlugin paretoFront() {
        return new ParetoFront(env());
    }

    public static void main(String... args) {
        new RunJavaInlineEval().setup(RunJavaInlineEval.class);
    }
}
