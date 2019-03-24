package de.upb.spl.jumpstarter;

import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.Bookkeeper;
import de.upb.spl.reasoner.SPLReasoner;
import jaicore.graphvisualizer.events.recorder.AlgorithmEventHistoryRecorder;
import jaicore.graphvisualizer.plugin.IGUIPlugin;
import jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VisualSPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(VisualSPLReasoner.class);

    private BenchmarkAgent agent;

    private BenchmarkEnvironment env;

    private AlgorithmEventHistoryRecorder eventRecorder;

    private IGUIPlugin main;
    private List<IGUIPlugin> tabs = new ArrayList<>();


    private List<SPLReasoner> reasoners = new ArrayList<>();

    private List<Runnable> finishers = new ArrayList<Runnable>();

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
                        addReasoner(reasoner);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                });

        Arrays.stream(runnerClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Finish.class))
                .filter(m->m.getAnnotation(Finish.class).enabled())
                .sorted(Comparator.comparingInt(m->m.getAnnotation(Finish.class).order()))
                .forEach(m-> {
                    try {
                        Object collector = m.invoke(this);
                        if(collector instanceof Runnable) {
                            Runnable finisher = (Runnable) collector;
                            addFinisher(finisher);
                        } else {
                            logger.error("Collector not recognized: " + collector.getClass().getName());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                });

        this.addFinishersToShutdownHook();

        this.start();
        this.finish();
    }

    private void addFinishersToShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                logger.info("Received shutdown signal:  {}.");
                if(!finished) {
                    finish();
                }
            }
        });
    }


    protected final boolean hasEnv() {
        return env != null;
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
        logger.info("Creating GUI with {} many tabs.", tabs.length);
        Platform.runLater(() -> new AlgorithmVisualizationWindow(eventRecorder.getHistory(), main, tabs).run());
    }

	private void addReasoner(SPLReasoner reasoner) {
        reasoners.add(reasoner);
    }


    protected void addFinisher(Runnable finisher) {
        finishers.add(finisher);
    }

    public void start() {
        logger.info("Starting spl benchmark.");
        reasoners.stream().forEach(reasoner -> {
            BenchmarkEnvironment billedEnv =  new Bookkeeper.Bill(env, env.bill(reasoner.name()));
            SPLReasonerAlgorithm alg = reasoner.algorithm(billedEnv);
            alg.registerListener(eventRecorder);
            try {
                logger.info("Starting reasoner {}.", reasoner.name());
                alg.call();
                logger.info("Reasoner {} finished.", reasoner.name());
            } catch (Exception ex) {
                logger.error("Execution Error in algorithm {}: ", reasoner.name(), ex);
            }
        });
//        ExecutorService evaluatorService
        logger.info("Benchmark finished. Starting evaluations.");
    }

    boolean finished = false;

    public void finish() {
        logger.info("Performing finishers.");
        finished = true;
        for(Runnable finisher : finishers) {
            try {
                logger.info("Running finisher: " + finisher.toString());
                CompletableFuture<Void> finishFuture = CompletableFuture.runAsync(finisher);
                finishFuture.get(10, TimeUnit.MINUTES);
            } catch(Exception ex) {
                logger.error("Error while running finisher: " , ex);
            }
        }
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

    public BenchmarkAgent agent() {
        return agent;
    }

    public BenchmarkEnvironment env() {
        return env;
    }


}