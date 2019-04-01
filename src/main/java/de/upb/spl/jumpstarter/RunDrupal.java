package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.drupal.DrupalBlackBox;
import de.upb.spl.benchmarks.drupal.DrupalFilteredObjectives;
import de.upb.spl.benchmarks.drupal.DrupalModel;
import de.upb.spl.benchmarks.drupal.DrupalWeightedFaults;
import de.upb.spl.benchmarks.env.*;
import de.upb.spl.finish.Finisher;
import de.upb.spl.finish.NBestSolutions;
import de.upb.spl.finish.ReasonerRecorder;
import de.upb.spl.finish.ReasonerSolutionContribution;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.sayyad.Sayyad;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import jaicore.planning.hierarchical.algorithms.forwarddecomposition.graphgenerators.tfd.TFDNodeInfoGenerator;
import jaicore.search.model.travesaltree.JaicoreNodeInfoGenerator;

import java.util.concurrent.ExecutionException;

@GUI(enabled = false)
public class RunDrupal extends VisualSPLReasoner{


    @Env(parallel = false)
    public BenchmarkEnvironment setupAttributeEnvironment() {
        BenchmarkEnvironment env = new DrupalFilteredObjectives(new DrupalBlackBox());
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.Size);
        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.CC);
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.Changes);
//        env = new DrupalWeightedFaults(env);
        return env;
    }

    @Reasoner(order = 1)
    public SPLReasoner guo() {
        Guo11 guo11 = new Guo11();
        return guo11;
    }


//    @Reasoner(order = 1, enabled = true)
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

    @Reasoner(order = -1)
    public SPLReasoner hasco() {
        HASCOSPLReasoner hasco = new HASCOSPLReasoner();
        return hasco;
    }


    @Finish(runOnExit = false)
    public ReasonerRecorder record() {
        return new ReasonerRecorder((BenchmarkEnvironmentDecoration) env());
    }

    @Finish(runOnExit = false)
    public Finisher[] calculateBestN() {
        NBestSolutions solutions = new NBestSolutions(new DrupalWeightedFaults(env()));
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

    @GUI(main = true)
    public IGUIPlugin graph() {
        return new GraphViewPlugin();
    }

    @GUI
    public IGUIPlugin nodeInfo() {
        return new NodeInfoGUIPlugin<>(new JaicoreNodeInfoGenerator<>(new TFDNodeInfoGenerator()));
    }

//    @GUI
    public IGUIPlugin paretoFront() {
        return new ParetoFront(env());
    }


    public static void main(String... args) {
        new RunDrupal().setup(RunDrupal.class);
    }
}
