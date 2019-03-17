package de.upb.spl.presentation;

import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.VideoEncoderEnv;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
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
    public VideoEncoderEnv videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent(), "/Users/aminfaez/Documents/BA/x264_1");
        return new VideoEncoderEnv(agent());
    }



    @Env()
    public AttributedFeatureModelEnv setupAttributeEnvironment() {
        return new AttributedFeatureModelEnv("src/main/resources", "random_1000");
    }

    @Reasoner(order = 1)
    public SPLReasoner guo() {
        Guo11 guo11 = new Guo11();
        return guo11;
    }

    @Reasoner(order = 1, enabled = false)
    public SPLReasoner ibea() throws ExecutionException, InterruptedException {
        BasicIbea basicIbea = new BasicIbea();
        return basicIbea;
    }

    @Reasoner(order = 2)
    public SPLReasoner sayyad() {
        Sayyad sayyad = new Sayyad();
        return sayyad;
    }

    @Reasoner(order = 3)
    public SPLReasoner henard()  {
        Henard henard = new Henard();
        return henard;
    }

    @Reasoner(order = 4)
    public SPLReasoner hierons() {
        Hierons hierons = new Hierons();
        return hierons;
    }

    @Reasoner(order = 0)
    public SPLReasoner hasco() {
        HASCOSPLReasoner hasco = new HASCOSPLReasoner();
        return hasco;
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

    @GUI(enabled = false)
    public IGUIPlugin hascoStatics() {
        return new HASCOModelStatisticsPlugin();
    }

    public static void main(String... args) {
        new RunAll().setup(RunAll.class);
    }
}
