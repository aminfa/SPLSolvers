package de.upb.spl.jumpstarter.randoms;

import de.upb.spl.benchmarks.env.*;
import de.upb.spl.finish.ReasonerRecorder;
import de.upb.spl.jumpstarter.*;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.reasoner.SampleReasoner;
import de.upb.spl.util.FileUtil;
import jaicore.graphvisualizer.plugin.IGUIPlugin;

import java.io.File;
import java.util.Objects;

@GUI(enabled = true)
public class SyntheticEnv extends VisualSPLReasoner{


    public final static String
            SPL_NAME = "random100",
            FM_PATH = FileUtil.getPathOfResource(SPL_NAME + File.separator + "feature-model.xml"),
            ATTR_DIR_PATH = Objects.requireNonNull(new File(FM_PATH).getParent(), "Syntetic feature model not found.");


    @Env(parallel = false)
    public BenchmarkEnvironment createEnv() {

        BenchmarkEnvironment env = new BaseEnv();

        // decorate with a configuration:
        env = new ConfiguredEnv(env);
        env.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "1000");
        env.configuration().setProperty("de.upb.spl.benchmark.synthetic.independentAttr", "bayes");

        // add a book keeper:
        env = new BookkeeperEnv(env);

        // decorate with the random1000.xml feature model:
        env = new FMXML(env, FM_PATH);

        // load attributes:
        env = new FMAttributes(env,
                ATTR_DIR_PATH + File.separator + env.configuration().getSyntheticBenchmarkAttributeType(),
                SPL_NAME);
        env = new AttributedFeatureModelEnv((FMAttributes) env);

        return env;
    }


    @Finish
    public ReasonerRecorder record() {
        return new ReasonerRecorder(env());
    }

    @GUI(order = -1)
    public IGUIPlugin timeline() {
        return new ReasonerPerformanceTimeline(env());
    }

    @GUI
    public IGUIPlugin paretoFront() {
        return new ParetoFront(env());
    }
}
