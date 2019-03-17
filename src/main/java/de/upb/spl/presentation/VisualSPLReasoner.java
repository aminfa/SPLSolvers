package de.upb.spl.presentation;

import de.upb.spl.FeatureSelection;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.ailibsintegration.FeatureSelectionEvaluatedEvent;
import de.upb.spl.ailibsintegration.FeatureSelectionPerformance;
import de.upb.spl.reasoner.SPLEvaluator;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.IAlgorithm;
import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.graphvisualizer.events.recorder.AlgorithmEventHistoryRecorder;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeoutException;

public abstract class VisualSPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(VisualSPLReasoner.class);

    private BenchmarkAgent agent;

    private BenchmarkEnvironment env;

    private AlgorithmEventHistoryRecorder eventRecorder;

    private IGUIPlugin main;
    private List<IGUIPlugin> tabs = new ArrayList<>();


    private List<SPLReasoner> reasoners = new ArrayList<>();

    public final void setup(Class<? extends VisualSPLReasoner> runnerClass) {
        Optional<Method> agentCreator = Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Agent.class))
                .findFirst();

        if(agentCreator.isPresent()) {
            try {
                BenchmarkAgent agent = (BenchmarkAgent) agentCreator.get().invoke(this);
                this.agent = agent;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            agent = new BenchmarkAgent(10000);
        }

        Optional<Method> envCreator = Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Env.class))
                .filter(m->m.getAnnotation(Env.class).enabled())
                .findFirst();
        if(envCreator.isPresent()) {
            try {
                BenchmarkEnvironment env = (BenchmarkEnvironment) envCreator.get().invoke(this);
                this.env = env;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(!this.hasEnv()) {
            logger.error("Benchmark Environment wasn't initialized!");
            System.exit(1);
        }


        new JFXPanel();

        Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(GUI.class))
                .filter(m->m.getAnnotation(GUI.class).enabled())
                .sorted(Comparator.comparingInt(m->m.getAnnotation(GUI.class).order()))
                .forEach(m-> {
                    GUI gui = m.getAnnotation(GUI.class);
                    IGUIPlugin plugin;
                    try {
                        plugin = (IGUIPlugin) m.invoke(this);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                    if(gui.main()){
                        setMain(plugin);
                    } else {
                        addTab(plugin);
                    }
                });


        this.createUI();

        Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Reasoner.class))
                .filter(m->m.getAnnotation(Reasoner.class).enabled())
                .sorted(Comparator.comparingInt(m->m.getAnnotation(Reasoner.class).order()))
                .forEach(m-> {
                    try {
                        SPLReasoner reasoner = (SPLReasoner) m.invoke(this);
                        registerReasoner(reasoner);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                });

        this.start();
    }

    protected final boolean hasEnv() {
        return env != null;
    }

	private void registerReasoner(SPLReasoner reasoner) {
        reasoners.add(reasoner);
    }

    private final void reevaluateCandidate(FeatureSelectionEvaluatedEvent event) {
        FeatureSelectionPerformance oldPerformance = event.getScore();
        FeatureSelection selection = event.getSolutionCandidate();
        double[] evaluation = SPLEvaluator.evaluateFeatureSelection(env, selection, null, true);
        FeatureSelectionPerformance reevaluatedPerformance = new FeatureSelectionPerformance(oldPerformance.violatedConstraints(), evaluation);
        event.setPerformance(reevaluatedPerformance);
    }

    public void start() {
        reasoners.stream().forEach(reasoner -> {
            SPLReasonerAlgorithm alg = reasoner.algorithm(env);
            alg.registerListener(eventRecorder);
            try {
                alg.call();
            } catch (Exception ex) {
                logger.error("Execution Error in algorithm {}: ", reasoner.name(), ex);
            }
        });
    }


    private void addTab(IGUIPlugin plugin) {
        if (plugin == null) {
            return;
        }
        tabs.add(plugin);
    }

    private void setMain(IGUIPlugin plugin) {
        if(plugin == null) {
            return;
        }
        if(main != null) {
            addTab(main);
        }
        main = plugin;
    }

    public void createUI() {
        eventRecorder = new AlgorithmEventHistoryRecorder();
        if(main == null && tabs.size() > 0) {
            main = tabs.remove(0);
        }
        if(main == null) {
            return;
        }

        final IGUIPlugin[] tabs = this.tabs.toArray(new IGUIPlugin[0]);

        Platform.runLater(() -> new AlgorithmVisualizationWindow(eventRecorder.getHistory(), main, tabs).run());
    }

    public BenchmarkAgent agent() {
        return agent;
    }

    public BenchmarkEnvironment env() {
        return env;
    }

}
