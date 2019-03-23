package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.Bookkeeper;
import de.upb.spl.benchmarks.env.VideoEncoderBlackBox;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.graphvisualizer.plugin.IGUIPlugin;

public class RunReplay extends VisualSPLReasoner{

    @Env()
    public Bookkeeper videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(), System.getProperty("user.home") + "/Documents/BA/x264_1");
        return new Bookkeeper(new VideoEncoderBlackBox(agent()));
    }


    @Env()
    public Bookkeeper setupAttributeEnvironment() {
        return new Bookkeeper(
                new AttributedFeatureModelEnv("src/main/resources", "video_encoder"));
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

    @Reasoner(order = 2)
    public SPLReasoner replayHASCO() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + HASCOSPLReasoner.NAME + ".json");
        return replayer;
    }

    @GUI(order = -1)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

    public static void main(String... args) {
        new RunReplay().setup(RunReplay.class);
    }

}
