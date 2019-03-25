package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.Bookkeeper;
import de.upb.spl.benchmarks.x264.VideoEncoderBlackBox;
import de.upb.spl.benchmarks.x264.VideoEncoderCustomer1;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.sayyad.Sayyad;
import jaicore.graphvisualizer.plugin.IGUIPlugin;

public class RunReplay extends VisualSPLReasoner{

    @Env()
    public Bookkeeper videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(), System.getProperty("user.home") + "/Documents/BA/x264_1");
        return new Bookkeeper(new VideoEncoderBlackBox(agent()));
    }


//    @Env()
    public Bookkeeper setupAttributeEnvironment() {
        return new Bookkeeper(
                new AttributedFeatureModelEnv("src/main/resources", "video_encoder"));
    }

    //    @Reasoner(order = 0)
    public SPLReasoner replayIBEA() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + BasicIbea.NAME + ".json");
        return replayer;
    }

    @Reasoner(order = 0)
    public SPLReasoner replayGUO() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Guo11.NAME + ".json");
        return replayer;
    }

    @Reasoner(order = 1)
    public SPLReasoner replaySayyad() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Sayyad.NAME + ".json");
        return replayer;
    }
    @Reasoner(order = 1)
    public SPLReasoner replayHernard() {
        ReasonerReplayer replayer = new ReasonerReplayer("replays/" + Henard.NAME + ".json");
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

    @GUI
    public IGUIPlugin paretor() {
        return new ParetoFront(env());
    }

    @GUI
    public IGUIPlugin timelineCustomer1() {
        return new ReasonerPerformanceTimeline(new VideoEncoderCustomer1(env()));
    }

    public static void main(String... args) {
        new RunReplay().setup(RunReplay.class);
    }

}
