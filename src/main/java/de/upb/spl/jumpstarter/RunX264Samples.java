package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.x264.VideoEncoderBlackBox;
import de.upb.spl.finish.ReasonerRecorder;
import de.upb.spl.finish.Shutdown;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.reasoner.SampleReasoner;
import de.upb.spl.sayyad.Sayyad;
import de.upb.spl.util.FileUtil;
import hasco.gui.statsplugin.HASCOModelStatisticsPlugin;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;

@GUI(enabled = true)
public class RunX264Samples extends VisualSPLReasoner{

    @Env(parallel = false)
    public BenchmarkEnvironmentDecoration videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(),  System.getProperty("user.home") + "/Documents/BA/x264_1");
        BenchmarkEnvironmentDecoration env =  new VideoEncoderBlackBox(agent());
        env.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "100");
        return env;
    }

    @Reasoner
    public SPLReasoner samples() {
        SampleReasoner reasoner = new SampleReasoner(
                FileUtil.getPathOfResource("x264/samples/random-sat.json")
        );
        return reasoner;
    }

    @Finish
    public ReasonerRecorder record() {
        return new ReasonerRecorder((BenchmarkEnvironmentDecoration) env());
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
        new RunX264Samples().setup(RunX264Samples.class);
    }
}
