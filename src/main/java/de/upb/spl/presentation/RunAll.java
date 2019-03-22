package de.upb.spl.presentation;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.Bookkeeper;
import de.upb.spl.benchmarks.env.VideoEncoderEnv;
import de.upb.spl.eval.ReasonerRecorder;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.presentation.panels.ParetoFront;
import de.upb.spl.presentation.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.sayyad.Sayyad;
import hasco.gui.statsplugin.HASCOModelStatisticsPlugin;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;

import java.util.concurrent.ExecutionException;

public class RunAll extends VisualSPLReasoner{

//    @Env()
    public Bookkeeper videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(),  System.getProperty("user.home") + "/Documents/BA/x264_1");
        return new Bookkeeper(new VideoEncoderEnv(agent()));
    }


    @Env()
    public Bookkeeper setupAttributeEnvironment() {
        return new Bookkeeper(
                new AttributedFeatureModelEnv("src/main/resources", "video_encoder"));
    }

    @Reasoner(order = 1)
    public SPLReasoner guo() {
        Guo11 guo11 = new Guo11();
        return guo11;
    }

    @Collect
    public ReasonerRecorder recordGUO() {
        return new ReasonerRecorder(env(), Guo11.NAME, "recordings/" + Guo11.NAME + ".json");
    }

    @Reasoner(order = 1, enabled = true)
    public SPLReasoner ibea() throws ExecutionException, InterruptedException {
        BasicIbea basicIbea = new BasicIbea();
        return basicIbea;
    }

    @Collect
    public ReasonerRecorder recordBaiscIbea() {
        return new ReasonerRecorder(env(), BasicIbea.NAME, "recordings/" + BasicIbea.NAME + ".json");
    }


    @Reasoner(order = 2)
    public SPLReasoner sayyad() {
        Sayyad sayyad = new Sayyad();
        return sayyad;
    }

    @Collect
    public ReasonerRecorder recordSayyad() {
        return new ReasonerRecorder(env(), Sayyad.NAME, "recordings/" + Sayyad.NAME + ".json");
    }


    @Reasoner(order = 3)
    public SPLReasoner henard()  {
        Henard henard = new Henard();
        return henard;
    }

    @Collect
    public ReasonerRecorder recordHenard() {
        return new ReasonerRecorder(env(), Henard.NAME, "recordings/" + Henard.NAME + ".json");
    }


    @Reasoner(order = 4)
    public SPLReasoner hierons() {
        Hierons hierons = new Hierons();
        return hierons;
    }

    @Collect
    public ReasonerRecorder recordHierons() {
        return new ReasonerRecorder(env(), Hierons.NAME, "recordings/" + Hierons.NAME + ".json");
    }

    @Reasoner(order = 0)
    public SPLReasoner hasco() {
        HASCOSPLReasoner hasco = new HASCOSPLReasoner();
        return hasco;
    }

    @Collect
    public ReasonerRecorder recordHasco() {
        return new ReasonerRecorder(env(), HASCOSPLReasoner.NAME, "recordings/" + HASCOSPLReasoner.NAME + ".json");
    }

    @GUI(order = -1)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

    @GUI(main = true)
    public IGUIPlugin graph() {
        return new GraphViewPlugin();
    }

    @GUI
    public IGUIPlugin nodeInfo() {
        return new NodeInfoGUIPlugin<>(new JaicoreNodeInfoGenerator<>(new TFDNodeInfoGenerator()));
    }

    @GUI
    public IGUIPlugin paretoFront() {
        return new ParetoFront(env());
    }

    @GUI(enabled = false)
    public IGUIPlugin hascoStatics() {
        return new HASCOModelStatisticsPlugin();
    }

    public static void main(String... args) {
        new RunAll().setup(RunAll.class);
    }
}
