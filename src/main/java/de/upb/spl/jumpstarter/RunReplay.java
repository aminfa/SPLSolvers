package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.drupal.DrupalBlackBox;
import de.upb.spl.benchmarks.drupal.DrupalFilteredObjectives;
import de.upb.spl.benchmarks.drupal.DrupalWeightedFaults;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.benchmarks.inline.InlineBenchmarkExecutor;
import de.upb.spl.benchmarks.inline.InlineBlackBox;
import de.upb.spl.benchmarks.x264.VideoEncoderBlackBox;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@GUI(enabled = false)
public class RunReplay extends VisualSPLReasoner{
    private final static Logger logger = LoggerFactory.getLogger(RunReplay.class);
//    @Env()
    public BookkeeperEnv videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(), System.getProperty("user.home") + "/Documents/BA/x264_1");
        return new BookkeeperEnv(new VideoEncoderBlackBox(agent()));
    }

//    @Env()
    public BookkeeperEnv setupAttributeEnvironment() {
        return new BookkeeperEnv(
                new AttributedFeatureModelEnv("src/main/resources", "video_encoder"));
    }

    @Env
    public BenchmarkEnvironment setupDrupal() {
        BenchmarkEnvironment env = new DrupalFilteredObjectives(new DrupalBlackBox());
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.Size);
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.CC);
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.Changes);
        return env;
    }

//    @Env
    public BenchmarkEnvironment setupJVMInline() {
        InlineBenchmarkExecutor executor1 = new InlineBenchmarkExecutor(agent());
        return new BookkeeperEnv(new InlineBlackBox(agent()));
    }



    @Reasoner
    public List<? extends SPLReasoner> replayALL() {
        return ReasonerReplayer.loadReplays("replays", true);
    }


    @GUI(order = -1)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

    @GUI
    public IGUIPlugin paretor() {
        return new ParetoFront(env());
    }

    @GUI(main = true)
    public IGUIPlugin timelineFaults() {
        return new ReasonerPerformanceTimeline(new DrupalWeightedFaults(env()));
    }

    public static void main(String... args) {
        new RunReplay().setup(RunReplay.class);
    }

}
