package de.upb.spl;

import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.VideoEncoderExecutor;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.VideoEncoderEnv;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.FeatureSelectionEvaluatedEvent;
import de.upb.spl.hasco.FeatureSelectionPerformance;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.presentation.GUIPluginEvalTimeline;
import de.upb.spl.presentation.ParetoPresentation;
import de.upb.spl.reasoner.EAReasoner;
import de.upb.spl.reasoner.SPLEvaluator;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.sayyad.Sayyad;
import jaicore.basic.algorithm.IAlgorithm;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.graphvisualizer.events.recorder.AlgorithmEventHistoryRecorder;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;

public class VisualSPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(VisualSPLReasoner.class);

    private BenchmarkAgent agent = new BenchmarkAgent(10000);

    private BenchmarkEnvironment env;

    private AlgorithmEventHistoryRecorder eventRecorder;

    private List<GUIPluginEvalTimeline> plugins = new ArrayList<>();

    private List<SPLReasoner> reasoners = new ArrayList<>();

    final static String spl = "random_1000";

    public VisualSPLReasoner() {
    }

    public void createUI() {
        eventRecorder = new AlgorithmEventHistoryRecorder();

        new JFXPanel();
        Platform.runLater(()-> {

            for (int i = 0; i < env.objectives().size(); i++) {
                GUIPluginEvalTimeline evalTimeline = new GUIPluginEvalTimeline(env, i);
                plugins.add(evalTimeline);
            }
            IGUIPlugin main = plugins.get(0);
            IGUIPlugin tabs[] = new IGUIPlugin[0];
            if(plugins.size() > 1) {
                List<IGUIPlugin> tabPluginList = new ArrayList<>(plugins);
                tabPluginList.remove(0);
                tabs = tabPluginList.toArray(tabs);
            }
            new AlgorithmVisualizationWindow(eventRecorder.getHistory(), main, tabs).run();
        });
    }

    public boolean hasEnv() {
        return env != null;
    }

    protected void setEnv(BenchmarkEnvironment env) {
        this.env = env;
    }

	public void registerReasoner(SPLReasoner reasoner) {
        reasoners.add(reasoner);
    }

    public void reevaluateCandidate(FeatureSelectionEvaluatedEvent event) {
        FeatureSelectionPerformance oldPerformance = event.getScore();
        FeatureSelection selection = event.getSolutionCandidate();
        double[] evaluation = SPLEvaluator.evaluateFeatureSelection(env, selection, null, true);
        FeatureSelectionPerformance reevaluatedPerformance = new FeatureSelectionPerformance(oldPerformance.violatedConstraints(), evaluation);
        event.setPerformance(reevaluatedPerformance);
    }

    public void start() {
        for(SPLReasoner reasoner : reasoners) {
            IAlgorithm<BenchmarkEnvironment, FeatureSelection> alg = reasoner.algorithm(env);

            for (AlgorithmEvent algEvent : alg) {
                if(algEvent instanceof FeatureSelectionEvaluatedEvent) {
                    FeatureSelectionEvaluatedEvent event = (FeatureSelectionEvaluatedEvent) algEvent;
                    reevaluateCandidate(event);
                    eventRecorder.handleAlgorithmEvent(event);
                }
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface SPLReasonerCreator {
        boolean enabled();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface SPLEnvironmentCreator {
        boolean enabled();
    }

    @SPLEnvironmentCreator(enabled = true)
    public void videoEncodingEnv() {
        VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent, "/Users/aminfaez/Documents/BA/x264_1");
        setEnv(new VideoEncoderEnv(agent));
    }

    @SPLEnvironmentCreator(enabled = false)
    public void setupAttributeEnvironment() {
        setEnv(new AttributedFeatureModelEnv("src/main/resources", spl));
    }

    @SPLReasonerCreator(enabled = false)
	public void registerRunGUO() {
		Guo11 guo11 = new Guo11();
        registerReasoner(guo11);
	}

    @SPLReasonerCreator(enabled = false)
    public void registerBasicIbea() throws ExecutionException, InterruptedException {
        BasicIbea basicIbea = new BasicIbea();
        registerReasoner(basicIbea);
	}

    @SPLReasonerCreator(enabled = true)
    public void registerSayyad() {
        Sayyad sayyad = new Sayyad();
        registerReasoner(sayyad);
    }

    @SPLReasonerCreator(enabled = true)
	public void registerRunHenard()  {
        Henard henard = new Henard();
        registerReasoner(henard);
	}

	@SPLReasonerCreator(enabled = true)
    public void registerRunHierons() {
        Hierons hierons = new Hierons();
        registerReasoner(hierons);
    }

    public static void main(String... args) {
        VisualSPLReasoner visual = new VisualSPLReasoner();

        Arrays.stream(VisualSPLReasoner.class.getMethods())
                .filter(m -> m.isAnnotationPresent(SPLEnvironmentCreator.class))
                .filter(m->m.getAnnotation(SPLEnvironmentCreator.class).enabled())
                .forEach(m-> {
                    try {
                        m.invoke(visual);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

        if(!visual.hasEnv()) {
            logger.error("Benchmark Environment wasn't initialized!");
            System.exit(1);
        }

        visual.createUI();

        Arrays.stream(VisualSPLReasoner.class.getMethods())
            .filter(m -> m.isAnnotationPresent(SPLReasonerCreator.class))
            .filter(m->m.getAnnotation(SPLReasonerCreator.class).enabled())
            .forEach(m-> {
                try {
                     m.invoke(visual);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        visual.start();
    }


}
