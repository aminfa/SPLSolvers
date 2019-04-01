package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.drupal.DrupalBlackBox;
import de.upb.spl.benchmarks.drupal.DrupalFilteredObjectives;
import de.upb.spl.benchmarks.drupal.DrupalModel;
import de.upb.spl.benchmarks.drupal.DrupalWeightedFaults;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.ConfiguredEnv;
import de.upb.spl.finish.Finisher;
import de.upb.spl.finish.NBestSolutions;
import de.upb.spl.finish.ReasonerSolutionContribution;
import de.upb.spl.jumpstarter.panels.ParetoFront;
import de.upb.spl.jumpstarter.panels.ReasonerPerformanceTimeline;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@GUI(enabled = false)
public class RunDrupalEval extends VisualSPLReasoner{
    private final static Logger logger = LoggerFactory.getLogger(RunDrupalEval.class);

    @Env
    public BenchmarkEnvironment setupDrupal() {
        BenchmarkEnvironment env = new DrupalBlackBox();
//        env = new DrupalFilteredObjectives(env);
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.Size);
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.CC);
//        env = new DrupalFilteredObjectives(env, DrupalModel.Objective.Changes);
//        env = new DrupalWeightedFaults(env);
        env = new ConfiguredEnv(env);
        env.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "100");
        env.configuration().setProperty("de.upb.spl.eval.solutionCount", "10");
        return env;
    }

    @Reasoner
    public List<SPLReasoner> replayALL() {
        return ReasonerReplayer.loadReplays(env(), "replays/drupal/", false);
    }

    @Finish(runOnExit = false)
    public Finisher[] evalALLObjectivesSolutions() {
        NBestSolutions solutions = new NBestSolutions(env());
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

    @Finish(runOnExit = false)
    public Finisher[] evalSizeSolutions() {
        BenchmarkEnvironment env = new DrupalFilteredObjectives(env(), DrupalModel.Objective.Size, DrupalModel.Objective.ModuleCount);
        NBestSolutions solutions = new NBestSolutions(env);
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

    @Finish(runOnExit = false)
    public Finisher[] evalWeightedFaultsSolutions() {
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

    @GUI
    public IGUIPlugin paretor() {
        return new ParetoFront(env());
    }

    @GUI(main = true)
    public IGUIPlugin timelineFaults() {
        return new ReasonerPerformanceTimeline(new DrupalWeightedFaults(env()));
    }

    public static void main(String... args) {
        new RunDrupalEval().setup(RunDrupalEval.class);
    }

}
