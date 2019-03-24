package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.Bookkeeper;
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
import de.upb.spl.sayyad.Sayyad;
import hasco.gui.statsplugin.HASCOModelStatisticsPlugin;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;

import java.util.concurrent.ExecutionException;

public class RunX264 extends VisualSPLReasoner{

    @Env()
    public Bookkeeper videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(),  System.getProperty("user.home") + "/Documents/BA/x264_1");
        return new Bookkeeper(new VideoEncoderBlackBox(agent()));
    }

    @Reasoner(order = 1)
    public SPLReasoner guo() {
        Guo11 guo11 = new Guo11();
        return guo11;
    }

    @Finish
    public ReasonerRecorder recordGUO() {
        return new ReasonerRecorder(env(), Guo11.NAME, "recordings/" + Guo11.NAME + ".json");
    }

//    @Reasoner(order = 1, enabled = true)
//    public SPLReasoner ibea() throws ExecutionException, InterruptedException {
//        BasicIbea basicIbea = new BasicIbea();
//        return basicIbea;
//    }
//
//    @Finish
//    public ReasonerRecorder recordBaiscIbea() {
//        return new ReasonerRecorder(env(), BasicIbea.NAME, "recordings/" + BasicIbea.NAME + ".json");
//    }


    @Reasoner(order = 2)
    public SPLReasoner sayyad() {
        Sayyad sayyad = new Sayyad();
        return sayyad;
    }

    @Finish
    public ReasonerRecorder recordSayyad() {
        return new ReasonerRecorder(env(), Sayyad.NAME, "recordings/" + Sayyad.NAME + ".json");
    }


    @Reasoner(order = 3)
    public SPLReasoner henard()  {
        Henard henard = new Henard();
        return henard;
    }

    @Finish
    public ReasonerRecorder recordHenard() {
        return new ReasonerRecorder(env(), Henard.NAME, "recordings/" + Henard.NAME + ".json");
    }


    @Reasoner(order = 4)
    public SPLReasoner hierons() {
        Hierons hierons = new Hierons();
        return hierons;
    }

    @Finish
    public ReasonerRecorder recordHierons() {
        return new ReasonerRecorder(env(), Hierons.NAME, "recordings/" + Hierons.NAME + ".json");
    }

    @Reasoner(order = 0)
    public SPLReasoner hasco() {
        HASCOSPLReasoner hasco = new HASCOSPLReasoner();
        return hasco;
    }

    @Finish
    public ReasonerRecorder recordHasco() {
        return new ReasonerRecorder(env(), HASCOSPLReasoner.NAME, "recordings/" + HASCOSPLReasoner.NAME + ".json");
    }

    @Finish(order = 1000, enabled = false)
    public Shutdown shutdownHook() {
        return new Shutdown();
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
        new RunX264().setup(RunX264.class);
    }
}
