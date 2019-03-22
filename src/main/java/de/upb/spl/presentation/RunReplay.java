package de.upb.spl.presentation;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.*;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.presentation.panels.ParetoFront;
import de.upb.spl.presentation.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.sayyad.Sayyad;
import jaicore.graphvisualizer.plugin.IGUIPlugin;

public class RunReplay extends VisualSPLReasoner{

    @Env()
    public BenchmarkEnvironment setupVideoEncoderEnv() {
        return new Bookkeeper(new VideoEncoderEnv(agent()));
    }


    @Reasoner(order = 0)
    public SPLReasoner replayGUO() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Guo11.NAME + ".json");
        return replayer;
    }

    @Reasoner(order = 1)
    public SPLReasoner replayHierons() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Hierons.NAME + ".json");
        return replayer;
    }

    @Reasoner(order = -1)
    public SPLReasoner replayHASCO() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + HASCOSPLReasoner.NAME + ".json");
        return replayer;
    }

    @Reasoner(order = 4)
    public SPLReasoner replaySayyad() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Sayyad.NAME + ".json");
        return replayer;
    }

    @Reasoner(order = 5)
    public SPLReasoner replayHenard() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Henard.NAME + ".json");
        return replayer;
    }

    @GUI(main = true)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

//    @GUI(order = -2)
    public IGUIPlugin timelineCustomer1() {
        BenchmarkEnvironment env = new VideoEncoderCustomer1(env());
        return new ReasonerPerformanceTimeline(env);
    }

//    @GUI(main = true)
    public IGUIPlugin front() {
        return new ParetoFront(env());
    }

//    @GUI(order = -2)
    public IGUIPlugin frontCustomer1() {
        BenchmarkEnvironment env = new VideoEncoderCustomer1(env());
        return new ParetoFront(env);
    }

    public static void main(String... args) {
        new RunReplay().setup(RunReplay.class);
    }

}
